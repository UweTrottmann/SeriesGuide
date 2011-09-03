
package com.battlelancer.seriesguide.service;

import com.battlelancer.seriesguide.SeriesGuideData;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import java.util.Date;

public class AlarmManagerService extends IntentService {

    public AlarmManagerService() {
        super("AlarmManagerService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // TODO: completely rework how upcoming episodes are defined. compare
        // their airtime with the current time, instead of just comparing the
        // airday

        final Date date = new Date();
        final String today = SeriesGuideData.theTVDBDateFormat.format(date);

        final String[] projection = new String[] {
                Tables.EPISODES + "." + Episodes._ID, Episodes.TITLE, Episodes.FIRSTAIRED,
                Shows.TITLE, Shows.AIRSTIME, Shows.NETWORK
        };
        final String sortOrder = Episodes.FIRSTAIRED + " ASC," + Shows.AIRSTIME + " ASC,"
                + Shows.TITLE + " ASC";

        // only future, unwatched episodes, only of favorite shows
        final String selection = Episodes.FIRSTAIRED + ">=? and " + Episodes.WATCHED + "=0 and "
                + Shows.FAVORITE + "=1";

        final Cursor upcomingEpisodes = getContentResolver().query(Episodes.CONTENT_URI_WITHSHOW,
                projection, selection, new String[] {
                    today
                }, sortOrder);

        // set alarm for next upcoming episode
        // TODO: do not set if episode is already airing/has already aired
        // TODO: strategy if there are two ore more next upcoming episodes
        // airing the same time
        if (upcomingEpisodes.moveToFirst()) {
            AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            Intent i = new Intent(this, OnAlarmReceiver.class);
            i.putExtra(Episodes._ID, upcomingEpisodes.getInt(0));
            i.putExtra(Episodes.TITLE, upcomingEpisodes.getString(1));
            i.putExtra(Shows.TITLE, upcomingEpisodes.getString(3));
            i.putExtra(Shows.NETWORK, upcomingEpisodes.getString(5));
            long airtimeMilliseconds = upcomingEpisodes.getLong(4);
            String airtime = SeriesGuideData.parseDateToLocalRelative(
                    upcomingEpisodes.getString(2), airtimeMilliseconds, this);
            i.putExtra(Shows.AIRSTIME, airtime);

            PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);

            // show notification 10 min in advance
            // TODO: calculate waketime out of airdate and airtime
            mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() - 600000, pi);
        }
    }
}
