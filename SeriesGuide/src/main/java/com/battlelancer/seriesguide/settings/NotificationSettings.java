
package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.R;

/**
 * Access settings related to the notification service.
 */
public class NotificationSettings {
    public static final String KEY_ENABLED = "com.battlelancer.seriesguide.notifications";

    public static final String KEY_FAVONLY = "com.battlelancer.seriesguide.notifications.favonly";

    public static final String KEY_THRESHOLD
            = "com.battlelancer.seriesguide.notifications.threshold";

    public static final String KEY_LAST_CLEARED
            = "com.battlelancer.seriesguide.notifications.latestcleared";

    public static final String KEY_LAST_NOTIFIED
            = "com.battlelancer.seriesguide.notifications.latestnotified";

    public static final String KEY_NEXT_TO_NOTIFY
            = "com.battlelancer.seriesguide.notifications.next";

    public static final String KEY_RINGTONE = "com.battlelancer.seriesguide.notifications.ringtone";

    public static final String KEY_VIBRATE = "com.battlelancer.seriesguide.notifications.vibrate";

    private static final int THRESHOLD_DEFAULT_MIN = 10;

    public static boolean isNotificationsEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_ENABLED, true);
    }

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
        if (minutes % (24 * 60) == 0) {
            value = minutes / (24 * 60);
            stringRes = R.plurals.days_before_plural;
        } else if (minutes % 60 == 0) {
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
     * Get the air time of the episode the user cleared last (or for below HC the last episode we
     * notified about).
     */
    public static long getLastCleared(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_LAST_CLEARED, 0);
    }

    /**
     * Get the air time of the episode we last notified about.
     */
    public static long getLastNotified(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_LAST_NOTIFIED, 0);
    }

    public static String getNotificationsRingtone(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_RINGTONE, Settings.System.DEFAULT_NOTIFICATION_URI.toString());
    }

    public static boolean isNotificationVibrating(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_VIBRATE, false);
    }
}
