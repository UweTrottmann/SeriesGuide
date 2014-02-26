/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.service;

import android.support.v4.app.TaskStackBuilder;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.NotificationSettings;
import com.battlelancer.seriesguide.ui.EpisodesActivity;
import com.battlelancer.seriesguide.ui.QuickCheckInActivity;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.Lists;
import com.battlelancer.seriesguide.R;

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

import java.util.List;
import timber.log.Timber;

public class NotificationService extends IntentService {

    private static final String KEY_EPISODE_CLEARED_TIME
            = "com.battlelancer.seriesguide.episode_cleared_time";

    private static final boolean DEBUG = false;

    private static final int REQUEST_CODE_DELETE_INTENT = 1;

    private static final int REQUEST_CODE_SINGLE_EPISODE = 2;

    private static final int REQUEST_CODE_MULTIPLE_EPISODES = 3;

    private static final int REQUEST_CODE_ACTION_CHECKIN = 4;

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

    // only within time frame, unwatched episodes
    private static final String SELECTION = Episodes.FIRSTAIREDMS + ">=?"
            + Episodes.SELECTION_NOWATCHED;

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

    public NotificationService() {
        super("Episode Notification Service");
        setIntentRedelivery(true);
    }

    @TargetApi(android.os.Build.VERSION_CODES.KITKAT)
    @Override
    protected void onHandleIntent(Intent intent) {
        Timber.d("Waking up...");
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        /*
         * Handle a possible delete intent.
         */
        if (handleDeleteIntent(this, intent)) {
            return;
        }

        /*
         * Unschedule notification service wake-ups for disabled notifications
         * and non-supporters.
         */
        if (!NotificationSettings.isNotificationsEnabled(this) || !Utils.hasAccessToX(this)) {
            Timber.d("Notification service disabled, removing wakup-up alarm");
            // cancel any pending alarm
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(this, OnAlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
            am.cancel(pi);

            resetLastEpisodeAirtime(prefs);

            return;
        }

        long wakeUpTime = 0;

        /*
         * Get pool of episodes which air from 12 hours ago until eternity which
         * match the users settings.
         */
        StringBuilder selection = new StringBuilder(SELECTION);
        boolean isFavsOnly = NotificationSettings.isNotifyAboutFavoritesOnly(this);
        Timber.d("Do notify about " + (isFavsOnly ? "favorites ONLY" : "ALL"));
        if (isFavsOnly) {
            selection.append(Shows.SELECTION_FAVORITES);
        }
        boolean isNoSpecials = DisplaySettings.isHidingSpecials(this);
        Timber.d("Do " + (isNoSpecials ? "NOT " : "") + "notify about specials");
        if (isNoSpecials) {
            selection.append(Episodes.SELECTION_NOSPECIALS);
        }

        final long customCurrentTime = TimeTools.getCurrentTime(this);
        final Cursor upcomingEpisodes = getContentResolver().query(Episodes.CONTENT_URI_WITHSHOW,
                PROJECTION, selection.toString(), new String[] {
                String.valueOf(customCurrentTime - 12 * DateUtils.HOUR_IN_MILLIS)
        }, SORTING);

        if (upcomingEpisodes != null) {
            int notificationThreshold = NotificationSettings.getLatestToIncludeTreshold(this);
            if (DEBUG) {
                Timber.d("DEBUG MODE: notification threshold is 1 week");
                // a week, for debugging (use only one show to get single
                // episode notifications)
                notificationThreshold = 10080;
                // notify again for same episodes
                resetLastEpisodeAirtime(prefs);
            }

            final long nextEpisodeReleaseTime = NotificationSettings.getNextToNotifyAbout(this);
            // wake user-defined amount of time earlier than next episode release time
            final long plannedWakeUpTime =
                    TimeTools.getEpisodeReleaseTime(this, nextEpisodeReleaseTime).getTime()
                            - DateUtils.MINUTE_IN_MILLIS * notificationThreshold;

            /*
             * Set to -1 as on first run nextTimePlanned will be 0. This assures
             * we still see notifications of upcoming episodes then.
             */
            int newEpisodesAvailable = -1;

            // Check if we did wake up earlier than planned
            if (System.currentTimeMillis() < plannedWakeUpTime) {
                Timber.d("Woke up earlier than planned, checking for new episodes");
                newEpisodesAvailable = 0;
                long latestTimeNotified = NotificationSettings.getLastNotified(this);

                // Check if there are any earlier episodes to notify about
                while (upcomingEpisodes.moveToNext()) {
                    final long releaseTime = upcomingEpisodes.getLong(
                            NotificationQuery.EPISODE_FIRST_RELEASE_MS);
                    if (releaseTime < nextEpisodeReleaseTime) {
                        if (releaseTime > latestTimeNotified) {
                            /**
                             * This will not get new episodes which would have
                             * aired the same time as the last one we notified
                             * about. Sad, but the best we can do right now.
                             */
                            newEpisodesAvailable = 1;
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }

            if (newEpisodesAvailable == 0) {
                // Go to sleep, wake up as planned
                Timber.d("No new episodes, going to sleep.");
                wakeUpTime = plannedWakeUpTime;
            } else {
                // Get episodes which are within the notification threshold
                // (user set) and not yet cleared
                final List<Integer> notifyPositions = Lists.newArrayList();
                final long latestTimeCleared = NotificationSettings.getLastCleared(this);
                final long latestTimeToInclude = customCurrentTime
                        + DateUtils.MINUTE_IN_MILLIS * notificationThreshold;

                int position = -1;
                upcomingEpisodes.moveToPosition(position);
                while (upcomingEpisodes.moveToNext()) {
                    position++;

                    final long releaseTime = upcomingEpisodes.getLong(
                            NotificationQuery.EPISODE_FIRST_RELEASE_MS);
                    if (releaseTime <= latestTimeToInclude) {
                        /*
                         * Only add those after the last one the user cleared.
                         * At most those of the last 24 hours (see query above).
                         */
                        if (releaseTime > latestTimeCleared) {
                            notifyPositions.add(position);
                        }
                    } else {
                        // Too far into the future, stop!
                        break;
                    }
                }

                // Notify if we found any episodes
                if (notifyPositions.size() > 0) {
                    // store latest air time of all episodes we notified about
                    upcomingEpisodes
                            .moveToPosition(notifyPositions.get(notifyPositions.size() - 1));
                    long latestAirtime = upcomingEpisodes.getLong(
                            NotificationQuery.EPISODE_FIRST_RELEASE_MS);
                    if (!AndroidUtils.isHoneycombOrHigher()) {
                        /*
                         * Everything below HC does not have delete intents, so
                         * we just never notify about the same episode twice.
                         */
                        Timber.d("Delete intent NOT supported, setting last cleared to: "
                                + latestAirtime);
                        prefs.edit().putLong(NotificationSettings.KEY_LAST_CLEARED,
                                latestAirtime).commit();
                    }
                    Timber.d("Found " + notifyPositions.size()
                            + " new episodes, setting last notified to: " + latestAirtime);
                    prefs.edit().putLong(NotificationSettings.KEY_LAST_NOTIFIED, latestAirtime)
                            .commit();

                    onNotify(upcomingEpisodes, notifyPositions, latestAirtime);
                }

                /*
                 * Plan next episode to notify about, calc wake-up alarm as
                 * early as user wants.
                 */
                upcomingEpisodes.moveToPosition(-1);
                while (upcomingEpisodes.moveToNext()) {
                    final long releaseTime = upcomingEpisodes.getLong(
                            NotificationQuery.EPISODE_FIRST_RELEASE_MS);
                    if (releaseTime > latestTimeToInclude) {
                        // store next episode we plan to notify about
                        Timber.d("Storing next episode time to notify about: " + releaseTime);
                        prefs.edit().putLong(NotificationSettings.KEY_NEXT_TO_NOTIFY, releaseTime)
                                .commit();

                        // calc actual wake up time
                        wakeUpTime = TimeTools.getEpisodeReleaseTime(this, releaseTime).getTime()
                                - DateUtils.MINUTE_IN_MILLIS * notificationThreshold;

                        break;
                    }
                }
            }

            upcomingEpisodes.close();
        }

        // Set a default wake-up time if there are no future episodes for now
        if (wakeUpTime <= 0) {
            wakeUpTime = System.currentTimeMillis() + 6 * DateUtils.HOUR_IN_MILLIS;
            Timber.d("No future episodes found, wake up in 6 hours");
        }

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(this, NotificationService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        Timber.d("Going to sleep, setting wake-up alarm to: " + wakeUpTime);
        if (AndroidUtils.isKitKatOrHigher()) {
            am.setExact(AlarmManager.RTC_WAKEUP, wakeUpTime, pi);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, wakeUpTime, pi);
        }
    }

    /**
     * Extracts the last cleared time and stores it in settings.
     */
    public static boolean handleDeleteIntent(Context context, Intent intent) {
        long clearedTime = intent.getLongExtra(KEY_EPISODE_CLEARED_TIME, 0);
        if (clearedTime != 0) {
            // Never show the cleared episode(s) again
            Timber.d("Notification cleared, setting last cleared episode time: " + clearedTime);
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putLong(NotificationSettings.KEY_LAST_CLEARED, clearedTime)
                    .commit();
            return true;
        }
        return false;
    }

    /**
     * Resets the air time of the last notified about episode. Afterwards notifications for
     * episodes
     * may appear, which were already notified about.
     */
    public static void resetLastEpisodeAirtime(final SharedPreferences prefs) {
        Timber.d("Resetting last cleared and last notified");
        prefs.edit().putLong(NotificationSettings.KEY_LAST_CLEARED, 0)
                .commit();
        prefs.edit().putLong(NotificationSettings.KEY_LAST_NOTIFIED, 0).commit();
    }

    private void onNotify(final Cursor upcomingEpisodes, List<Integer> notifyPositions,
            long latestAirtime) {
        final Context context = getApplicationContext();

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
            Timber.d("Notifying about 1 new episode");
            upcomingEpisodes.moveToPosition(notifyPositions.get(0));

            final String showTitle = upcomingEpisodes.getString(NotificationQuery.SHOW_TITLE);
            tickerText = getString(R.string.upcoming_show, showTitle);
            contentTitle = showTitle
                    + " "
                    + Utils.getEpisodeNumber(
                    this,
                    upcomingEpisodes.getInt(NotificationQuery.SEASON),
                    upcomingEpisodes.getInt(NotificationQuery.NUMBER));

            // "8:00 PM on Network"
            final String releaseTime = TimeTools.formatToLocalReleaseTime(this, TimeTools
                    .getEpisodeReleaseTime(this,
                            upcomingEpisodes.getLong(NotificationQuery.EPISODE_FIRST_RELEASE_MS)));
            final String network = upcomingEpisodes.getString(NotificationQuery.NETWORK);
            contentText = getString(R.string.upcoming_show_detailed, releaseTime, network);

            Intent episodeDetailsIntent = new Intent(context, EpisodesActivity.class);
            episodeDetailsIntent.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID,
                    upcomingEpisodes.getInt(NotificationQuery._ID));
            episodeDetailsIntent.putExtra(KEY_EPISODE_CLEARED_TIME, latestAirtime);

            contentIntent = TaskStackBuilder.create(context)
                    .addNextIntent(showsIntent)
                    .addNextIntent(episodeDetailsIntent)
                    .getPendingIntent(REQUEST_CODE_SINGLE_EPISODE, PendingIntent.FLAG_ONE_SHOT);
        } else {
            // notify about multiple episodes
            Timber.d("Notifying about " + count + " new episodes");
            tickerText = getString(R.string.upcoming_episodes);
            contentTitle = getString(R.string.upcoming_episodes_number, count);
            contentText = getString(R.string.upcoming_display);

            contentIntent = TaskStackBuilder.create(context)
                    .addNextIntent(showsIntent.putExtra(KEY_EPISODE_CLEARED_TIME, latestAirtime))
                    .getPendingIntent(REQUEST_CODE_MULTIPLE_EPISODES, 0);
        }

        final NotificationCompat.Builder nb = new NotificationCompat.Builder(context);

        if (AndroidUtils.isJellyBeanOrHigher()) {
            Timber.d("Building rich notification (JB+)");
            // JELLY BEAN and above
            if (count == 1) {
                // single episode
                upcomingEpisodes.moveToPosition(notifyPositions.get(0));
                final String imagePath = upcomingEpisodes
                        .getString(NotificationQuery.POSTER);
                nb.setLargeIcon(ImageProvider.getInstance(context)
                        .getImage(imagePath, true));

                final String episodeTitle = upcomingEpisodes
                        .getString(NotificationQuery.TITLE);
                final String episodeSummary = upcomingEpisodes
                        .getString(NotificationQuery.OVERVIEW);

                final SpannableStringBuilder bigText = new SpannableStringBuilder();
                bigText.append(episodeTitle);
                bigText.setSpan(new ForegroundColorSpan(Color.WHITE), 0, bigText.length(),
                        0);
                bigText.append("\n");
                bigText.append(episodeSummary);

                nb.setStyle(new NotificationCompat.BigTextStyle().bigText(bigText)
                        .setSummaryText(contentText));

                // Action button to check in
                Intent checkInActionIntent = new Intent(context, QuickCheckInActivity.class);
                checkInActionIntent.putExtra(QuickCheckInActivity.InitBundle.EPISODE_TVDBID,
                        upcomingEpisodes.getInt(NotificationQuery._ID));
                checkInActionIntent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                PendingIntent checkInIntent = PendingIntent.getActivity(context,
                        REQUEST_CODE_ACTION_CHECKIN,
                        checkInActionIntent, 0);
                nb.addAction(R.drawable.ic_action_checkin, getString(R.string.checkin),
                        checkInIntent);
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

                    // show title
                    lineText.append(upcomingEpisodes.getString(NotificationQuery.SHOW_TITLE));
                    lineText.setSpan(new ForegroundColorSpan(Color.WHITE), 0, lineText.length(), 0);

                    lineText.append(" ");

                    // "8:00 PM on Network"
                    String releaseTime = TimeTools.formatToLocalReleaseTime(this, TimeTools
                            .getEpisodeReleaseTime(this, upcomingEpisodes
                                    .getLong(NotificationQuery.EPISODE_FIRST_RELEASE_MS)));
                    String network = upcomingEpisodes
                            .getString(NotificationQuery.NETWORK);
                    lineText.append(getString(R.string.upcoming_show_detailed, releaseTime,
                            network));

                    inboxStyle.addLine(lineText);
                }

                // tell if we could not display all episodes
                if (count > 5) {
                    inboxStyle.setSummaryText(getString(R.string.more, count - 5));
                }

                nb.setStyle(inboxStyle);
                nb.setContentInfo(String.valueOf(count));
            }
        } else {
            // ICS and below
            if (count == 1) {
                // single episode
                upcomingEpisodes.moveToPosition(notifyPositions.get(0));
                final String posterPath = upcomingEpisodes.getString(NotificationQuery.POSTER);
                nb.setLargeIcon(ImageProvider.getInstance(context).getImage(posterPath, true));
            }
        }

        // notification sound
        final String ringtoneUri = NotificationSettings.getNotificationsRingtone(context);
        // If the string is empty, the user chose silent...
        if (ringtoneUri.length() != 0) {
            // ...otherwise set the specified ringtone
            Timber.d("Notification has sound");
            nb.setSound(Uri.parse(ringtoneUri));
        }
        // vibration
        if (NotificationSettings.isNotificationVibrating(context)) {
            Timber.d("Notification vibrates");
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
        nb.setPriority(NotificationCompat.PRIORITY_DEFAULT);

        Timber.d("Setting delete intent with episode time: " + latestAirtime);
        Intent i = new Intent(this, NotificationService.class);
        i.putExtra(KEY_EPISODE_CLEARED_TIME, latestAirtime);
        PendingIntent deleteIntent = PendingIntent.getService(this, REQUEST_CODE_DELETE_INTENT, i,
                PendingIntent.FLAG_UPDATE_CURRENT);
        nb.setDeleteIntent(deleteIntent);

        // build the notification
        Notification notification = nb.build();

        // use string resource id, always unique within app
        final NotificationManager nm = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        nm.notify(R.string.upcoming_show, notification);
    }
}
