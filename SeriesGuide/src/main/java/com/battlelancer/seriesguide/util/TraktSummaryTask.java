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

import com.battlelancer.seriesguide.util.TraktSummaryTask.RatingsWrapper;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.Ratings;
import com.jakewharton.trakt.entities.TvEntity;
import com.jakewharton.trakt.entities.TvShow;
import com.jakewharton.trakt.enumerations.Extended;
import com.jakewharton.trakt.enumerations.Rating;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.battlelancer.seriesguide.R;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;
import java.util.LinkedHashMap;

import retrofit.RetrofitError;

public class TraktSummaryTask extends AsyncTask<Void, Void, RatingsWrapper> {

    private static final int HARD_CACHE_CAPACITY = 10;

    // Hard cache, with a fixed maximum capacity
    @SuppressWarnings("serial")
    private final static HashMap<String, TvEntity> sHardEntityCache
            = new LinkedHashMap<String, TvEntity>(
            HARD_CACHE_CAPACITY / 2, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(LinkedHashMap.Entry<String, TvEntity> eldest) {
            // remove eldest if capacity is exceeded
            return size() > HARD_CACHE_CAPACITY;
        }
    };

    private View mView;

    private Context mContext;

    private int mShowTvdbId;

    private int mSeason;

    private int mEpisode;

    private TextView mTraktLoves;

    private TextView mTraktVotes;

    private TextView mTraktUserRating;

    private boolean mIsDoCacheLookup;

    /**
     * Sets values for predefined views which have to be children of the given view. Make sure to
     * call either {@code show(tvdbId)} or {@code episode(tvdbId, season, episode)}, too.
     */
    public TraktSummaryTask(Context context, View view, boolean isUseCachedValue) {
        mView = view;
        mContext = context;
        mIsDoCacheLookup = isUseCachedValue;
    }

    public TraktSummaryTask show(int showTvdbId) {
        mShowTvdbId = showTvdbId;
        mSeason = -1;
        return this;
    }

    public TraktSummaryTask episode(int showTvdbId, int season, int episode) {
        mShowTvdbId = showTvdbId;
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

        if (isCancelled()) {
            return null;
        }

        try {
            // decide whether we have a show or an episode
            if (mSeason == -1) {
                if (AndroidUtils.isNetworkConnected(mContext)) {
                    // get the shows summary from trakt
                    TvShow entity = getTrakt().showService().summary(mShowTvdbId, Extended.DEFAULT);
                    if (entity != null) {
                        RatingsWrapper results = new RatingsWrapper();
                        results.rating = entity.rating_advanced;
                        results.ratings = entity.ratings;
                        return results;
                    }
                }
            } else {
                TvEntity entity = null;
                String key = String.valueOf(mShowTvdbId) + String.valueOf(mSeason)
                        + String.valueOf(mEpisode);

                if (mIsDoCacheLookup) {
                    // look if the episode summary is cached

                    synchronized (sHardEntityCache) {
                        entity = sHardEntityCache.remove(key);
                    }
                }

                // on cache miss load the summary from trakt
                if (entity == null && AndroidUtils.isNetworkConnected(mContext)) {
                    entity = getTrakt().showService()
                            .episodeSummary(mShowTvdbId, mSeason, mEpisode);
                }

                if (entity != null) {
                    synchronized (sHardEntityCache) {
                        sHardEntityCache.put(key, entity);
                    }
                    RatingsWrapper results = new RatingsWrapper();
                    results.rating = entity.episode.rating_advanced;
                    results.ratings = entity.episode.ratings;
                    return results;
                }
            }
        } catch (RetrofitError e) {
            return null;
        }

        return null;
    }

    private Trakt getTrakt() {
        Trakt trakt = ServiceUtils.getTraktWithAuth(mContext);
        if (trakt == null) {
            // don't have auth data
            trakt = ServiceUtils.getTrakt(mContext);
        }
        return trakt;
    }

    @Override
    protected void onCancelled() {
        releaseReferences();
    }

    @Override
    protected void onPostExecute(RatingsWrapper results) {
        // set the final rating values
        if (results != null && mTraktLoves != null && mTraktVotes != null
                && mTraktUserRating != null) {
            String rating = results.ratings.percentage + "%";
            if (results.rating != null) {
                int resId = 0;
                switch (results.rating) {
                    case WeakSauce:
                        resId = R.string.hate;
                        break;
                    case Terrible:
                        resId = R.string.rating2;
                        break;
                    case Bad:
                        resId = R.string.rating3;
                        break;
                    case Poor:
                        resId = R.string.rating4;
                        break;
                    case Meh:
                        resId = R.string.rating5;
                        break;
                    case Fair:
                        resId = R.string.rating6;
                        break;
                    case Good:
                        resId = R.string.rating7;
                        break;
                    case Great:
                        resId = R.string.rating8;
                        break;
                    case Superb:
                        resId = R.string.rating9;
                        break;
                    case TotallyNinja:
                        resId = R.string.love;
                        break;
                    default:
                        break;
                }
                if (resId != 0) {
                    mTraktUserRating.setText(mContext.getString(resId));
                }
            }
            mTraktLoves.setText(rating);
            mTraktVotes.setText(mContext.getResources().getQuantityString(R.plurals.votes,
                    results.ratings.votes, results.ratings.votes));
        }

        releaseReferences();
    }

    private void releaseReferences() {
        mContext = null;
        mTraktLoves = null;
        mTraktVotes = null;
        mView = null;
    }

    static class RatingsWrapper {

        Ratings ratings;

        Rating rating;
    }

}
