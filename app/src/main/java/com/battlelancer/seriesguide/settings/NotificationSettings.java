
package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.uwetrottmann.androidutils.AndroidUtils;

/**
 * Access settings related to the notification service.
 */
public class NotificationSettings {
    public static final String KEY_ENABLED = "com.battlelancer.seriesguide.notifications";

    public static final String KEY_FAVONLY = "com.battlelancer.seriesguide.notifications.favonly";

    public static final String KEY_THRESHOLD
            = "com.battlelancer.seriesguide.notifications.threshold";

    /** Just a link to a screen to select shows to notify about. */
    public static final String KEY_SELECTION
            = "com.battlelancer.seriesguide.notifications.shows";

    /** Only visible on O+. Link to system settings app to modify further notification settings. */
    public static final String KEY_CHANNELS
            = "com.battlelancer.seriesguide.notifications.channels";

    public static final String KEY_LAST_CLEARED
            = "com.battlelancer.seriesguide.notifications.latestcleared";

    public static final String KEY_LAST_NOTIFIED
            = "com.battlelancer.seriesguide.notifications.latestnotified";

    public static final String KEY_NEXT_TO_NOTIFY
            = "com.battlelancer.seriesguide.notifications.next";

    /** Only visible on pre-O. */
    public static final String KEY_RINGTONE = "com.battlelancer.seriesguide.notifications.ringtone";

    /** Only visible on pre-O. */
    public static final String KEY_VIBRATE = "com.battlelancer.seriesguide.notifications.vibrate";

    public static final String KEY_IGNORE_HIDDEN = "com.battlelancer.seriesguide.notifications.hidden";

    public static final String KEY_ONLY_NEXT_EPISODE = "com.uwetrottmann.seriesguide.notifications.nextonly";

    private static final int THRESHOLD_DEFAULT_MIN = 10;

    public static boolean isNotificationsEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_ENABLED, true);
    }

    /**
     * @deprecated Notifications are enabled on a per-show basis since {@link
     * SeriesGuideDatabase#DBVER_40_NOTIFY_PER_SHOW}.
     */
    @Deprecated
    public static boolean isNotifyAboutFavoritesOnly(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_FAVONLY, false);
    }

    /**
     * How far into the future to include upcoming episodes in minutes.
     */
    public static int getLatestToIncludeTreshold(Context context) {
        int threshold = THRESHOLD_DEFAULT_MIN;

        try {
            threshold = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(KEY_THRESHOLD, String.valueOf(THRESHOLD_DEFAULT_MIN)));
        } catch (NumberFormatException ignored) {
        }

        return threshold;
    }

    /**
     * Text value when notifications for new episodes are shown, such as '10 minutes before'.
     */
    @NonNull
    public static CharSequence getLatestToIncludeTresholdValue(Context context) {
        int minutes = getLatestToIncludeTreshold(context);

        int value;
        int stringRes;
        if (minutes != 0 && minutes % (24 * 60) == 0) {
            value = minutes / (24 * 60);
            stringRes = R.plurals.days_before_plural;
        } else if (minutes != 0 && minutes % 60 == 0) {
            value = minutes / 60;
            stringRes = R.plurals.hours_before_plural;
        } else {
            value = minutes;
            stringRes = R.plurals.minutes_before_plural;
        }

        return context.getResources().getQuantityString(stringRes, value, value);
    }

    /**
     * Get the air time of the next episode we plan to notify about.
     */
    public static long getNextToNotifyAbout(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_NEXT_TO_NOTIFY, 0);
    }

    /**
     * Get the air time of the episode the user cleared last.
     */
    public static long getLastCleared(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_LAST_CLEARED, 0);
    }

    /**
     * Get the air time of the episode we last notified about.
     */
    public static long getLastNotifiedAbout(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_LAST_NOTIFIED, 0);
    }

    /**
     * @return Empty (!) string if silent, otherwise ringtone URI.
     */
    @NonNull
    public static String getNotificationsRingtone(Context context) {
        String ringtoneUri = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_RINGTONE, null);
        if (ringtoneUri == null) {
            ringtoneUri = Settings.System.DEFAULT_NOTIFICATION_URI.toString();
        }
        if (AndroidUtils.isNougatOrHigher()) {
            // Xiaomi devices incorrectly allowed file:// uris
            // protect against FileUriExposedException
            if (ringtoneUri.length() > 0 /* not silent */ && !ringtoneUri.startsWith("content")) {
                ringtoneUri = Settings.System.DEFAULT_NOTIFICATION_URI.toString();
                PreferenceManager.getDefaultSharedPreferences(context).edit()
                        .putString(KEY_RINGTONE, ringtoneUri)
                        .apply();
            }
        }
        return ringtoneUri;
    }

    public static boolean isNotificationVibrating(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_VIBRATE, false);
    }

    /**
     * Whether hidden shows should be ignored when notifying about upcoming episodes.
     */
    public static boolean isIgnoreHiddenShows(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_IGNORE_HIDDEN, true);
    }

    /**
     * Whether only notifications for a shows next episode should be shown.
     */
    public static boolean isOnlyNextEpisodes(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_ONLY_NEXT_EPISODE, false);
    }

    public static void setDefaultsForChannelErrors(Context context,
            NotificationCompat.Builder builder) {
        builder.setColor(ContextCompat.getColor(context, R.color.sg_color_primary));
        builder.setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_LIGHTS);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setCategory(NotificationCompat.CATEGORY_ERROR);
    }
}
