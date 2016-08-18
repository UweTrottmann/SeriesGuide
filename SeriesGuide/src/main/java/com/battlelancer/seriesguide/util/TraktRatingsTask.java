package com.battlelancer.seriesguide.util;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt5.entities.Ratings;
import com.uwetrottmann.trakt5.services.Episodes;
import com.uwetrottmann.trakt5.services.Shows;
import dagger.Lazy;
import javax.inject.Inject;
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
    @Inject Lazy<Shows> traktShows;
    @Inject Lazy<Episodes> traktEpisodes;

    /**
     * Loads the latest ratings for the given show from trakt and saves them to the database. If
     * ratings were loaded recently, might do nothing.
     */
    public TraktRatingsTask(SgApp app, int showTvdbId) {
        this(app, showTvdbId, 0, 0, 0);
    }

    /**
     * Loads the latest ratings for the given episode from trakt and saves them to the database. If
     * ratings were loaded recently, might do nothing.
     */
    public TraktRatingsTask(SgApp app, int showTvdbId, int episodeTvdbId, int season,
            int episode) {
        this.context = app;
        app.getServicesComponent().inject(this);
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
        boolean isShowNotEpisode = episodeTvdbId == 0;

        Ratings ratings;
        if (isShowNotEpisode) {
            ratings = SgTrakt.executeCall(context, traktShows.get().ratings(showTraktIdString),
                    "get show rating");
        } else {
            ratings = SgTrakt.executeCall(context,
                    traktEpisodes.get().ratings(showTraktIdString, season, episode),
                    "get episode rating");
        }
        if (ratings != null && ratings.rating != null && ratings.votes != null) {
            if (isShowNotEpisode) {
                saveShowRating(ratings);
            } else {
                saveEpisodeRating(ratings);
            }
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

        // notify withshow uri as well (used by episode details view)
        context.getContentResolver()
                .notifyChange(SeriesGuideContract.Episodes.buildEpisodeWithShowUri(episodeTvdbId),
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
