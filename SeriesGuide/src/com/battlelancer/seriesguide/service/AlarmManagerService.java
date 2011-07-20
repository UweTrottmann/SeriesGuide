
package com.battlelancer.seriesguide.service;

import com.battlelancer.seriesguide.provider.SeriesContract.Shows;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;

public class AlarmManagerService extends IntentService {

    public AlarmManagerService() {
        super("AlarmManagerService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final Cursor favoriteShows = getContentResolver().query(Shows.CONTENT_URI, new String[] {
                Shows._ID, Shows.NEXTAIRDATE, Shows.NEXTEPISODE
        }, Shows.FAVORITE + "=?", new String[] {
            "1"
        }, null);

        while (favoriteShows.moveToNext()) {
            // set alarm for each upcoming episode
        }
        
        favoriteShows.close();
    }

}
