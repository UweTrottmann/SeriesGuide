// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0
package com.battlelancer.seriesguide.traktapi

import android.content.Context
import android.text.format.DateUtils
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.util.TimeTools
import org.threeten.bp.OffsetDateTime

/**
 * Settings related to trakt.tv integration.
 */
object TraktSettings {

    const val KEY_LAST_ACTIVITY_DOWNLOAD
            : String = "com.battlelancer.seriesguide.lasttraktupdate"

    const val KEY_LAST_SHOWS_RATED_AT
            : String = "trakt.last_activity.shows.rated"

    const val KEY_LAST_EPISODES_WATCHED_AT
            : String = "trakt.last_activity.episodes.watched"

    const val KEY_LAST_EPISODES_COLLECTED_AT
            : String = "trakt.last_activity.episodes.collected"

    const val KEY_LAST_EPISODES_RATED_AT
            : String = "trakt.last_activity.episodes.rated"

    const val KEY_LAST_MOVIES_WATCHLISTED_AT
            : String = "trakt.last_activity.movies.watchlisted"

    const val KEY_LAST_MOVIES_COLLECTED_AT
            : String = "trakt.last_activity.movies.collected"

    const val KEY_LAST_MOVIES_RATED_AT
            : String = "trakt.last_activity.movies.rated"

    const val KEY_LAST_MOVIES_WATCHED_AT
            : String = "trakt.last_activity.movies.watched"

    const val KEY_LAST_FULL_EPISODE_SYNC
            : String = "com.battlelancer.seriesguide.trakt.lastfullsync"

    const val KEY_AUTO_ADD_TRAKT_SHOWS
            : String = "com.battlelancer.seriesguide.autoaddtraktshows"

    const val KEY_HAS_MERGED_EPISODES: String = "com.battlelancer.seriesguide.trakt.mergedepisodes"

    const val KEY_HAS_MERGED_MOVIES
            : String = "com.battlelancer.seriesguide.trakt.mergedmovies"

    const val KEY_QUICK_CHECKIN
            : String = "com.battlelancer.seriesguide.trakt.quickcheckin"

    private const val FULL_SYNC_INTERVAL_MILLIS = 24 * DateUtils.HOUR_IN_MILLIS

    /**
     * The last time trakt episode activity was successfully downloaded.
     */
    fun getLastActivityDownloadTime(context: Context): Long {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(KEY_LAST_ACTIVITY_DOWNLOAD, System.currentTimeMillis())
    }

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

    fun isAutoAddingShows(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_AUTO_ADD_TRAKT_SHOWS, true)
    }

    /**
     * Whether watched and collected episodes were merged with the users trakt profile since she
     * connected to trakt.
     */
    fun hasMergedEpisodes(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_HAS_MERGED_EPISODES, true)
    }

    /**
     * Whether the list of movies was merged with the users trakt profile since she connected to
     * trakt.
     */
    fun hasMergedMovies(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_HAS_MERGED_MOVIES, false)
    }

    /**
     * Determines if enough time has passed since the last full trakt episode sync.
     */
    fun isTimeForFullEpisodeSync(context: Context, currentTime: Long): Boolean {
        val previousUpdateTime = PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(KEY_LAST_FULL_EPISODE_SYNC, currentTime)
        return (currentTime - previousUpdateTime) > FULL_SYNC_INTERVAL_MILLIS
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
