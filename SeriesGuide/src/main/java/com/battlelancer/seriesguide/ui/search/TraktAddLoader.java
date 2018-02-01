package com.battlelancer.seriesguide.ui.search;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.util.SparseArrayCompat;
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.ui.shows.ShowTools;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt5.entities.BaseShow;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.enums.Extended;
import com.uwetrottmann.trakt5.services.Recommendations;
import com.uwetrottmann.trakt5.services.Shows;
import com.uwetrottmann.trakt5.services.Sync;
import dagger.Lazy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.inject.Inject;
import retrofit2.Response;

/**
 * Loads either the connected trakt user's recommendations, library or watchlist.
 */
public class TraktAddLoader extends GenericSimpleLoader<TraktAddLoader.Result> {

    public static class Result {
        public List<SearchResult> results;
        public String emptyText;

        public Result(List<SearchResult> results, String emptyText) {
            this.results = results;
            this.emptyText = emptyText;
        }

        public Result(List<SearchResult> results, Context context, @StringRes int emptyTextResId) {
            this.results = results;
            this.emptyText = context.getString(emptyTextResId);
        }
    }

    @Inject Lazy<Recommendations> traktRecommendations;
    @Inject Lazy<Sync> traktSync;
    @Inject Lazy<Shows> traktShows;
    private final TraktShowsLink type;

    TraktAddLoader(Context context, TraktShowsLink type) {
        super(context);
        this.type = type;
        SgApp.getServicesComponent(context).inject(this);
    }

    @Override
    public Result loadInBackground() {
        List<Show> shows = new LinkedList<>();
        String action = null;
        try {
            if (type == TraktShowsLink.POPULAR) {
                action = "load popular shows";
                Response<List<Show>> response = traktShows.get()
                        .popular(null, 25, Extended.FULL)
                        .execute();
                if (response.isSuccessful()) {
                    shows = response.body();
                } else {
                    SgTrakt.trackFailedRequest(getContext(), action, response);
                    return buildResultGenericFailure();
                }
            } else if (type == TraktShowsLink.RECOMMENDED) {
                action = "load recommended shows";
                Response<List<Show>> response = traktRecommendations.get()
                        .shows(Extended.FULL)
                        .execute();
                if (response.isSuccessful()) {
                    shows = response.body();
                } else {
                    if (SgTrakt.isUnauthorized(getContext(), response)) {
                        return buildResultFailure(R.string.trakt_error_credentials);
                    } else {
                        SgTrakt.trackFailedRequest(getContext(), action, response);
                        return buildResultGenericFailure();
                    }
                }
            } else {
                Response<List<BaseShow>> response;
                if (type == TraktShowsLink.WATCHED) {
                    action = "load watched shows";
                    response = traktSync.get().watchedShows(Extended.NOSEASONS).execute();
                } else if (type == TraktShowsLink.COLLECTION) {
                    action = "load show collection";
                    response = traktSync.get().collectionShows(null).execute();
                } else if (type == TraktShowsLink.WATCHLIST) {
                    action = "load show watchlist";
                    response = traktSync.get().watchlistShows(Extended.FULL).execute();
                } else {
                    throw new IllegalArgumentException("Unknown type " + type);
                }
                if (response.isSuccessful()) {
                    extractShows(response.body(), shows);
                } else {
                    if (SgTrakt.isUnauthorized(getContext(), response)) {
                        return buildResultFailure(R.string.trakt_error_credentials);
                    }
                    SgTrakt.trackFailedRequest(getContext(), action, response);
                    return buildResultGenericFailure();
                }
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(getContext(), action, e);
            // only check for network here to allow hitting the response cache
            return AndroidUtils.isNetworkConnected(getContext())
                    ? buildResultGenericFailure() : buildResultFailure(R.string.offline);
        }

        // return empty list right away if there are no results
        if (shows == null || shows.size() == 0) {
            return buildResultSuccess(new LinkedList<SearchResult>());
        }

        return buildResultSuccess(parseTraktShowsToSearchResults(getContext(), shows));
    }

    private void extractShows(List<BaseShow> watchedShows, List<Show> shows) {
        for (BaseShow show : watchedShows) {
            if (show.show == null || show.show.ids == null
                    || show.show.ids.tvdb == null) {
                continue; // skip if required values are missing
            }
            shows.add(show.show);
        }
    }

    private Result buildResultSuccess(List<SearchResult> results) {
        return new Result(results, getContext(), R.string.add_empty);
    }

    private Result buildResultGenericFailure() {
        return new Result(new LinkedList<SearchResult>(),
                getContext().getString(R.string.api_error_generic,
                        getContext().getString(R.string.trakt)));
    }

    private Result buildResultFailure(@StringRes int errorResId) {
        return new Result(new LinkedList<SearchResult>(), getContext(), errorResId);
    }

    private static List<SearchResult> parseTraktShowsToSearchResults(Context context,
            @NonNull List<Show> traktShows) {
        return parseTraktShowsToSearchResults(context, traktShows, null);
    }

    /**
     * Transforms a list of trakt shows to a list of {@link SearchResult}, marks shows already in
     * the local database as added.
     */
    static List<SearchResult> parseTraktShowsToSearchResults(Context context,
            @NonNull List<Show> traktShows, @Nullable String overrideLanguage) {
        List<SearchResult> results = new ArrayList<>();

        // build list
        SparseArrayCompat<String> existingPosterPaths = ShowTools.getShowTvdbIdsAndPosters(context);
        for (Show show : traktShows) {
            if (show.ids == null || show.ids.tvdb == null) {
                // has no TheTVDB id
                continue;
            }
            SearchResult result = new SearchResult();
            result.setTvdbid(show.ids.tvdb);
            result.setTitle(show.title);
            // search results return an overview, while trending and other lists do not
            result.setOverview(!TextUtils.isEmpty(show.overview) ? show.overview
                    : show.year != null ? String.valueOf(show.year) : "");
            if (existingPosterPaths != null && existingPosterPaths.indexOfKey(show.ids.tvdb) >= 0) {
                // is already in local database
                result.setState(SearchResult.STATE_ADDED);
                // use the poster we fetched for it (or null if there is none)
                result.setPosterPath(existingPosterPaths.get(show.ids.tvdb));
            }
            if (overrideLanguage != null) {
                result.setLanguage(overrideLanguage);
            }
            results.add(result);
        }

        return results;
    }
}
