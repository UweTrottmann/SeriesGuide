
package com.battlelancer.seriesguide.service;

import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.ui.EpisodeDetailsActivity;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.ui.UpcomingRecentActivity;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.thetvdbapi.ImageCache;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;

public class NotificationService extends IntentService {

    private static final long[] VIBRATION_PATTERN = new long[] {
            0, 100, 200, 100, 100, 100
    };

    private static final String[] PROJECTION = new String[] {
            Tables.EPISODES + "." + Episodes._ID, Episodes.TITLE, Episodes.FIRSTAIREDMS,
            Shows.TITLE, Shows.NETWORK, Episodes.NUMBER, Episodes.SEASON, Shows.POSTER,
            Episodes.OVERVIEW
    };

    // by airdate, then by show, then lowest number first
    private static final String SORTING = Episodes.FIRSTAIREDMS + " ASC," + Shows.TITLE + " ASC,"
            + Episodes.NUMBER + " ASC";

    // only within time frame, unwatched episodes, only of favorite shows
    private static final String SELECTION = Episodes.FIRSTAIREDMS + ">=? AND "
            + Episodes.FIRSTAIREDMS + "<=? AND " + Episodes.WATCHED + "=? AND " + Shows.FAVORITE
            + "=?";

    interface NotificationQuery {
        int _ID = 0;

        int TITLE = 1;

        int FIRSTAIREDMS = 2;

        int SHOW_TITLE = 3;

        int NETWORK = 4;

        int NUMBER = 5;

        int SEASON = 6;

        int POSTER = 7;

        int OVERVIEW = 8;
    }

    public NotificationService() {
        super("AlarmManagerService");
    }

    @SuppressWarnings("deprecation")
    @TargetApi(16)
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

        long wakeUpTime = 0;
        final long fakeNow = Utils.getFakeCurrentTime(prefs);
        final long inOneHour = fakeNow + DateUtils.HOUR_IN_MILLIS;

        // get episodes which air between 15 mins ago and one hour in the future
        final Cursor upcomingEpisodes = getContentResolver().query(
                Episodes.CONTENT_URI_WITHSHOW,
                PROJECTION,
                SELECTION,
                new String[] {
                        String.valueOf(fakeNow - 15 * DateUtils.MINUTE_IN_MILLIS),
                        String.valueOf(inOneHour), "0", "1"
                }, SORTING);

        if (upcomingEpisodes != null) {

            // look if we have found something to notify about
            final int count = upcomingEpisodes.getCount();
            if (count != 0) {

                final Context context = getApplicationContext();
                CharSequence tickerText = "";
                CharSequence contentTitle = "";
                CharSequence contentText = "";
                PendingIntent contentIntent = null;

                // notification sound
                final String ringtoneUri = prefs.getString(SeriesGuidePreferences.KEY_RINGTONE,
                        "content://settings/system/notification_sound");
                // vibration
                final boolean isVibrating = prefs.getBoolean(SeriesGuidePreferences.KEY_VIBRATE,
                        false);

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
                } else if (count > 1) {
                    // notify about multiple episodes
                    tickerText = getString(R.string.upcoming_episodes);
                    contentTitle = getString(R.string.upcoming_episodes) + " ("
                            + String.valueOf(count) + ")";
                    contentText = getString(R.string.upcoming_display);

                    Intent notificationIntent = new Intent(context, UpcomingRecentActivity.class);
                    contentIntent = PendingIntent.getActivity(context, 3, notificationIntent, 0);
                }

                // build the notification
                Notification notification;

                if (Utils.isHoneycombOrHigher()) {
                    // HONEYCOMB and above (with some extensions for JELLY BEAN)
                    Notification.Builder anb = new Notification.Builder(context);

                    if (count == 1) {
                        // single episode
                        upcomingEpisodes.moveToFirst();
                        String posterPath = upcomingEpisodes.getString(NotificationQuery.POSTER);
                        anb.setLargeIcon(ImageCache.getInstance(context)
                                .getThumb(posterPath, false));

                        // Jelly Bean and above can display more information
                        if (Utils.isJellyBeanOrHigher()) {
                            String episodeTitle = upcomingEpisodes
                                    .getString(NotificationQuery.TITLE);
                            String episodeSummary = upcomingEpisodes
                                    .getString(NotificationQuery.OVERVIEW);

                            SpannableStringBuilder bigText = new SpannableStringBuilder();
                            bigText.append(episodeTitle);
                            bigText.setSpan(new ForegroundColorSpan(Color.WHITE), 0,
                                    bigText.length(), 0);
                            bigText.append("\n");
                            bigText.append(episodeSummary);

                            anb.setStyle(new Notification.BigTextStyle().bigText(bigText)
                                    .setSummaryText(contentText));

                            // TODO allow check ins via intent
                            // anb.addAction(R.drawable.ic_notification,
                            // getString(R.string.checkin), null);
                        }
                    } else {
                        // multiple episodes
                        if (Utils.isJellyBeanOrHigher()) {
                            Notification.InboxStyle inboxStyle = new Notification.InboxStyle();

                            // display the first five
                            for (int i = 0; i < 5; i++) {
                                if (upcomingEpisodes.moveToPosition(i)) {
                                    // add show title, air time and network
                                    SpannableStringBuilder lineText = new SpannableStringBuilder();
                                    lineText.append(upcomingEpisodes
                                            .getString(NotificationQuery.SHOW_TITLE));
                                    lineText.setSpan(new ForegroundColorSpan(Color.WHITE), 0,
                                            lineText.length(), 0);
                                    lineText.append(" ");
                                    String airs = Utils.formatToTimeAndDay(upcomingEpisodes
                                            .getLong(NotificationQuery.FIRSTAIREDMS), this)[0];
                                    String network = upcomingEpisodes
                                            .getString(NotificationQuery.NETWORK);
                                    lineText.append(getString(R.string.upcoming_show_detailed,
                                            airs, network));
                                    inboxStyle.addLine(lineText);
                                }
                            }

                            // tell if we could not display all episodes
                            if (count >= 5) {
                                inboxStyle.setSummaryText(getString(R.string.more, count));
                            }

                            anb.setStyle(inboxStyle);
                        }
                    }

                    // If the string is empty, the user chose silent. So only
                    // set a sound if necessary.
                    if (ringtoneUri.length() != 0) {
                        anb.setSound(Uri.parse(ringtoneUri));
                    }
                    if (isVibrating) {
                        anb.setVibrate(VIBRATION_PATTERN);
                    }
                    anb.setDefaults(Notification.DEFAULT_LIGHTS);
                    anb.setWhen(System.currentTimeMillis());
                    anb.setAutoCancel(true);
                    anb.setTicker(tickerText);
                    anb.setContentTitle(contentTitle);
                    anb.setContentText(contentText);
                    anb.setContentIntent(contentIntent);
                    anb.setSmallIcon(R.drawable.ic_notification);

                    if (Utils.isJellyBeanOrHigher()) {
                        anb.setPriority(Notification.PRIORITY_DEFAULT);
                        notification = anb.build();
                    } else {
                        notification = anb.getNotification();
                    }
                } else {
                    // GINGERBREAD and below
                    NotificationCompat.Builder nb = new NotificationCompat.Builder(context);

                    if (count == 1) {
                        // single episode
                        upcomingEpisodes.moveToFirst();
                        String posterPath = upcomingEpisodes.getString(NotificationQuery.POSTER);
                        nb.setLargeIcon(ImageCache.getInstance(context).getThumb(posterPath, false));
                    }

                    // If the string is empty, the user chose silent. So only
                    // set a
                    // sound if necessary.
                    if (ringtoneUri.length() != 0) {
                        nb.setSound(Uri.parse(ringtoneUri));
                    }
                    if (isVibrating) {
                        nb.setVibrate(VIBRATION_PATTERN);
                    }
                    nb.setDefaults(Notification.DEFAULT_LIGHTS);
                    nb.setWhen(System.currentTimeMillis());
                    nb.setAutoCancel(true);
                    nb.setTicker(tickerText);
                    nb.setContentTitle(contentTitle);
                    nb.setContentText(contentText);
                    nb.setContentIntent(contentIntent);
                    nb.setSmallIcon(R.drawable.ic_notification);

                    notification = nb.getNotification();
                }

                // use string resource id, always unique within app
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(R.string.upcoming_show, notification);
            }

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
        }

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
