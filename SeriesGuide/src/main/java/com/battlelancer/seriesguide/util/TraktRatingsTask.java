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
import android.os.AsyncTask;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.Ratings;
import java.io.IOException;
import retrofit2.Response;
import timber.log.Timber;

public class TraktRatingsTask extends AsyncTask<Void, Void, Void> {

    private static final int HARD_CACHE_CAPACITY = 50;
    private static final long MAXIMUM_AGE = 5 * DateUtils.MINUTE_IN_MILLIS;

    // Hard cache, with a fixed maximum capacity
    private final static android.support.v4.util.LruCache<Long, Long> sCache
            = new android.support.v4.util.LruCache<>(HARD_CACHE_CAPACITY);

    private final Context context;
    private final int showTvdbId;
    private final int episodeTvdbId;
    private final int season;
    private final int episode;

    /**
     * Loads the latest ratings for the given show from trakt and saves them to the database. If
     * ratings were loaded recently, might do nothing.
     */
    public TraktRatingsTask(Context context, int showTvdbId) {
        this(context, showTvdbId, 0, 0, 0);
    }

    /**
     * Loads the latest ratings for the given episode from trakt and saves them to the database. If
     * ratings were loaded recently, might do nothing.
     */
    public TraktRatingsTask(Context context, int showTvdbId, int episodeTvdbId, int season,
            int episode) {
        this.context = context.getApplicationContext();
        this.showTvdbId = showTvdbId;
        this.episodeTvdbId = episodeTvdbId;
        this.season = season;
        this.episode = episode;
    }

    @Override
    protected Void doInBackground(Void... params) {
        long ratingId = createUniqueId(showTvdbId, episodeTvdbId);

        // avoid saving ratings too frequently
        // (network requests are cached, but also avoiding database writes)
        long currentTimeMillis = System.currentTimeMillis();
        synchronized (sCache) {
            Long lastUpdateMillis = sCache.get(ratingId);
            // if the ratings were just updated, do nothing
            if (lastUpdateMillis != null && lastUpdateMillis > currentTimeMillis - MAXIMUM_AGE) {
                Timber.d("Just loaded rating for %s, skip.", ratingId);
                return null;
            }
        }

        if (isCancelled() || !AndroidUtils.isNetworkConnected(context)) {
            return null;
        }

        // look up show trakt id
        Integer showTraktId = ShowTools.getShowTraktId(context, showTvdbId);
        if (showTraktId == null) {
            Timber.d("Show %s has no trakt id, skip.", showTvdbId);
            return null;
        }
        String showTraktIdString = String.valueOf(showTraktId);

        String action = null;
        TraktV2 trakt = ServiceUtils.getTrakt(context);
        boolean isShowNotEpisode = episodeTvdbId == 0;
        try {
            Response<Ratings> response;
            if (isShowNotEpisode) {
                action = "get show rating";
                response = trakt.shows().ratings(showTraktIdString).execute();
            } else {
                action = "get episode rating";
                response = trakt.episodes().ratings(showTraktIdString, season, episode)
                        .execute();
            }
            if (response.isSuccessful()) {
                Ratings ratings = response.body();
                if (ratings != null && ratings.rating != null && ratings.votes != null) {
                    if (isShowNotEpisode) {
                        saveEpisodeRating(ratings);
                    } else {
                        saveShowRating(ratings);
                    }
                }
            } else {
                SgTrakt.trackFailedRequest(context, action, response);
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(context, action, e);
        }

        // cache download time to avoid saving ratings too frequently
        synchronized (sCache) {
            sCache.put(ratingId, currentTimeMillis);
        }

        return null;
    }

    private void saveEpisodeRating(Ratings ratings) {
        ContentValues values = new ContentValues();
        values.put(SeriesGuideContract.Episodes.RATING_GLOBAL, ratings.rating);
        values.put(SeriesGuideContract.Episodes.RATING_VOTES, ratings.votes);
        context.getContentResolver()
                .update(SeriesGuideContract.Episodes.buildEpisodeUri(episodeTvdbId), values, null,
                        null);
    }

    private void saveShowRating(Ratings ratings) {
        ContentValues values = new ContentValues();
        values.put(SeriesGuideContract.Shows.RATING_GLOBAL, ratings.rating);
        values.put(SeriesGuideContract.Shows.RATING_VOTES, ratings.votes);
        context.getContentResolver()
                .update(SeriesGuideContract.Shows.buildShowUri(showTvdbId), values, null, null);
    }

    /**
     * Creates a unique id using the <a href="https://en.wikipedia.org/wiki/Cantor_pairing_function">Cantor
     * pairing</a> function.
     */
    private long createUniqueId(int showTvdbId, int episodeTvdbId) {
        return ((showTvdbId + episodeTvdbId) * (showTvdbId + episodeTvdbId + 1) / 2)
                + episodeTvdbId;
    }
}
