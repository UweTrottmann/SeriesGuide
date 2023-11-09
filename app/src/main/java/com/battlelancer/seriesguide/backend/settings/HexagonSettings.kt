// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.backend.settings

import android.content.Context
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.util.Utils

object HexagonSettings {

    const val KEY_ENABLED = "com.battlelancer.seriesguide.hexagon.v2.enabled"
    const val KEY_SHOULD_VALIDATE_ACCOUNT =
        "com.battlelancer.seriesguide.hexagon.v2.shouldFixAccount"
    const val KEY_ACCOUNT_NAME = "com.battlelancer.seriesguide.hexagon.v2.accountname"
    const val KEY_SETUP_COMPLETED = "com.battlelancer.seriesguide.hexagon.v2.setup_complete"
    const val KEY_MERGED_SHOWS = "com.battlelancer.seriesguide.hexagon.v2.merged.shows"
    const val KEY_MERGED_MOVIES = "com.battlelancer.seriesguide.hexagon.v2.merged.movies2"
    const val KEY_MERGED_LISTS = "com.battlelancer.seriesguide.hexagon.v2.merged.lists"
    const val KEY_LAST_SYNC_SHOWS = "com.battlelancer.seriesguide.hexagon.v2.lastsync.shows"
    const val KEY_LAST_SYNC_EPISODES = "com.battlelancer.seriesguide.hexagon.v2.lastsync.episodes"
    const val KEY_LAST_SYNC_MOVIES = "com.battlelancer.seriesguide.hexagon.v2.lastsync.movies"
    const val KEY_LAST_SYNC_LISTS = "com.battlelancer.seriesguide.hexagon.v2.lastsync.lists"

    /**
     * If Cloud is enabled and Cloud specific actions should be performed or UI be shown.
     */
    @JvmStatic
    fun isEnabled(context: Context): Boolean =
        Utils.hasAccessToX(context) && PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_ENABLED, false)

    fun shouldValidateAccount(context: Context, value: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(KEY_SHOULD_VALIDATE_ACCOUNT, value)
            .apply()
    }

    /**
     * Returns true if there is an issue with the current account and the user should be sent to the
     * setup screen.
     */
    @JvmStatic
    fun shouldValidateAccount(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_SHOULD_VALIDATE_ACCOUNT, false)
    }

    /**
     * Returns the account name used for authenticating against Hexagon, or null if not signed in.
     */
    fun getAccountName(context: Context): String? {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_ACCOUNT_NAME, null)
    }

    /**
     * Whether the Hexagon setup has been completed after the last sign in.
     */
    fun hasCompletedSetup(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_SETUP_COMPLETED, true)
    }

    fun setSetupCompleted(context: Context) {
        setSetupState(context, true)
    }

    fun setSetupIncomplete(context: Context) {
        setSetupState(context, false)
    }

    private fun setSetupState(context: Context, complete: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean(KEY_SETUP_COMPLETED, complete)
            .apply()
    }

    /**
     * Reset the sync state of shows, episodes, movies and lists so a data merge is triggered when
     * next syncing with Hexagon.
     *
     * @return If the sync settings reset was committed successfully.
     */
    fun resetSyncState(context: Context): Boolean {
        // set all shows as not merged with Hexagon
        SgRoomDatabase.getInstance(context).sgShow2Helper().setHexagonMergeNotCompletedForAll()

        // reset sync properties
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putBoolean(KEY_MERGED_SHOWS, false)
        editor.putBoolean(KEY_MERGED_MOVIES, false)
        editor.putBoolean(KEY_MERGED_LISTS, false)
        editor.remove(KEY_LAST_SYNC_EPISODES)
        editor.remove(KEY_LAST_SYNC_SHOWS)
        editor.remove(KEY_LAST_SYNC_MOVIES)
        editor.remove(KEY_LAST_SYNC_LISTS)
        return editor.commit()
    }

    /**
     * Resets the merged state of lists so a data merge is triggered when next syncing with Hexagon.
     * Returns if the sync settings reset was committed successfully.
     */
    fun setListsNotMerged(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(KEY_MERGED_LISTS, false)
            .commit()
    }

    /**
     * Whether shows in the local database have been merged with those on Hexagon.
     */
    @JvmStatic
    fun hasMergedShows(context: Context): Boolean {
        return hasMerged(context, KEY_MERGED_SHOWS)
    }

    /**
     * Whether movies in the local database have been merged with those on Hexagon.
     */
    @JvmStatic
    fun hasMergedMovies(context: Context): Boolean {
        return hasMerged(context, KEY_MERGED_MOVIES)
    }

    /**
     * Whether lists in the local database have been merged with those on Hexagon.
     */
    @JvmStatic
    fun hasMergedLists(context: Context): Boolean {
        return hasMerged(context, KEY_MERGED_LISTS)
    }

    private fun hasMerged(context: Context, key: String): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(key, false)
    }

    /**
     * Set the [.KEY_MERGED_SHOWS] setting.
     */
    @JvmStatic
    fun setHasMergedShows(context: Context, hasMergedShows: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(KEY_MERGED_SHOWS, hasMergedShows)
            .apply()
    }

    @JvmStatic
    fun getLastEpisodesSyncTime(context: Context): Long {
        return getLastSyncTime(context, KEY_LAST_SYNC_EPISODES)
    }

    fun getLastShowsSyncTime(context: Context): Long {
        return getLastSyncTime(context, KEY_LAST_SYNC_SHOWS)
    }

    fun getLastMoviesSyncTime(context: Context): Long {
        return getLastSyncTime(context, KEY_LAST_SYNC_MOVIES)
    }

    @JvmStatic
    fun getLastListsSyncTime(context: Context): Long {
        return getLastSyncTime(context, KEY_LAST_SYNC_LISTS)
    }

    private fun getLastSyncTime(context: Context, key: String): Long {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        var lastSync = prefs.getLong(key, 0)
        if (lastSync == 0L) {
            lastSync = System.currentTimeMillis() // not synced yet, then last time is now!
            prefs.edit().putLong(key, lastSync).apply()
        }
        return lastSync
    }
}