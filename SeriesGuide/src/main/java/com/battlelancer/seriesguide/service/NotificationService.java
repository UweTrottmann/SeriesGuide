package com.battlelancer.seriesguide.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.NotificationSettings;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.battlelancer.seriesguide.traktapi.QuickCheckInActivity;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.ui.episodes.EpisodesActivity;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import org.threeten.bp.Instant;
import timber.log.Timber;

/**
 * To debug set {@link #DEBUG} to true.
 *
 * <p>Useful commands:
 *
 * <p>Dump alarms:
 * <pre>{@code
 * adb shell dumpsys alarm > alarms.txt
 * }</pre>
 *
 * <p>Doze mode:
 * <pre>{@code
 * adb shell dumpsys deviceidle force-idle
 *
 * adb shell dumpsys deviceidle unforce
 * }</pre>
 *
 * <p>App Standby:
 * <pre>{@code
 * adb shell dumpsys battery unplug
 * adb shell am set-inactive com.battlelancer.seriesguide true
 *
 * adb shell am set-inactive com.battlelancer.seriesguide false
 *
 * adb shell am get-inactive com.battlelancer.seriesguide
 * }</pre>
 *
 * <p>https://developer.android.com/training/monitoring-device-state/doze-standby.html
 */
public class NotificationService {

    public static final String ACTION_CLEARED = "seriesguide.intent.action.CLEARED";
    public static final String EXTRA_EPISODE_TVDBID
            = "com.battlelancer.seriesguide.EXTRA_EPISODE_TVDBID";
    private static final String EXTRA_EPISODE_CLEARED_TIME
            = "com.battlelancer.seriesguide.episode_cleared_time";

    private static final boolean DEBUG = false;

    private static final int REQUEST_CODE_DELETE_INTENT = 1;
    private static final int REQUEST_CODE_SINGLE_EPISODE = 2;
    private static final int REQUEST_CODE_MULTIPLE_EPISODES = 3;
    private static final int REQUEST_CODE_ACTION_CHECKIN = 4;
    private static final int REQUEST_CODE_ACTION_SET_WATCHED = 4;

    public static final long[] VIBRATION_PATTERN = new long[] {
            0, 100, 200, 100, 100, 100
    };

    private static final String[] PROJECTION = new String[] {
            Tables.EPISODES + "." + Episodes._ID, Episodes.TITLE, Episodes.FIRSTAIREDMS,
            Shows.TITLE, Shows.NETWORK, Episodes.NUMBER, Episodes.SEASON, Shows.POSTER,
            Episodes.OVERVIEW
    };

    // by airdate, then by show, then lowest number first
    private static final String SORTING = Episodes.FIRSTAIREDMS + " ASC,"
            + Shows.SORT_TITLE + ","
            + Episodes.NUMBER + " ASC";

    // only if notifications are on: unwatched episodes released on or after arg
    private static final String SELECTION = Shows.SELECTION_NOTIFY + " AND "
            + Episodes.SELECTION_UNWATCHED + " AND "
            + Episodes.FIRSTAIREDMS + ">=?";

    interface NotificationQuery {
        int _ID = 0;
        int TITLE = 1;
        int EPISODE_FIRST_RELEASE_MS = 2;
        int SHOW_TITLE = 3;
        int NETWORK = 4;
        int NUMBER = 5;
        int SEASON = 6;
        int POSTER = 7;
        int OVERVIEW = 8;
    }

    static final Executor SERIAL_EXECUTOR = new SerialExecutor();

    private final Context context;

    /**
     * Send broadcast to run the notification service to display and (re)schedule upcoming episode
     * alarms.
     */
    public static void trigger(Context context) {
        context.sendBroadcast(NotificationAlarmReceiver.intent(context));
    }

    public NotificationService(Context context) {
        this.context = context.getApplicationContext();
    }

    private PendingIntent getWakeUpPendingIntent() {
        return PendingIntent.getBroadcast(context, 0, NotificationAlarmReceiver.intent(context), 0);
    }

    public void run() {
        Timber.d("Waking up...");

        // remove notification service wake-up alarm if notifications are disabled or not unlocked
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!NotificationSettings.isNotificationsEnabled(context) || !Utils.hasAccessToX(context)) {
            Timber.d("Notifications disabled, removing wake-up alarm");
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pi = getWakeUpPendingIntent();
            if (am != null) {
                am.cancel(pi);
            }

            resetLastEpisodeAirtime(prefs);
            return;
        }

        long nextWakeUpTime = 0;

        final long customCurrentTime = TimeTools.getCurrentTime(context);
        final Cursor upcomingEpisodes = queryUpcomingEpisodes(customCurrentTime);
        if (upcomingEpisodes != null) {
            int notificationThreshold = NotificationSettings.getLatestToIncludeTreshold(context);
            if (DEBUG) {
                Timber.d("DEBUG MODE: always notify about next episode within 1 week");
                // a week, for debugging (use only one show to get single
                // episode notifications)
                notificationThreshold = 10080;
                // notify again for same episodes
                resetLastEpisodeAirtime(prefs);
            }

            final long nextEpisodeReleaseTime = NotificationSettings.getNextToNotifyAbout(context);
            // wake user-defined amount of time earlier than next episode release time
            final long plannedWakeUpTime =
                    TimeTools.applyUserOffset(context, nextEpisodeReleaseTime).getTime()
                            - DateUtils.MINUTE_IN_MILLIS * notificationThreshold;

            // note: on first run plannedWakeUpTime will be <= 0
            boolean checkForNewEpisodes = true;

            if (System.currentTimeMillis() < plannedWakeUpTime) {
                Timber.d("Woke up earlier than planned, checking for new episodes");
                checkForNewEpisodes = false;
                long releaseTimeLastNotified = NotificationSettings.getLastNotifiedAbout(context);

                while (upcomingEpisodes.moveToNext()) {
                    final long releaseTime = upcomingEpisodes.getLong(
                            NotificationQuery.EPISODE_FIRST_RELEASE_MS);
                    // any episodes added that release before the next one planned to notify about?
                    if (releaseTime < nextEpisodeReleaseTime) {
                        // limit to those released after the episode we last notified about to avoid
                        // notifying about an episode we already notified about
                        // limitation: so if added episodes release at or before that last episode
                        // they will not be notified about
                        if (releaseTime > releaseTimeLastNotified) {
                            checkForNewEpisodes = true;
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }

            if (checkForNewEpisodes) {
                final long latestTimeToInclude = customCurrentTime
                        + DateUtils.MINUTE_IN_MILLIS * notificationThreshold;

                maybeNotify(prefs, upcomingEpisodes, latestTimeToInclude);

                // plan next episode to notify about
                upcomingEpisodes.moveToPosition(-1);
                while (upcomingEpisodes.moveToNext()) {
                    final long releaseTime = upcomingEpisodes.getLong(
                            NotificationQuery.EPISODE_FIRST_RELEASE_MS);
                    if (releaseTime > latestTimeToInclude) {
                        prefs.edit()
                                .putLong(NotificationSettings.KEY_NEXT_TO_NOTIFY, releaseTime)
                                .apply();
                        Timber.d("Next notification planned for episode released at: %s",
                                Instant.ofEpochMilli(releaseTime));

                        // calc wake up time to notify about this episode
                        // taking into account time offset and notification threshold
                        nextWakeUpTime = TimeTools.applyUserOffset(context, releaseTime).getTime()
                                - DateUtils.MINUTE_IN_MILLIS * notificationThreshold;
                        break;
                    }
                }
            } else {
                // Go to sleep, wake up as planned
                Timber.d("No new episodes");
                nextWakeUpTime = plannedWakeUpTime;
            }

            upcomingEpisodes.close();
        }

        // Set a default wake-up time if there are no future episodes for now
        if (nextWakeUpTime <= 0) {
            nextWakeUpTime = System.currentTimeMillis() + 6 * DateUtils.HOUR_IN_MILLIS;
            Timber.d("No future episodes found, wake up in 6 hours");
        }

        if (DEBUG) {
            Timber.d("DEBUG MODE: wake up in 1 minute");
            nextWakeUpTime = System.currentTimeMillis() + DateUtils.MINUTE_IN_MILLIS;
        }

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = getWakeUpPendingIntent();
        Timber.d("Going to sleep, setting wake-up alarm to: %s",
                Instant.ofEpochMilli(nextWakeUpTime));
        if (am != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextWakeUpTime, pi);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                am.setExact(AlarmManager.RTC_WAKEUP, nextWakeUpTime, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, nextWakeUpTime, pi);
            }
        }
    }

    /**
     * Extracts the last cleared time and stores it in settings.
     */
    public static void handleDeleteIntent(Context context, @Nullable Intent intent) {
        if (intent == null) {
            return;
        }

        long clearedTime = intent.getLongExtra(EXTRA_EPISODE_CLEARED_TIME, 0);
        if (clearedTime != 0) {
            // Never show the cleared episode(s) again
            Timber.d("Notification cleared, setting last cleared episode time: %d", clearedTime);
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putLong(NotificationSettings.KEY_LAST_CLEARED, clearedTime)
                    .apply();
        }
    }

    /**
     * Resets the air time of the last notified about episode. Afterwards notifications for episodes
     * may appear, which were already notified about.
     */
    public static void resetLastEpisodeAirtime(final SharedPreferences prefs) {
        Timber.d("Resetting last cleared and last notified");
        prefs.edit()
                .putLong(NotificationSettings.KEY_LAST_CLEARED, 0)
                .putLong(NotificationSettings.KEY_LAST_NOTIFIED, 0)
                .apply();
    }

    /**
     * Get episodes which air from 12 hours ago until eternity, excludes some episodes based on user
     * settings.
     */
    private Cursor queryUpcomingEpisodes(long customCurrentTime) {
        StringBuilder selection = new StringBuilder(SELECTION);

        boolean isNoSpecials = DisplaySettings.isHidingSpecials(context);
        Timber.d("Settings: specials: %s", isNoSpecials ? "YES" : "NO");
        if (isNoSpecials) {
            selection.append(" AND ").append(Episodes.SELECTION_NO_SPECIALS);
        }
        // always exclude hidden shows
        selection.append(" AND ").append(Shows.SELECTION_NO_HIDDEN);

        return context.getContentResolver().query(Episodes.CONTENT_URI_WITHSHOW,
                PROJECTION, selection.toString(), new String[] {
                        String.valueOf(customCurrentTime - 12 * DateUtils.HOUR_IN_MILLIS)
                }, SORTING
        );
    }

    private void maybeNotify(SharedPreferences prefs, Cursor upcomingEpisodes,
            long latestTimeToInclude) {
        final List<Integer> notifyPositions = new ArrayList<>();
        final long latestTimeCleared = NotificationSettings.getLastCleared(context);

        int position = -1;
        upcomingEpisodes.moveToPosition(position);
        while (upcomingEpisodes.moveToNext()) {
            position++;
            // get episodes which are within the notification threshold (user set)...
            final long releaseTime = upcomingEpisodes.getLong(
                    NotificationQuery.EPISODE_FIRST_RELEASE_MS);
            if (releaseTime <= latestTimeToInclude) {
                // ...and released after the last one the user cleared.
                // Note: should be at most those of the last few hours (see cursor query).
                if (releaseTime > latestTimeCleared) {
                    notifyPositions.add(position);
                }
            } else {
                // Too far into the future, stop!
                break;
            }
        }

        // Notify if we found any episodes, store latest release time we notify about
        if (notifyPositions.size() > 0) {
            upcomingEpisodes.moveToPosition(notifyPositions.get(notifyPositions.size() - 1));
            long latestAirtime = upcomingEpisodes.getLong(
                    NotificationQuery.EPISODE_FIRST_RELEASE_MS);
            prefs.edit().putLong(NotificationSettings.KEY_LAST_NOTIFIED, latestAirtime).apply();

            Timber.d("Notify about %d episodes, latest released at: %s",
                    notifyPositions.size(), Instant.ofEpochMilli(latestAirtime));

            notifyAbout(upcomingEpisodes, notifyPositions, latestAirtime);
        }
    }

    private void notifyAbout(final Cursor upcomingEpisodes, List<Integer> notifyPositions,
            long latestAirtime) {
        CharSequence tickerText;
        CharSequence contentTitle;
        CharSequence contentText;
        PendingIntent contentIntent;
        // base intent for task stack
        final Intent showsIntent = new Intent(context, ShowsActivity.class);
        showsIntent.putExtra(ShowsActivity.InitBundle.SELECTED_TAB,
                ShowsActivity.InitBundle.INDEX_TAB_UPCOMING);

        final int count = notifyPositions.size();
        if (count == 1) {
            // notify in detail about one episode
            upcomingEpisodes.moveToPosition(notifyPositions.get(0));

            final String showTitle = upcomingEpisodes.getString(NotificationQuery.SHOW_TITLE);
            // show title and number, like 'Show 1x01'
            contentTitle = TextTools.getShowWithEpisodeNumber(
                    context,
                    showTitle,
                    upcomingEpisodes.getInt(NotificationQuery.SEASON),
                    upcomingEpisodes.getInt(NotificationQuery.NUMBER)
            );
            tickerText = context.getString(R.string.upcoming_show, contentTitle);

            // "8:00 PM Network"
            final String time = TimeTools.formatToLocalTime(context,
                    TimeTools.applyUserOffset(context,
                            upcomingEpisodes.getLong(NotificationQuery.EPISODE_FIRST_RELEASE_MS)));
            final String network = upcomingEpisodes.getString(NotificationQuery.NETWORK);
            contentText = TextTools.dotSeparate(time, network); // switch on purpose

            Intent episodeDetailsIntent = new Intent(context, EpisodesActivity.class);
            episodeDetailsIntent.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID,
                    upcomingEpisodes.getInt(NotificationQuery._ID));
            episodeDetailsIntent.putExtra(EXTRA_EPISODE_CLEARED_TIME, latestAirtime);

            contentIntent = TaskStackBuilder.create(context)
                    .addNextIntent(showsIntent)
                    .addNextIntent(episodeDetailsIntent)
                    .getPendingIntent(REQUEST_CODE_SINGLE_EPISODE,
                            PendingIntent.FLAG_CANCEL_CURRENT);
        } else {
            // notify about multiple episodes
            tickerText = context.getString(R.string.upcoming_episodes);
            contentTitle = context.getString(R.string.upcoming_episodes_number,
                    NumberFormat.getIntegerInstance().format(count));
            contentText = context.getString(R.string.upcoming_display);

            contentIntent = TaskStackBuilder.create(context)
                    .addNextIntent(showsIntent.putExtra(EXTRA_EPISODE_CLEARED_TIME, latestAirtime))
                    .getPendingIntent(REQUEST_CODE_MULTIPLE_EPISODES,
                            PendingIntent.FLAG_CANCEL_CURRENT);
        }

        final NotificationCompat.Builder nb =
                new NotificationCompat.Builder(context, SgApp.NOTIFICATION_CHANNEL_EPISODES);

        boolean richNotification = AndroidUtils.isJellyBeanOrHigher();
        if (richNotification) {
            // JELLY BEAN and above
            if (count == 1) {
                // single episode
                upcomingEpisodes.moveToPosition(notifyPositions.get(0));
                maybeSetPoster(nb, upcomingEpisodes.getString(NotificationQuery.POSTER));

                if (!DisplaySettings.preventSpoilers(context)) {
                    final String episodeTitle = TextTools.getEpisodeTitle(context,
                            upcomingEpisodes.getString(NotificationQuery.TITLE),
                            upcomingEpisodes.getInt(NotificationQuery.NUMBER));
                    final String episodeSummary = upcomingEpisodes
                            .getString(NotificationQuery.OVERVIEW);

                    final SpannableStringBuilder bigText = new SpannableStringBuilder();
                    bigText.append(episodeTitle);
                    bigText.setSpan(new StyleSpan(Typeface.BOLD), 0, bigText.length(), 0);
                    if (!TextUtils.isEmpty(episodeSummary)) {
                        bigText.append("\n").append(episodeSummary);
                    }

                    nb.setStyle(new NotificationCompat.BigTextStyle().bigText(bigText)
                            .setSummaryText(contentText));
                }

                // Action button to check in
                Intent checkInActionIntent = new Intent(context, QuickCheckInActivity.class);
                checkInActionIntent.putExtra(QuickCheckInActivity.InitBundle.EPISODE_TVDBID,
                        upcomingEpisodes.getInt(NotificationQuery._ID));
                checkInActionIntent.putExtra(EXTRA_EPISODE_CLEARED_TIME, latestAirtime);
                checkInActionIntent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                PendingIntent checkInIntent = PendingIntent.getActivity(context,
                        REQUEST_CODE_ACTION_CHECKIN,
                        checkInActionIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                // icon only shown on Wear and 4.1 (API 16) to 6.0 (API 23)
                nb.addAction(R.drawable.ic_action_checkin, context.getString(R.string.checkin),
                        checkInIntent);

                // Action button to set watched
                Intent setWatchedIntent = new Intent(context, NotificationActionReceiver.class);
                setWatchedIntent.putExtra(EXTRA_EPISODE_TVDBID,
                        upcomingEpisodes.getInt(NotificationQuery._ID));
                // data to handle delete
                checkInActionIntent.putExtra(EXTRA_EPISODE_CLEARED_TIME, latestAirtime);
                PendingIntent setWatchedPendingIntent = PendingIntent.getBroadcast(context,
                        REQUEST_CODE_ACTION_SET_WATCHED,
                        setWatchedIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                // icon only shown on Wear and 4.1 (API 16) to 6.0 (API 23)
                nb.addAction(R.drawable.ic_action_tick, context.getString(R.string.action_watched),
                        setWatchedPendingIntent);

                nb.setNumber(1);
            } else {
                // multiple episodes
                NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

                // display at most the first five
                for (int displayIndex = 0; displayIndex < Math.min(count, 5); displayIndex++) {
                    if (!upcomingEpisodes.moveToPosition(notifyPositions.get(displayIndex))) {
                        // could not go to the desired position (testing just in case)
                        break;
                    }

                    final SpannableStringBuilder lineText = new SpannableStringBuilder();

                    // show title and number, like 'Show 1x01'
                    String title = TextTools.getShowWithEpisodeNumber(
                            context,
                            upcomingEpisodes.getString(NotificationQuery.SHOW_TITLE),
                            upcomingEpisodes.getInt(NotificationQuery.SEASON),
                            upcomingEpisodes.getInt(NotificationQuery.NUMBER)
                    );
                    lineText.append(title);
                    lineText.setSpan(new StyleSpan(Typeface.BOLD), 0, lineText.length(), 0);

                    lineText.append(" ");

                    // "8:00 PM Network"
                    String time = TimeTools.formatToLocalTime(context, TimeTools
                            .applyUserOffset(context, upcomingEpisodes
                                    .getLong(NotificationQuery.EPISODE_FIRST_RELEASE_MS)));
                    String network = upcomingEpisodes
                            .getString(NotificationQuery.NETWORK);
                    lineText.append(TextTools.dotSeparate(time, network)); // switch on purpose

                    inboxStyle.addLine(lineText);
                }

                // tell if we could not display all episodes
                if (count > 5) {
                    inboxStyle.setSummaryText(context.getString(R.string.more, count - 5));
                }

                nb.setStyle(inboxStyle);
                nb.setNumber(count);
            }
        } else {
            // ICS and below
            if (count == 1) {
                // single episode
                upcomingEpisodes.moveToPosition(notifyPositions.get(0));
                maybeSetPoster(nb, upcomingEpisodes.getString(NotificationQuery.POSTER));
            }
        }

        // notification sound
        final String ringtoneUri = NotificationSettings.getNotificationsRingtone(context);
        // If the string is empty, the user chose silent...
        boolean hasSound = ringtoneUri.length() != 0;
        if (hasSound) {
            // ...otherwise set the specified ringtone
            nb.setSound(Uri.parse(ringtoneUri));
        }
        // vibration
        boolean vibrates = NotificationSettings.isNotificationVibrating(context);
        if (vibrates) {
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
        nb.setColor(ContextCompat.getColor(context, R.color.accent_primary));
        nb.setPriority(NotificationCompat.PRIORITY_DEFAULT);

        Intent i = new Intent(context, NotificationActionReceiver.class);
        i.setAction(ACTION_CLEARED);
        i.putExtra(EXTRA_EPISODE_CLEARED_TIME, latestAirtime);
        PendingIntent deleteIntent = PendingIntent.getBroadcast(context, REQUEST_CODE_DELETE_INTENT,
                i,
                PendingIntent.FLAG_CANCEL_CURRENT);
        nb.setDeleteIntent(deleteIntent);

        // build the notification
        Notification notification = nb.build();

        // use a unique id within the app
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        nm.notify(SgApp.NOTIFICATION_EPISODE_ID, notification);

        Timber.d("Notification: count=%d, rich(JB+)=%s, sound=%s, vibrate=%s, delete=%s",
                count,
                richNotification ? "YES" : "NO",
                hasSound ? "YES" : "NO",
                vibrates ? "YES" : "NO",
                Instant.ofEpochMilli(latestAirtime));
    }

    private void maybeSetPoster(NotificationCompat.Builder nb, String posterPath) {
        try {
            Bitmap poster = ServiceUtils.loadWithPicasso(context,
                    TvdbImageTools.smallSizeUrl(posterPath))
                    .centerCrop()
                    .resizeDimen(R.dimen.show_poster_width, R.dimen.show_poster_height)
                    .get();
            nb.setLargeIcon(poster);

            // add special large resolution background for wearables
            // https://developer.android.com/training/wearables/notifications/creating.html#AddWearableFeatures
            Bitmap posterSquare = ServiceUtils.loadWithPicasso(context,
                    TvdbImageTools.fullSizeUrl(posterPath))
                    .centerCrop()
                    .resize(400, 400)
                    .get();
            NotificationCompat.WearableExtender wearableExtender =
                    new NotificationCompat.WearableExtender()
                            .setBackground(posterSquare);
            nb.extend(wearableExtender);
        } catch (IOException e) {
            Timber.e(e, "maybeSetPoster: failed.");
        }
    }
}
