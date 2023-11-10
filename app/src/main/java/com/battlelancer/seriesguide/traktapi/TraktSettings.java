// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.traktapi;

import android.content.Context;
import android.text.format.DateUtils;
import androidx.preference.PreferenceManager;
import com.battlelancer.seriesguide.util.TimeTools;
import org.threeten.bp.OffsetDateTime;

/**
 * Settings related to trakt.tv integration.
 */
public class TraktSettings {

    public static final String KEY_LAST_ACTIVITY_DOWNLOAD
            = "com.battlelancer.seriesguide.lasttraktupdate";

    public static final String KEY_LAST_SHOWS_RATED_AT
            = "trakt.last_activity.shows.rated";

    public static final String KEY_LAST_EPISODES_WATCHED_AT
            = "trakt.last_activity.episodes.watched";

    public static final String KEY_LAST_EPISODES_COLLECTED_AT
            = "trakt.last_activity.episodes.collected";

    public static final String KEY_LAST_EPISODES_RATED_AT
            = "trakt.last_activity.episodes.rated";

    public static final String KEY_LAST_MOVIES_WATCHLISTED_AT
            = "trakt.last_activity.movies.watchlisted";

    public static final String KEY_LAST_MOVIES_COLLECTED_AT
            = "trakt.last_activity.movies.collected";

    public static final String KEY_LAST_MOVIES_RATED_AT
            = "trakt.last_activity.movies.rated";

    public static final String KEY_LAST_MOVIES_WATCHED_AT
            = "trakt.last_activity.movies.watched";

    public static final String KEY_LAST_FULL_EPISODE_SYNC
            = "com.battlelancer.seriesguide.trakt.lastfullsync";

    public static final String KEY_AUTO_ADD_TRAKT_SHOWS
            = "com.battlelancer.seriesguide.autoaddtraktshows";

    public static final String KEY_HAS_MERGED_EPISODES =
            "com.battlelancer.seriesguide.trakt.mergedepisodes";

    public static final String KEY_HAS_MERGED_MOVIES
            = "com.battlelancer.seriesguide.trakt.mergedmovies";

    public static final String KEY_QUICK_CHECKIN
            = "com.battlelancer.seriesguide.trakt.quickcheckin";

    private static final long FULL_SYNC_INTERVAL_MILLIS = 24 * DateUtils.HOUR_IN_MILLIS;

    /**
     * The last time trakt episode activity was successfully downloaded.
     */
    public static long getLastActivityDownloadTime(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_LAST_ACTIVITY_DOWNLOAD, System.currentTimeMillis());
    }

    /**
     * The last time show ratings have changed or 0 if no value exists.
     */
    public static long getLastShowsRatedAt(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_LAST_SHOWS_RATED_AT, 0);
    }

    /**
     * The last time watched flags for episodes have changed.
     */
    public static long getLastEpisodesWatchedAt(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_LAST_EPISODES_WATCHED_AT, 0);
    }

    /**
     * The last time collected flags for episodes have changed.
     */
    public static long getLastEpisodesCollectedAt(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_LAST_EPISODES_COLLECTED_AT, 0);
    }

    /**
     * The last time episode ratings have changed or 0 if no value exists.
     */
    public static long getLastEpisodesRatedAt(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_LAST_EPISODES_RATED_AT, 0);
    }

    /**
     * The last time watched flags for movies have changed.
     */
    public static long getLastMoviesWatchlistedAt(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_LAST_MOVIES_WATCHLISTED_AT, 0);
    }

    /**
     * The last time collected flags for movies have changed.
     */
    public static long getLastMoviesCollectedAt(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_LAST_MOVIES_COLLECTED_AT, 0);
    }

    /**
     * The last time movie ratings have changed or 0 if no value exists.
     */
    public static long getLastMoviesRatedAt(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_LAST_MOVIES_RATED_AT, 0);
    }

    /**
     * The last time movie watched flags have changed or 0 if no value exists.
     */
    public static long getLastMoviesWatchedAt(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_LAST_MOVIES_WATCHED_AT, 0);
    }

    /**
     * If either collection, watchlist or watched list have changes newer than last stored.
     */
    public static boolean isMovieListsChanged(
            Context context,
            OffsetDateTime collectedAt,
            OffsetDateTime watchlistedAt,
            OffsetDateTime watchedAt
    ) {
        return TimeTools.isAfterMillis(collectedAt, getLastMoviesCollectedAt(context))
                || TimeTools.isAfterMillis(watchlistedAt, getLastMoviesWatchlistedAt(context))
                || TimeTools.isAfterMillis(watchedAt, getLastMoviesWatchedAt(context));
    }

    /**
     * Store last collected, watchlisted and watched timestamps.
     */
    public static void storeLastMoviesChangedAt(
            Context context,
            OffsetDateTime collectedAt,
            OffsetDateTime watchlistedAt,
            OffsetDateTime watchedAt
    ) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong(TraktSettings.KEY_LAST_MOVIES_COLLECTED_AT,
                        collectedAt.toInstant().toEpochMilli())
                .putLong(TraktSettings.KEY_LAST_MOVIES_WATCHLISTED_AT,
                        watchlistedAt.toInstant().toEpochMilli())
                .putLong(TraktSettings.KEY_LAST_MOVIES_WATCHED_AT,
                        watchedAt.toInstant().toEpochMilli())
                .apply();
    }

    /**
     * Reset {@link #KEY_LAST_MOVIES_RATED_AT} to 0 so all movie ratings will be downloaded the next
     * time a sync runs.
     */
    public static boolean resetMoviesLastRatedAt(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong(TraktSettings.KEY_LAST_MOVIES_RATED_AT, 0)
                .commit();
    }

    /**
     * Remove {@link #KEY_LAST_MOVIES_WATCHED_AT} so all watched movies will be downloaded the
     * next time a sync runs.
     */
    public static boolean resetMoviesLastWatchedAt(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .remove(KEY_LAST_MOVIES_WATCHED_AT)
                .commit();
    }

    public static boolean isAutoAddingShows(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_AUTO_ADD_TRAKT_SHOWS, true);
    }

    /**
     * Whether watched and collected episodes were merged with the users trakt profile since she
     * connected to trakt.
     */
    public static boolean hasMergedEpisodes(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_HAS_MERGED_EPISODES, true);
    }

    /**
     * Whether the list of movies was merged with the users trakt profile since she connected to
     * trakt.
     */
    public static boolean hasMergedMovies(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_HAS_MERGED_MOVIES, false);
    }

    /**
     * Determines if enough time has passed since the last full trakt episode sync.
     */
    public static boolean isTimeForFullEpisodeSync(Context context, long currentTime) {
        long previousUpdateTime = PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_LAST_FULL_EPISODE_SYNC, currentTime);
        return (currentTime - previousUpdateTime) > FULL_SYNC_INTERVAL_MILLIS;
    }

    /**
     * Whether the check-in dialog should not wait for the user to enter a message, but immediately
     * start the check-in.
     */
    public static boolean useQuickCheckin(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_QUICK_CHECKIN, false);
    }
}
