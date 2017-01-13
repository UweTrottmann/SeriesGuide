package com.battlelancer.seriesguide.loaders;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.util.SparseArrayCompat;
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.thetvdbapi.TvdbException;
import com.battlelancer.seriesguide.thetvdbapi.TvdbTools;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.util.ShowTools;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.entities.TrendingShow;
import com.uwetrottmann.trakt5.enums.Extended;
import com.uwetrottmann.trakt5.enums.Type;
import com.uwetrottmann.trakt5.services.Search;
import com.uwetrottmann.trakt5.services.Shows;
import dagger.Lazy;
import java.util.LinkedList;
import java.util.List;
import javax.inject.Inject;
import timber.log.Timber;

public class TvdbAddLoader extends GenericSimpleLoader<TvdbAddLoader.Result> {

    public static class Result {
        @NonNull
        public List<SearchResult> results;
        public String emptyText;
        /** Whether the network call completed. Does not mean there are any results. */
        public boolean successful;

        public Result(@NonNull List<SearchResult> results, String emptyText, boolean successful) {
            this.results = results;
            this.emptyText = emptyText;
            this.successful = successful;
        }
    }

    private final SgApp app;
    private final String query;
    private final String language;
    @Inject Lazy<Shows> traktShows;
    @Inject Lazy<Search> traktSearch;

    /**
     * Loads a list of trending shows from trakt (query is empty) or searches trakt/TheTVDB for
     * shows matching the given query.
     *
     * @param language If not provided, will search for results in all languages.
     */
    public TvdbAddLoader(SgApp app, @Nullable String query, @Nullable String language) {
        super(app);
        this.app = app;
        app.getServicesComponent().inject(this);
        this.query = query;
        this.language = language;
    }

    @Override
    public Result loadInBackground() {
        List<SearchResult> results;

        if (TextUtils.isEmpty(query)) {
            // no query? load a list of trending shows from trakt
            List<TrendingShow> trendingShows = SgTrakt.executeCall(app,
                    traktShows.get().trending(1, 35, Extended.FULL),
                    "get trending shows"
            );
            if (trendingShows != null) {
                List<Show> shows = new LinkedList<>();
                for (TrendingShow show : trendingShows) {
                    if (show.show == null || show.show.ids == null || show.show.ids.tvdb == null) {
                        // skip if required values are missing
                        continue;
                    }
                    shows.add(show.show);
                }
                // manually set the language to the current search language
                results = TraktAddLoader.parseTraktShowsToSearchResults(getContext(), shows,
                        language);
                return buildResultSuccess(results, R.string.add_empty);
            } else {
                return buildResultFailure(R.string.trakt);
            }
        } else {
            // have a query?
            // search trakt (has better search) when using English
            // use TheTVDB search for all other (or any) languages
            if (DisplaySettings.LANGUAGE_EN.equals(language)) {
                List<com.uwetrottmann.trakt5.entities.SearchResult> searchResults
                        = SgTrakt.executeCall(app,
                        traktSearch.get().textQueryShow(query,
                                null, null, null, null, null, null, null, null, null,
                                Extended.FULL, 1, 30),
                        "search shows"
                );
                if (searchResults != null) {
                    List<Show> shows = new LinkedList<>();
                    for (com.uwetrottmann.trakt5.entities.SearchResult result : searchResults) {
                        if (result.show == null || result.show.ids == null
                                || result.show.ids.tvdb == null) {
                            // skip, TVDB id required
                            continue;
                        }
                        shows.add(result.show);
                    }

                    // manually set the language to English
                    results = TraktAddLoader.parseTraktShowsToSearchResults(getContext(),
                            shows, DisplaySettings.LANGUAGE_EN);
                    return buildResultSuccess(results, R.string.no_results);
                } else {
                    return buildResultFailure(R.string.trakt);
                }
            } else {
                try {
                    if (TextUtils.isEmpty(language)) {
                        // use the v1 API to do an any language search not supported by v2
                        results = TvdbTools.getInstance(app).searchShow(query, null);
                    } else {
                        results = TvdbTools.getInstance(app).searchSeries(query, language);
                    }
                    markLocalShows(results);
                    return buildResultSuccess(results, R.string.no_results);
                } catch (TvdbException e) {
                    Timber.e(e, "Searching show failed");
                }
                return buildResultFailure(R.string.tvdb);
            }
        }
    }

    private void markLocalShows(@Nullable List<SearchResult> results) {
        SparseArrayCompat<String> localShows = ShowTools.getShowTvdbIdsAndPosters(getContext());
        if (localShows == null || results == null) {
            return;
        }

        for (SearchResult result : results) {
            result.overview = String.format("(%s) %s", result.language, result.overview);

            if (localShows.indexOfKey(result.tvdbid) >= 0) {
                // is already in local database
                result.isAdded = true;
                // use the poster we fetched for it (or null if there is none)
                result.poster = localShows.get(result.tvdbid);
            }
        }
    }

    private Result buildResultSuccess(List<SearchResult> results, @StringRes int emptyTextResId) {
        if (results == null) {
            results = new LinkedList<>();
        }
        return new Result(results, getContext().getString(emptyTextResId), true);
    }

    private Result buildResultFailure(@StringRes int serviceResId) {
        // only check for network here to allow hitting the response cache
        String emptyText;
        if (AndroidUtils.isNetworkConnected(getContext())) {
            emptyText = getContext().getString(R.string.api_error_generic,
                    getContext().getString(serviceResId));
        } else {
            emptyText = getContext().getString(R.string.offline);
        }
        return new Result(new LinkedList<SearchResult>(), emptyText, false);
    }
}
