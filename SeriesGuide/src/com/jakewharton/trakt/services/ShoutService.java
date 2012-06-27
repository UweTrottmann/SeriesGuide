package com.jakewharton.trakt.services;

import com.google.myjson.reflect.TypeToken;
import com.jakewharton.trakt.TraktApiBuilder;
import com.jakewharton.trakt.TraktApiService;
import com.jakewharton.trakt.entities.Response;

public class ShoutService extends TraktApiService {
    /**
     * Add a shout to an episode on trakt.
     *
     * @param imdbId IMDB ID for the show.
     * @return Builder instance.
     */
    public EpisodeBuilder episode(String imdbId) {
        return new EpisodeBuilder(this).imdbId(imdbId);
    }

    /**
     * Add a shout to an episode on trakt.
     *
     * @param tvdbId TVDB ID (thetvdb.com) for the show.
     * @return Builder instance.
     */
    public EpisodeBuilder episode(int tvdbId) {
        return new EpisodeBuilder(this).tvdbId(tvdbId);
    }

    /**
     * Add a shout to an episode on trakt.
     *
     * @param title Show title.
     * @param year Show title.
     * @return Builder instance.
     */
    public EpisodeBuilder episode(String title, int year) {
        return new EpisodeBuilder(this).title(title).year(year);
    }

    /**
     * Add a shout to a movie on trakt.
     *
     * @param imdbId IMDB ID for the movie.
     * @return Builder instance.
     */
    public MovieBuilder movie(String imdbId) {
        return new MovieBuilder(this).imdbId(imdbId);
    }

    /**
     * Add a shout to a movie on trakt.
     *
     * @param tmdbId TMDB (themoviedb.org) ID for the movie.
     * @return Builder instance.
     */
    public MovieBuilder movie(int tmdbId) {
        return new MovieBuilder(this).tmdbId(tmdbId);
    }

    /**
     * Add a shout to a movie on trakt.
     *
     * @param title Movie title.
     * @param year Movie year.
     * @return Builder instance.
     */
    public MovieBuilder movie(String title, int year) {
        return new MovieBuilder(this).title(title).year(year);
    }

    /**
     * Add a shout to a show on trakt.
     *
     * @param imdbId IMDB ID for the show.
     * @return Builder instance.
     */
    public ShowBuilder show(String imdbId) {
        return new ShowBuilder(this).imdbId(imdbId);
    }

    /**
     * Add a shout to a show on trakt.
     *
     * @param tvdbId TVDB ID (thetvdb.com) for the show.
     * @return Builder instance.
     */
    public ShowBuilder show(int tvdbId) {
        return new ShowBuilder(this).tvdbId(tvdbId);
    }

    /**
     * Add a shout to a show on trakt.
     *
     * @param title Show title.
     * @param year Show year.
     * @return Builder instance.
     */
    public ShowBuilder show(String title, int year) {
        return new ShowBuilder(this).title(title).year(year);
    }


    public static final class EpisodeBuilder extends TraktApiBuilder<Response> {
        private static final String POST_IMDB_ID = "imdb_id";
        private static final String POST_TVDB_ID = "tvdb_id";
        private static final String POST_TITLE = "title";
        private static final String POST_YEAR = "year";
        private static final String POST_SEASON = "season";
        private static final String POST_EPISODE = "episode";
        private static final String POST_SHOUT = "shout";
        private static final String POST_SPOILER = "spoiler";

        private static final String URI = "/shout/episode/" + FIELD_API_KEY;

        private EpisodeBuilder(ShoutService service) {
            super(service, new TypeToken<Response>() {}, URI, HttpMethod.Post);
        }

        /**
         * Show IMDB ID.
         *
         * @param imdbId Value.
         * @return Builder instance.
         */
        public EpisodeBuilder imdbId(String imdbId) {
            this.postParameter(POST_IMDB_ID, imdbId);
            return this;
        }

        /**
         * Show TVDB ID.
         *
         * @param tvdbId Value.
         * @return Builder instance.
         */
        public EpisodeBuilder tvdbId(int tvdbId) {
            this.postParameter(POST_TVDB_ID, tvdbId);
            return this;
        }

        /**
         * Show title.
         *
         * @param title Value.
         * @return Builder instance.
         */
        public EpisodeBuilder title(String title) {
            this.postParameter(POST_TITLE, title);
            return this;
        }

        /**
         * Show year.
         *
         * @param year Value.
         * @return Builder instance.
         */
        public EpisodeBuilder year(int year) {
            this.postParameter(POST_YEAR, year);
            return this;
        }

        /**
         * Show season. Send 0 if watching a special.
         *
         * @param season Value.
         * @return Builder instance.
         */
        public EpisodeBuilder season(int season) {
            this.postParameter(POST_SEASON, season);
            return this;
        }

        /**
         * Show episode.
         *
         * @param episode Value.
         * @return Builder instance.
         */
        public EpisodeBuilder episode(int episode) {
            this.postParameter(POST_EPISODE, episode);
            return this;
        }

        /**
         * Text for the shout.
         *
         * @param shout Value.
         * @return Builder instance.
         */
        public EpisodeBuilder shout(String shout) {
            this.postParameter(POST_SHOUT, shout);
            return this;
        }
        
        /**
         * Shout contains a spoiler
         *
         * @param spoiler Value.
         * @return Builder instance.
         */
        public EpisodeBuilder spoiler(boolean spoiler) {
            this.postParameter(POST_SPOILER, spoiler);
            return this;
        }
    }
    public static final class MovieBuilder extends TraktApiBuilder<Response> {
        private static final String POST_IMDB_ID = "imdb_id";
        private static final String POST_TMDB_ID = "tmdb_id";
        private static final String POST_TITLE = "title";
        private static final String POST_YEAR = "year";
        private static final String POST_SHOUT = "shout";
        private static final String POST_SPOILER = "spoiler";

        private static final String URI = "/shout/movie/" + FIELD_API_KEY;

        private MovieBuilder(ShoutService service) {
            super(service, new TypeToken<Response>() {}, URI, HttpMethod.Post);
        }

        /**
         * Show IMDB ID.
         *
         * @param imdbId Value.
         * @return Builder instance.
         */
        public MovieBuilder imdbId(String imdbId) {
            this.postParameter(POST_IMDB_ID, imdbId);
            return this;
        }

        /**
         * Show TMDB ID.
         *
         * @param tmdbId Value.
         * @return Builder instance.
         */
        public MovieBuilder tmdbId(int tmdbId) {
            this.postParameter(POST_TMDB_ID, tmdbId);
            return this;
        }

        /**
         * Show title.
         *
         * @param title Value.
         * @return Builder instance.
         */
        public MovieBuilder title(String title) {
            this.postParameter(POST_TITLE, title);
            return this;
        }

        /**
         * Show year.
         *
         * @param year Value.
         * @return Builder instance.
         */
        public MovieBuilder year(int year) {
            this.postParameter(POST_YEAR, year);
            return this;
        }

        /**
         * Text for the shout.
         *
         * @param shout Value.
         * @return Builder instance.
         */
        public MovieBuilder shout(String shout) {
            this.postParameter(POST_SHOUT, shout);
            return this;
        }
        
        /**
         * Shout contains a spoiler
         *
         * @param spoiler Value.
         * @return Builder instance.
         */
        public MovieBuilder spoiler(boolean spoiler) {
            this.postParameter(POST_SPOILER, spoiler);
            return this;
        }
    }
    public static final class ShowBuilder extends TraktApiBuilder<Response> {
        private static final String POST_IMDB_ID = "imdb_id";
        private static final String POST_TVDB_ID = "tvdb_id";
        private static final String POST_TITLE = "title";
        private static final String POST_YEAR = "year";
        private static final String POST_SHOUT = "shout";
        private static final String POST_SPOILER = "spoiler";

        private static final String URI = "/shout/show/" + FIELD_API_KEY;

        private ShowBuilder(ShoutService service) {
            super(service, new TypeToken<Response>() {}, URI, HttpMethod.Post);
        }

        /**
         * Show IMDB ID.
         *
         * @param imdbId Value.
         * @return Builder instance.
         */
        public ShowBuilder imdbId(String imdbId) {
            this.postParameter(POST_IMDB_ID, imdbId);
            return this;
        }

        /**
         * Show TVDB ID.
         *
         * @param tvdbId Value.
         * @return Builder instance.
         */
        public ShowBuilder tvdbId(int tvdbId) {
            this.postParameter(POST_TVDB_ID, tvdbId);
            return this;
        }

        /**
         * Show title.
         *
         * @param title Value.
         * @return Builder instance.
         */
        public ShowBuilder title(String title) {
            this.postParameter(POST_TITLE, title);
            return this;
        }

        /**
         * Show year.
         *
         * @param year Value.
         * @return Builder instance.
         */
        public ShowBuilder year(int year) {
            this.postParameter(POST_YEAR, year);
            return this;
        }

        /**
         * Text for the shout.
         *
         * @param shout Value.
         * @return Builder instance.
         */
        public ShowBuilder shout(String shout) {
            this.postParameter(POST_SHOUT, shout);
            return this;
        }
        
        
        /**
         * Shout contains a spoiler
         *
         * @param spoiler Value.
         * @return Builder instance.
         */
        public ShowBuilder spoiler(boolean spoiler) {
            this.postParameter(POST_SPOILER, spoiler);
            return this;
        }
    }
}