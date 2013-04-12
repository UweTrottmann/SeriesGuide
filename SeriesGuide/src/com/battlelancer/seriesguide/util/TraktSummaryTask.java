/*
 * Copyright 2012 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;

import com.battlelancer.seriesguide.util.TraktSummaryTask.RatingsWrapper;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.Ratings;
import com.jakewharton.trakt.entities.TvEntity;
import com.jakewharton.trakt.entities.TvShow;
import com.jakewharton.trakt.enumerations.Rating;
import com.uwetrottmann.seriesguide.R;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class TraktSummaryTask extends AsyncTask<Void, Void, RatingsWrapper> {

    private static final int HARD_CACHE_CAPACITY = 10;

    // Hard cache, with a fixed maximum capacity
    @SuppressWarnings("serial")
    private final static HashMap<String, TvEntity> sHardEntityCache = new LinkedHashMap<String, TvEntity>(
            HARD_CACHE_CAPACITY / 2, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(LinkedHashMap.Entry<String, TvEntity> eldest) {
            if (size() > HARD_CACHE_CAPACITY) {
                // remove eldest if capacity is exceeded
                return true;
            } else
                return false;
        }
    };

    private View mView;

    private Context mContext;

    private int mTvdbId;

    private int mSeason;

    private int mEpisode;

    private TextView mTraktLoves;

    private TextView mTraktVotes;

    private String mTvdbIdString;

    private TextView mTraktUserRating;

    private boolean mIsDoCacheLookup;

    /**
     * Sets values for predefined views which have to be children of the given
     * view. Make sure to call either {@code show(tvdbId)} or
     * {@code episode(tvdbId, season, episode)}, too.
     * 
     * @param context
     * @param view
     */
    public TraktSummaryTask(Context context, View view, boolean isUseCachedValue) {
        mView = view;
        mContext = context;
        mIsDoCacheLookup = isUseCachedValue;
    }

    public TraktSummaryTask show(String tvdbId) {
        mTvdbIdString = tvdbId;
        return this;
    }

    public TraktSummaryTask episode(int tvdbId, int season, int episode) {
        mTvdbId = tvdbId;
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
            if (mTvdbIdString != null) {
                if (Utils.isAllowedConnection(mContext)) {
                    // get the shows summary from trakt
                    TvShow entity = getServiceManager()
                            .showService()
                            .summary(mTvdbIdString)
                            .fire();
                    if (entity != null) {
                        RatingsWrapper results = new RatingsWrapper();
                        results.rating = entity.rating_advanced;
                        results.ratings = entity.ratings;
                        return results;
                    }
                }
            } else {
                TvEntity entity = null;
                String key = String.valueOf(mTvdbId) + String.valueOf(mSeason)
                        + String.valueOf(mEpisode);

                if (mIsDoCacheLookup) {
                    // look if the episode summary is cached

                    synchronized (sHardEntityCache) {
                        entity = sHardEntityCache.remove(key);
                    }
                }

                // on cache miss load the summary from trakt
                if (entity == null && Utils.isAllowedConnection(mContext)) {
                    entity = getServiceManager()
                            .showService()
                            .episodeSummary(mTvdbId, mSeason, mEpisode)
                            .fire();
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
        } catch (TraktException te) {
            return null;
        } catch (ApiException e) {
            return null;
        }

        return null;
    }

    private ServiceManager getServiceManager() {
        ServiceManager serviceManager;
        if (ServiceUtils.isTraktCredentialsValid(mContext)) {
            serviceManager = ServiceUtils.getTraktServiceManagerWithAuth(mContext,
                    false);
        } else {
            serviceManager = ServiceUtils.getTraktServiceManager(mContext);
        }
        return serviceManager;
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
