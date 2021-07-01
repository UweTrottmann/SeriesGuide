package com.battlelancer.seriesguide.backend.settings;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.util.Utils;

public class HexagonSettings {

    public static final String AUDIENCE
            = "server:client_id:137959300653-9pg0ulu5d3d6jhm4fotn2onk789vsob7.apps.googleusercontent.com";

    public static final String KEY_ENABLED = "com.battlelancer.seriesguide.hexagon.v2.enabled";

    public static final String KEY_SHOULD_VALIDATE_ACCOUNT
            = "com.battlelancer.seriesguide.hexagon.v2.shouldFixAccount";

    public static final String KEY_ACCOUNT_NAME
            = "com.battlelancer.seriesguide.hexagon.v2.accountname";

    public static final String KEY_SETUP_COMPLETED
            = "com.battlelancer.seriesguide.hexagon.v2.setup_complete";

    public static final String KEY_MERGED_SHOWS
            = "com.battlelancer.seriesguide.hexagon.v2.merged.shows";

    public static final String KEY_MERGED_MOVIES
            = "com.battlelancer.seriesguide.hexagon.v2.merged.movies2";

    public static final String KEY_MERGED_LISTS
            = "com.battlelancer.seriesguide.hexagon.v2.merged.lists";

    public static final String KEY_LAST_SYNC_SHOWS
            = "com.battlelancer.seriesguide.hexagon.v2.lastsync.shows";

    public static final String KEY_LAST_SYNC_EPISODES
            = "com.battlelancer.seriesguide.hexagon.v2.lastsync.episodes";

    public static final String KEY_LAST_SYNC_MOVIES
            = "com.battlelancer.seriesguide.hexagon.v2.lastsync.movies";

    public static final String KEY_LAST_SYNC_LISTS
            = "com.battlelancer.seriesguide.hexagon.v2.lastsync.lists";

    /**
     * If Cloud is enabled and Cloud specific actions should be performed or UI be shown.
     */
    public static boolean isEnabled(Context context) {
        return Utils.hasAccessToX(context) && PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_ENABLED, false);
    }

    public static void shouldValidateAccount(Context context, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_SHOULD_VALIDATE_ACCOUNT, value)
                .apply();
    }

    /**
     * Returns true if there is an issue with the current account and the user should be sent to the
     * setup screen.
     */
    public static boolean shouldValidateAccount(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_SHOULD_VALIDATE_ACCOUNT, false);
    }

    /**
     * Returns the account name used for authenticating against Hexagon, or null if not signed in.
     */
    @Nullable
    public static String getAccountName(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_ACCOUNT_NAME, null);
    }

    /**
     * Whether the Hexagon setup has been completed after the last sign in.
     */
    public static boolean hasCompletedSetup(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_SETUP_COMPLETED, true);
    }

    public static void setSetupCompleted(Context context) {
        setSetupState(context, true);
    }

    public static void setSetupIncomplete(Context context) {
        setSetupState(context, false);
    }

    private static void setSetupState(Context context, boolean complete) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(HexagonSettings.KEY_SETUP_COMPLETED, complete)
                .apply();
    }

    /**
     * Reset the sync state of shows, episodes, movies and lists so a data merge is triggered when
     * next syncing with Hexagon.
     *
     * @return If the sync settings reset was committed successfully.
     */
    public static boolean resetSyncState(Context context) {
        // set all shows as not merged with Hexagon
        SgRoomDatabase.getInstance(context).sgShow2Helper().setHexagonMergeNotCompletedForAll();

        // reset sync properties
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context)
                .edit();
        editor.putBoolean(HexagonSettings.KEY_MERGED_SHOWS, false);
        editor.putBoolean(HexagonSettings.KEY_MERGED_MOVIES, false);
        editor.putBoolean(HexagonSettings.KEY_MERGED_LISTS, false);
        editor.remove(HexagonSettings.KEY_LAST_SYNC_EPISODES);
        editor.remove(HexagonSettings.KEY_LAST_SYNC_SHOWS);
        editor.remove(HexagonSettings.KEY_LAST_SYNC_MOVIES);
        editor.remove(HexagonSettings.KEY_LAST_SYNC_LISTS);
        return editor.commit();
    }

    /**
     * Resets the merged state of lists so a data merge is triggered when next syncing with Hexagon.
     * Returns if the sync settings reset was committed successfully.
     */
    public static boolean setListsNotMerged(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(HexagonSettings.KEY_MERGED_LISTS, false)
                .commit();
    }

    /**
     * Whether shows in the local database have been merged with those on Hexagon.
     */
    public static boolean hasMergedShows(Context context) {
        return hasMerged(context, KEY_MERGED_SHOWS);
    }

    /**
     * Whether movies in the local database have been merged with those on Hexagon.
     */
    public static boolean hasMergedMovies(Context context) {
        return hasMerged(context, KEY_MERGED_MOVIES);
    }

    /**
     * Whether lists in the local database have been merged with those on Hexagon.
     */
    public static boolean hasMergedLists(Context context) {
        return hasMerged(context, KEY_MERGED_LISTS);
    }

    private static boolean hasMerged(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(key, false);
    }

    /**
     * Set the {@link #KEY_MERGED_SHOWS} setting.
     */
    public static void setHasMergedShows(Context context, boolean hasMergedShows) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_MERGED_SHOWS, hasMergedShows)
                .apply();
    }

    public static long getLastEpisodesSyncTime(Context context) {
        return getLastSyncTime(context, KEY_LAST_SYNC_EPISODES);
    }

    public static long getLastShowsSyncTime(Context context) {
        return getLastSyncTime(context, KEY_LAST_SYNC_SHOWS);
    }

    public static long getLastMoviesSyncTime(Context context) {
        return getLastSyncTime(context, KEY_LAST_SYNC_MOVIES);
    }

    public static long getLastListsSyncTime(Context context) {
        return getLastSyncTime(context, KEY_LAST_SYNC_LISTS);
    }

    private static long getLastSyncTime(Context context, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long lastSync = prefs.getLong(key, 0);
        if (lastSync == 0) {
            lastSync = System.currentTimeMillis(); // not synced yet, then last time is now!
            prefs.edit().putLong(key, lastSync).apply();
        }

        return lastSync;
    }
}
