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

package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import com.uwetrottmann.trakt.v2.entities.LastActivityMore;

/**
 * Settings related to trakt.tv integration.
 */
public class TraktSettings {

    public static final String KEY_LAST_ACTIVITY_DOWNLOAD
            = "com.battlelancer.seriesguide.lasttraktupdate";

    public static final String KEY_LAST_ACTIVITY_EPISODES_WATCHED
            = "trakt.last_activity.episodes.watched";

    public static final String KEY_LAST_ACTIVITY_EPISODES_COLLECTED
            = "trakt.last_activity.episodes.collected";

    public static final String KEY_LAST_ACTIVITY_MOVIES_WATCHLISTED
            = "trakt.last_activity.movies.watchlisted";

    public static final String KEY_LAST_ACTIVITY_MOVIES_COLLECTED
            = "trakt.last_activity.movies.collected";

    public static final String KEY_LAST_FULL_EPISODE_SYNC
            = "com.battlelancer.seriesguide.trakt.lastfullsync";

    public static final String KEY_SHARE_WITH_TRAKT = "com.battlelancer.seriesguide.sharewithtrakt";

    public static final String KEY_AUTO_ADD_TRAKT_SHOWS
            = "com.battlelancer.seriesguide.autoaddtraktshows";

    public static final String KEY_SYNC_UNWATCHED_EPISODES
            = "com.battlelancer.seriesguide.syncunseenepisodes";

    public static final String KEY_HAS_MERGED_EPISODES =
            "com.battlelancer.seriesguide.trakt.mergedepisodes";

    public static final String KEY_HAS_MERGED_MOVIES
            = "com.battlelancer.seriesguide.trakt.mergedmovies";

    private static final long FULL_SYNC_INTERVAL_MILLIS = 24 * DateUtils.HOUR_IN_MILLIS;

    public static final String POSTER_SIZE_SPEC_DEFAULT = ".jpg";

    public static final String POSTER_SIZE_SPEC_138 = "-138.jpg";
    public static final String POSTER_SIZE_SPEC_300 = "-300.jpg";

    /**
     * The last time trakt episode activity was successfully downloaded.
     */
    public static long getLastActivityDownloadTime(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_LAST_ACTIVITY_DOWNLOAD, System.currentTimeMillis());
    }

    /**
     * The last time watched flags for episodes have changed.
     */
    public static long getLastActivityEpisodesWatched(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_LAST_ACTIVITY_EPISODES_WATCHED, 0);
    }

    /**
     * The last time collected flags for episodes have changed.
     */
    public static long getLastActivityEpisodesCollected(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_LAST_ACTIVITY_EPISODES_COLLECTED, 0);
    }

    /**
     * The last time watched flags for movies have changed.
     */
    public static long getLastActivityMoviesWatchlisted(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_LAST_ACTIVITY_MOVIES_WATCHLISTED, 0);
    }

    /**
     * If either collection or watchlist have changes newer than last stored.
     */
    public static boolean isTraktMovieListsChanged(Context context, LastActivityMore activity) {
        long lastCollected = activity.collected_at == null ? 0
                : activity.collected_at.getMillis();
        long lastWatchlisted = activity.watchlisted_at == null ? 0
                : activity.watchlisted_at.getMillis();

        // if either collection or watchlist has changed, return true
        return lastCollected > TraktSettings.getLastActivityMoviesCollected(context)
                || lastWatchlisted > TraktSettings.getLastActivityMoviesWatchlisted(context);
    }

    /**
     * Store last collected and watchlisted timestamps.
     */
    public static void storeLastActivityMovies(Context context, LastActivityMore activity) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context)
                .edit();
        if (activity.collected_at != null) {
            editor.putLong(TraktSettings.KEY_LAST_ACTIVITY_MOVIES_COLLECTED,
                    activity.collected_at.getMillis());
        }
        if (activity.watchlisted_at != null) {
            editor.putLong(TraktSettings.KEY_LAST_ACTIVITY_MOVIES_WATCHLISTED,
                    activity.watchlisted_at.getMillis());
        }
        editor.apply();
    }

    /**
     * The last time collected flags for movies have changed.
     */
    public static long getLastActivityMoviesCollected(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_LAST_ACTIVITY_MOVIES_COLLECTED, 0);
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

    public static boolean isSharingWithTrakt(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_SHARE_WITH_TRAKT, false);
    }

    public static boolean isSyncingUnwatchedEpisodes(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_SYNC_UNWATCHED_EPISODES, false);
    }

    /**
     * Determines if enough time has passed since the last full trakt episode sync.
     */
    public static boolean isTimeForFullEpisodeSync(Context context, long currentTime) {
        long previousUpdateTime = PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_LAST_FULL_EPISODE_SYNC, currentTime);
        return (currentTime - previousUpdateTime) > FULL_SYNC_INTERVAL_MILLIS;
    }
}
