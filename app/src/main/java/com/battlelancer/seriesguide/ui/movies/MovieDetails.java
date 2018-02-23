package com.battlelancer.seriesguide.ui.movies;

import com.uwetrottmann.tmdb2.entities.Movie;
import com.uwetrottmann.trakt5.entities.Ratings;

/**
 * Holder object for trakt and TMDb entities related to a movie.
 */
public class MovieDetails {

    private Ratings traktRatings;
    private Movie tmdbMovie;

    private boolean inCollection;
    private boolean inWatchlist;
    private boolean isWatched;

    private int userRating;

    public Ratings traktRatings() {
        return traktRatings;
    }

    public void traktRatings(Ratings traktRatings) {
        this.traktRatings = traktRatings;
    }

    public Movie tmdbMovie() {
        return tmdbMovie;
    }

    public void tmdbMovie(Movie movie) {
        tmdbMovie = movie;
    }

    public boolean isInCollection() {
        return inCollection;
    }

    public void setInCollection(boolean inCollection) {
        this.inCollection = inCollection;
    }

    public boolean isInWatchlist() {
        return inWatchlist;
    }

    public void setInWatchlist(boolean inWatchlist) {
        this.inWatchlist = inWatchlist;
    }

    public boolean isWatched() {
        return isWatched;
    }

    public void setWatched(boolean watched) {
        isWatched = watched;
    }

    public int getUserRating() {
        return userRating;
    }

    public void setUserRating(int userRating) {
        this.userRating = userRating;
    }
}
