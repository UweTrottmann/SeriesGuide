// SPDX-License-Identifier: Apache-2.0
// Copyright 2013-2024 Uwe Trottmann

package com.battlelancer.seriesguide.traktapi

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.util.TimeTools
import org.threeten.bp.OffsetDateTime

/**
 * Settings related to Trakt integration.
 */
object TraktSettings {

    /**
     * Unused, but kept for reference.
     *
     * Replaced by type specific last activity timestamps.
     */
    private const val KEY_LAST_ACTIVITY_DOWNLOAD
            : String = "com.battlelancer.seriesguide.lasttraktupdate"

    const val KEY_LAST_SHOWS_RATED_AT
            : String = "trakt.last_activity.shows.rated"

    const val KEY_LAST_EPISODES_WATCHED_AT
            : String = "trakt.last_activity.episodes.watched"

    const val KEY_LAST_EPISODES_COLLECTED_AT
            : String = "trakt.last_activity.episodes.collected"

    const val KEY_LAST_EPISODES_RATED_AT
            : String = "trakt.last_activity.episodes.rated"

    private const val KEY_LAST_MOVIES_WATCHLISTED_AT
            : String = "trakt.last_activity.movies.watchlisted"

    private const val KEY_LAST_MOVIES_COLLECTED_AT
            : String = "trakt.last_activity.movies.collected"

    const val KEY_LAST_MOVIES_RATED_AT
            : String = "trakt.last_activity.movies.rated"

    private const val KEY_LAST_MOVIES_WATCHED_AT
            : String = "trakt.last_activity.movies.watched"

    /**
     * Unused, but kept for reference.
     *
     * Replaced by [KEY_LAST_EPISODES_WATCHED_AT] and [KEY_LAST_EPISODES_COLLECTED_AT].
     */
    private const val KEY_LAST_FULL_EPISODE_SYNC
            : String = "com.battlelancer.seriesguide.trakt.lastfullsync"

    /**
     * Unused, but kept for reference.
     */
    private const val KEY_AUTO_ADD_TRAKT_SHOWS
            : String = "com.battlelancer.seriesguide.autoaddtraktshows"

    private const val KEY_HAS_MERGED_EPISODES
            : String = "com.battlelancer.seriesguide.trakt.mergedepisodes"

    private const val KEY_HAS_MERGED_MOVIES
            : String = "com.battlelancer.seriesguide.trakt.mergedmovies"

    /**
     * Used in settings_basic.xml.
     */
    private const val KEY_QUICK_CHECKIN
            : String = "com.battlelancer.seriesguide.trakt.quickcheckin"

    /**
     * The last time show ratings have changed or 0 if no value exists.
     */
    fun getLastShowsRatedAt(context: Context): Long {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(KEY_LAST_SHOWS_RATED_AT, 0)
    }

    /**
     * The last time watched flags for episodes have changed.
     */
    fun getLastEpisodesWatchedAt(context: Context): Long {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(KEY_LAST_EPISODES_WATCHED_AT, 0)
    }

    /**
     * The last time collected flags for episodes have changed.
     */
    fun getLastEpisodesCollectedAt(context: Context): Long {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(KEY_LAST_EPISODES_COLLECTED_AT, 0)
    }

    /**
     * The last time episode ratings have changed or 0 if no value exists.
     */
    fun getLastEpisodesRatedAt(context: Context): Long {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(KEY_LAST_EPISODES_RATED_AT, 0)
    }

    /**
     * The last time watched flags for movies have changed.
     */
    fun getLastMoviesWatchlistedAt(context: Context): Long {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(KEY_LAST_MOVIES_WATCHLISTED_AT, 0)
    }

    /**
     * The last time collected flags for movies have changed.
     */
    fun getLastMoviesCollectedAt(context: Context): Long {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(KEY_LAST_MOVIES_COLLECTED_AT, 0)
    }

    /**
     * The last time movie ratings have changed or 0 if no value exists.
     */
    fun getLastMoviesRatedAt(context: Context): Long {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(KEY_LAST_MOVIES_RATED_AT, 0)
    }

    /**
     * The last time movie watched flags have changed or 0 if no value exists.
     */
    fun getLastMoviesWatchedAt(context: Context): Long {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(KEY_LAST_MOVIES_WATCHED_AT, 0)
    }

    /**
     * If either collection, watchlist or watched list have changes newer than last stored.
     */
    fun isMovieListsChanged(
        context: Context,
        collectedAt: OffsetDateTime,
        watchlistedAt: OffsetDateTime,
        watchedAt: OffsetDateTime
    ): Boolean {
        return (TimeTools.isAfterMillis(collectedAt, getLastMoviesCollectedAt(context))
                || TimeTools.isAfterMillis(watchlistedAt, getLastMoviesWatchlistedAt(context))
                || TimeTools.isAfterMillis(watchedAt, getLastMoviesWatchedAt(context)))
    }

    /**
     * Store last collected, watchlisted and watched timestamps.
     */
    fun storeLastMoviesChangedAt(
        context: Context,
        collectedAt: OffsetDateTime,
        watchlistedAt: OffsetDateTime,
        watchedAt: OffsetDateTime
    ) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putLong(
                KEY_LAST_MOVIES_COLLECTED_AT,
                collectedAt.toInstant().toEpochMilli()
            )
            .putLong(
                KEY_LAST_MOVIES_WATCHLISTED_AT,
                watchlistedAt.toInstant().toEpochMilli()
            )
            .putLong(
                KEY_LAST_MOVIES_WATCHED_AT,
                watchedAt.toInstant().toEpochMilli()
            )
            .apply()
    }

    /**
     * Reset [KEY_LAST_MOVIES_RATED_AT] to 0 so all movie ratings will be downloaded the next
     * time a sync runs.
     */
    @JvmStatic
    fun resetMoviesLastRatedAt(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putLong(KEY_LAST_MOVIES_RATED_AT, 0)
            .commit()
    }

    /**
     * Remove [KEY_LAST_MOVIES_WATCHED_AT] so all watched movies will be downloaded the
     * next time a sync runs.
     */
    fun resetMoviesLastWatchedAt(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .remove(KEY_LAST_MOVIES_WATCHED_AT)
            .commit()
    }

    /**
     * Returns if episodes have not been synced with the current Trakt account.
     */
    fun isInitialSyncEpisodes(context: Context): Boolean {
        return !PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_HAS_MERGED_EPISODES, false)
    }

    fun setInitialSyncEpisodesCompleted(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(KEY_HAS_MERGED_EPISODES, true)
        }
    }

    /**
     * Returns if movies have not been synced with the current Trakt account.
     */
    fun isInitialSyncMovies(context: Context): Boolean {
        return !PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_HAS_MERGED_MOVIES, false)
    }

    fun setInitialSyncMoviesCompleted(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(KEY_HAS_MERGED_MOVIES, true)
        }
    }

    fun resetToInitialSync(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(KEY_HAS_MERGED_EPISODES, false)
            putBoolean(KEY_HAS_MERGED_MOVIES, false)
            // Not actually necessary, but also reset timestamps for episodes and movies
            putLong(KEY_LAST_EPISODES_WATCHED_AT, 0)
            putLong(KEY_LAST_EPISODES_COLLECTED_AT, 0)
            putLong(KEY_LAST_MOVIES_WATCHED_AT, 0)

            // Reset timestamps for ratings so they are downloaded immediately
            putLong(KEY_LAST_SHOWS_RATED_AT, 0)
            putLong(KEY_LAST_EPISODES_RATED_AT, 0)
            putLong(KEY_LAST_MOVIES_RATED_AT, 0)
        }
    }

    /**
     * Whether the check-in dialog should not wait for the user to enter a message, but immediately
     * start the check-in.
     */
    fun useQuickCheckin(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_QUICK_CHECKIN, false)
    }
}
