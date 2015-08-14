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

package com.battlelancer.seriesguide.util;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.EpisodeHistory;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.MovieHistory;
import timber.log.Timber;

/**
 * Helper methods for dealing with the {@link com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables#EPISODE_HISTORY}
 * and {@link com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables#MOVIE_HISTORY}
 * tables.
 */
public class HistoryTools {

    private static final long HISTORY_THRESHOLD = 30 * DateUtils.DAY_IN_MILLIS;

    /**
     * Adds an activity entry for the given episode with the current time as timestamp. If an entry
     * already exists it is replaced.
     *
     * <p>Also cleans up old entries.
     */
    public static void addEpisodeActivity(Context context, int episodeTvdbId, int showTvdbId) {
        deleteOutdatedActivity(context, EpisodeHistory.CONTENT_URI, EpisodeHistory.TIMESTAMP);

        // add new entry
        ContentValues values = new ContentValues();
        values.put(EpisodeHistory.EPISODE_TVDB_ID, episodeTvdbId);
        values.put(EpisodeHistory.SHOW_TVDB_ID, showTvdbId);
        long currentTime = System.currentTimeMillis();
        values.put(EpisodeHistory.TIMESTAMP, currentTime);

        context.getContentResolver()
                .insert(EpisodeHistory.CONTENT_URI, values);
        Timber.d("addEpisodeActivity: episode: %s timestamp: %s", episodeTvdbId, currentTime);
    }

    /**
     * Tries to remove any activity with the given episode TheTVDB id.
     */
    public static void removeEpisodeActivity(Context context, int episodeTvdbId) {
        int deleted = context.getContentResolver()
                .delete(EpisodeHistory.CONTENT_URI,
                        EpisodeHistory.EPISODE_TVDB_ID + "=" + episodeTvdbId,
                        null);
        Timber.d("removeEpisodeActivity: deleted %s activity entries", deleted);
    }

    /**
     * Adds an activity entry for the given movie with the current time as timestamp. If an entry
     * already exists it is replaced.
     *
     * <p>Also cleans up old entries.
     */
    public static void addMovieActivity(Context context, int movieTmdbId) {
        deleteOutdatedActivity(context, MovieHistory.CONTENT_URI, MovieHistory.TIMESTAMP);

        // add new entry
        ContentValues values = new ContentValues();
        values.put(MovieHistory.MOVIE_TMDB_ID, movieTmdbId);
        long currentTime = System.currentTimeMillis();
        values.put(MovieHistory.TIMESTAMP, currentTime);

        context.getContentResolver().insert(MovieHistory.CONTENT_URI, values);
        Timber.d("addMovieActivity: movie: %s timestamp: %s", movieTmdbId, currentTime);
    }

    /**
     * Tries to remove any activity with the given movie TMDb id.
     */
    public static void removeMovieActivity(Context context, int movieTmdbId) {
        int deleted = context.getContentResolver()
                .delete(MovieHistory.CONTENT_URI,
                        MovieHistory.MOVIE_TMDB_ID + "=" + movieTmdbId,
                        null);
        Timber.d("removeMovieActivity: deleted %s activity entries", deleted);
    }

    private static void deleteOutdatedActivity(Context context, Uri uri, String timestampColumn) {
        long timeMonthAgo = System.currentTimeMillis() - HISTORY_THRESHOLD;

        // delete all entries older than 30 days
        int deleted = context.getContentResolver()
                .delete(uri, timestampColumn + "<" + timeMonthAgo, null);
        Timber.d("removeOldActivity: removed %s outdated activities from %s", deleted, uri);
    }
}
