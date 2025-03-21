// SPDX-License-Identifier: Apache-2.0
// Copyright 2013-2025 Uwe Trottmann

package com.battlelancer.seriesguide.backend.settings

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.util.Utils

object HexagonSettings {

    private const val KEY_ENABLED =
        "com.battlelancer.seriesguide.hexagon.v2.enabled"
    private const val KEY_SHOULD_VALIDATE_ACCOUNT =
        "com.battlelancer.seriesguide.hexagon.v2.shouldFixAccount"
    private const val KEY_ACCOUNT_NAME =
        "com.battlelancer.seriesguide.hexagon.v2.accountname"
    private const val KEY_SETUP_COMPLETED =
        "com.battlelancer.seriesguide.hexagon.v2.setup_complete"
    private const val KEY_MERGED_SHOWS =
        "com.battlelancer.seriesguide.hexagon.v2.merged.shows"
    private const val KEY_MERGED_MOVIES =
        "com.battlelancer.seriesguide.hexagon.v2.merged.movies2"
    private const val KEY_MERGED_LISTS =
        "com.battlelancer.seriesguide.hexagon.v2.merged.lists"
    private const val KEY_LAST_SYNC_SHOWS =
        "com.battlelancer.seriesguide.hexagon.v2.lastsync.shows"
    private const val KEY_LAST_SYNC_EPISODES =
        "com.battlelancer.seriesguide.hexagon.v2.lastsync.episodes"
    private const val KEY_LAST_SYNC_MOVIES =
        "com.battlelancer.seriesguide.hexagon.v2.lastsync.movies"
    private const val KEY_LAST_SYNC_LISTS =
        "com.battlelancer.seriesguide.hexagon.v2.lastsync.lists"

    /**
     * If Cloud is enabled and Cloud specific actions should be performed or UI be shown.
     */
    fun isEnabled(context: Context): Boolean =
        Utils.hasAccessToX(context) && PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_ENABLED, false)

    /**
     * Enable Hexagon.
     */
    fun setEnabled(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(KEY_ENABLED, true)
            putBoolean(KEY_SHOULD_VALIDATE_ACCOUNT, false)
        }
    }

    /**
     * Disable Hexagon. Re-enabling will reset any previous sync state.
     */
    fun setDisabled(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(KEY_ENABLED, false)
            putBoolean(KEY_SHOULD_VALIDATE_ACCOUNT, false)
        }
    }

    /**
     * Returns true if there is an issue with the current account and the user should be sent to the
     * setup screen.
     */
    fun shouldValidateAccount(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_SHOULD_VALIDATE_ACCOUNT, false)
    }

    fun setShouldValidateAccount(context: Context, value: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(KEY_SHOULD_VALIDATE_ACCOUNT, value)
        }
    }

    /**
     * Returns the account name used for authenticating against Hexagon, or null if not signed in.
     */
    fun getAccountName(context: Context): String? {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_ACCOUNT_NAME, null)
    }

    /**
     * Store or remove account name.
     */
    fun setAccountName(context: Context, name: String?) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(KEY_ACCOUNT_NAME, name)
        }
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
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(KEY_SETUP_COMPLETED, complete)
        }
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
     * Like [resetSyncState], but only for movies.
     */
    fun resetMovieSyncState(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(KEY_MERGED_MOVIES, false)
            remove(KEY_LAST_SYNC_MOVIES)
        }
    }

    /**
     * Whether shows in the local database have been merged with those on Hexagon.
     */
    fun hasMergedShows(context: Context): Boolean {
        return hasMerged(context, KEY_MERGED_SHOWS)
    }

    fun setHasMergedShows(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(KEY_MERGED_SHOWS, true)
        }
    }

    /**
     * Whether movies in the local database have been merged with those on Hexagon.
     */
    fun hasMergedMovies(context: Context): Boolean {
        return hasMerged(context, KEY_MERGED_MOVIES)
    }

    fun setHasMergedMovies(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(KEY_MERGED_MOVIES, true)
        }
    }

    /**
     * Whether lists in the local database have been merged with those on Hexagon.
     */
    fun hasMergedLists(context: Context): Boolean {
        return hasMerged(context, KEY_MERGED_LISTS)
    }

    fun setHasMergedLists(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(KEY_MERGED_LISTS, true)
        }
    }

    /**
     * Resets the merged state of lists so a data merge is triggered when next syncing with Hexagon.
     * Returns if the sync settings reset was committed successfully.
     */
    fun setHasNotMergedLists(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(KEY_MERGED_LISTS, false)
            .commit()
    }

    private fun hasMerged(context: Context, key: String): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(key, false)
    }

    fun getLastEpisodesSyncTime(context: Context): Long {
        return getLastSyncTime(context, KEY_LAST_SYNC_EPISODES)
    }

    fun setLastEpisodesSyncTime(context: Context, timeInMs: Long) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putLong(KEY_LAST_SYNC_EPISODES, timeInMs)
        }
    }

    fun getLastShowsSyncTime(context: Context): Long {
        return getLastSyncTime(context, KEY_LAST_SYNC_SHOWS)
    }

    fun setLastShowsSyncTime(context: Context, timeInMs: Long) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putLong(KEY_LAST_SYNC_SHOWS, timeInMs)
        }
    }

    fun getLastMoviesSyncTime(context: Context): Long {
        return getLastSyncTime(context, KEY_LAST_SYNC_MOVIES)
    }

    fun setLastMoviesSyncTime(context: Context, timeInMs: Long) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putLong(KEY_LAST_SYNC_MOVIES, timeInMs)
        }
    }

    fun getLastListsSyncTime(context: Context): Long {
        return getLastSyncTime(context, KEY_LAST_SYNC_LISTS)
    }

    fun setLastListsSyncTime(context: Context, timeInMs: Long) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putLong(KEY_LAST_SYNC_LISTS, timeInMs)
        }
    }

    private fun getLastSyncTime(context: Context, key: String): Long {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        var lastSync = prefs.getLong(key, 0)
        if (lastSync == 0L) {
            lastSync = System.currentTimeMillis() // not synced yet, then last time is now!
            prefs.edit { putLong(key, lastSync) }
        }
        return lastSync
    }
}