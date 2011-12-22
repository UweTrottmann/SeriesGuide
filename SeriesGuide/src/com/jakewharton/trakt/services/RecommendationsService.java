package com.jakewharton.trakt.services;

import com.google.myjson.reflect.TypeToken;
import com.jakewharton.trakt.TraktApiBuilder;
import com.jakewharton.trakt.TraktApiService;
import com.jakewharton.trakt.entities.DismissResponse;
import com.jakewharton.trakt.entities.Genre;
import com.jakewharton.trakt.entities.Movie;
import com.jakewharton.trakt.entities.TvShow;

import java.util.List;

public class RecommendationsService extends TraktApiService {
    /**
     * Get a list of movie recommendations created from your watching history
     * and your friends. Results returned with the top recommendation first.
     *
     * @return Builder instance.
     */
    public MoviesBuilder movies() {
        return new MoviesBuilder(this);
    }

    /**
     * Get a list of show recommendations created from your watching history
     * and your friends. Results returned with the top recommendation first.
     *
     * @return Builder instance.
     */
    public ShowsBuilder shows() {
        return new ShowsBuilder(this);
    }

    /**
     * Dismiss a movie recommendation.
     *
     * @param imdbId IMDB ID for the movie.
     * @return Builder instance.
     */
    public DismissMovieBuilder dismissMovie(String imdbId) {
        return new DismissMovieBuilder(this).imdbId(imdbId);
    }

    /**
     * Dismiss a movie recommendation.
     *
     * @param tmdbId TMDB (themoviedb.org) ID for the movie.
     * @return Builder instance.
     */
    public DismissMovieBuilder dismissMovie(int tmdbId) {
        return new DismissMovieBuilder(this).tmdbId(tmdbId);
    }

    /**
     * Dismiss a movie recommendation.
     *
     * @param title Movie title.
     * @param year Movie year.
     * @return Builder instance.
     */
    public DismissMovieBuilder dismissMovie(String title, int year) {
        return new DismissMovieBuilder(this).title(title).year(year);
    }

    /**
     * Dismiss a show recommendation.
     *
     * @param tvdbId TVDB ID for the show.
     * @return Builder instance.
     */
    public DismissShowBuilder dismissShow(int tvdbId) {
        return new DismissShowBuilder(this).tvdbId(tvdbId);
    }

    /**
     * Dismiss a show recommendation.
     *
     * @param title Show title.
     * @param year Show year.
     * @return Builder instance.
     */
    public DismissShowBuilder dismissShow(String title, int year) {
        return new DismissShowBuilder(this).title(title).year(year);
    }

    public static final class MoviesBuilder extends TraktApiBuilder<List<Movie>> {
        private static final String POST_GENRE = "genre";
        private static final String POST_YEAR_START = "start_year";
        private static final String POST_YEAR_END = "end_year";

        private static final String URI = "/recommendations/movies/" + FIELD_API_KEY;

        private MoviesBuilder(RecommendationsService service) {
            super(service, new TypeToken<List<Movie>>() {}, URI, HttpMethod.Post);
        }

        /**
         * 4 digit year to filter movies released in this year or later.
         *
         * @param year Value.
         * @return Builder instance.
         */
        public MoviesBuilder startYear(int year) {
            this.postParameter(POST_YEAR_START, year);
            return this;
        }

        /**
         * 4 digit year to filter movies released in this year or earlier.
         *
         * @param year Value.
         * @return Builder instance.
         */
        public MoviesBuilder endYear(int year) {
            this.postParameter(POST_YEAR_END, year);
            return this;
        }

        /**
         * Genre slug to filter by. See {@link GenreService#movies()} for a
         * list of valid genres.
         *
         * @param genre
         * @return
         */
        public MoviesBuilder genre(Genre genre) {
            this.postParameter(POST_GENRE, genre.slug);
            return this;
        }
    }
    public static final class ShowsBuilder extends TraktApiBuilder<List<TvShow>> {
        private static final String POST_GENRE = "genre";
        private static final String POST_YEAR_START = "start_year";
        private static final String POST_YEAR_END = "end_year";

        private static final String URI = "/recommendations/shows/" + FIELD_API_KEY;

        private ShowsBuilder(RecommendationsService service) {
            super(service, new TypeToken<List<TvShow>>() {}, URI, HttpMethod.Post);
        }

        /**
         * 4 digit year to filter shows premiering in this year or later.
         *
         * @param year Value.
         * @return Builder instance.
         */
        public ShowsBuilder startYear(int year) {
            this.postParameter(POST_YEAR_START, year);
            return this;
        }

        /**
         * 4 digit year to filter shows premiering in this year or earlier.
         *
         * @param year Value.
         * @return Builder instance.
         */
        public ShowsBuilder endYear(int year) {
            this.postParameter(POST_YEAR_END, year);
            return this;
        }

        /**
         * Genre slug to filter by. See {@link GenreService#shows()} for a
         * list of valid genres.
         *
         * @param genre
         * @return
         */
        public ShowsBuilder genre(Genre genre) {
            this.postParameter(POST_GENRE, genre.slug);
            return this;
        }
    }
    public static final class DismissMovieBuilder extends TraktApiBuilder<DismissResponse> {
        private static final String POST_IMDB_ID = "imdb_id";
        private static final String POST_TMDB_ID = "tmdb_id";
        private static final String POST_TITLE = "title";
        private static final String POST_YEAR = "year";

        private static final String URI = "/recommendations/movies/dismiss/" + FIELD_API_KEY;

        private DismissMovieBuilder(RecommendationsService service) {
            super(service, new TypeToken<DismissResponse>() {}, URI, HttpMethod.Post);
        }

        /**
         * IMDB ID for the movie.
         *
         * @param imdbId Value.
         * @return Builder instance.
         */
        public DismissMovieBuilder imdbId(String imdbId) {
            this.postParameter(POST_IMDB_ID, imdbId);
            return this;
        }

        /**
         * TMDB (themoviedb.org) ID for the movie.
         *
         * @param tmdbId Value.
         * @return Builder instance.
         */
        public DismissMovieBuilder tmdbId(int tmdbId) {
            this.postParameter(POST_TMDB_ID, tmdbId);
            return this;
        }

        /**
         * Movie title.
         *
         * @param title Value.
         * @return Builder instance.
         */
        public DismissMovieBuilder title(String title) {
            this.postParameter(POST_TITLE, title);
            return this;
        }

        /**
         * Movie year.
         *
         * @param year Value.
         * @return Builder instance.
         */
        public DismissMovieBuilder year(int year) {
            this.postParameter(POST_YEAR, year);
            return this;
        }
    }
    public static final class DismissShowBuilder extends TraktApiBuilder<DismissResponse> {
        private static final String POST_TVDB_ID = "tvdb_id";
        private static final String POST_TITLE = "title";
        private static final String POST_YEAR = "year";

        private static final String URI = "/recommendations/shows/dismiss/" + FIELD_API_KEY;

        private DismissShowBuilder(RecommendationsService service) {
            super(service, new TypeToken<DismissResponse>() {}, URI, HttpMethod.Post);
        }

        /**
         * TVDB ID for the show.
         *
         * @param tmdbId Value.
         * @return Builder instance.
         */
        public DismissShowBuilder tvdbId(int tmdbId) {
            this.postParameter(POST_TVDB_ID, tmdbId);
            return this;
        }

        /**
         * Show title.
         *
         * @param title Value.
         * @return Builder instance.
         */
        public DismissShowBuilder title(String title) {
            this.postParameter(POST_TITLE, title);
            return this;
        }

        /**
         * Show year.
         *
         * @param year Value.
         * @return Builder instance.
         */
        public DismissShowBuilder year(int year) {
            this.postParameter(POST_YEAR, year);
            return this;
        }
    }
}
