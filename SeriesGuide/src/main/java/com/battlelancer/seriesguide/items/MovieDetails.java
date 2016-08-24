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
