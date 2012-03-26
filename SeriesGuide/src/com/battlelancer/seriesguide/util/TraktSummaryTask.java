
package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.Ratings;
import com.jakewharton.trakt.entities.TvEntity;
import com.jakewharton.trakt.entities.TvShow;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class TraktSummaryTask extends AsyncTask<Void, Void, Ratings> {

    private static final int HARD_CACHE_CAPACITY = 10;

    // Hard cache, with a fixed maximum capacity and a life duration
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

    /**
     * Sets values for predefined views which have to be children of the given
     * view. Make sure to call either {@code show(tvdbId)} or
     * {@code episode(tvdbId, season, episode)}, too.
     * 
     * @param context
     * @param view
     */
    public TraktSummaryTask(Context context, View view) {
        mView = view;
        mContext = context;
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
        mTraktLoves = (TextView) mView.findViewById(R.id.traktvalue);
        mTraktVotes = (TextView) mView.findViewById(R.id.traktvotes);

        // set place-holder values
        if (mTraktLoves != null && mTraktVotes != null) {
            mTraktLoves.setText(R.string.notraktrating);
            mTraktVotes.setText("");
        }
    }

    @Override
    protected Ratings doInBackground(Void... params) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        final boolean isOnlyWifiAllowed = prefs.getBoolean(
                SeriesGuidePreferences.KEY_AUTOUPDATEWLANONLY, true);

        if ((isOnlyWifiAllowed && Utils.isWifiConnected(mContext))
                || (!isOnlyWifiAllowed && Utils.isNetworkConnected(mContext))) {

            try {
                if (isCancelled()) {
                    return null;
                }

                // decide whether we have a show or an episode
                if (mTvdbIdString != null) {
                    // get the shows summary from trakt
                    TvShow entity = Utils.getServiceManager(mContext).showService()
                            .summary(mTvdbIdString).fire();
                    if (entity != null) {
                        return entity.ratings;
                    }
                } else {
                    // look if the episode summary is cached
                    String key = String.valueOf(mTvdbId) + String.valueOf(mSeason)
                            + String.valueOf(mEpisode);
                    TvEntity entity = sHardEntityCache.remove(key);

                    // on cache miss load the summary from trakt
                    if (entity == null) {
                        entity = Utils.getServiceManager(mContext).showService()
                                .episodeSummary(mTvdbId, mSeason, mEpisode).fire();
                    }

                    if (entity != null) {
                        sHardEntityCache.put(key, entity);
                        return entity.episode.ratings;
                    }
                }
            } catch (TraktException te) {
                return null;
            } catch (ApiException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    protected void onCancelled(Ratings result) {
        releaseReferences();
    }

    @Override
    protected void onPostExecute(Ratings ratings) {
        // set the final rating values
        if (ratings != null && mTraktLoves != null && mTraktVotes != null) {
            mTraktLoves.setText(ratings.percentage + "%");
            mTraktVotes.setText(mContext.getResources().getQuantityString(R.plurals.votes,
                    ratings.votes, ratings.votes));
        }

        releaseReferences();
    }

    private void releaseReferences() {
        mContext = null;
        mTraktLoves = null;
        mTraktVotes = null;
        mView = null;
    }

}
