/*
 * Copyright 2015 Uwe Trottmann
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

package com.battlelancer.seriesguide.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.thetvdbapi.TheTVDB;
import com.battlelancer.seriesguide.thetvdbapi.TvdbException;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShowTools;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt.v2.entities.Show;
import com.uwetrottmann.trakt.v2.entities.TrendingShow;
import com.uwetrottmann.trakt.v2.enums.Extended;
import com.uwetrottmann.trakt.v2.enums.Type;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import retrofit.RetrofitError;
import timber.log.Timber;

public class TvdbAddLoader extends GenericSimpleLoader<TvdbAddLoader.Result> {

    public static class Result {
        public List<SearchResult> results;
        public int emptyTextResId;
        /** Whether the network call completed. Does not mean there are any results. */
        public boolean successful;

        public Result(List<SearchResult> results, int emptyTextResId, boolean successful) {
            this.results = results;
            this.emptyTextResId = emptyTextResId;
            this.successful = successful;
        }
    }

    private final String query;
    private final String language;

    /**
     * Loads a list of trending shows from trakt (query is empty) or searches trakt/TheTVDB for
     * shows matching the given query.
     *
     * @param language If not provided, will search for results in all languages.
     */
    public TvdbAddLoader(@NonNull Context context, @Nullable String query,
            @Nullable String language) {
        super(context);
        this.query = query;
        this.language = language;
    }

    @Override
    public Result loadInBackground() {
        List<SearchResult> results;

        if (TextUtils.isEmpty(query)) {
            // no query? load a list of trending shows from trakt
            try {
                List<TrendingShow> trendingShows = ServiceUtils.getTraktV2(getContext())
                        .shows()
                        .trending(1, 35, Extended.IMAGES);
                List<Show> shows = new LinkedList<>();
                for (TrendingShow show : trendingShows) {
                    if (show.show == null || show.show.ids == null
                            || show.show.ids.tvdb == null) {
                        // skip if required values are missing
                        continue;
                    }
                    shows.add(show.show);
                }
                results = TraktAddLoader.parseTraktShowsToSearchResults(getContext(), shows);
                // manually set the language to the current search language
                for (SearchResult result : results) {
                    result.language = language;
                }
            } catch (RetrofitError e) {
                Timber.e(e, "Loading trending shows failed");
                return buildResultFailure(getContext(), R.string.trakt_error_general);
            }
        } else {
            // have a query?
            // search trakt (has better search) when using English
            // use TheTVDB search for all other (or any) languages
            if (DisplaySettings.LANGUAGE_EN.equals(language)) {
                try {
                    List<com.uwetrottmann.trakt.v2.entities.SearchResult> traktResults
                            = ServiceUtils.getTraktV2(getContext()).search()
                            .textQuery(query, Type.SHOW, null, 1, 30);

                    List<Show> shows = new LinkedList<>();
                    for (com.uwetrottmann.trakt.v2.entities.SearchResult result : traktResults) {
                        if (result.show == null || result.show.ids == null
                                || result.show.ids.tvdb == null) {
                            // skip, TVDB id required
                            continue;
                        }
                        shows.add(result.show);
                    }

                    results = TraktAddLoader.parseTraktShowsToSearchResults(getContext(), shows);
                    // manually set the language to English
                    for (SearchResult result : results) {
                        result.language = DisplaySettings.LANGUAGE_EN;
                    }
                } catch (RetrofitError e) {
                    Timber.e(e, "Searching show failed");
                    return buildResultFailure(getContext(), R.string.search_error);
                }
            } else {
                try {
                    if (TextUtils.isEmpty(language)) {
                        // use the v1 API to do an any language search not supported by v2
                        results = TheTVDB.searchShow(getContext(), query, null);
                    } else {
                        results = TheTVDB.searchSeries(getContext(), query, language);
                    }
                    markLocalShows(getContext(), results);
                } catch (TvdbException e) {
                    Timber.e(e, "Searching show failed");
                    return buildResultFailure(getContext(), R.string.search_error);
                }
            }
        }

        return buildResultSuccess(results, R.string.no_results);
    }

    private static void markLocalShows(Context context, @Nullable List<SearchResult> results) {
        HashSet<Integer> localShows = ShowTools.getShowTvdbIdsAsSet(context);
        if (localShows == null || results == null) {
            return;
        }

        for (SearchResult result : results) {
            result.overview = "(" + result.language + ") " + result.overview;

            if (localShows.contains(result.tvdbid)) {
                // is already in local database
                result.isAdded = true;
            }
        }
    }

    private static Result buildResultSuccess(List<SearchResult> results, int emptyTextResId) {
        if (results == null) {
            results = new LinkedList<>();
        }
        return new Result(results, emptyTextResId, true);
    }

    private static Result buildResultFailure(Context context, int emptyTextResId) {
        // only check for network here to allow hitting the response cache
        if (!AndroidUtils.isNetworkConnected(context)) {
            emptyTextResId = R.string.offline;
        }
        return new Result(new LinkedList<SearchResult>(), emptyTextResId, false);
    }
}
