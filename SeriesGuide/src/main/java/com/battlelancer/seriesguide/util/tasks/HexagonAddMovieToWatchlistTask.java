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

package com.battlelancer.seriesguide.util.tasks;

import android.content.Context;
import com.uwetrottmann.seriesguide.backend.movies.model.Movie;

public class HexagonAddMovieToWatchlistTask extends BaseHexagonMovieTask {

    public HexagonAddMovieToWatchlistTask(Context context, int movieTmdbId) {
        super(context, movieTmdbId);
    }

    @Override
    protected void setMovieProperties(Movie movie) {
        movie.setIsInWatchlist(true);
    }
}
