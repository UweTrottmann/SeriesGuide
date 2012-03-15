
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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

public class NotificationService extends IntentService {

    private static final String[] PROJECTION = new String[] {
            Tables.EPISODES + "." + Episodes._ID, Episodes.TITLE, Episodes.FIRSTAIREDMS,
            Shows.TITLE, Shows.NETWORK, Episodes.NUMBER, Episodes.SEASON
    };

    // by airdate, then by show, then lowest number first
    private static final String SORTING = Episodes.FIRSTAIREDMS + " ASC," + Shows.TITLE + " ASC,"
            + Episodes.NUMBER + " ASC";

    // only future, unwatched episodes, only of favorite shows
    private static final String SELECTION = Episodes.FIRSTAIREDMS + ">=? AND " + Episodes.WATCHED
            + "=? AND " + Shows.FAVORITE + "=?";

    private Handler mHandler;

    interface NotificationQuery {
        int _ID = 0;

        int TITLE = 1;

        int FIRSTAIREDMS = 2;

        int SHOW_TITLE = 3;

        int NETWORK = 4;

        int NUMBER = 5;

        int SEASON = 6;
    }

    public NotificationService() {
        super("AlarmManagerService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
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

        // get episodes which air between 15 mins ago and the future
        final Cursor upcomingEpisodes = getContentResolver().query(Episodes.CONTENT_URI_WITHSHOW,
                PROJECTION, SELECTION, new String[] {
                        String.valueOf(fakeNow - 15 * DateUtils.MINUTE_IN_MILLIS), "0", "1"
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

            int icon = R.drawable.ic_notification;
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
                contentTitle = showTitle
                        + " "
                        + Utils.getEpisodeNumber(
                                PreferenceManager.getDefaultSharedPreferences(this),
                                upcomingEpisodes.getString(NotificationQuery.SEASON),
                                upcomingEpisodes.getString(NotificationQuery.NUMBER));
                contentText = getString(R.string.upcoming_show_detailed, airs, network);

                Intent notificationIntent = new Intent(context, EpisodeDetailsActivity.class);
                notificationIntent.putExtra(EpisodeDetailsActivity.InitBundle.EPISODE_ID,
                        upcomingEpisodes.getString(NotificationQuery._ID));
                contentIntent = PendingIntent.getActivity(context, 0, notificationIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT);

            } else if (count > 1) {
                // notify about multiple episodes
                tickerText = getString(R.string.upcoming_episodes);
                contentTitle = getString(R.string.upcoming_episodes) + " (" + String.valueOf(count)
                        + ")";
                contentText = getString(R.string.upcoming_display);

                Intent notificationIntent = new Intent(context, UpcomingRecentActivity.class);
                contentIntent = PendingIntent.getActivity(context, 0, notificationIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT);
            }

            Notification notification = new Notification(icon, tickerText,
                    System.currentTimeMillis());
            notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            notification.defaults |= Notification.DEFAULT_LIGHTS;
            notification.defaults |= Notification.DEFAULT_SOUND;

            // use string resource id, always unique within app
            nm.notify(R.string.upcoming_show, notification);
        }

        // set an alarm to wake us up later to notify about future episodes
        long wakeUpTime = 0;

        upcomingEpisodes.moveToPosition(-1);
        while (upcomingEpisodes.moveToNext()) {
            long airtime = upcomingEpisodes.getLong(NotificationQuery.FIRSTAIREDMS);
            if (airtime > inOneHour) {
                // wake up an hour before the next episode airs
                wakeUpTime = Utils.convertToFakeTime(airtime, prefs, false)
                        - DateUtils.HOUR_IN_MILLIS;
                break;
            }
        }

        upcomingEpisodes.close();

        // set a default wake-up time if there are no future episodes for now
        if (wakeUpTime == 0) {
            wakeUpTime = System.currentTimeMillis() + 6 * DateUtils.HOUR_IN_MILLIS;
        }

        // TODO remove for release
        final long time = wakeUpTime;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                DateFormat df = DateFormat.getDateTimeInstance();
                Toast.makeText(getApplicationContext(),
                        "Alarm set for " + df.format(new Date(time)), Toast.LENGTH_SHORT).show();
            }
        });

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(this, OnAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
        am.set(AlarmManager.RTC_WAKEUP, wakeUpTime, pi);
    }
}
