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
import android.os.AsyncTask;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.uwetrottmann.seriesguide.backend.movies.model.Movie;
import com.uwetrottmann.seriesguide.backend.movies.model.MovieList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

abstract class BaseHexagonMovieTask extends AsyncTask<Void, Void, Void> {

    private Context mContext;
    private int mMovieTmdbId;

    public BaseHexagonMovieTask(Context context, int movieTmdbId) {
        mContext = context.getApplicationContext();
        mMovieTmdbId = movieTmdbId;
    }

    protected abstract void setMovieProperties(Movie movie);

    @Override
    protected Void doInBackground(Void... params) {
        Movie movie = new Movie();
        movie.setTmdbId(mMovieTmdbId);

        setMovieProperties(movie);

        List<Movie> movies = new ArrayList<>();
        movies.add(movie);

        MovieList movieList = new MovieList();
        movieList.setMovies(movies);

        try {
            HexagonTools.getMoviesService(mContext).save(movieList).execute();
        } catch (IOException e) {
            Timber.e(e, "Failed to upload movie " + mMovieTmdbId + " to hexagon.");
        }

        return null;
    }

}
