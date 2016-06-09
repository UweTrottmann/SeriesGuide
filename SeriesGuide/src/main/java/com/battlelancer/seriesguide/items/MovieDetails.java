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

package com.battlelancer.seriesguide.items;

import com.uwetrottmann.tmdb2.entities.Movie;
import com.uwetrottmann.trakt5.entities.Ratings;

/**
 * Holder object for trakt and TMDb entities related to a movie.
 */
public class MovieDetails {

    private Ratings traktRatings;

    private Movie tmdbMovie;

    public boolean inCollection;
    public boolean inWatchlist;
    public boolean isWatched;

    public int userRating;

    public Ratings traktRatings() {
        return traktRatings;
    }

    public MovieDetails traktRatings(Ratings traktRatings) {
        this.traktRatings = traktRatings;
        return this;
    }

    public Movie tmdbMovie() {
        return tmdbMovie;
    }

    public MovieDetails tmdbMovie(Movie movie) {
        tmdbMovie = movie;
        return this;
    }
}
