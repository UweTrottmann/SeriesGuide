/*
 * Copyright 2014 Uwe Trottmann
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

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.AsyncTask;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import timber.log.Timber;

/**
 * Retrieves and stores images from/to disk, uses a low-memory auto-evicting LRU cache to speed up
 * responses.
 *
 * Built with code from http://code.google.com/p/iogallery of Google I/O 2012 by Jeff Sharkey.
 */
public class ImageProvider {

    private static final CompressFormat IMAGE_FORMAT = CompressFormat.JPEG;

    private static final int IMAGE_QUALITY = 98;

    private static final String THUMB_SUFFIX = "thumb";

    private static final float THUMBNAIL_WIDTH_DIP = 68.0f;

    private static final float THUMBNAIL_WIDTH_LARGE = 102.0f;

    private static final float THUMBNAIL_HEIGHT_DIP = 100.0f;

    private static final float THUMBNAIL_HEIGHT_LARGE = 150.0f;

    private static ImageProvider _instance;

    private ImageCache mCache;

    private Context mContext;

    private OnSharedPreferenceChangeListener mListener;

    private float mScale;

    @TargetApi(14)
    public ImageProvider(Context context) {
        mContext = context.getApplicationContext();
        mScale = context.getResources().getDisplayMetrics().density;

        // Pick cache size based on memory class of device
        final ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        final int maxCacheSizeBytes = (am.getMemoryClass() * 1024 * 1024) / 8;
        mCache = new ImageCache(maxCacheSizeBytes);

        Timber.d("Init cache with size: " + maxCacheSizeBytes + " bytes, cache directory: "
                + mContext.getExternalFilesDir(null));

        // listen to trim or low memory callbacks so we can shrink our memory
        // footprint
        if (AndroidUtils.isICSOrHigher()) {
            context.registerComponentCallbacks(new ComponentCallbacks2() {

                @Override
                public void onLowMemory() {
                }

                @Override
                public void onConfigurationChanged(Configuration newConfig) {
                }

                @Override
                public void onTrimMemory(int level) {
                    Timber.d("onTrimMemory() with level=" + level);

                    // Memory we can release here will help overall system
                    // performance, and make us a smaller target as the system
                    // looks for memory

                    if (level >= TRIM_MEMORY_MODERATE) { // 60
                        // Nearing middle of list of cached background apps;
                        // evict our entire thumbnail cache
                        Timber.d("evicting entire thumbnail cache");
                        mCache.evictAll();
                    } else if (level >= TRIM_MEMORY_BACKGROUND) { // 40
                        // Entering list of cached background apps; evict oldest
                        // half of our thumbnail cache
                        Timber.d("evicting oldest half of thumbnail cache");
                        mCache.trimToSize(mCache.size() / 2);
                    }
                }
            });
        }

        // listen if user toggles .nomedia file pref
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mListener = new OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equalsIgnoreCase(SeriesGuidePreferences.KEY_HIDEIMAGES)) {
                    updateNoMediaFile(sharedPreferences);
                    if (prefs.getBoolean(SeriesGuidePreferences.KEY_HIDEIMAGES, true)) {
                        Utils.trackCustomEvent(mContext, "Settings", "Hide images", "Enabled");
                    } else {
                        Utils.trackCustomEvent(mContext, "Settings", "Hide images", "Disabled");
                    }
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(mListener);
    }

    public static synchronized ImageProvider getInstance(Context ctx) {
        if (_instance == null) {
            _instance = new ImageProvider(ctx.getApplicationContext());
        }
        return _instance;
    }

    /**
     * Calls {@link #loadImage(android.widget.ImageView, String, android.widget.ImageView.ScaleType,
     * boolean)} with
     * <ul>
     *     <li> <code>scaleType</code> set to {@link android.widget.ImageView.ScaleType#CENTER_CROP}
     *     <li> <code>loadThumbnail</code> set to <code>false</code>.
     * </ul>
     */
    public void loadPoster(ImageView imageView, String imagePath) {
        loadImage(imageView, imagePath, ScaleType.CENTER_CROP, false);
    }

    /**
     * Calls {@link #loadImage(android.widget.ImageView, String, android.widget.ImageView.ScaleType,
     * boolean)} with
     * <ul>
     *     <li> <code>scaleType</code> set to {@link android.widget.ImageView.ScaleType#CENTER_CROP}
     *     <li> <code>loadThumbnail</code> set to <code>true</code>.
     * </ul>
     */
    public void loadPosterThumb(ImageView imageView, String imagePath) {
        loadImage(imageView, imagePath, ScaleType.CENTER_CROP, true);
    }

    /**
     * Sets the image bitmap, either directly from cache or loads it asynchronously from external
     * storage.
     *
     * <p> If the image path is empty will clear the image view. If the image could not be
     * retrieved, will show a placeholder.
     *
     * @param scaleType     The {@link android.widget.ImageView.ScaleType} to use for the image.
     *                      The
     *                      placeholder will always use {@link ScaleType#CENTER_INSIDE}.
     * @param loadThumbnail Will load a down-sized version of the image requested.
     */
    public void loadImage(ImageView imageView, String imagePath, ScaleType scaleType,
            boolean loadThumbnail) {
        if (TextUtils.isEmpty(imagePath)) {
            // there is no image available
            setNothingToImageView(imageView);
            return;
        }

        // cancel any pending loader task, since this view is now bound to a new image
        final ImageLoaderTask oldTask = (ImageLoaderTask) imageView.getTag();
        if (oldTask != null) {
            oldTask.cancel(false);
        }

        // looking for a smaller size?
        if (loadThumbnail) {
            imagePath += THUMB_SUFFIX;
        }

        // check the cache for this image
        final Bitmap cachedResult = mCache.get(imagePath);
        if (cachedResult != null) {
            // found it!
            setImageToImageView(imageView, cachedResult, scaleType);
            return;
        }

        // cache miss, so we need to load from disk
        final ImageLoaderTask task = new ImageLoaderTask(imageView, scaleType);
        imageView.setTag(task);
        
        /*
         * NOTE: This uses a custom version of AsyncTask that has been
         * pulled from the framework and slightly modified. Refer to the
         * docs at the top of the class for more info on what was changed.
         */
        task.executeOnExecutor(AsyncTask.DUAL_THREAD_EXECUTOR, imagePath);
    }

    /**
     * This will synchronously (!) access external storage to get the image if it is not cached
     * already. Make sure to run this on a background thread or use {@code loadPoster} instead.
     */
    public Bitmap getImage(String imagePath, boolean loadThumbnail) {
        if (TextUtils.isEmpty(imagePath)) {
            // There is no poster for this show
            return null;
        }

        if (loadThumbnail) {
            // look for the thumbnail of this poster
            imagePath += THUMB_SUFFIX;
        }

        // Check the cache for this image
        Bitmap result = mCache.get(imagePath);
        if (result != null) {
            return result;
        }

        result = getImageFromExternalStorage(imagePath);
        return result;
    }

    private Bitmap getImageFromExternalStorage(final String imagePath) {
        // try to get image from disk
        final File imageFile = getImageFile(imagePath);
        if (imageFile != null && imageFile.exists()) {
            // disk cache hit
            final Bitmap result = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            if (result == null) {
                // treat decoding errors as a cache miss
                Timber.d("getImageFromExternalStorage: decoding bitmap failed " + imageFile);
                return null;
            }

            mCache.put(imagePath, result);

            return result;
        }

        Timber.d("getImageFromExternalStorage: image not on disk " + imageFile);
        return null;
    }

    public void storeImage(String imagePath, Bitmap bitmap, boolean createThumbnail) {
        if (!AndroidUtils.isExtStorageAvailable()) {
            Timber.e("storeImage: external storage not available, storing failed");
            return;
        }
        // make sure directories exist
        createDirectories();

        final File imageFile = getImageFile(imagePath);
        if (imageFile == null) {
            return;
        }
        try {
            imageFile.createNewFile();
            FileOutputStream ostream = new FileOutputStream(imageFile);
            try {
                bitmap.compress(IMAGE_FORMAT, IMAGE_QUALITY, ostream);
            } finally {
                ostream.close();
            }
        } catch (IOException e) {
            Timber.e(e, "Saving image to disk failed " + imageFile);
        }

        // create a thumbnail, too, if requested
        if (createThumbnail) {
            int scaledWidth;
            int scaledHeight;
            // create bigger thumbnails on large screen devices
            if (DisplaySettings.isVeryLargeScreen(mContext)) {
                scaledWidth = (int) (THUMBNAIL_WIDTH_LARGE * mScale + 0.5f);
                scaledHeight = (int) (THUMBNAIL_HEIGHT_LARGE * mScale + 0.5f);
            } else {
                scaledWidth = (int) (THUMBNAIL_WIDTH_DIP * mScale + 0.5f);
                scaledHeight = (int) (THUMBNAIL_HEIGHT_DIP * mScale + 0.5f);
            }
            storeImage(imagePath + THUMB_SUFFIX,
                    Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true), false);
        }
    }

    /**
     * Remove the given image and a potentially existing thumbnail from the external storage cache.
     */
    public void removeImage(String imagePath) {
        File image = getImageFile(imagePath);
        File thumbnail = getImageFile(imagePath + THUMB_SUFFIX);
        try {
            if (image != null) {
                image.delete();
            }
            if (thumbnail != null) {
                thumbnail.delete();
            }
        } catch (SecurityException e) {
            Timber.e(e, "removeImage: failed " + image + " " + thumbnail);
        }
    }

    public boolean exists(String imagePath) {
        File file = getImageFile(imagePath);
        return file != null && file.exists();
    }

    /**
     * Returns a path for the given image on external storage or null if {@link
     * Context#getExternalFilesDir(String)} returns null (e.g. external storage is currently not
     * available).
     */
    private File getImageFile(String imagePath) {
        File path = mContext.getExternalFilesDir(null);
        if (path != null) {
            return new File(path,
                    Integer.toHexString(imagePath.hashCode()) + "." + IMAGE_FORMAT.name());
        }
        return null;
    }

    private void createDirectories() {
        File path = mContext.getExternalFilesDir(null);
        if (path != null) {
            path.mkdirs();
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        updateNoMediaFile(prefs);
    }

    private void updateNoMediaFile(SharedPreferences prefs) {
        File path = mContext.getExternalFilesDir(null);
        if (path == null) {
            Timber.w("Could not create .nomedia file, external storage not available");
            return;
        }

        final File noMediaFile = new File(path, ".nomedia");
        if (prefs.getBoolean(SeriesGuidePreferences.KEY_HIDEIMAGES, true)) {
            try {
                boolean created = noMediaFile.createNewFile();
                Timber.d("Created .nomedia file: " + created);
            } catch (IOException e) {
                Timber.w(e, "Could not create .nomedia file");
            }
        } else {
            noMediaFile.delete();
            Timber.d("Deleted .nomedia file");
        }
    }

    /**
     * Clear in memory cache.
     */
    public void clearCache() {
        Timber.d("evicting entire thumbnail cache");
        mCache.evictAll();
    }

    /**
     * Clear all files in cache directory.
     */
    public void clearExternalStorageCache() {
        File path = mContext.getExternalFilesDir(null);
        if (path == null) {
            Timber.w("Could not clear cache, external storage not available");
            return;
        }

        final File[] files = path.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    private void setPlaceholderToImageView(ImageView imageView) {
        imageView.setScaleType(ScaleType.CENTER_INSIDE);
        imageView.setImageResource(R.drawable.ic_image_missing);
    }

    private void setImageToImageView(ImageView imageView, Bitmap bitmap, ScaleType scaleType) {
        imageView.setScaleType(scaleType);
        imageView.setImageBitmap(bitmap);
    }

    private void setNothingToImageView(ImageView imageView) {
        imageView.setImageBitmap(null);
    }

    public class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {

        private ImageView mImageView;
        private final ScaleType mScaleType;

        public ImageLoaderTask(ImageView imageView, ScaleType scaleType) {
            mImageView = imageView;
            mScaleType = scaleType;
        }

        @Override
        protected void onPreExecute() {
            mImageView.setTag(this);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            final String imagePath = params[0];
            return getImageFromExternalStorage(imagePath);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (mImageView.getTag() == this) {
                if (result != null) {
                    setImageToImageView(mImageView, result, mScaleType);
                } else {
                    setPlaceholderToImageView(mImageView);
                }
                mImageView.setTag(null);
            }
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
