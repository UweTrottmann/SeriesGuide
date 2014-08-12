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

import android.content.Context;
import android.os.AsyncTask;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.TraktSummaryTask.RatingsWrapper;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.Ratings;
import com.jakewharton.trakt.entities.TvEntity;
import com.jakewharton.trakt.entities.TvShow;
import com.jakewharton.trakt.enumerations.Extended;
import com.jakewharton.trakt.enumerations.Rating;
import com.uwetrottmann.androidutils.AndroidUtils;
import retrofit.RetrofitError;
import timber.log.Timber;

public class TraktSummaryTask extends AsyncTask<Void, Void, RatingsWrapper> {

    private static final int HARD_CACHE_CAPACITY = 10;

    private static final long MAXIMUM_AGE = 5 * DateUtils.MINUTE_IN_MILLIS;

    // Hard cache, with a fixed maximum capacity
    private final static android.support.v4.util.LruCache<Long, RatingsWrapper> sCache
            = new android.support.v4.util.LruCache<>(HARD_CACHE_CAPACITY);

    private View mView;

    private Context mContext;

    private int mShowTvdbId;

    private int mEpisodeTvdbId;

    private int mSeason;

    private int mEpisode;

    private TextView mTraktLoves;

    private TextView mTraktVotes;

    private TextView mTraktUserRating;
    private boolean mDoCacheLookup;

    /**
     * Sets values for predefined views which have to be children of the given view. Make sure to
     * call either {@code show(tvdbId)} or {@code episode(tvdbId, season, episode)}, too.
     */
    public TraktSummaryTask(Context context, View view, boolean isUseCachedValue) {
        mView = view;
        mContext = context;
        mDoCacheLookup = isUseCachedValue;
    }

    public TraktSummaryTask show(int showTvdbId) {
        mShowTvdbId = showTvdbId;
        mEpisodeTvdbId = 0;
        return this;
    }

    public TraktSummaryTask episode(int showTvdbId, int episodeTvdbId, int season, int episode) {
        mShowTvdbId = showTvdbId;
        mEpisodeTvdbId = episodeTvdbId;
        mSeason = season;
        mEpisode = episode;
        return this;
    }

    @Override
    protected void onPreExecute() {
        mTraktLoves = (TextView) mView.findViewById(R.id.textViewRatingsTraktValue);
        mTraktVotes = (TextView) mView.findViewById(R.id.textViewRatingsTraktVotes);
        mTraktUserRating = (TextView) mView.findViewById(R.id.textViewRatingsTraktUser);
    }

    @Override
    protected RatingsWrapper doInBackground(Void... params) {
        RatingsWrapper ratings = null;

        long currentTime = System.currentTimeMillis();
        long key = createUniqueId(mShowTvdbId, mEpisodeTvdbId);

        // cache lookup
        if (mDoCacheLookup) {
            synchronized (sCache) {
                ratings = sCache.get(key);
            }
            if (ratings != null && ratings.downloadedAt > currentTime - MAXIMUM_AGE) {
                return ratings;
            }
        }

        if (isCancelled() || !AndroidUtils.isNetworkConnected(mContext)) {
            return null;
        }

        try {
            // decide whether we have a show or an episode
            if (mEpisodeTvdbId == 0) {
                // get the shows summary from trakt
                TvShow show = getTrakt().showService().summary(mShowTvdbId, Extended.DEFAULT);
                if (show != null) {
                    ratings = new RatingsWrapper();
                    ratings.rating = show.rating_advanced;
                    ratings.ratings = show.ratings;
                }
            } else {
                // on cache miss load the summary from trakt
                TvEntity tvEntity = getTrakt().showService()
                        .episodeSummary(mShowTvdbId, mSeason, mEpisode);
                if (tvEntity != null && tvEntity.episode != null) {
                    ratings = new RatingsWrapper();
                    ratings.rating = tvEntity.episode.rating_advanced;
                    ratings.ratings = tvEntity.episode.ratings;
                }
            }
        } catch (RetrofitError e) {
            Timber.e(e, "Loading ratings failed");
            return null;
        }

        if (ratings != null) {
            ratings.downloadedAt = currentTime;
            synchronized (sCache) {
                sCache.put(key, ratings);
            }
        }

        return ratings;
    }

    @Override
    protected void onCancelled() {
        releaseReferences();
    }

    @Override
    protected void onPostExecute(RatingsWrapper results) {
        // set the final rating values
        if (results != null
                && mTraktLoves != null && mTraktVotes != null && mTraktUserRating != null) {
            mTraktUserRating.setText(TraktTools.buildUserRatingString(mContext, results.rating));
            if (results.ratings != null) {
                mTraktLoves.setText(
                        TraktTools.buildRatingPercentageString(results.ratings.percentage));
                mTraktVotes.setText(
                        TraktTools.buildRatingVotesString(mContext, results.ratings.votes));
            }
        }

        releaseReferences();
    }

    private void releaseReferences() {
        mContext = null;
        mTraktLoves = null;
        mTraktVotes = null;
        mView = null;
    }

    /**
     * Creates a unique id using the <a href="https://en.wikipedia.org/wiki/Cantor_pairing_function">Cantor
     * pairing</a> function.
     */
    private long createUniqueId(int showTvdbId, int episodeTvdbId) {
        return ((showTvdbId + episodeTvdbId) * (showTvdbId + episodeTvdbId + 1) / 2)
                + episodeTvdbId;
    }

    private Trakt getTrakt() {
        Trakt trakt = ServiceUtils.getTraktWithAuth(mContext);
        if (trakt == null) {
            // don't have auth data
            trakt = ServiceUtils.getTrakt(mContext);
        }
        return trakt;
    }

    static class RatingsWrapper {

        long downloadedAt;

        Ratings ratings;

        Rating rating;
    }
}
