
package com.battlelancer.seriesguide;

import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.thetvdbapi.ImageCache;

import android.app.Application;
import android.preference.PreferenceManager;

public class SeriesGuideApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // already create an instance of ImageCache
        ImageCache.getInstance(getApplicationContext());

        Utils.runNotificationService(this);

        String theme = PreferenceManager.getDefaultSharedPreferences(this).getString(
                SeriesGuidePreferences.KEY_THEME, "0");
        Utils.updateTheme(theme);
    }

    @Override
    public void onLowMemory() {
        ImageCache.getInstance(getApplicationContext()).clear();
        if (!Utils.isICSOrHigher()) {
            // clear the whole cache as Honeycomb and below don't support
            // onTrimMemory (used directly in our ImageProvider)
            ImageProvider.getInstance(this).clearCache();
        }
    }

}
