/*
 * Copyright 2012 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.R;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;

/**
 * Built with code from http://code.google.com/p/iogallery of Google I/O 2012 by
 * Jeff Sharkey.
 * 
 * @author Uwe Trottmann
 */
public class ImageProvider {

    private static final CompressFormat IMAGE_FORMAT = CompressFormat.JPEG;

    private static final String THUMB_SUFFIX = "thumb";

    protected static final String TAG = "ImageProvider";

    private static ImageProvider _instance;

    private ImageCache mCache;

    private String mCacheDir;

    @TargetApi(14)
    public ImageProvider(Context context) {
        // Pick cache size based on memory class of device
        final ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        final int memoryClassBytes = am.getMemoryClass() * 1024 * 1024;
        mCache = new ImageCache(memoryClassBytes / 8);

        // determine cache path on external storage
        // TODO enable once implemented storing with ImageProvider
        // if (Utils.isFroyoOrHigher()) {
        // mCacheDir = null;
        // File cacheDir = context.getExternalCacheDir();
        // if (cacheDir != null) {
        // mCacheDir = cacheDir.getAbsolutePath();
        // }
        // } else {
        // mCacheDir =
        // Environment.getExternalStorageDirectory().getAbsolutePath()
        // + "/Android/data/" + context.getPackageName() + "/cache";
        // }

        mCacheDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/"
                + context.getPackageName() + "/files";

        // listen to trim or low memory callbacks so we can shrink our memory
        // footprint
        if (Utils.isICSOrHigher()) {
            context.registerComponentCallbacks(new ComponentCallbacks2() {

                @Override
                public void onLowMemory() {
                }

                @Override
                public void onConfigurationChanged(Configuration newConfig) {
                }

                @Override
                public void onTrimMemory(int level) {
                    Log.v(TAG, "onTrimMemory() with level=" + level);

                    // Memory we can release here will help overall system
                    // performance, and make us a smaller target as the system
                    // looks for memory

                    if (level >= TRIM_MEMORY_MODERATE) { // 60
                        // Nearing middle of list of cached background apps;
                        // evict our entire thumbnail cache
                        Log.v(TAG, "evicting entire thumbnail cache");
                        mCache.evictAll();

                    } else if (level >= TRIM_MEMORY_BACKGROUND) { // 40
                        // Entering list of cached background apps; evict oldest
                        // half of our thumbnail cache
                        Log.v(TAG, "evicting oldest half of thumbnail cache");
                        mCache.trimToSize(mCache.size() / 2);
                    }
                }
            });
        }
    }

    public static synchronized ImageProvider getInstance(Context ctx) {
        if (_instance == null) {
            _instance = new ImageProvider(ctx.getApplicationContext());
        }
        return _instance;
    }

    public void loadPosterThumb(ImageView imageView, String imagePath) {
        loadPoster(imageView, imagePath, true);
    }

    /**
     * Set the poster bitmap, either directly from cache or load it async from
     * external storage.
     * 
     * @param imageView
     * @param imagePath
     * @param loadThumbnail
     */
    public void loadPoster(ImageView imageView, String imagePath, boolean loadThumbnail) {
        if (TextUtils.isEmpty(imagePath)) {
            // There is no poster for this show, display a generic one
            imageView.setImageResource(R.drawable.show_generic);
            return;
        }

        // Cancel any pending thumbnail task, since this view is now bound
        // to new thumbnail
        final ImageLoaderTask oldTask = (ImageLoaderTask) imageView.getTag();
        if (oldTask != null) {
            oldTask.cancel(false);
        }

        if (loadThumbnail) {
            // look for the thumbnail of this poster
            imagePath += THUMB_SUFFIX;
        }

        // Check the cache for this image
        final Bitmap cachedResult = mCache.get(imagePath);
        if (cachedResult != null) {
            imageView.setImageBitmap(cachedResult);
            return;
        }

        // If we arrived here, either cache is disabled or cache miss, so we
        // need to kick task to load manually
        final ImageLoaderTask task = new ImageLoaderTask(imageView);
        imageView.setImageBitmap(null);
        imageView.setTag(task);
        Utils.executeAsyncTask(task, imagePath);
    }

    public class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {

        private ImageView mImageView;

        public ImageLoaderTask(ImageView imageView) {
            mImageView = imageView;
        }

        @Override
        protected void onPreExecute() {
            mImageView.setTag(this);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            final String imagePath = params[0];

            // try to get image from disk
            final File imageFile = getImageFile(imagePath);
            if (imageFile.exists() && Utils.isExtStorageAvailable()) {
                // disk cache hit
                final Bitmap result = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                if (result == null) {
                    // treat decoding errors as a cache miss
                    return null;
                }

                mCache.put(imagePath, result);

                return result;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (mImageView.getTag() == this) {
                if (result != null) {
                    mImageView.setImageBitmap(result);
                } else {
                    mImageView.setImageResource(R.drawable.show_generic);
                }
                mImageView.setTag(null);
            }
        }

        private File getImageFile(String imagePath) {
            String fileName = Integer.toHexString(imagePath.hashCode()) + "." + IMAGE_FORMAT.name();
            return new File(mCacheDir + "/" + fileName);
        }

    }

    public static class ImageCache extends LruCache<String, Bitmap> {

        public ImageCache(int maxSizeBytes) {
            super(maxSizeBytes);
        }

        @TargetApi(12)
        @Override
        protected int sizeOf(String key, Bitmap value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                return value.getByteCount();
            } else {
                return value.getRowBytes() * value.getHeight();
            }
        }

    }
}
