
package com.battlelancer.thetvdbapi;

import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.AnalyticsUtils;
import com.battlelancer.seriesguide.util.Utils;

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
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class ImageCache {

    private static final String THUMB_SUFFIX = "thumb";

    private int mCachedImageQuality = 98;

    private String mSecondLevelCacheDir;

    private static final CompressFormat mCompressedImageFormat = CompressFormat.JPEG;

    private final float mScale;

    private static final float THUMBNAIL_WIDTH_DIP = 68.0f;

    private static final float THUMBNAIL_HEIGHT_DIP = 100.0f;

    private Context mCtx;

    private OnSharedPreferenceChangeListener listener;

    private static ImageCache _instance;

    private static final String TAG = "ImageCache";

    private static final int HARD_CACHE_CAPACITY = 20;

    @SuppressWarnings("serial")
    private final HashMap<String, Bitmap> mHardBitmapCache = new LinkedHashMap<String, Bitmap>(
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

    private ImageCache(Context ctx) {
        this.mCtx = ctx;
        this.mSecondLevelCacheDir = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/Android/data/" + ctx.getPackageName() + "/files";
        mScale = mCtx.getResources().getDisplayMetrics().density;
        createDirectories();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mCtx);
        listener = new OnSharedPreferenceChangeListener() {

            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equalsIgnoreCase(SeriesGuidePreferences.KEY_HIDEIMAGES)) {

                    // remove or add .nomedia file
                    if (sharedPreferences.getBoolean(SeriesGuidePreferences.KEY_HIDEIMAGES, true)) {
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
            _instance = new ImageCache(ctx.getApplicationContext());
        }
        return _instance;
    }

    private void createDirectories() {
        new File(mSecondLevelCacheDir).mkdirs();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mCtx);
        if (sharedPreferences.getBoolean(SeriesGuidePreferences.KEY_HIDEIMAGES, true)) {
            try {
                new File(mSecondLevelCacheDir + "/.nomedia").createNewFile();
            } catch (IOException e) {
                Log.w(TAG, "Could not create .nomedia file");
            }
        }
    }

    /**
     * Returns whether this image exists in the cache or on disk already.
     * 
     * @param imageUrl
     * @return
     */
    public boolean contains(String imageUrl) {
        if (mHardBitmapCache.containsKey(imageUrl) || sSoftBitmapCache.containsKey(imageUrl)) {
            return true;
        } else {
            File imageFile = getImageFile(imageUrl);
            return imageFile.exists();
        }
    }

    public Bitmap get(String key) {
        return getIfNotBusy(key, false);
    }

    public Bitmap getIfNotBusy(String imageUrl, boolean isBusy) {
        synchronized (mHardBitmapCache) {
            final Bitmap bitmap = mHardBitmapCache.get(imageUrl);
            if (bitmap != null) {
                // 1st level cache hit (memory)
                // Move element to first position, so that it is removed last
                mHardBitmapCache.remove(imageUrl);
                mHardBitmapCache.put(imageUrl, bitmap);
                return bitmap;
            }
        }

        SoftReference<Bitmap> bitmapReference = sSoftBitmapCache.get(imageUrl);
        if (bitmapReference != null) {
            final Bitmap bitmap = bitmapReference.get();
            if (bitmap != null) {
                // Bitmap found in soft cache
                return bitmap;
            } else {
                // Soft reference has been Garbage Collected
                sSoftBitmapCache.remove(imageUrl);
            }
        }

        // if the caller is busy doing other work, don't load from disk
        if (isBusy) {
            return null;
        }

        File imageFile = getImageFile(imageUrl);
        if (imageFile.exists()) {
            // 2nd level cache hit (disk)
            final Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            if (bitmap == null) {
                // treat decoding errors as a cache miss
                return null;
            }

            synchronized (mHardBitmapCache) {
                mHardBitmapCache.put(imageUrl, bitmap);
            }
            return bitmap;
        }

        // bitmap could not be found anywhere
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
        if (Utils.isExtStorageAvailable()) {
            // make sure directories exist
            createDirectories();
            File imageFile = getImageFile(imageUrl);

            try {
                imageFile.createNewFile();

                FileOutputStream ostream = new FileOutputStream(imageFile);
                bitmap.compress(mCompressedImageFormat, mCachedImageQuality, ostream);
                ostream.close();

                synchronized (mHardBitmapCache) {
                    mHardBitmapCache.put(imageUrl, bitmap);
                }

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
        mHardBitmapCache.clear();
        sSoftBitmapCache.clear();
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
