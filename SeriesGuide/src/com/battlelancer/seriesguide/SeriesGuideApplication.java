
package com.battlelancer.seriesguide;

import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.thetvdbapi.ImageCache;

import android.app.Application;

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
