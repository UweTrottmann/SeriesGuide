/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.AsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * From http://code.google.com/p/android-imagedownloader. This helper class
 * download images from the Internet and binds those with the provided
 * ImageView. A local cache of downloaded images is maintained internally to
 * improve performance.
 */
public class ImageDownloader {
    private static final String LOG_TAG = "ImageDownloader";

    private static ImageDownloader _instance;

    private String mDiskCacheDir;

    private ImageDownloader(Context context) {
        // TODO replace with getExternalFilesDir (but can be null!) once we are
        // min API level 8
        mDiskCacheDir = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/Android/data/" + context.getPackageName() + "/files";
    }

    public static synchronized ImageDownloader getInstance(Context context) {
        if (_instance == null) {
            _instance = new ImageDownloader(context);
        }
        return _instance;
    }

    /**
     * Download the specified image from the Internet and binds it to the
     * provided ImageView. The binding is immediate if the image is found in the
     * cache and will be done asynchronously otherwise. A null bitmap will be
     * associated to the ImageView if an error occurs.
     * 
     * @param mUrl The URL of the image to download.
     * @param imageView The ImageView to bind the downloaded image to.
     */
    public void download(String url, ImageView imageView) {
        download(url, imageView, true);
    }

    /**
     * Download the specified image from the Internet and binds it to the
     * provided ImageView. The binding is immediate if the image is found in the
     * cache and will be done asynchronously otherwise. A null bitmap will be
     * associated to the ImageView if an error occurs.
     * 
     * @param mUrl The URL of the image to download.
     * @param imageView The ImageView to bind the downloaded image to.
     * @param isDiskCaching Whether to cache the image to disk or just memory.
     */
    public void download(String url, ImageView imageView, boolean isDiskCaching) {
        resetPurgeTimer();
        Bitmap bitmap = getBitmapFromCache(url);

        if (bitmap == null) {
            forceDownload(url, imageView, isDiskCaching);
        } else {
            cancelPotentialDownload(url, imageView);
            imageView.setImageBitmap(bitmap);
        }
    }

    /**
     * Same as download but the image is always downloaded and the cache is not
     * used. Kept private at the moment as its interest is not clear.
     */
    private void forceDownload(String url, ImageView imageView, boolean isDiskCaching) {
        // State sanity: mUrl is guaranteed to never be null in
        // DownloadedDrawable and cache keys.
        if (url == null) {
            imageView.setImageDrawable(null);
            return;
        }

        if (cancelPotentialDownload(url, imageView)) {
            BitmapDownloaderTask task = new BitmapDownloaderTask(imageView, isDiskCaching);
            DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
            imageView.setImageDrawable(downloadedDrawable);

            /*
             * NOTE: This uses a custom version of AsyncTask that has been
             * pulled from the framework and slightly modified. Refer to the
             * docs at the top of the class for more info on what was changed.
             */
            task.executeOnExecutor(AsyncTask.DUAL_THREAD_EXECUTOR, url);
        }
    }

    /**
     * Returns true if the current download has been canceled or if there was no
     * download in progress on this image view. Returns false if the download in
     * progress deals with the same mUrl. The download is not stopped in that
     * case.
     */
    private static boolean cancelPotentialDownload(String url, ImageView imageView) {
        BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);

        if (bitmapDownloaderTask != null) {
            String bitmapUrl = bitmapDownloaderTask.mUrl;
            if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
                bitmapDownloaderTask.cancel(true);
            } else {
                // The same URL is already being downloaded.
                return false;
            }
        }
        return true;
    }

    /**
     * @param imageView Any imageView
     * @return Retrieve the currently active download task (if any) associated
     *         with this imageView. null if there is no such task.
     */
    private static BitmapDownloaderTask getBitmapDownloaderTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadedDrawable) {
                DownloadedDrawable downloadedDrawable = (DownloadedDrawable) drawable;
                return downloadedDrawable.getBitmapDownloaderTask();
            }
        }
        return null;
    }

    private Bitmap getBitmapFromDisk(String urlString, File imageFile) {
        if (AndroidUtils.isExtStorageAvailable()) {
            if (imageFile.exists()) {
                // disk cache hit
                final Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                if (bitmap != null) {
                    return bitmap;
                }
            }
        }
        return null;
    }

    private Bitmap downloadBitmap(String urlString, boolean isDiskCaching, File imageFile) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        Bitmap bitmap = null;
        try {
            connection = AndroidUtils.buildHttpUrlConnection(urlString);
            inputStream = connection.getInputStream();
            // return BitmapFactory.decodeStream(inputStream);
            // Bug on slow connections, fixed in future release.
            // Bitmap bitmap = BitmapFactory.decodeStream(new
            // FlushedInputStream(inputStream));

            // write directly to disk
            if (isDiskCaching && AndroidUtils.isExtStorageAvailable()) {
                FileOutputStream outputstream = new FileOutputStream(imageFile);
                try {
                    AndroidUtils.copy(new FlushedInputStream(inputStream), outputstream);
                } finally {
                    outputstream.close();
                }
                bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            } else {
                // if we have no external storage, decode directly
                bitmap = BitmapFactory.decodeStream(new FlushedInputStream(inputStream));
            }

            /*
             * TODO look if we can return the bitmap first, then save in the
             * background
             */
            // if (Utils.isExtStorageAvailable()) {
            // // write the bitmap to the disk cache
            // FileOutputStream os = new FileOutputStream(imagefile);
            //
            // @SuppressWarnings("unused")
            // boolean isreconstructable =
            // bitmap.compress(CompressFormat.JPEG, 90, os);
            // os.close();
            // }
        } catch (IOException e) {
            Log.w(LOG_TAG, "I/O error while retrieving bitmap from " + urlString, e);
        } catch (Exception e) {
            Log.w(LOG_TAG, "Error while retrieving bitmap from " + urlString, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
                connection = null;
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                    // Nothing to do
                }
            }
        }
        return bitmap;
    }

    /*
     * An InputStream that skips the exact number of bytes provided, unless it
     * reaches EOF.
     */
    static class FlushedInputStream extends FilterInputStream {
        public FlushedInputStream(InputStream inputStream) {
            super(inputStream);
        }

        @Override
        public long skip(long n) throws IOException {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n) {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L) {
                    int b = read();
                    if (b < 0) {
                        break; // we reached EOF
                    } else {
                        bytesSkipped = 1; // we read one byte
                    }
                }
                totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }
    }

    /**
     * The actual AsyncTask that will asynchronously download the image.
     */
    class BitmapDownloaderTask extends AsyncTask<String, Void, Bitmap> {
        private String mUrl;

        private final WeakReference<ImageView> imageViewReference;

        private final boolean mIsDiskCaching;

        public BitmapDownloaderTask(ImageView imageView, boolean isDiskCaching) {
            imageViewReference = new WeakReference<ImageView>(imageView);
            mIsDiskCaching = isDiskCaching;
        }

        /**
         * Actual download method.
         */
        @Override
        protected Bitmap doInBackground(String... params) {
            mUrl = params[0];

            final String fileName = Integer.toHexString(mUrl.hashCode()) + "."
                    + CompressFormat.JPEG.name();
            File imageFile = new File(mDiskCacheDir + "/" + fileName);

            // try to get bitmap from disk cache first
            if (mIsDiskCaching) {
                Bitmap bitmap = getBitmapFromDisk(mUrl, imageFile);
                if (bitmap != null) {
                    return bitmap;
                }
            }

            if (isCancelled()) {
                return null;
            }

            // if loading from disk fails, download it
            return downloadBitmap(mUrl, mIsDiskCaching, imageFile);
        }

        /**
         * Once the image is downloaded, associates it to the imageView
         */
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            addBitmapToCache(mUrl, bitmap);

            if (imageViewReference != null) {
                ImageView imageView = imageViewReference.get();
                BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);
                // Change bitmap only if this process is still associated with
                // it
                // Or if we don't use any bitmap to task association
                // (NO_DOWNLOADED_DRAWABLE mode)
                if (this == bitmapDownloaderTask) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    /**
     * A fake Drawable that will be attached to the imageView while the download
     * is in progress.
     * <p>
     * Contains a reference to the actual download task, so that a download task
     * can be stopped if a new binding is required, and makes sure that only the
     * last started download process can bind its result, independently of the
     * download finish order.
     * </p>
     */
    static class DownloadedDrawable extends ColorDrawable {
        private final WeakReference<BitmapDownloaderTask> bitmapDownloaderTaskReference;

        public DownloadedDrawable(BitmapDownloaderTask bitmapDownloaderTask) {
            super(Color.TRANSPARENT);
            bitmapDownloaderTaskReference = new WeakReference<BitmapDownloaderTask>(
                    bitmapDownloaderTask);
        }

        public BitmapDownloaderTask getBitmapDownloaderTask() {
            return bitmapDownloaderTaskReference.get();
        }
    }

    /*
     * Cache-related fields and methods. We use a hard and a soft cache. A soft
     * reference cache is too aggressively cleared by the Garbage Collector.
     */

    private static final int HARD_CACHE_CAPACITY = 10;

    private static final int DELAY_BEFORE_PURGE = 10 * 1000; // in milliseconds

    // Hard cache, with a fixed maximum capacity and a life duration
    @SuppressWarnings("serial")
    private final HashMap<String, Bitmap> sHardBitmapCache = new LinkedHashMap<String, Bitmap>(
            HARD_CACHE_CAPACITY / 2, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(LinkedHashMap.Entry<String, Bitmap> eldest) {
            if (size() > HARD_CACHE_CAPACITY) {
                // Entries push-out of hard reference cache are transferred to
                // soft reference cache
                sSoftBitmapCache.put(eldest.getKey(), new SoftReference<Bitmap>(eldest.getValue()));
                return true;
            } else
                return false;
        }
    };

    // Soft cache for bitmaps kicked out of hard cache
    private final static ConcurrentHashMap<String, SoftReference<Bitmap>> sSoftBitmapCache = new ConcurrentHashMap<String, SoftReference<Bitmap>>(
            HARD_CACHE_CAPACITY / 2);

    private final Handler purgeHandler = new Handler();

    private final Runnable purger = new Runnable() {
        public void run() {
            clearCache();
        }
    };

    /**
     * Adds this bitmap to the cache.
     * 
     * @param bitmap The newly downloaded bitmap.
     */
    private void addBitmapToCache(String url, Bitmap bitmap) {
        if (bitmap != null) {
            synchronized (sHardBitmapCache) {
                sHardBitmapCache.put(url, bitmap);
            }
        }
    }

    /**
     * @param mUrl The URL of the image that will be retrieved from the cache.
     * @return The cached bitmap or null if it was not found.
     */
    private Bitmap getBitmapFromCache(String url) {
        // First try the hard reference cache
        synchronized (sHardBitmapCache) {
            final Bitmap bitmap = sHardBitmapCache.get(url);
            if (bitmap != null) {
                // Bitmap found in hard cache
                // Move element to first position, so that it is removed last
                sHardBitmapCache.remove(url);
                sHardBitmapCache.put(url, bitmap);
                return bitmap;
            }
        }

        // Then try the soft reference cache
        SoftReference<Bitmap> bitmapReference = sSoftBitmapCache.get(url);
        if (bitmapReference != null) {
            final Bitmap bitmap = bitmapReference.get();
            if (bitmap != null) {
                // Bitmap found in soft cache
                return bitmap;
            } else {
                // Soft reference has been Garbage Collected
                sSoftBitmapCache.remove(url);
            }
        }

        return null;
    }

    /**
     * Clears the image cache used internally to improve performance. Note that
     * for memory efficiency reasons, the cache will automatically be cleared
     * after a certain inactivity delay.
     */
    public void clearCache() {
        sHardBitmapCache.clear();
        sSoftBitmapCache.clear();
    }

    /**
     * Allow a new delay before the automatic cache clear is done.
     */
    private void resetPurgeTimer() {
        purgeHandler.removeCallbacks(purger);
        purgeHandler.postDelayed(purger, DELAY_BEFORE_PURGE);
    }
}
