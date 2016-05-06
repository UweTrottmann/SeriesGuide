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

package com.battlelancer.seriesguide.backend.settings;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;

public class HexagonSettings {

    public static final String AUDIENCE
            = "server:client_id:137959300653-9pg0ulu5d3d6jhm4fotn2onk789vsob7.apps.googleusercontent.com";

    public static final String KEY_ACCOUNT_NAME
            = "com.battlelancer.seriesguide.hexagon.v2.accountname";

    public static final String KEY_SETUP_COMPLETED
            = "com.battlelancer.seriesguide.hexagon.v2.setup_complete";

    public static final String KEY_MERGED_SHOWS
            = "com.battlelancer.seriesguide.hexagon.v2.merged.shows";

    public static final String KEY_MERGED_MOVIES
            = "com.battlelancer.seriesguide.hexagon.v2.merged.movies";

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
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putBoolean(HexagonSettings.KEY_SETUP_COMPLETED, complete).commit();
    }

    /**
     * Reset the sync state of shows, episodes, movies and lists so a data merge is triggered when
     * next syncing with Hexagon.
     *
     * @return If the sync settings reset was committed successfully.
     */
    public static boolean resetSyncState(Context context) {
        // set all shows as not merged with Hexagon
        ContentValues values = new ContentValues();
        values.put(SeriesGuideContract.Shows.HEXAGON_MERGE_COMPLETE, false);
        context.getContentResolver()
                .update(SeriesGuideContract.Shows.CONTENT_URI, values, null, null);

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
                .commit();
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

    @SuppressLint("CommitPrefEdits")
    private static long getLastSyncTime(Context context, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long lastSync = prefs.getLong(key, 0);
        if (lastSync == 0) {
            lastSync = System.currentTimeMillis(); // not synced yet, then last time is now!
            prefs.edit().putLong(key, lastSync).commit();
        }

        return lastSync;
    }
}
