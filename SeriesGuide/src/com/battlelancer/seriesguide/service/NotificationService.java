
package com.battlelancer.seriesguide.service;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.ui.EpisodeDetailsActivity;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.ui.UpcomingRecentActivity;
import com.battlelancer.seriesguide.util.Utils;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

public class NotificationService extends IntentService {

    private static final int NOTIFICATION_ID = 1;

    private static final String[] PROJECTION = new String[] {
            Tables.EPISODES + "." + Episodes._ID, Episodes.TITLE, Episodes.FIRSTAIREDMS,
            Shows.TITLE, Shows.NETWORK, Episodes.NUMBER, Episodes.SEASON
    };

    // by airdate, then by show, then lowest number first
    private static final String SORTING = Episodes.FIRSTAIREDMS + " ASC," + Shows.TITLE + " ASC,"
            + Episodes.NUMBER + " ASC";

    // only future, unwatched episodes, only of favorite shows
    private static final String SELECTION = Episodes.FIRSTAIRED + ">=? and " + Episodes.WATCHED
            + "=0 and " + Shows.FAVORITE + "=1";

    interface NotificationQuery {
        int _ID = 1;

        int TITLE = 2;

        int FIRSTAIREDMS = 3;

        int SHOW_TITLE = 4;

        int NETWORK = 5;

        int NUMBER = 6;

        int SEASON = 7;
    }

    public NotificationService() {
        super("AlarmManagerService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean(SeriesGuidePreferences.KEY_NOTIFICATIONS_ENABLED, true)) {
            // cancel any pending alarm
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(this, OnAlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
            am.cancel(pi);

            return;
        }

        long fakeNow = Utils.getFakeCurrentTime(prefs);

        // get episodes which air between one hour ago and the future
        final Cursor upcomingEpisodes = getContentResolver().query(Episodes.CONTENT_URI_WITHSHOW,
                PROJECTION, SELECTION, new String[] {
                    String.valueOf(fakeNow - DateUtils.HOUR_IN_MILLIS)
                }, SORTING);

        // look if we have found something to notify about
        int count = 0;
        long inOneHour = fakeNow + DateUtils.HOUR_IN_MILLIS;
        while (upcomingEpisodes.moveToNext()) {
            long airtime = upcomingEpisodes.getLong(NotificationQuery.FIRSTAIREDMS);
            if (airtime <= inOneHour) {
                count++;
            } else {
                break;
            }
        }

        if (count != 0) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            int icon = R.drawable.ic_stat_ic_notification;
            Context context = getApplicationContext();
            CharSequence tickerText = "";
            CharSequence contentTitle = "";
            CharSequence contentText = "";
            PendingIntent contentIntent = null;

            if (count == 1) {
                // notify in detail about one episode
                upcomingEpisodes.moveToFirst();
                String showTitle = upcomingEpisodes.getString(NotificationQuery.SHOW_TITLE);
                String airs = Utils.formatToTimeAndDay(
                        upcomingEpisodes.getLong(NotificationQuery.FIRSTAIREDMS), this)[2];
                String network = upcomingEpisodes.getString(NotificationQuery.NETWORK);

                tickerText = getString(R.string.upcoming_show, showTitle);
                contentTitle = getString(R.string.upcoming_show_detailed, showTitle, airs, network);
                // TODO special episodes
                contentText = Utils.getEpisodeNumber(
                        PreferenceManager.getDefaultSharedPreferences(this),
                        upcomingEpisodes.getString(NotificationQuery.SEASON),
                        upcomingEpisodes.getString(NotificationQuery.NUMBER))
                        + " " + upcomingEpisodes.getString(NotificationQuery.TITLE);

                Intent notificationIntent = new Intent(context, EpisodeDetailsActivity.class);
                notificationIntent.putExtra(Episodes._ID,
                        upcomingEpisodes.getString(NotificationQuery._ID));
                contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

            } else if (count > 1) {
                // notify about multiple episodes
                tickerText = getString(R.string.upcoming_episodes);
                contentTitle = getString(R.string.upcoming_episodes);
                contentText = String.valueOf(count);

                Intent notificationIntent = new Intent(context, UpcomingRecentActivity.class);
                contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
            }

            Notification notification = new Notification(icon, tickerText,
                    System.currentTimeMillis());
            notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            notification.defaults |= Notification.DEFAULT_LIGHTS;

            nm.notify(NOTIFICATION_ID, notification);
        }

        // set an alarm to wake us up later to notify about future episodes
        long wakeUpTime = 0;
        while (upcomingEpisodes.moveToNext()) {
            long airtime = upcomingEpisodes.getLong(NotificationQuery.FIRSTAIREDMS);
            if (airtime > inOneHour) {
                // wake up an hour before the next episode airs
                wakeUpTime = Utils.convertToFakeTime(airtime, prefs) - DateUtils.HOUR_IN_MILLIS;
                break;
            }
        }

        // set a default wake-up time if there are no future episodes for now
        if (wakeUpTime == 0) {
            wakeUpTime = System.currentTimeMillis() + 12 * DateUtils.HOUR_IN_MILLIS;
        }

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(this, OnAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
        am.set(AlarmManager.RTC_WAKEUP, wakeUpTime, pi);
    }
}
