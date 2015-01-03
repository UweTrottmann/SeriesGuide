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
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.Ratings;
import retrofit.RetrofitError;
import timber.log.Timber;

public class TraktRatingsTask extends AsyncTask<Void, Void, Void> {

    private static final int HARD_CACHE_CAPACITY = 50;
    private static final long MAXIMUM_AGE = 5 * DateUtils.MINUTE_IN_MILLIS;

    // Hard cache, with a fixed maximum capacity
    private final static android.support.v4.util.LruCache<Long, Long> sCache
            = new android.support.v4.util.LruCache<>(HARD_CACHE_CAPACITY);

    private Context mContext;

    private int mShowTvdbId;

    private int mEpisodeTvdbId;

    private int mSeason;

    private int mEpisode;

    /**
     * Loads the latest ratings for the given show from trakt and saves them to the database. If
     * ratings were loaded recently, might do nothing.
     */
    public TraktRatingsTask(Context context, int showTvdbId) {
        mContext = context;
        mShowTvdbId = showTvdbId;
        mEpisodeTvdbId = 0;
    }

    /**
     * Loads the latest ratings for the given episode from trakt and saves them to the database. If
     * ratings were loaded recently, might do nothing.
     */
    public TraktRatingsTask(Context context, int showTvdbId, int episodeTvdbId, int season,
            int episode) {
        this(context, showTvdbId);
        mEpisodeTvdbId = episodeTvdbId;
        mSeason = season;
        mEpisode = episode;
    }

    @Override
    protected Void doInBackground(Void... params) {
        long ratingId = createUniqueId(mShowTvdbId, mEpisodeTvdbId);
        long currentTimeMillis = System.currentTimeMillis();

        // avoid saving ratings too frequently
        // (network requests are cached, but also avoiding database writes)
        synchronized (sCache) {
            Long lastUpdateMillis = sCache.get(ratingId);
            // if the ratings were just updated, do nothing
            if (lastUpdateMillis != null && lastUpdateMillis > currentTimeMillis - MAXIMUM_AGE) {
                Timber.d("Skip loading ratings for " + ratingId + ": just recently did");
                return null;
            }
        }

        if (isCancelled() || !AndroidUtils.isNetworkConnected(mContext)) {
            return null;
        }

        TraktV2 trakt = ServiceUtils.getTraktV2(mContext);

        try {
            // look up show trakt id
            String showTraktId = TraktTools.lookupShowTraktId(mContext, mShowTvdbId);
            if (showTraktId == null) {
                return null;
            }

            if (mEpisodeTvdbId == 0) {
                // download latest show ratings
                Ratings ratings = trakt.shows().ratings(showTraktId);
                if (ratings == null || ratings.rating == null || ratings.votes == null) {
                    return null;
                }
                // save ratings to database
                ContentValues values = new ContentValues();
                values.put(SeriesGuideContract.Shows.RATING_GLOBAL, ratings.rating);
                values.put(SeriesGuideContract.Shows.RATING_VOTES, ratings.votes);
                mContext.getContentResolver()
                        .update(SeriesGuideContract.Shows.buildShowUri(mShowTvdbId), values, null,
                                null);
            } else {
                // download latest episode ratings
                Ratings ratings = trakt.episodes().ratings(showTraktId, mSeason, mEpisode);
                if (ratings == null || ratings.rating == null || ratings.votes == null) {
                    return null;
                }
                // save ratings to database
                ContentValues values = new ContentValues();
                values.put(SeriesGuideContract.Episodes.RATING_GLOBAL, ratings.rating);
                values.put(SeriesGuideContract.Episodes.RATING_VOTES, ratings.votes);
                mContext.getContentResolver()
                        .update(SeriesGuideContract.Episodes.buildEpisodeUri(mEpisodeTvdbId),
                                values, null, null);
            }
        } catch (RetrofitError e) {
            Timber.e(e, "Loading ratings failed");
        }

        // cache download time to avoid saving ratings too frequently
        synchronized (sCache) {
            sCache.put(ratingId, currentTimeMillis);
        }

        return null;
    }

    @Override
    protected void onCancelled() {
        releaseReferences();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        releaseReferences();
    }

    /**
     * Creates a unique id using the <a href="https://en.wikipedia.org/wiki/Cantor_pairing_function">Cantor
     * pairing</a> function.
     */
    private long createUniqueId(int showTvdbId, int episodeTvdbId) {
        return ((showTvdbId + episodeTvdbId) * (showTvdbId + episodeTvdbId + 1) / 2)
                + episodeTvdbId;
    }

    private void releaseReferences() {
        mContext = null;
    }
}
