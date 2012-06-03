
package com.battlelancer.seriesguide.service;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.ui.EpisodeDetailsActivity;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.ui.UpcomingRecentActivity;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.thetvdbapi.ImageCache;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateUtils;

public class NotificationService extends IntentService {

    private static final String[] PROJECTION = new String[] {
            Tables.EPISODES + "." + Episodes._ID, Episodes.TITLE, Episodes.FIRSTAIREDMS,
            Shows.TITLE, Shows.NETWORK, Episodes.NUMBER, Episodes.SEASON, Shows.POSTER
    };

    // by airdate, then by show, then lowest number first
    private static final String SORTING = Episodes.FIRSTAIREDMS + " ASC," + Shows.TITLE + " ASC,"
            + Episodes.NUMBER + " ASC";

    // only future, unwatched episodes, only of favorite shows
    private static final String SELECTION = Episodes.FIRSTAIREDMS + ">=? AND " + Episodes.WATCHED
            + "=? AND " + Shows.FAVORITE + "=?";

    interface NotificationQuery {
        int _ID = 0;

        int TITLE = 1;

        int FIRSTAIREDMS = 2;

        int SHOW_TITLE = 3;

        int NETWORK = 4;

        int NUMBER = 5;

        int SEASON = 6;

        int POSTER = 7;
    }

    public NotificationService() {
        super("AlarmManagerService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // unschedule notification service wake-ups for disabled notifications
        // and non-supporters
        if (!prefs.getBoolean(SeriesGuidePreferences.KEY_NOTIFICATIONS_ENABLED, true)
                || !Utils.isSupporterChannel(this)) {
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

            Context context = getApplicationContext();
            CharSequence tickerText = "";
            CharSequence contentTitle = "";
            CharSequence contentText = "";
            PendingIntent contentIntent = null;

            NotificationCompat.Builder nb = new NotificationCompat.Builder(context);

            if (count == 1) {
                // notify in detail about one episode
                upcomingEpisodes.moveToFirst();
                String showTitle = upcomingEpisodes.getString(NotificationQuery.SHOW_TITLE);
                String airs = Utils.formatToTimeAndDay(
                        upcomingEpisodes.getLong(NotificationQuery.FIRSTAIREDMS), this)[0];
                String network = upcomingEpisodes.getString(NotificationQuery.NETWORK);

                tickerText = getString(R.string.upcoming_show, showTitle);
                contentTitle = showTitle
                        + " "
                        + Utils.getEpisodeNumber(
                                PreferenceManager.getDefaultSharedPreferences(this),
                                upcomingEpisodes.getInt(NotificationQuery.SEASON),
                                upcomingEpisodes.getInt(NotificationQuery.NUMBER));
                contentText = getString(R.string.upcoming_show_detailed, airs, network);

                Intent notificationIntent = new Intent(context, EpisodeDetailsActivity.class);
                notificationIntent.putExtra(EpisodeDetailsActivity.InitBundle.EPISODE_TVDBID,
                        upcomingEpisodes.getInt(NotificationQuery._ID));
                contentIntent = PendingIntent.getActivity(context, 2, notificationIntent, 0);

                String posterPath = upcomingEpisodes.getString(NotificationQuery.POSTER);
                nb.setLargeIcon(ImageCache.getInstance(context).getThumb(posterPath, false));
            } else if (count > 1) {
                // notify about multiple episodes
                tickerText = getString(R.string.upcoming_episodes);
                contentTitle = getString(R.string.upcoming_episodes) + " (" + String.valueOf(count)
                        + ")";
                contentText = getString(R.string.upcoming_display);

                Intent notificationIntent = new Intent(context, UpcomingRecentActivity.class);
                contentIntent = PendingIntent.getActivity(context, 3, notificationIntent, 0);

            }

            // notification sound
            final String ringtoneUri = prefs.getString(SeriesGuidePreferences.KEY_RINGTONE,
                    "content://settings/system/notification_sound");
            // If the string is empty, the user chose silent. So only set a
            // sound if necessary.
            if (ringtoneUri.length() != 0) {
                nb.setSound(Uri.parse(ringtoneUri));
            }

            // vibration
            final boolean isVibrating = prefs.getBoolean(SeriesGuidePreferences.KEY_VIBRATE, false);
            if (isVibrating) {
                nb.setVibrate(new long[] {
                        0, 100, 200, 100, 100, 100
                });
            }

            nb.setDefaults(Notification.DEFAULT_LIGHTS);
            nb.setWhen(System.currentTimeMillis());
            nb.setAutoCancel(true);
            nb.setTicker(tickerText);
            nb.setContentTitle(contentTitle);
            nb.setContentText(contentText);
            nb.setContentIntent(contentIntent);
            nb.setSmallIcon(R.drawable.ic_notification);

            // use string resource id, always unique within app
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(R.string.upcoming_show, nb.getNotification());
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

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(this, OnAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
        am.set(AlarmManager.RTC_WAKEUP, wakeUpTime, pi);
    }
}
