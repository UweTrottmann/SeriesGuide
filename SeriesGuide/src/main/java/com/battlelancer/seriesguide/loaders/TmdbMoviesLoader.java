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

package com.battlelancer.seriesguide.loaders;

import android.content.Context;
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb.Tmdb;
import com.uwetrottmann.tmdb.entities.Movie;
import com.uwetrottmann.tmdb.entities.MovieResultsPage;
import java.util.List;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Loads a list of movies from TMDb.
 */
public class TmdbMoviesLoader extends GenericSimpleLoader<TmdbMoviesLoader.Result> {

    public static class Result {
        public List<Movie> results;
        public int emptyTextResId;

        public Result(List<Movie> results, int emptyTextResId) {
            this.results = results;
            this.emptyTextResId = emptyTextResId;
        }
    }

    private String mQuery;

    public TmdbMoviesLoader(Context context, String query) {
        super(context);
        mQuery = query;
    }

    @Override
    public Result loadInBackground() {
        Tmdb tmdb = ServiceUtils.getTmdb(getContext());
        String languageCode = DisplaySettings.getContentLanguage(getContext());

        List<Movie> results = null;

        try {
            MovieResultsPage page;

            if (TextUtils.isEmpty(mQuery)) {
                page = tmdb.moviesService().nowPlaying(null, languageCode);
            } else {
                page = tmdb.searchService()
                        .movie(mQuery, null, languageCode, false, null, null, null);
            }

            if (page != null) {
                results = page.results;
            }
        } catch (RetrofitError e) {
            Timber.e(e, "Loading movies from TMDb failed.");
            // only check for connection here to allow hitting the response cache
            return new Result(null,
                    AndroidUtils.isNetworkConnected(getContext()) ? R.string.search_error
                            : R.string.offline);
        }

        return new Result(results, R.string.no_results);
    }
}
