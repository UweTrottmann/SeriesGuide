/* Copyright (c) 2009 Matthias Käppler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Modified for SeriesGuide - Uwe Trottmann 2011
 */

package com.battlelancer.thetvdbapi;

import com.battlelancer.seriesguide.SeriesGuideData;
import com.battlelancer.seriesguide.util.AnalyticsUtils;
import com.battlelancer.seriesguide.util.UIUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * A simple 2-level cache for bitmap images consisting of a small and fast
 * in-memory cache (1st level cache) and a slower but bigger disk cache (2nd
 * level cache). For second level caching, the application's cache directory
 * will be used. Please note that Android may at any point decide to wipe that
 * directory.
 * </p>
 * <p>
 * When pulling from the cache, it will first attempt to load the image from
 * memory. If that fails, it will try to load it from disk. If that succeeds,
 * the image will be put in the 1st level cache and returned. Otherwise it's a
 * cache miss, and the caller is responsible for loading the image from
 * elsewhere (probably the Internet).
 * </p>
 * <p>
 * Pushes to the cache are always write-through (i.e., the image will be stored
 * both on disk and in memory).
 * </p>
 * 
 * @author Matthias Kaeppler
 */

/**
 * Modified to use external storage directory as second level cache.
 * 
 * @author Uwe Trottmann
 */

public class ImageCache {

    private static final String THUMB_SUFFIX = "thumb";

    private int mCachedImageQuality = 98;

    private String mSecondLevelCacheDir;

    private final Map<String, Bitmap> mCache;

    private CompressFormat mCompressedImageFormat = CompressFormat.JPEG;

    private final float mScale;

    private static final float THUMBNAIL_WIDTH_DIP = 68.0f;

    private static final float THUMBNAIL_HEIGHT_DIP = 100.0f;

    private Context mCtx;

    private OnSharedPreferenceChangeListener listener;

    private static ImageCache _instance;

    private static final String TAG = "ImageCache";

    private ImageCache(Context ctx) {
        this.mCtx = ctx;
        this.mCache = new HashMap<String, Bitmap>(50);
        final String packageName = ctx.getApplicationInfo().packageName;
        this.mSecondLevelCacheDir = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/Android/data/" + packageName + "/files";
        mScale = mCtx.getResources().getDisplayMetrics().density;
        createDirectories();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mCtx);
        listener = new OnSharedPreferenceChangeListener() {

            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equalsIgnoreCase(SeriesGuideData.KEY_HIDEIMAGES)) {

                    // remove or add .nomedia file
                    if (sharedPreferences.getBoolean(SeriesGuideData.KEY_HIDEIMAGES, true)) {
                        // track event
                        AnalyticsUtils.getInstance(mCtx).trackEvent("Settings", "Hide images",
                                "Enable", 0);

                        try {
                            new File(mSecondLevelCacheDir + "/.nomedia").createNewFile();
                        } catch (IOException e) {
                            Log.w(TAG, "Could not create .nomedia file");
                        }
                        Log.d(TAG, "Creating .nomedia file");
                    } else {
                        // track event
                        AnalyticsUtils.getInstance(mCtx).trackEvent("Settings", "Hide images",
                                "Disable", 0);

                        new File(mSecondLevelCacheDir + "/.nomedia").delete();
                        Log.d(TAG, "Deleting .nomedia file");
                    }
                }
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    public static synchronized ImageCache getInstance(Context ctx) {
        if (_instance == null) {
            _instance = new ImageCache(ctx);
        }
        return _instance;
    }

    private void createDirectories() {
        new File(mSecondLevelCacheDir).mkdirs();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mCtx);
        if (sharedPreferences.getBoolean(SeriesGuideData.KEY_HIDEIMAGES, true)) {
            try {
                new File(mSecondLevelCacheDir + "/.nomedia").createNewFile();
            } catch (IOException e) {
                Log.w(TAG, "Could not create .nomedia file");
            }
        }
    }

    /**
     * The image format that should be used when caching images on disk. The
     * default value is {@link CompressFormat#PNG}. Note that when switching to
     * a format like JPEG, you will lose any transparency that was part of the
     * image.
     * 
     * @param compressedImageFormat the {@link CompressFormat}
     */
    public void setCompressedImageFormat(CompressFormat compressedImageFormat) {
        this.mCompressedImageFormat = compressedImageFormat;
    }

    public CompressFormat getCompressedImageFormat() {
        return mCompressedImageFormat;
    }

    /**
     * @param cachedImageQuality the quality of images being compressed and
     *            written to disk (2nd level cache) as a number in [0..100]
     */
    public void setCachedImageQuality(int cachedImageQuality) {
        this.mCachedImageQuality = cachedImageQuality;
    }

    public int getCachedImageQuality() {
        return mCachedImageQuality;
    }

    public Bitmap get(Object key) {
        return getIfNotBusy(key, false);
    }

    public synchronized Bitmap getIfNotBusy(Object key, boolean isBusy) {
        String imageUrl = (String) key;
        Bitmap bitmapRef = mCache.get(imageUrl);

        if (bitmapRef != null) {
            // 1st level cache hit (memory)
            return bitmapRef;
        }

        // if the caller is busy doing other work, don't load from disk
        if (isBusy) {
            return null;
        }

        File imageFile = getImageFile(imageUrl);
        if (imageFile.exists()) {
            // 2nd level cache hit (disk)
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            if (bitmap == null) {
                // treat decoding errors as a cache miss
                return null;
            }

            mCache.put(imageUrl, bitmap);
            return bitmap;
        }

        // cache miss
        return null;
    }

    /**
     * Fetches the thumbnail for an image, creates one if it does not exist
     * already.
     * 
     * @param key
     * @return Bitmap containing the thumb version of this image
     */
    public synchronized Bitmap getThumb(Object key, boolean isBusy) {
        String imageUrl = (String) key;
        String imageThumbUrl = imageUrl + THUMB_SUFFIX;

        // see if thumbnail already exists
        Bitmap thumbnail = getIfNotBusy(imageThumbUrl, isBusy);
        if (thumbnail != null) {
            return thumbnail;
        }

        // if the caller is busy doing other work, don't load from disk
        if (isBusy) {
            return null;
        }

        return getThumbHelper(imageUrl);
    }

    public synchronized Bitmap getThumbHelper(String imageUrl) {
        String imageThumbUrl = imageUrl + THUMB_SUFFIX;
        // create a thumbnail if possible
        Bitmap original = get(imageUrl);
        if (original != null) {
            // calculate the width and height corresponding to screen density
            int scaledWidth = (int) (THUMBNAIL_WIDTH_DIP * mScale + 0.5f);
            int scaledHeight = (int) (THUMBNAIL_HEIGHT_DIP * mScale + 0.5f);
            return put(imageThumbUrl,
                    Bitmap.createScaledBitmap(original, scaledWidth, scaledHeight, true));
        } else {
            return null;
        }
    }

    public Bitmap put(String imageUrl, Bitmap bitmap) {
        if (UIUtils.isExtStorageAvailable()) {
            // make sure directories exist
            createDirectories();
            File imageFile = getImageFile(imageUrl);

            try {
                imageFile.createNewFile();

                FileOutputStream ostream = new FileOutputStream(imageFile);
                bitmap.compress(mCompressedImageFormat, mCachedImageQuality, ostream);
                ostream.close();

                mCache.put(imageUrl, bitmap);

                return bitmap;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // if all failes
        return null;
    }

    /**
     * Remove the given image and a potentially existing thumbnail from the
     * external storage cache.
     * 
     * @param imageUrl
     * @return
     */
    public void removeFromDisk(String imageUrl) {
        try {
            getImageFile(imageUrl).delete();
            getImageFile(imageUrl + THUMB_SUFFIX).delete();
        } catch (SecurityException se) {
            // we don't care
        }
    }

    public void clear() {
        mCache.clear();
    }

    public void clearExternal() {
        File directory = new File(mSecondLevelCacheDir);

        // Get all files in directory
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    private File getImageFile(String imageUrl) {
        String fileName = Integer.toHexString(imageUrl.hashCode()) + "."
                + mCompressedImageFormat.name();
        return new File(mSecondLevelCacheDir + "/" + fileName);
    }

    public void resizeThumbs(ArrayList<String> paths) {
        for (String path : paths) {
            getThumbHelper(path);
        }
    }
}
