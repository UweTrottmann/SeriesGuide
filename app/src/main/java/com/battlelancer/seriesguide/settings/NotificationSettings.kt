package com.battlelancer.seriesguide.settings

import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.uwetrottmann.androidutils.AndroidUtils
import timber.log.Timber

/**
 * Access settings related to the notification service.
 */
object NotificationSettings {

    const val KEY_ENABLED = "com.battlelancer.seriesguide.notifications"
    const val KEY_FAVONLY = "com.battlelancer.seriesguide.notifications.favonly"
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

    fun isNotificationsEnabled(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_ENABLED, true)
    }

    @Deprecated(
        """Notifications are enabled on a per-show basis since {@link
     * SeriesGuideDatabase#DBVER_40_NOTIFY_PER_SHOW}."""
    )
    fun isNotifyAboutFavoritesOnly(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_FAVONLY, false)
    }

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