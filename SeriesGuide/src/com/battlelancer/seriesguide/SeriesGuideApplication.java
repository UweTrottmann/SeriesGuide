
package com.battlelancer.seriesguide;

import android.app.Application;

import com.battlelancer.thetvdbapi.ImageCache;

public class SeriesGuideApplication extends Application {

    private ImageCache mImageCache;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public synchronized ImageCache getImageCache() {
        if (mImageCache == null) {
            mImageCache = ImageCache.getInstance(getApplicationContext());
        }
        return mImageCache;
    }

    @Override
    public void onLowMemory() {
        if (mImageCache != null) {
            mImageCache.clear();
        }
    }

}
