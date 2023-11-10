// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.settings

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.notifications.NotificationService.Companion.canScheduleExactAlarmsCompat
import com.uwetrottmann.androidutils.AndroidUtils
import timber.log.Timber

/**
 * Access settings related to the notification service.
 */
object NotificationSettings {

    const val KEY_ENABLED = "com.battlelancer.seriesguide.notifications"

//    @Deprecated(
//        """Notifications are enabled on a per-show basis since {@link
//     * SeriesGuideDatabase#DBVER_40_NOTIFY_PER_SHOW}."""
//    )
//    const val KEY_FAVONLY = "com.battlelancer.seriesguide.notifications.favonly"

    const val KEY_THRESHOLD = "com.battlelancer.seriesguide.notifications.threshold"

    /** Just a link to a screen to select shows to notify about.  */
    const val KEY_SELECTION = "com.battlelancer.seriesguide.notifications.shows"

    /** Only visible on O+. Link to system settings app to modify further notification settings.  */
    const val KEY_CHANNELS = "com.battlelancer.seriesguide.notifications.channels"

    private const val KEY_LAST_CLEARED = "com.battlelancer.seriesguide.notifications.latestcleared"
    private const val KEY_LAST_NOTIFIED =
        "com.battlelancer.seriesguide.notifications.latestnotified"
    const val KEY_NEXT_TO_NOTIFY = "com.battlelancer.seriesguide.notifications.next"

    /** Only visible on pre-O.  */
    const val KEY_RINGTONE = "com.battlelancer.seriesguide.notifications.ringtone"

    /** Only visible on pre-O.  */
    const val KEY_VIBRATE = "com.battlelancer.seriesguide.notifications.vibrate"

    const val KEY_IGNORE_HIDDEN = "com.battlelancer.seriesguide.notifications.hidden"
    const val KEY_ONLY_NEXT_EPISODE = "com.uwetrottmann.seriesguide.notifications.nextonly"

    private const val THRESHOLD_DEFAULT_MIN = 10

    /**
     * On Android 8+, returns if notifications are enabled in system settings.
     * On older versions, checks if the app preference is true.
     *
     * Note that even if enabled, check [com.battlelancer.seriesguide.util.Utils.hasAccessToX] if
     * the user is eligible to receive convenience notifications (for new episodes).
     */
    fun isNotificationsEnabled(context: Context): Boolean {
        return if (AndroidUtils.isAtLeastOreo) {
            return areNotificationsAllowed(context)
        } else {
            PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_ENABLED, true)
        }
    }

    /**
     * On Android 8+, returns if notifications are enabled in system settings.
     * On older versions always returns true.
     */
    fun areNotificationsAllowed(context: Context): Boolean {
        return if (AndroidUtils.isAtLeastOreo) {
            context.getSystemService<NotificationManager>()?.areNotificationsEnabled()
                ?: false
        } else {
            true
        }
    }

    /**
     * On Android 12+, returns if scheduling of exact alarms is allowed.
     * On older versions always returns true.
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        // https://developer.android.com/training/scheduling/alarms
        return if (AndroidUtils.isAtLeastS) {
            context.getSystemService<AlarmManager>()?.canScheduleExactAlarmsCompat() == true
        } else {
            true
        }
    }

    /**
     * For Android 12+, builds an intent to open the settings screen to allow alarms and reminders.
     * See [Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM].
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun buildRequestExactAlarmSettingsIntent(context: Context) =
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            .setData(Uri.parse("package:" + context.packageName))


    /**
     * How far into the future to include upcoming episodes in minutes.
     */
    fun getLatestToIncludeTreshold(context: Context): Int {
        var threshold = THRESHOLD_DEFAULT_MIN
        try {
            val value = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_THRESHOLD, null)
            if (value != null) {
                threshold = value.toInt()
            }
        } catch (ignored: NumberFormatException) {
        }
        return threshold
    }

    /**
     * Text value when notifications for new episodes are shown, such as '10 minutes before'.
     */
    fun getLatestToIncludeTresholdValue(context: Context): CharSequence {
        val minutes = getLatestToIncludeTreshold(context)
        val value: Int
        val stringRes: Int
        if (minutes != 0 && minutes % (24 * 60) == 0) {
            value = minutes / (24 * 60)
            stringRes = R.plurals.days_before_plural
        } else if (minutes != 0 && minutes % 60 == 0) {
            value = minutes / 60
            stringRes = R.plurals.hours_before_plural
        } else {
            value = minutes
            stringRes = R.plurals.minutes_before_plural
        }
        return context.resources.getQuantityString(stringRes, value, value)
    }

    /**
     * Get the air time of the next episode we plan to notify about.
     */
    fun getNextToNotifyAbout(context: Context): Long {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(KEY_NEXT_TO_NOTIFY, 0)
    }

    /**
     * Get the air time of the episode the user cleared last.
     */
    fun getLastCleared(context: Context): Long {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(KEY_LAST_CLEARED, 0)
    }

    fun setLastCleared(context: Context, clearedTime: Long) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putLong(KEY_LAST_CLEARED, clearedTime)
            .apply()
    }

    /**
     * Get the air time of the episode we last notified about.
     */
    fun getLastNotifiedAbout(context: Context): Long {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(KEY_LAST_NOTIFIED, 0)
    }

    fun setLastNotifiedAbout(context: Context, releaseTime: Long) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putLong(KEY_LAST_NOTIFIED, releaseTime)
            .apply()
    }

    /**
     * Resets the air time of the last notified about episode. Afterwards notifications for episodes
     * may appear, which were already notified about.
     */
    fun resetLastEpisodeAirtime(context: Context) {
        Timber.d("Resetting last cleared and last notified")
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putLong(KEY_LAST_CLEARED, 0)
            .putLong(KEY_LAST_NOTIFIED, 0)
            .apply()
    }

    /**
     * @return Empty (!) string if silent, otherwise ringtone URI.
     */
    fun getNotificationsRingtone(context: Context): String {
        var ringtoneUri = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_RINGTONE, null)
        if (ringtoneUri == null) {
            ringtoneUri = Settings.System.DEFAULT_NOTIFICATION_URI.toString()
        }
        if (AndroidUtils.isNougatOrHigher) {
            // Xiaomi devices incorrectly allowed file:// uris
            // protect against FileUriExposedException
            if (ringtoneUri.isNotEmpty() /* not silent */ && !ringtoneUri.startsWith("content")) {
                ringtoneUri = Settings.System.DEFAULT_NOTIFICATION_URI.toString()
                PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putString(KEY_RINGTONE, ringtoneUri)
                    .apply()
            }
        }
        return ringtoneUri
    }

    fun isNotificationVibrating(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_VIBRATE, false)
    }

    /**
     * Whether hidden shows should be ignored when notifying about upcoming episodes.
     */
    fun isIgnoreHiddenShows(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_IGNORE_HIDDEN, true)
    }

    /**
     * Whether only notifications for a shows next episode should be shown.
     */
    fun isOnlyNextEpisodes(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_ONLY_NEXT_EPISODE, false)
    }

    @JvmStatic
    fun setDefaultsForChannelErrors(
        context: Context,
        builder: NotificationCompat.Builder
    ) {
        builder.color = ContextCompat.getColor(context, R.color.sg_color_primary)
        builder.setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_LIGHTS)
        builder.priority = NotificationCompat.PRIORITY_HIGH
        builder.setCategory(NotificationCompat.CATEGORY_ERROR)
    }
}