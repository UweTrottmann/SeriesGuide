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
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt.v2.entities.Show;
import com.uwetrottmann.trakt.v2.entities.TrendingShow;
import com.uwetrottmann.trakt.v2.enums.Extended;
import com.uwetrottmann.trakt.v2.enums.Type;
import java.util.LinkedList;
import java.util.List;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Loads a list of trending shows or searches TheTVDB for shows matching the given query.
 */
public class TvdbAddLoader extends GenericSimpleLoader<TvdbAddLoader.Result> {

    public static class Result {
        public List<SearchResult> results;
        public int emptyTextResId;

        public Result(List<SearchResult> results, int emptyTextResId) {
            this.results = results;
            this.emptyTextResId = emptyTextResId;
        }
    }

    private final String query;

    public TvdbAddLoader(Context context, String query) {
        super(context);
        this.query = query;
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
            } catch (RetrofitError e) {
                Timber.e(e, "Loading trending shows failed");
                return buildResultFailure(getContext(), R.string.trakt_error_general);
            }
        } else {
            // have a query? search trakt (has better search) for TheTVDB shows
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
            } catch (RetrofitError e) {
                Timber.e(e, "Searching show failed");
                return buildResultFailure(getContext(), R.string.search_error);
            }
        }

        return buildResultSuccess(results, R.string.no_results);
    }

    private static Result buildResultSuccess(List<SearchResult> results, int emptyTextResId) {
        if (results == null) {
            results = new LinkedList<>();
        }
        return new Result(results, emptyTextResId);
    }

    private static Result buildResultFailure(Context context, int emptyTextResId) {
        // only check for network here to allow hitting the response cache
        if (!AndroidUtils.isNetworkConnected(context)) {
            emptyTextResId = R.string.offline;
        }
        return new Result(new LinkedList<SearchResult>(), emptyTextResId);
    }
}
