package com.battlelancer.seriesguide.ui.movies;

import android.content.ContentValues;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;
import com.battlelancer.seriesguide.util.DBUtils;
import com.uwetrottmann.tmdb2.entities.Movie;
import com.uwetrottmann.trakt5.entities.Ratings;
import java.util.Date;

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

    /**
     * Extracts ratings from trakt, all other properties from TMDb data.
     *
     * <p> If either movie data is null, will still extract the properties of others.
     *
     * <p> Does not add TMDB id or collection and watchlist flag.
     */
    public ContentValues toContentValuesUpdate() {
        ContentValues values = new ContentValues();

        // data from trakt
        if (traktRatings != null) {
            values.put(Movies.RATING_TRAKT, traktRatings.rating != null ? traktRatings.rating : 0);
            values.put(Movies.RATING_VOTES_TRAKT, traktRatings.votes != null
                    ? traktRatings.votes : 0);
        }

        // data from TMDb
        if (tmdbMovie != null) {
            values.put(Movies.IMDB_ID, tmdbMovie.imdb_id);
            values.put(Movies.TITLE, tmdbMovie.title);
            values.put(Movies.TITLE_NOARTICLE,
                    DBUtils.trimLeadingArticle(tmdbMovie.title));
            values.put(Movies.OVERVIEW, tmdbMovie.overview);
            values.put(Movies.POSTER, tmdbMovie.poster_path);
            values.put(Movies.RUNTIME_MIN, tmdbMovie.runtime != null ? tmdbMovie.runtime : 0);
            values.put(Movies.RATING_TMDB, tmdbMovie.vote_average != null
                    ? tmdbMovie.vote_average : 0);
            values.put(Movies.RATING_VOTES_TMDB, tmdbMovie.vote_count != null
                    ? tmdbMovie.vote_count : 0);
            // if there is no release date, store Long.MAX as it is likely in the future
            // also helps correctly sorting movies by release date
            Date releaseDate = tmdbMovie.release_date;
            values.put(Movies.RELEASED_UTC_MS,
                    releaseDate == null ? Long.MAX_VALUE : releaseDate.getTime());
        }

        return values;
    }

    /**
     * Like {@link #toContentValuesUpdate()} and adds TMDB id and IN_COLLECTION and IN_WATCHLIST
     * values.
     */
    public ContentValues toContentValuesInsert() {
        ContentValues values = toContentValuesUpdate();
        values.put(Movies.TMDB_ID, tmdbMovie.id);
        values.put(Movies.IN_COLLECTION, DBUtils.convertBooleanToInt(inCollection));
        values.put(Movies.IN_WATCHLIST, DBUtils.convertBooleanToInt(inWatchlist));
        return values;
    }
}
