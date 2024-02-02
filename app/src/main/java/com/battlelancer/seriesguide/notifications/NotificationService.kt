// SPDX-License-Identifier: Apache-2.0
// Copyright 2012-2023 Uwe Trottmann

package com.battlelancer.seriesguide.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.format.DateUtils
import android.text.style.StyleSpan
import androidx.annotation.VisibleForTesting
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
import com.battlelancer.seriesguide.provider.SgRoomDatabase.Companion.getInstance
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.settings.DisplaySettings.isHidingSpecials
import com.battlelancer.seriesguide.settings.NotificationSettings
import com.battlelancer.seriesguide.shows.ShowsActivityImpl
import com.battlelancer.seriesguide.shows.database.SgEpisode2WithShow
import com.battlelancer.seriesguide.shows.episodes.EpisodesActivity.Companion.intentEpisode
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.traktapi.QuickCheckInActivity
import com.battlelancer.seriesguide.ui.ShowsActivity
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.ImageTools.tmdbOrTvdbPosterUrl
import com.battlelancer.seriesguide.util.PendingIntentCompat
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.util.Utils
import com.uwetrottmann.androidutils.AndroidUtils
import org.threeten.bp.Instant
import timber.log.Timber
import java.io.IOException
import java.text.NumberFormat

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
        if (!NotificationSettings.isNotificationsEnabled(context) || !Utils.hasAccessToX(context)) {
            Timber.d("Notifications disabled, removing wake-up alarm")
            val am = context.getSystemService<AlarmManager>()
            am?.cancel(wakeUpPendingIntent)
            NotificationSettings.resetLastEpisodeAirtime(context)
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
            NotificationSettings.resetLastEpisodeAirtime(context)
        }
        val upcomingEpisodes = queryUpcomingEpisodes(customCurrentTime)

        val nextEpisodeReleaseTime = NotificationSettings.getNextToNotifyAbout(context)
        // wake user-defined amount of time earlier than next episode release time
        val plannedWakeUpTime = (TimeTools.applyUserOffset(context, nextEpisodeReleaseTime).time
                - DateUtils.MINUTE_IN_MILLIS * notificationThreshold)

        val checkForNewEpisodes =
            shouldCheckToNotify(plannedWakeUpTime, nextEpisodeReleaseTime, upcomingEpisodes)

        var nextWakeUpTime: Long = 0
        var needExactAlarm = true
        if (checkForNewEpisodes) {
            val latestTimeToInclude = (customCurrentTime
                    + DateUtils.MINUTE_IN_MILLIS * notificationThreshold)

            maybeNotify(upcomingEpisodes, latestTimeToInclude)

            // plan next episode to notify about
            for (episode in upcomingEpisodes) {
                val releaseTime = episode.episode_firstairedms
                if (releaseTime > latestTimeToInclude) {
                    PreferenceManager.getDefaultSharedPreferences(context).edit()
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

        if (nextWakeUpTime <= 0) {
            // Set a default wake-up time if there are currently no future episodes,
            // schedule after the default sync interval as sync likely wakes this up already.
            nextWakeUpTime = System.currentTimeMillis() +
                    SgSyncAdapter.SYNC_INTERVAL_SECONDS * DateUtils.SECOND_IN_MILLIS
            needExactAlarm = false
            Timber.d("No future episodes found, wake up after next sync")
        }

        if (DEBUG) {
            Timber.d("DEBUG MODE: wake up in 1 minute")
            nextWakeUpTime = System.currentTimeMillis() + DateUtils.MINUTE_IN_MILLIS
            needExactAlarm = true
        }

        val am = context.getSystemService<AlarmManager>()
        val pi = wakeUpPendingIntent
        Timber.d(
            "Setting alarm: exact=%s time=%s",
            needExactAlarm,
            Instant.ofEpochMilli(nextWakeUpTime)
        )
        if (am != null) {
            if (needExactAlarm) {
                if (AndroidUtils.isMarshmallowOrHigher) {
                    if (am.canScheduleExactAlarmsCompat()) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextWakeUpTime, pi)
                    } else {
                        // At least trigger while idle so the notification might still be timely.
                        Timber.d("Not allowed to set exact alarm")
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextWakeUpTime, pi)
                    }
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, nextWakeUpTime, pi)
                }
            } else {
                am.set(AlarmManager.RTC_WAKEUP, nextWakeUpTime, pi)
            }
        }
    }

    /**
     * Returns if the service may need to display notifications and should find a new wake-up time,
     * or otherwise should go to sleep and wake-up as planned.
     *
     * True if running for the first time ([plannedWakeUpTime] is <= 0) or woken up as planned
     * (or later). Also if woken up earlier than planned and
     * - there are no [upcomingEpisodes], or
     * - there is an upcoming episode released before [nextEpisodeReleaseTime] and after the release
     *   time of the last one notified about, or
     * - there is no upcoming episode released at [nextEpisodeReleaseTime].
     */
    @VisibleForTesting
    fun shouldCheckToNotify(
        plannedWakeUpTime: Long,
        nextEpisodeReleaseTime: Long,
        upcomingEpisodes: List<SgEpisode2WithShow>
    ): Boolean {
        // Note: on first run plannedWakeUpTime will be <= 0.
        if (System.currentTimeMillis() >= plannedWakeUpTime) {
            return true // Running the first time or woken up as planned or later, check to notify.
        }

        Timber.d("Woke up earlier than planned, checking to notify or reschedule")
        val releaseTimeLastNotified = NotificationSettings.getLastNotifiedAbout(context)

        if (upcomingEpisodes.isEmpty()) {
            // No upcoming episodes to notify about, always find a new wake-up time.
            return true
        }

        for (episode in upcomingEpisodes) {
            val releaseTime = episode.episode_firstairedms
            // Any episodes added, or with changed release time, or where notifications
            // where enabled for a show that release before the next one planned to notify
            // about?
            if (releaseTime < nextEpisodeReleaseTime) {
                // limit to those released after the episode we last notified about to avoid
                // notifying about an episode we already notified about
                // limitation: so if added episodes release at or before that last episode
                // they will not be notified about
                if (releaseTime > releaseTimeLastNotified) {
                    return true
                }
            } else {
                // Episode released at or after the next planned one.
                @Suppress("RedundantIf")
                return if (releaseTime > nextEpisodeReleaseTime) {
                    // Episode is not the one planned to notify about, find a new one.
                    // This can happen if the episode release time has changed, it was
                    // removed or notifications for a show have been disabled.
                    true
                } else {
                    // Next to notify about is the planned one, continue sleeping until then.
                    false
                }
            }
        }

        // No episodes to notify about now or later, find new wake-up time.
        return true
    }

    /**
     * Get episodes which released 12 hours ago until in 14 days (to avoid
     * loading too much data), excludes some episodes based on user settings.
     * Ordered by [ORDER].
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
            NotificationSettings.setLastNotifiedAbout(context, latestAirtime)
            Timber.d(
                "Notify about %d episodes, latest released at: %s",
                notifyPositions.size, Instant.ofEpochMilli(latestAirtime)
            )
            notifyAbout(upcomingEpisodes, notifyPositions, latestAirtime)
        }
    }

    /**
     * Only visible for debugging.
     */
    // The core library is lying, the POST_NOTIFICATION permission is only
    // required when targeting Android 13.
    @SuppressLint("MissingPermission")
    fun notifyAbout(
        upcomingEpisodes: List<SgEpisode2WithShow>,
        notifyPositions: List<Int>,
        latestAirtime: Long
    ) {
        // base intent for task stack
        val showsIntent = ShowsActivity.newIntent(context, ShowsActivityImpl.Tab.UPCOMING.index)

        val count = notifyPositions.size
        val hasMultiple = count > 1
        val notificationsById = buildMap {
            // Create a notification for each episode to notify about.
            // Note: if a whole season is released at once, this might be many notifications. But
            // letting the Android System deal with that (for example dropping some).
            val episodes = buildList {
                for (displayIndex in 0..<count) {
                    add(upcomingEpisodes[notifyPositions[displayIndex]])
                }
            }
            for (episode in episodes) {
                // To have an (almost) unique ID for each notification use the episode ID and to
                // avoid exceeding the Int range map it to large blocks. As a block is very large
                // that should make it unlikely for episode notification IDs to ever collide.
                val notificationId =
                    SgApp.BASE_NOTIFICATION_ID_EPISODES + episode.id.mod(100_000)
                buildEpisodeNotification(
                    notificationId,
                    episode,
                    latestAirtime,
                    showsIntent,
                    addToGroup = hasMultiple
                ).also { put(notificationId, it) }
            }
            if (hasMultiple) {
                // Add summary notification
                buildEpisodeSummaryNotification(latestAirtime, showsIntent, episodes)
                    .also { put(SgApp.NOTIFICATION_EPISODE_ID, it) }
            }
        }

        val nm = NotificationManagerCompat.from(context)
        notificationsById.forEach { (id, notification) ->
            nm.notify(id, notification)
        }

        Timber.d(
            "Notification: count=%d, delete=%s",
            count,
            Instant.ofEpochMilli(latestAirtime)
        )
    }

    private fun buildEpisodeNotification(
        notificationId: Int,
        episode: SgEpisode2WithShow,
        latestAirtime: Long,
        showsIntent: Intent,
        addToGroup: Boolean
    ): Notification {
        val nb = NotificationCompat.Builder(context, SgApp.NOTIFICATION_CHANNEL_EPISODES)

        val (id, episodetitle, episodenumber, season, episode_firstairedms, _, _, episodeSummary, showTitle, network, series_poster_small) = episode

        maybeSetPoster(nb, series_poster_small)

        // show title and number, like 'Show 1x01'
        nb.setContentTitle(
            TextTools.getShowWithEpisodeNumber(context, showTitle, season, episodenumber)
        )

        // "8:00 PM Network"
        val time = TimeTools.formatToLocalTime(
            context,
            TimeTools.applyUserOffset(context, episode_firstairedms)
        )
        val contentText = TextTools.dotSeparate(time, network) // switch on purpose
        nb.setContentText(contentText)

        if (!DisplaySettings.preventSpoilers(context)) {
            val episodeTitle = TextTools.getEpisodeTitle(context, episodetitle, episodenumber)

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

        // Set unique data for this notifications intents to ensure each notification can have its
        // own PendingIntents (extras that include the episode ID are not considered when creating
        // a PendingIntent).
        val uniqueData = Uri.parse("content://episodes/$id")

        // click intent
        val episodeDetailsIntent = intentEpisode(id, context).apply {
            data = uniqueData
            // data to handle delete
            putExtra(EXTRA_EPISODE_CLEARED_TIME, latestAirtime)
        }
        nb.setContentIntent(
            TaskStackBuilder.create(context)
                .addNextIntent(showsIntent)
                .addNextIntent(episodeDetailsIntent)
                .getPendingIntent(
                    REQUEST_CODE_SINGLE_EPISODE,
                    // Use FLAG_UPDATE_CURRENT so intent of a previous notification for this episode
                    // is updated, but continues to work.
                    PendingIntentCompat.flagImmutable or PendingIntent.FLAG_UPDATE_CURRENT
                )!!
        )

        // Action button to check in
        val checkInActionIntent = QuickCheckInActivity.intent(id, context).apply {
            data = uniqueData
            // data to handle delete
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_EPISODE_CLEARED_TIME, latestAirtime)
        }
        val checkInIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_ACTION_CHECKIN,
            checkInActionIntent,
            // Use FLAG_UPDATE_CURRENT so intent of a previous notification for this episode
            // is updated, but continues to work.
            PendingIntentCompat.flagImmutable or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // icon only shown on Wear and 4.1 (API 16) to 6.0 (API 23)
        // note: Wear and Galaxy Watch devices do typically not support vector icons
        nb.addAction(
            R.drawable.ic_action_checkin, context.getString(R.string.checkin),
            checkInIntent
        )

        // Action button to set watched
        val setWatchedIntent = NotificationActionReceiver.intent(id, context).apply {
            data = uniqueData
            // data to handle delete
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_EPISODE_CLEARED_TIME, latestAirtime)
        }
        val setWatchedPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ACTION_SET_WATCHED,
            setWatchedIntent,
            // Use FLAG_UPDATE_CURRENT so intent of a previous notification for this episode
            // is updated, but continues to work.
            PendingIntentCompat.flagImmutable or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // icon only shown on Wear and 4.1 (API 16) to 6.0 (API 23)
        // note: Wear and Galaxy Watch devices do typically not support vector icons
        nb.addAction(
            R.drawable.ic_action_tick, context.getString(R.string.action_watched),
            setWatchedPendingIntent
        )

        // delete intent
        // When displayed in a group, will always receive the delete intent of the summary
        // notification when the last notification of a group was (or the whole group is) dismissed.
        if (!addToGroup) {
            nb.setDeleteIntent(createDeleteIntent(latestAirtime))
        }

        if (addToGroup) {
            nb.setGroup(SgApp.NOTIFICATION_GROUP_EPISODES)
            // As posting the notifications all at once, only sound/vibrate once
            // (for the summary notification).
            nb.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
        }

        return nb
            .setSgDefaults()
            .build()
    }

    private fun buildEpisodeSummaryNotification(
        latestAirtime: Long,
        showsIntent: Intent,
        episodes: List<SgEpisode2WithShow>
    ): Notification {
        val nb = NotificationCompat.Builder(context, SgApp.NOTIFICATION_CHANNEL_EPISODES)

        // notify about multiple episodes
        nb.setContentTitle(
            context.getString(
                R.string.upcoming_episodes_number,
                NumberFormat.getIntegerInstance().format(episodes.size.toLong())
            )
        )
        val contentText = context.getString(R.string.upcoming_display)
        nb.setContentText(contentText)

        nb.setContentIntent(
            TaskStackBuilder.create(context)
                .addNextIntent(showsIntent.putExtra(EXTRA_EPISODE_CLEARED_TIME, latestAirtime))
                .getPendingIntent(
                    REQUEST_CODE_MULTIPLE_EPISODES,
                    // Use FLAG_UPDATE_CURRENT so intent of a previous summary notification
                    // is updated, but continues to work.
                    PendingIntentCompat.flagImmutable or PendingIntent.FLAG_UPDATE_CURRENT
                )!!
        )
        nb.setDeleteIntent(createDeleteIntent(latestAirtime))

        // multiple episodes
        val inboxStyle = NotificationCompat.InboxStyle()
        for (episode in episodes) {
            val (_, _, episodenumber, season, episode_firstairedms, _, _, _, seriestitle, network) = episode

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

        nb.setStyle(inboxStyle)
        nb.setNumber(episodes.size)

        return nb
            .setGroup(SgApp.NOTIFICATION_GROUP_EPISODES)
            .setGroupSummary(true)
            .setSgDefaults()
            .build()
    }

    private fun NotificationCompat.Builder.setSgDefaults(): NotificationCompat.Builder {
        // notification sound
        val ringtoneUri = NotificationSettings.getNotificationsRingtone(context)
        // If the string is empty, the user chose silent...
        val hasSound = ringtoneUri.isNotEmpty()
        if (hasSound) {
            // ...otherwise set the specified ringtone
            setSound(Uri.parse(ringtoneUri))
        }
        // vibration
        val vibrates = NotificationSettings.isNotificationVibrating(context)
        if (vibrates) {
            setVibrate(VIBRATION_PATTERN)
        }
        setDefaults(Notification.DEFAULT_LIGHTS)
        setWhen(System.currentTimeMillis())
        setAutoCancel(true)
        setSmallIcon(R.drawable.ic_notification)
        color = ContextCompat.getColor(context, R.color.sg_color_primary)
        priority = NotificationCompat.PRIORITY_DEFAULT
        return this
    }

    private fun createDeleteIntent(latestAirtime: Long): PendingIntent {
        val deleteIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_CLEARED
            // Set unique data to make this PendingIntent unique (extras are not considered) to
            // avoid breaking it for previous notifications.
            data = Uri.parse("content://$latestAirtime")
            putExtra(EXTRA_EPISODE_CLEARED_TIME, latestAirtime)
        }
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE_DELETE_INTENT,
            deleteIntent,
            // Use FLAG_UPDATE_CURRENT in case another notification with the same latest time is
            // posted so its intent is not cancelled, but just updated.
            PendingIntentCompat.flagImmutable or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun maybeSetPoster(nb: NotificationCompat.Builder, posterPath: String?) {
        try {
            val poster = ImageTools.loadWithPicasso(
                context,
                tmdbOrTvdbPosterUrl(posterPath, context, false)
            )
                .centerCrop()
                .resizeDimen(R.dimen.show_poster_width, R.dimen.show_poster_height)
                .get()
            nb.setLargeIcon(poster)

            // add special large resolution background for wearables
            // https://developer.android.com/training/wearables/notifications/creating.html#AddWearableFeatures
            val posterSquare = ImageTools.loadWithPicasso(
                context,
                tmdbOrTvdbPosterUrl(posterPath, context, false)
            )
                .centerCrop()
                .resize(400, 400)
                .get()

            // Note: background may not be supported on newer Wear devices,
            // but keep supporting the old ones.
            @Suppress("DEPRECATION")
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
            "com.uwetrottmann.seriesguide.episode_cleared_time"
        private const val EXTRA_NOTIFICATION_ID =
            "com.uwetrottmann.seriesguide.notification_id"

        private const val DEBUG = false

        private const val REQUEST_CODE_DELETE_INTENT = 1
        private const val REQUEST_CODE_SINGLE_EPISODE = 2
        private const val REQUEST_CODE_MULTIPLE_EPISODES = 3
        private const val REQUEST_CODE_ACTION_CHECKIN = 4
        private const val REQUEST_CODE_ACTION_SET_WATCHED = 5

        val VIBRATION_PATTERN = longArrayOf(
            0, 100, 200, 100, 100, 100
        )

        // only if notifications are on: unwatched episodes released on or after arg
        private const val SELECTION = (SgShow2Columns.NOTIFY + " = 1 AND "
                + SgEpisode2Columns.SELECTION_UNWATCHED + " AND "
                + SgEpisode2Columns.FIRSTAIREDMS + ">= ? AND "
                + SgEpisode2Columns.FIRSTAIREDMS + "< ?")

        /** By release date, then by show, then lowest number first. */
        private const val ORDER = (SgEpisode2Columns.FIRSTAIREDMS + " ASC,"
                + SgShow2Columns.SORT_TITLE + ","
                + SgEpisode2Columns.NUMBER + " ASC")

        /**
         * Send broadcast to run the notification service to display and (re)schedule upcoming episode
         * alarms.
         */
        fun trigger(context: Context) {
            context.sendBroadcast(NotificationAlarmReceiver.intent(context))
        }

        fun deleteNotification(context: Context, intent: Intent) {
            val manager = NotificationManagerCompat.from(context)
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
            if (notificationId > 0) {
                manager.cancel(notificationId)
                // replicate delete intent
                handleDeleteIntent(context, intent)
            }
        }

        /**
         * Extracts the last cleared time and stores it in settings.
         */
        fun handleDeleteIntent(context: Context, intent: Intent?) {
            if (intent == null) {
                return
            }
            Timber.d("Notification cleared, received delete intent $intent")
            val clearedTime = intent.getLongExtra(EXTRA_EPISODE_CLEARED_TIME, 0)
            if (clearedTime != 0L) {
                // Never show the cleared episode(s) again
                Timber.d("Notification cleared, setting last cleared episode time: %d", clearedTime)
                NotificationSettings.setLastCleared(context, clearedTime)
            }
        }

        // Note: do not move to NotificationSettings as ScheduleExactAlarm Lint check will fail.
        /**
         * On Android 12+, returns if scheduling of exact alarms is allowed.
         * On older versions always returns true.
         */
        fun AlarmManager.canScheduleExactAlarmsCompat(): Boolean {
            // https://developer.android.com/training/scheduling/alarms#exact-permission-check
            return if (AndroidUtils.isAtLeastS) {
                canScheduleExactAlarms()
            } else {
                true
            }
        }

    }
}