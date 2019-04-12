package com.battlelancer.seriesguide.ui.search;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.collection.SparseArrayCompat;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.ui.shows.ShowTools;
import com.battlelancer.seriesguide.util.Errors;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.BaseShow;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.enums.Extended;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import retrofit2.Response;

/**
 * Loads either the connected trakt user's watched, collected or watchlist-ed shows.
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

    private final TraktV2 trakt;
    private final TraktShowsLink type;

    TraktAddLoader(Context context, TraktShowsLink type) {
        super(context);
        this.type = type;
        this.trakt = SgApp.getServicesComponent(context).trakt();
    }

    @Override
    public Result loadInBackground() {
        List<Show> shows = new LinkedList<>();
        String action = null;
        try {
            Response<List<BaseShow>> response;
            if (type == TraktShowsLink.WATCHED) {
                action = "load watched shows";
                response = trakt.sync().watchedShows(Extended.NOSEASONS).execute();
            } else if (type == TraktShowsLink.COLLECTION) {
                action = "load show collection";
                response = trakt.sync().collectionShows(null).execute();
            } else if (type == TraktShowsLink.WATCHLIST) {
                action = "load show watchlist";
                response = trakt.sync().watchlistShows(Extended.FULL).execute();
            } else {
                action = "load unknown type";
                throw new IllegalArgumentException("Unknown type " + type);
            }

            if (response.isSuccessful()) {
                List<BaseShow> body = response.body();
                if (body != null) {
                    extractShows(body, shows);
                }
            } else {
                if (SgTrakt.isUnauthorized(getContext(), response)) {
                    return buildResultFailure(R.string.trakt_error_credentials);
                }
                Errors.logAndReport(action, response);
                return buildResultGenericFailure();
            }
        } catch (Exception e) {
            Errors.logAndReport(action, e);
            // only check for network here to allow hitting the response cache
            return AndroidUtils.isNetworkConnected(getContext())
                    ? buildResultGenericFailure() : buildResultFailure(R.string.offline);
        }

        // return empty list right away if there are no results
        if (shows.size() == 0) {
            return buildResultSuccess(new LinkedList<>());
        }

        return buildResultSuccess(parseTraktShowsToSearchResults(getContext(), shows,
                DisplaySettings.getSearchLanguageOrFallbackIfAny(getContext())));
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
        return new Result(new LinkedList<>(),
                getContext().getString(R.string.api_error_generic,
                        getContext().getString(R.string.trakt)));
    }

    private Result buildResultFailure(@StringRes int errorResId) {
        return new Result(new LinkedList<>(), getContext(), errorResId);
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
