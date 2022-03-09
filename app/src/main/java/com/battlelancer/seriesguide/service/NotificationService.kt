package com.battlelancer.seriesguide.service

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.format.DateUtils
import android.text.style.StyleSpan
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import androidx.sqlite.db.SimpleSQLiteQuery
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase
import com.battlelancer.seriesguide.provider.SgEpisode2WithShow
import com.battlelancer.seriesguide.provider.SgRoomDatabase.Companion.getInstance
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.settings.DisplaySettings.isHidingSpecials
import com.battlelancer.seriesguide.settings.NotificationSettings
import com.battlelancer.seriesguide.traktapi.QuickCheckInActivity
import com.battlelancer.seriesguide.ui.ShowsActivity
import com.battlelancer.seriesguide.ui.episodes.EpisodesActivity.Companion.intentEpisode
import com.battlelancer.seriesguide.util.ImageTools.tmdbOrTvdbPosterUrl
import com.battlelancer.seriesguide.util.PendingIntentCompat
import com.battlelancer.seriesguide.util.ServiceUtils
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.util.Utils
import com.uwetrottmann.androidutils.AndroidUtils
import org.threeten.bp.Instant
import timber.log.Timber
import java.io.IOException
import java.text.NumberFormat
import java.util.concurrent.Executor

/**
 * To debug set [.DEBUG] to true.
 *
 * Useful commands:
 *
 * Dump alarms:
 *
 * `adb shell dumpsys alarm > alarms.txt`
 *
 * Doze mode:
 * ```
 * adb shell dumpsys deviceidle force-idle
 *
 * adb shell dumpsys deviceidle unforce
 * ```
 *
 * App Standby:
 * ```
 * adb shell dumpsys battery unplug
 * adb shell am set-inactive com.battlelancer.seriesguide true
 *
 * adb shell am set-inactive com.battlelancer.seriesguide false
 *
 * adb shell am get-inactive com.battlelancer.seriesguide
 * ```
 *
 * https://developer.android.com/training/monitoring-device-state/doze-standby.html
 */
class NotificationService(context: Context) {

    private val context = context.applicationContext

    private val wakeUpPendingIntent: PendingIntent
        get() = PendingIntent.getBroadcast(
            context,
            0,
            NotificationAlarmReceiver.intent(context),
            // Mutable because used to schedule alarm.
            PendingIntentCompat.flagMutable
        )

    fun run() {
        Timber.d("Waking up...")

        // remove notification service wake-up alarm if notifications are disabled or not unlocked
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (!NotificationSettings.isNotificationsEnabled(context) || !Utils.hasAccessToX(context)) {
            Timber.d("Notifications disabled, removing wake-up alarm")
            val am = context.getSystemService<AlarmManager>()
            am?.cancel(wakeUpPendingIntent)
            resetLastEpisodeAirtime(prefs)
            return
        }

        val customCurrentTime = TimeTools.getCurrentTime(context)
        val notificationThreshold: Int
        if (!DEBUG) {
            notificationThreshold = NotificationSettings.getLatestToIncludeTreshold(context)
        } else {
            Timber.d("DEBUG MODE: always notify about next episode within 1 week")
            // a week, for debugging (use only one show to get single episode notifications)
            notificationThreshold = 10080
            // notify again for same episodes
            resetLastEpisodeAirtime(prefs)
        }
        val upcomingEpisodes = queryUpcomingEpisodes(customCurrentTime)

        val nextEpisodeReleaseTime = NotificationSettings.getNextToNotifyAbout(context)
        // wake user-defined amount of time earlier than next episode release time
        val plannedWakeUpTime = (TimeTools.applyUserOffset(context, nextEpisodeReleaseTime).time
                - DateUtils.MINUTE_IN_MILLIS * notificationThreshold)

        // note: on first run plannedWakeUpTime will be <= 0
        var checkForNewEpisodes = true

        if (System.currentTimeMillis() < plannedWakeUpTime) {
            Timber.d("Woke up earlier than planned, checking for new episodes")
            checkForNewEpisodes = false
            val releaseTimeLastNotified = NotificationSettings.getLastNotifiedAbout(context)

            for (episode in upcomingEpisodes) {
                val releaseTime = episode.episode_firstairedms
                // any episodes added that release before the next one planned to notify about?
                if (releaseTime < nextEpisodeReleaseTime) {
                    // limit to those released after the episode we last notified about to avoid
                    // notifying about an episode we already notified about
                    // limitation: so if added episodes release at or before that last episode
                    // they will not be notified about
                    if (releaseTime > releaseTimeLastNotified) {
                        checkForNewEpisodes = true
                        break
                    }
                } else {
                    break
                }
            }
        }

        var nextWakeUpTime: Long = 0
        if (checkForNewEpisodes) {
            val latestTimeToInclude = (customCurrentTime
                    + DateUtils.MINUTE_IN_MILLIS * notificationThreshold)

            maybeNotify(prefs, upcomingEpisodes, latestTimeToInclude)

            // plan next episode to notify about
            for (episode in upcomingEpisodes) {
                val releaseTime = episode.episode_firstairedms
                if (releaseTime > latestTimeToInclude) {
                    prefs.edit()
                        .putLong(NotificationSettings.KEY_NEXT_TO_NOTIFY, releaseTime)
                        .apply()
                    Timber.d(
                        "Next notification planned for episode released at: %s",
                        Instant.ofEpochMilli(releaseTime)
                    )

                    // calc wake up time to notify about this episode
                    // taking into account time offset and notification threshold
                    nextWakeUpTime = (TimeTools.applyUserOffset(context, releaseTime).time
                            - DateUtils.MINUTE_IN_MILLIS * notificationThreshold)
                    break
                }
            }
        } else {
            // Go to sleep, wake up as planned
            Timber.d("No new episodes")
            nextWakeUpTime = plannedWakeUpTime
        }

        // Set a default wake-up time if there are no future episodes for now
        if (nextWakeUpTime <= 0) {
            nextWakeUpTime = System.currentTimeMillis() + 6 * DateUtils.HOUR_IN_MILLIS
            Timber.d("No future episodes found, wake up in 6 hours")
        }

        if (DEBUG) {
            Timber.d("DEBUG MODE: wake up in 1 minute")
            nextWakeUpTime = System.currentTimeMillis() + DateUtils.MINUTE_IN_MILLIS
        }

        val am = context.getSystemService<AlarmManager>()
        val pi = wakeUpPendingIntent
        Timber.d(
            "Going to sleep, setting wake-up alarm to: %s",
            Instant.ofEpochMilli(nextWakeUpTime)
        )
        if (am != null) {
            if (AndroidUtils.isMarshmallowOrHigher) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextWakeUpTime, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, nextWakeUpTime, pi)
            }
        }
    }

    /**
     * Get episodes which released 12 hours ago until in 14 days (to avoid
     * loading too much data), excludes some episodes based on user settings.
     */
    private fun queryUpcomingEpisodes(customCurrentTime: Long): List<SgEpisode2WithShow> {
        val selection = StringBuilder(SELECTION)

        // Should be able to return at least 1 episode where release time is later
        // than latestTimeToInclude. That can be up to 7 days. But also as small as a few minutes.
        // To avoid waking up too often if there are no episodes for a few days,
        // look ahead up to 14 days.
        val bindArgs = arrayOf<Any>(
            customCurrentTime - 12 * DateUtils.HOUR_IN_MILLIS,
            customCurrentTime + 14 * DateUtils.DAY_IN_MILLIS
        )

        val isNoSpecials = isHidingSpecials(context)
        Timber.d("Settings: specials: %s", if (isNoSpecials) "YES" else "NO")
        if (isNoSpecials) {
            selection.append(" AND ").append(SgEpisode2Columns.SELECTION_NO_SPECIALS)
        }
        if (NotificationSettings.isIgnoreHiddenShows(context)) {
            selection.append(" AND ").append(SgShow2Columns.SELECTION_NO_HIDDEN)
        }
        if (NotificationSettings.isOnlyNextEpisodes(context)) {
            selection.append(" AND (")
                .append(SgShow2Columns.NEXTEPISODE + "=''")
                .append(" OR ")
                .append(SgShow2Columns.NEXTEPISODE + "=" + SeriesGuideDatabase.Qualified.SG_EPISODE_ID)
                .append(")")
        }

        val query = (SgEpisode2WithShow.SELECT
                + " WHERE " + selection
                + " ORDER BY " + ORDER)

        return getInstance(context).sgEpisode2Helper()
            .getEpisodesWithShow(SimpleSQLiteQuery(query, bindArgs))
    }

    private fun maybeNotify(
        prefs: SharedPreferences,
        upcomingEpisodes: List<SgEpisode2WithShow>,
        latestTimeToInclude: Long
    ) {
        val notifyPositions: MutableList<Int> = ArrayList()
        val latestTimeCleared = NotificationSettings.getLastCleared(context)

        for (i in upcomingEpisodes.indices) {
            // get episodes which are within the notification threshold (user set)...
            val releaseTime = upcomingEpisodes[i].episode_firstairedms
            if (releaseTime <= latestTimeToInclude) {
                // ...and released after the last one the user cleared.
                // Note: should be at most those of the last few hours (see cursor query).
                if (releaseTime > latestTimeCleared) {
                    notifyPositions.add(i)
                }
            } else {
                // Too far into the future, stop!
                break
            }
        }

        // Notify if we found any episodes, store latest release time we notify about
        if (notifyPositions.size > 0) {
            val latestAirtime = upcomingEpisodes[notifyPositions[notifyPositions.size - 1]]
                .episode_firstairedms
            prefs.edit().putLong(NotificationSettings.KEY_LAST_NOTIFIED, latestAirtime).apply()
            Timber.d(
                "Notify about %d episodes, latest released at: %s",
                notifyPositions.size, Instant.ofEpochMilli(latestAirtime)
            )
            notifyAbout(upcomingEpisodes, notifyPositions, latestAirtime)
        }
    }

    private fun notifyAbout(
        upcomingEpisodes: List<SgEpisode2WithShow>,
        notifyPositions: List<Int>,
        latestAirtime: Long
    ) {
        val contentTitle: CharSequence
        val contentText: CharSequence
        val contentIntent: PendingIntent
        // base intent for task stack
        val showsIntent = Intent(context, ShowsActivity::class.java)
        showsIntent.putExtra(
            ShowsActivity.EXTRA_SELECTED_TAB,
            ShowsActivity.INDEX_TAB_UPCOMING
        )

        val count = notifyPositions.size
        if (count == 1) {
            // notify in detail about one episode
            val (id, _, episodenumber, season, episode_firstairedms, _, _, _, showTitle, network) = upcomingEpisodes[notifyPositions[0]]

            // show title and number, like 'Show 1x01'
            contentTitle = TextTools.getShowWithEpisodeNumber(
                context,
                showTitle,
                season,
                episodenumber
            )

            // "8:00 PM Network"
            val time = TimeTools.formatToLocalTime(
                context,
                TimeTools.applyUserOffset(context, episode_firstairedms)
            )
            contentText = TextTools.dotSeparate(time, network) // switch on purpose

            val episodeDetailsIntent = intentEpisode(id, context)
            episodeDetailsIntent.putExtra(EXTRA_EPISODE_CLEARED_TIME, latestAirtime)

            contentIntent = TaskStackBuilder.create(context)
                .addNextIntent(showsIntent)
                .addNextIntent(episodeDetailsIntent)
                .getPendingIntent(
                    REQUEST_CODE_SINGLE_EPISODE,
                    PendingIntentCompat.flagImmutable or PendingIntent.FLAG_CANCEL_CURRENT
                )!!
        } else {
            // notify about multiple episodes
            contentTitle = context.getString(
                R.string.upcoming_episodes_number,
                NumberFormat.getIntegerInstance().format(count.toLong())
            )
            contentText = context.getString(R.string.upcoming_display)

            contentIntent = TaskStackBuilder.create(context)
                .addNextIntent(showsIntent.putExtra(EXTRA_EPISODE_CLEARED_TIME, latestAirtime))
                .getPendingIntent(
                    REQUEST_CODE_MULTIPLE_EPISODES,
                    PendingIntentCompat.flagImmutable or PendingIntent.FLAG_CANCEL_CURRENT
                )!!
        }

        val nb = NotificationCompat.Builder(context, SgApp.NOTIFICATION_CHANNEL_EPISODES)

        // JELLY BEAN and above
        if (count == 1) {
            // single episode
            val (id, episodetitle, episodenumber, _, _, _, _, episodeSummary, _, _, series_poster_small) = upcomingEpisodes[notifyPositions[0]]
            maybeSetPoster(nb, series_poster_small)

            if (!DisplaySettings.preventSpoilers(context)) {
                val episodeTitle = TextTools.getEpisodeTitle(
                    context,
                    episodetitle,
                    episodenumber
                )

                val bigText = SpannableStringBuilder()
                bigText.append(episodeTitle)
                bigText.setSpan(StyleSpan(Typeface.BOLD), 0, bigText.length, 0)
                if (!TextUtils.isEmpty(episodeSummary)) {
                    bigText.append("\n").append(episodeSummary)
                }

                nb.setStyle(
                    NotificationCompat.BigTextStyle().bigText(bigText)
                        .setSummaryText(contentText)
                )
            }

            // Action button to check in
            val checkInActionIntent = QuickCheckInActivity.intent(id, context)
            checkInActionIntent.putExtra(EXTRA_EPISODE_CLEARED_TIME, latestAirtime)
            val checkInIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE_ACTION_CHECKIN,
                checkInActionIntent,
                PendingIntentCompat.flagImmutable or PendingIntent.FLAG_CANCEL_CURRENT
            )
            // icon only shown on Wear and 4.1 (API 16) to 6.0 (API 23)
            // note: Wear and Galaxy Watch devices do typically not support vector icons
            nb.addAction(
                R.drawable.ic_action_checkin, context.getString(R.string.checkin),
                checkInIntent
            )

            // Action button to set watched
            val setWatchedIntent = NotificationActionReceiver.intent(id, context)
            // data to handle delete
            checkInActionIntent.putExtra(EXTRA_EPISODE_CLEARED_TIME, latestAirtime)
            val setWatchedPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_ACTION_SET_WATCHED,
                setWatchedIntent,
                PendingIntentCompat.flagImmutable or PendingIntent.FLAG_CANCEL_CURRENT
            )
            // icon only shown on Wear and 4.1 (API 16) to 6.0 (API 23)
            // note: Wear and Galaxy Watch devices do typically not support vector icons
            nb.addAction(
                R.drawable.ic_action_tick, context.getString(R.string.action_watched),
                setWatchedPendingIntent
            )
            nb.setNumber(1)
        } else {
            // multiple episodes
            val inboxStyle = NotificationCompat.InboxStyle()

            // display at most the first five
            for (displayIndex in 0 until count.coerceAtMost(5)) {
                val (_, _, episodenumber, season, episode_firstairedms, _, _, _, seriestitle, network) = upcomingEpisodes[notifyPositions[displayIndex]]

                val lineText = SpannableStringBuilder()

                // show title and number, like 'Show 1x01'
                val title = TextTools.getShowWithEpisodeNumber(
                    context,
                    seriestitle,
                    season,
                    episodenumber
                )
                lineText.append(title)
                lineText.setSpan(StyleSpan(Typeface.BOLD), 0, lineText.length, 0)

                lineText.append(" ")

                // "8:00 PM Network", switch on purpose
                val time = TimeTools.formatToLocalTime(
                    context, TimeTools.applyUserOffset(context, episode_firstairedms)
                )
                lineText.append(TextTools.dotSeparate(time, network))

                inboxStyle.addLine(lineText)
            }

            // tell if we could not display all episodes
            if (count > 5) {
                inboxStyle.setSummaryText(context.getString(R.string.more, count - 5))
            }

            nb.setStyle(inboxStyle)
            nb.setNumber(count)
        }

        // notification sound
        val ringtoneUri = NotificationSettings.getNotificationsRingtone(context)
        // If the string is empty, the user chose silent...
        val hasSound = ringtoneUri.isNotEmpty()
        if (hasSound) {
            // ...otherwise set the specified ringtone
            nb.setSound(Uri.parse(ringtoneUri))
        }
        // vibration
        val vibrates = NotificationSettings.isNotificationVibrating(context)
        if (vibrates) {
            nb.setVibrate(VIBRATION_PATTERN)
        }
        nb.setDefaults(Notification.DEFAULT_LIGHTS)
        nb.setWhen(System.currentTimeMillis())
        nb.setAutoCancel(true)
        nb.setContentTitle(contentTitle)
        nb.setContentText(contentText)
        nb.setContentIntent(contentIntent)
        nb.setSmallIcon(R.drawable.ic_notification)
        nb.color = ContextCompat.getColor(context, R.color.sg_color_primary)
        nb.priority = NotificationCompat.PRIORITY_DEFAULT

        val i = Intent(context, NotificationActionReceiver::class.java)
        i.action = ACTION_CLEARED
        i.putExtra(EXTRA_EPISODE_CLEARED_TIME, latestAirtime)
        val deleteIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE_DELETE_INTENT,
            i,
            PendingIntentCompat.flagImmutable or PendingIntent.FLAG_CANCEL_CURRENT
        )
        nb.setDeleteIntent(deleteIntent)

        // build the notification
        val notification = nb.build()

        // use a unique id within the app
        val nm = NotificationManagerCompat.from(context)
        nm.notify(SgApp.NOTIFICATION_EPISODE_ID, notification)

        Timber.d(
            "Notification: count=%d, sound=%s, vibrate=%s, delete=%s",
            count,
            if (hasSound) "YES" else "NO",
            if (vibrates) "YES" else "NO",
            Instant.ofEpochMilli(latestAirtime)
        )
    }

    private fun maybeSetPoster(nb: NotificationCompat.Builder, posterPath: String?) {
        try {
            val poster = ServiceUtils.loadWithPicasso(
                context,
                tmdbOrTvdbPosterUrl(posterPath, context, false)
            )
                .centerCrop()
                .resizeDimen(R.dimen.show_poster_width, R.dimen.show_poster_height)
                .get()
            nb.setLargeIcon(poster)

            // add special large resolution background for wearables
            // https://developer.android.com/training/wearables/notifications/creating.html#AddWearableFeatures
            val posterSquare = ServiceUtils.loadWithPicasso(
                context,
                tmdbOrTvdbPosterUrl(posterPath, context, false)
            )
                .centerCrop()
                .resize(400, 400)
                .get()
            val wearableExtender = NotificationCompat.WearableExtender()
                .setBackground(posterSquare)
            nb.extend(wearableExtender)
        } catch (e: IOException) {
            Timber.e(e, "maybeSetPoster: failed.")
        }
    }

    companion object {
        const val ACTION_CLEARED = "seriesguide.intent.action.CLEARED"
        private const val EXTRA_EPISODE_CLEARED_TIME =
            "com.battlelancer.seriesguide.episode_cleared_time"

        private const val DEBUG = false

        private const val REQUEST_CODE_DELETE_INTENT = 1
        private const val REQUEST_CODE_SINGLE_EPISODE = 2
        private const val REQUEST_CODE_MULTIPLE_EPISODES = 3
        private const val REQUEST_CODE_ACTION_CHECKIN = 4
        private const val REQUEST_CODE_ACTION_SET_WATCHED = 4

        val VIBRATION_PATTERN = longArrayOf(
            0, 100, 200, 100, 100, 100
        )

        // only if notifications are on: unwatched episodes released on or after arg
        private const val SELECTION = (SgShow2Columns.NOTIFY + " = 1 AND "
                + SgEpisode2Columns.SELECTION_UNWATCHED + " AND "
                + SgEpisode2Columns.FIRSTAIREDMS + ">= ? AND "
                + SgEpisode2Columns.FIRSTAIREDMS + "< ?")

        // by airdate, then by show, then lowest number first
        private const val ORDER = (SgEpisode2Columns.FIRSTAIREDMS + " ASC,"
                + SgShow2Columns.SORT_TITLE + ","
                + SgEpisode2Columns.NUMBER + " ASC")

        val SERIAL_EXECUTOR: Executor = SerialExecutor()

        /**
         * Send broadcast to run the notification service to display and (re)schedule upcoming episode
         * alarms.
         */
        fun trigger(context: Context) {
            context.sendBroadcast(NotificationAlarmReceiver.intent(context))
        }

        /**
         * Extracts the last cleared time and stores it in settings.
         */
        fun handleDeleteIntent(context: Context, intent: Intent?) {
            if (intent == null) {
                return
            }
            val clearedTime = intent.getLongExtra(EXTRA_EPISODE_CLEARED_TIME, 0)
            if (clearedTime != 0L) {
                // Never show the cleared episode(s) again
                Timber.d("Notification cleared, setting last cleared episode time: %d", clearedTime)
                PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putLong(NotificationSettings.KEY_LAST_CLEARED, clearedTime)
                    .apply()
            }
        }

        /**
         * Resets the air time of the last notified about episode. Afterwards notifications for episodes
         * may appear, which were already notified about.
         */
        fun resetLastEpisodeAirtime(prefs: SharedPreferences) {
            Timber.d("Resetting last cleared and last notified")
            prefs.edit()
                .putLong(NotificationSettings.KEY_LAST_CLEARED, 0)
                .putLong(NotificationSettings.KEY_LAST_NOTIFIED, 0)
                .apply()
        }
    }
}