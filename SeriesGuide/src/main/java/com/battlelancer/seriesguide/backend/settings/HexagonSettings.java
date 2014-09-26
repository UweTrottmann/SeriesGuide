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
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

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

    public static final String KEY_LAST_SYNC_SHOWS
            = "com.battlelancer.seriesguide.hexagon.v2.lastsync.shows";

    public static final String KEY_LAST_SYNC_EPISODES
            = "com.battlelancer.seriesguide.hexagon.v2.lastsync.episodes";

    public static final String KEY_LAST_SYNC_MOVIES
            = "com.battlelancer.seriesguide.hexagon.v2.lastsync.movies";

    /**
     * Returns the account name used for authenticating against Hexagon.
     */
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
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putBoolean(HexagonSettings.KEY_SETUP_COMPLETED, true).commit();
    }

    public static void setSetupIncomplete(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putBoolean(HexagonSettings.KEY_SETUP_COMPLETED, false).commit();
    }

    /**
     * Whether shows in the local database have been merged with those on Hexagon.
     */
    public static boolean hasMergedShows(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_MERGED_SHOWS, false);
    }

    /**
     * Whether movies in the local database have been merged with those on Hexagon.
     */
    public static boolean hasMergedMovies(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_MERGED_MOVIES, false);
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

    @SuppressLint("CommitPrefEdits")
    public static long getLastEpisodesSyncTime(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long lastSync = prefs.getLong(KEY_LAST_SYNC_EPISODES, 0);
        if (lastSync == 0) {
            lastSync = System.currentTimeMillis();
            prefs.edit().putLong(KEY_LAST_SYNC_EPISODES, lastSync).commit();
        }

        return lastSync;
    }

    @SuppressLint("CommitPrefEdits")
    public static long getLastShowsSyncTime(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long lastSync = prefs.getLong(KEY_LAST_SYNC_SHOWS, 0);
        if (lastSync == 0) {
            lastSync = System.currentTimeMillis();
            prefs.edit().putLong(KEY_LAST_SYNC_SHOWS, lastSync).commit();
        }

        return lastSync;
    }

    @SuppressLint("CommitPrefEdits")
    public static long getLastMoviesSyncTime(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long lastSync = prefs.getLong(KEY_LAST_SYNC_MOVIES, 0);
        if (lastSync == 0) {
            lastSync = System.currentTimeMillis();
            prefs.edit().putLong(KEY_LAST_SYNC_MOVIES, lastSync).commit();
        }

        return lastSync;
    }
}
