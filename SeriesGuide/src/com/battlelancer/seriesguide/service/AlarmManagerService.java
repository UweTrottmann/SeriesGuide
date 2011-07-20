
package com.battlelancer.seriesguide.service;

import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
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

            AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(this, OnAlarmReceiver.class);
            i.putExtra(Episodes._ID, 2545921);
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);

            mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 20000, pi);
//            mgr.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 20000,
//                    AlarmManager.INTERVAL_DAY * 7, pi);
            break;

        }

        favoriteShows.close();
    }

}
