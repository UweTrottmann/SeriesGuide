
package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.ArrayRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

    private static final String THRESHOLD_DEFAULT_MIN = "60";

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
        int threshold = 60;

        try {
            threshold = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(KEY_THRESHOLD, THRESHOLD_DEFAULT_MIN));
        } catch (NumberFormatException ignored) {
        }

        return threshold;
    }

    /**
     * How far into the future to include upcoming episodes in minutes as text value.
     */
    @Nullable
    public static CharSequence getLatestToIncludeTresholdValue(Context context) {
        String value = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_THRESHOLD, THRESHOLD_DEFAULT_MIN);
        return getEntryForValue(context, R.array.notificationThresholdData,
                R.array.notificationThreshold, value);
    }

    @Nullable
    private static CharSequence getEntryForValue(@NonNull Context context,
            @ArrayRes int entryValuesResId, @ArrayRes int entriesResId, @Nullable String value) {
        if (value == null) {
            return null;
        }
        CharSequence[] entryValues = context.getResources().getTextArray(entryValuesResId);
        if (entryValues == null) {
            return null;
        }
        int index = findIndexOfValue(value, entryValues);
        CharSequence[] entries = context.getResources().getTextArray(entriesResId);
        return index >= 0 && entries != null ? entries[index] : null;
    }

    private static int findIndexOfValue(@NonNull String value,
            @NonNull CharSequence[] entryValues) {
        for (int i = entryValues.length - 1; i >= 0; i--) {
            if (entryValues[i].equals(value)) {
                return i;
            }
        }
        return -1;
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
