
package com.battlelancer.seriesguide;

import com.battlelancer.seriesguide.service.NotificationService;
import com.battlelancer.thetvdbapi.ImageCache;

import android.app.Application;
import android.content.Intent;

public class SeriesGuideApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // already create an instance of ImageCache
        ImageCache.getInstance(getApplicationContext());

        // start the notifications service
        // TODO maybe do this every time a show is added
        Intent i = new Intent(this, NotificationService.class);
        startService(i);
    }

    @Override
    public void onLowMemory() {
        ImageCache.getInstance(getApplicationContext()).clear();
    }

}
