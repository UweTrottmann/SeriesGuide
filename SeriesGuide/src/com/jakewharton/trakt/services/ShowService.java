package com.jakewharton.trakt.services;

import com.google.myjson.JsonArray;
import com.google.myjson.JsonObject;
import com.google.myjson.reflect.TypeToken;
import com.jakewharton.trakt.TraktApiBuilder;
import com.jakewharton.trakt.TraktApiService;
import com.jakewharton.trakt.entities.Response;
import com.jakewharton.trakt.entities.Shout;
import com.jakewharton.trakt.entities.TvEntity;
import com.jakewharton.trakt.entities.TvShow;
import com.jakewharton.trakt.entities.TvShowEpisode;
import com.jakewharton.trakt.entities.TvShowSeason;
import com.jakewharton.trakt.entities.UserProfile;

import java.util.List;

public class ShowService extends TraktApiService {
    /**
     * <p>Notify Trakt that a user has stopped watching a show.</p>
     *
     * <p><em>Warning</em>: This method requires a developer API key.</p>
     *
     * @return Builder instance.
     */
    public CancelWatchingBuilder cancelWatching() {
        return new CancelWatchingBuilder(this);
    }

    /**
     * Add unwatched episodes to your library.
     *
     * @param imdbId Show IMDB ID.
     * @return Builder instance.
     */
    public EpisodeLibraryBuilder episodeLibrary(String imdbId) {
        return new EpisodeLibraryBuilder(this).imdbId(imdbId);
    }

    /**
     * Add unwatched episodes to your library.
     *
     * @param tvdbId Show TVDB ID.
     * @return Builder instance.
     */
    public EpisodeLibraryBuilder episodeLibrary(int tvdbId) {
        return new EpisodeLibraryBuilder(this).tvdbId(tvdbId);
    }

    /**
     * Add unwatched episodes to your library.
     *
     * @param title Show title.
     * @param year Show year.
     * @return Builder instance.
     */
    public EpisodeLibraryBuilder episodeLibrary(String title, int year) {
        return new EpisodeLibraryBuilder(this).title(title).year(year);
    }

    /**
     * Add episodes watched outside of trakt to your library.
     *
     * @param imdbId Show IMDB ID.
     * @return Builder instance.
     */
    public EpisodeSeenBuilder episodeSeen(String imdbId) {
        return new EpisodeSeenBuilder(this).imdbId(imdbId);
    }

    /**
     * Add episodes watched outside of trakt to your library.
     *
     * @param tvdbId Show TVDB ID.
     * @return Builder instance.
     */
    public EpisodeSeenBuilder episodeSeen(int tvdbId) {
        return new EpisodeSeenBuilder(this).tvdbId(tvdbId);
    }

    /**
     * Add episodes watched outside of trakt to your library.
     *
     * @param title Show title.
     * @param year Show year.
     * @return Builder instance.
     */
    public EpisodeSeenBuilder episodeSeen(String title, int year) {
        return new EpisodeSeenBuilder(this).title(title).year(year);
    }

    /**
     * Returns information for an episode including ratings.
     *
     * @param tvdbId The TVDB ID.
     * @param season The season number. Use 0 if you want the specials.
     * @param episode The episode number.
     * @return Builder instance.
     */
    public EpisodeSummaryBuilder episodeSummary(int tvdbId, int season, int episode) {
        return (new EpisodeSummaryBuilder(this)).tvdbId(tvdbId).season(season).episode(episode);
    }

    /**
     * Returns information for an episode including ratings.
     *
     * @param title Either the slug (i.e. the-walking-dead) or TVDB ID. You can
     * get a show's slug by browsing the website and looking at the URL when on
     * a show summary page.
     * @param season The season number. Use 0 if you want the specials.
     * @param episode The episode number.
     * @return Builder instance.
     */
    public EpisodeSummaryBuilder episodeSummary(String title, int season, int episode) {
        return (new EpisodeSummaryBuilder(this)).title(title).season(season).episode(episode);
    }

    /**
     * Remove episodes from your library collection.
     *
     * @param imdbId Show IMDB ID.
     * @return Builder instance.
     */
    public EpisodeUnlibraryBuilder episodeUnlibrary(String imdbId) {
        return new EpisodeUnlibraryBuilder(this).imdbId(imdbId);
    }

    /**
     * Remove episodes from your library collection.
     *
     * @param tvdbId Show TVDB ID.
     * @return Builder instance.
     */
    public EpisodeUnlibraryBuilder episodeUnlibrary(int tvdbId) {
        return new EpisodeUnlibraryBuilder(this).tvdbId(tvdbId);
    }

    /**
     * Remove episodes from your library collection.
     *
     * @param title Show title.
     * @param year Show year.
     * @return Builder instance.
     */
    public EpisodeUnlibraryBuilder episodeUnlibrary(String title, int year) {
        return new EpisodeUnlibraryBuilder(this).title(title).year(year);
    }

    /**
     * Remove episodes watched outside of trakt from your library.
     *
     * @param imdbId Show IMDB ID.
     * @return Builder instance.
     */
    public EpisodeUnseenBuilder episodeUnseen(String imdbId) {
        return new EpisodeUnseenBuilder(this).imdbId(imdbId);
    }

    /**
     * Remove episodes watched outside of trakt from your library.
     *
     * @param tvdbId Show TVDB ID.
     * @return Builder instance.
     */
    public EpisodeUnseenBuilder episodeUnseen(int tvdbId) {
        return new EpisodeUnseenBuilder(this).tvdbId(tvdbId);
    }

    /**
     * Remove episodes watched outside of trakt from your library.
     *
     * @param title Show title.
     * @param year Show year.
     * @return Builder instance.
     */
    public EpisodeUnseenBuilder episodeUnseen(String title, int year) {
        return new EpisodeUnseenBuilder(this).title(title).year(year);
    }

    /**
     * Remove one or more episodes for a specific show from your watchlist.
     *
     * @param imdbId Show IMDB ID.
     * @return Builder instance.
     */
    public EpisodeUnwatchlistBuilder episodeUnwatchlist(String imdbId) {
        return new EpisodeUnwatchlistBuilder(this).imdbId(imdbId);
    }

    /**
     * Remove one or more episodes for a specific show from your watchlist.
     *
     * @param tvdbId Show TVDB ID.
     * @return Builder instance.
     */
    public EpisodeUnwatchlistBuilder episodeUnwatchlist(int tvdbId) {
        return new EpisodeUnwatchlistBuilder(this).tvdbId(tvdbId);
    }

    /**
     * Remove one or more episodes for a specific show from your watchlist.
     *
     * @param title Show title.
     * @param year Show year.
     * @return Builder instance.
     */
    public EpisodeUnwatchlistBuilder episodeUnwatchlist(String title, int year) {
        return new EpisodeUnwatchlistBuilder(this).title(title).year(year);
    }

    /**
     * Returns a array of all users watching an episode.
     *
     * @param title Either the slug (i.e. the-walking-dead) or TVDB ID. You can
     * get a show's slug by browsing the website and looking at the URL when on
     * a show summary page.
     * @param season The season number. Use 0 if you want the specials.
     * @param episode The episode number.
     * @return Builder instance.
     */
    public EpisodeWatchingNowBuilder episodeWatchingNow(String title, int season, int episode) {
        return new EpisodeWatchingNowBuilder(this).title(title).season(season).episode(episode);
    }

    /**
     * Add one or more episodes for a specific show to your watchlist.
     *
     * @param imdbId Show IMDB ID.
     * @return Builder instance.
     */
    public EpisodeWatchlistBuilder episodeWatchlist(String imdbId) {
        return new EpisodeWatchlistBuilder(this).imdbId(imdbId);
    }

    /**
     * Add one or more episodes for a specific show to your watchlist.
     *
     * @param tvdbId Show TVDB ID.
     * @return Builder instance.
     */
    public EpisodeWatchlistBuilder episodeWatchlist(int tvdbId) {
        return new EpisodeWatchlistBuilder(this).tvdbId(tvdbId);
    }

    /**
     * Add one or more episodes for a specific show to your watchlist.
     *
     * @param title Show title.
     * @param year Show year.
     * @return Builder instance.
     */
    public EpisodeWatchlistBuilder episodeWatchlist(String title, int year) {
        return new EpisodeWatchlistBuilder(this).title(title).year(year);
    }

    /**
     * <p>Notify Trakt that a user has finished watching a show. This commits
     * the show to the users profile. You should use show/watching prior to
     * calling this method.</p>
     *
     * <p><em>Warning</em>: This method requires a developer API key.</p>
     *
     * @param imdbId IMDB ID for the show.
     * @return Builder instance.
     */
    public ScrobbleBuilder scrobble(String imdbId) {
        return new ScrobbleBuilder(this).imdbId(imdbId);
    }

    /**
     * <p>Notify Trakt that a user has finished watching a show. This commits
     * the show to the users profile. You should use show/watching prior to
     * calling this method.</p>
     *
     * <p><em>Warning</em>: This method requires a developer API key.</p>
     *
     * @param tvdbId TVDB ID for the show.
     * @return Builder instance.
     */
    public ScrobbleBuilder scrobble(int tvdbId) {
        return new ScrobbleBuilder(this).tvdbId(tvdbId);
    }

    /**
     * <p>Notify Trakt that a user has finished watching a show. This commits
     * the show to the users profile. You should use show/watching prior to
     * calling this method.</p>
     *
     * <p><em>Warning</em>: This method requires a developer API key.</p>
     *
     * @param title Show title.
     * @param year Show year.
     * @return Builder instance.
     */
    public ScrobbleBuilder scrobble(String title, int year) {
        return new ScrobbleBuilder(this).title(title).year(year);
    }

    /**
     * Returns detailed episode info for a specific season of a show.
     *
     * @param query Either the slug (i.e. the-walking-dead) or TVDB ID. You can
     * get a show's slug by browsing the website and looking at the URL when on
     * a show summary page.
     * @param season The season number. Use 0 if you want the specials.
     * @return Builder instance.
     */
    public SeasonBuilder season(String query, int season) {
        return new SeasonBuilder(this, query, season);
    }

    /**
     * Returns basic season info for a show.
     *
     * @param query Either the slug (i.e. the-walking-dead) or TVDB ID. You can
     * get a show's slug by browsing the website and looking at the URL when on
     * a show summary page.
     * @return Builder instance.
     */
    public SeasonsBuilder seasons(String query) {
        return new SeasonsBuilder(this, query);
    }
    
    /**
     * Add all episodes from a season watched outside of trakt to your library.
     *
     * @param imdbId Show IMDB ID.
     * @return Builder instance.
     */
    public SeasonSeenBuilder seasonSeen(String imdbId) {
        return new SeasonSeenBuilder(this).imdbId(imdbId);
    }

    /**
     * Add all episodes from a season watched outside of trakt to your library.
     *
     * @param tvdbId Show TVDB ID.
     * @return Builder instance.
     */
    public SeasonSeenBuilder seasonSeen(int tvdbId) {
        return new SeasonSeenBuilder(this).tvdbId(tvdbId);
    }

    /**
     * Add all episodes from a season watched outside of trakt to your library.
     *
     * @param title Show title.
     * @param year Show year.
     * @return Builder instance.
     */
    public SeasonSeenBuilder seasonSeen(String title, int year) {
        return new SeasonSeenBuilder(this).title(title).year(year);
    }
    
    /**
     * Add all episodes for a show watched outside of trakt to your library.
     *
     * @param imdbId Show IMDB ID.
     * @return Builder instance.
     */
    public ShowSeenBuilder showSeen(String imdbId) {
        return new ShowSeenBuilder(this).imdbId(imdbId);
    }

    /**
     * Add all episodes for a show watched outside of trakt to your library.
     *
     * @param tvdbId Show TVDB ID.
     * @return Builder instance.
     */
    public ShowSeenBuilder showSeen(int tvdbId) {
        return new ShowSeenBuilder(this).tvdbId(tvdbId);
    }

    /**
     * Add all episodes for a show watched outside of trakt to your library.
     *
     * @param title Show title.
     * @param year Show year.
     * @return Builder instance.
     */
    public ShowSeenBuilder showSeen(String title, int year) {
        return new ShowSeenBuilder(this).title(title).year(year);
    }

    /**
     * Returns information for a TV show including ratings, top watchers, and
     * most watched episodes.
     *
     * @param title Either the slug (i.e. the-walking-dead) or TVDB ID. You can
     * get a show's slug by browsing the website and looking at the URL when on
     * a show summary page.
     * @return Builder instance.
     */
    public SummaryBuilder summary(String title) {
        return (new SummaryBuilder(this)).title(title);
    }

    /**
     * Returns all shows being watched right now.
     *
     * @return Builder instance.
     */
    public TrendingBuilder trending() {
        return new TrendingBuilder(this);
    }

    /**
     * Remove an entire show (including all episodes) from your library collection.
     *
     * @param imdbId IMDB ID for the show.
     * @return Builder instance.
     */
    public UnlibraryBuilder unlibrary(String imdbId) {
        return new UnlibraryBuilder(this).imdbId(imdbId);
    }

    /**
     * Remove an entire show (including all episodes) from your library collection.
     *
     * @param tvdbId TVDB ID for the show.
     * @return Builder instance.
     */
    public UnlibraryBuilder unlibrary(int tvdbId) {
        return new UnlibraryBuilder(this).tvdbId(tvdbId);
    }

    /**
     * Remove an entire show (including all episodes) from your library collection.
     *
     * @param title Show title.
     * @param year Show year.
     * @return Builder instance.
     */
    public UnlibraryBuilder unlibrary(String title, int year) {
        return new UnlibraryBuilder(this).title(title).year(year);
    }

    /**
     * Remove one or more shows from your watchlist.
     *
     * @return Builder instance.
     */
    public UnwatchlistBuilder unwatchlist() {
        return new UnwatchlistBuilder(this);
    }

    /**
     * <p>Notify Trakt that a user has started watching a show.</p>
     *
     * <p><em>Warning</em>: This method requires a developer API key.</p>
     *
     * @param imdbId IMDB ID for the show.
     * @return Builder instance.
     */
    public WatchingBuilder watching(String imdbId) {
        return new WatchingBuilder(this).imdbId(imdbId);
    }

    /**
     * <p>Notify Trakt that a user has started watching a show.</p>
     *
     * <p><em>Warning</em>: This method requires a developer API key.</p>
     *
     * @param tvdbId TVDB ID for the show.
     * @return Builder instance.
     */
    public WatchingBuilder watching(int tvdbId) {
        return new WatchingBuilder(this).tvdbId(tvdbId);
    }

    /**
     * <p>Notify Trakt that a user has started watching a show.</p>
     *
     * <p><em>Warning</em>: This method requires a developer API key.</p>
     *
     * @param title Show title.
     * @param year Show year.
     * @return Builder instance.
     */
    public WatchingBuilder watching(String title, int year) {
        return new WatchingBuilder(this).title(title).year(year);
    }

    /**
     * Returns a array of all users watching a show.
     *
     * @param query Either the slug (i.e. the-walking-dead) or TVDB ID. You can
     * get a show's slug by browsing the website and looking at the URL when on
     * a show summary page.
     * @return Builder instance.
     */
    public WatchingNowBuilder watchingNow(String query) {
        return new WatchingNowBuilder(this).query(query);
    }

    /**
     * Add one or more shows to your watchlist.
     *
     * @return Builder instance.
     */
    public WatchlistBuilder watchlist() {
        return new WatchlistBuilder(this);
    }

    /**
     * Returns all shouts for a show. Most recent shouts returned first.
     *
     * @param title Show title.
     * @return Builder instance.
     */
    public ShoutsBuilder shouts(String title) {
        return new ShoutsBuilder(this).title(title);
    }

    /**
     * Returns all shouts for a show. Most recent shouts returned first.
     *
     * @param tvdbId TVDB ID.
     * @return Builder instance.
     */
    public ShoutsBuilder shouts(int tvdbId) {
        return new ShoutsBuilder(this).title(tvdbId);
    }

    /**
     * Returns all shouts for an episode. Most recent shouts returned first.
     *
     * @param title Show title.
     * @param season The season number. Use 0 if you want the specials.
     * @param episode The episode number.
     * @return Builder instance.
     */
    public EpisodeShoutsBuilder episodeShouts(String title, int season, int episode) {
        return new EpisodeShoutsBuilder(this).title(title).season(season).episode(episode);
    }

    /**
     * Returns all shouts for an episode. Most recent shouts returned first.
     *
     * @param tvdbId TMDB ID.
     * @param season The season number. Use 0 if you want the specials.
     * @param episode The episode number.
     * @return Builder instance.
     */
    public EpisodeShoutsBuilder episodeShouts(int tvdbId, int season, int episode) {
        return new EpisodeShoutsBuilder(this).title(tvdbId).season(season).episode(episode);
    }

    /**
     * Get the top 10 related shows.
     *
     * @param slug Show title slug.
     * @return Builder instance.
     */
    public RelatedBuilder related(String slug) {
        return new RelatedBuilder(this).title(slug);
    }

    /**
     * Get the top 10 related shows.
     *
     * @param tvdbId TVDB ID.
     * @return Builder instance.
     */
    public RelatedBuilder related(int tvdbId) {
        return new RelatedBuilder(this).title(tvdbId);
    }

    /**
     * <p>Check into a show on trakt. Think of this method as in between a seen
     * and a scrobble. After checking in, the trakt will automatically display
     * it as watching then switch over to watched status once the duration has
     * elapsed.</p>
     *
     * <p><em>Warning</em>: This method requires a developer API key.</p>
     *
     * @param tvdbId TVDB ID for the show.
     * @return Builder instance.
     */
    public CheckinBuilder checkin(int tvdbId) {
        return new CheckinBuilder(this).tvdbId(tvdbId);
    }

    /**
     * <p>Check into a show on trakt. Think of this method as in between a seen
     * and a scrobble. After checking in, the trakt will automatically display
     * it as watching then switch over to watched status once the duration has
     * elapsed.</p>
     *
     * <p><em>Warning</em>: This method requires a developer API key.</p>
     *
     * @param title Show title.
     * @param year Show year.
     * @return Builder instance.
     */
    public CheckinBuilder checkin(String title, int year) {
        return new CheckinBuilder(this).title(title).year(year);
    }

    /**
     * <p>Notify trakt that a user wants to cancel their current check in.</p>
     *
     * <p><em>Warning</em>: This method requires a developer API key.</p>
     *
     * @return Builder instance.
     */
    public CancelCheckinBuilder cancelCheckin() {
        return new CancelCheckinBuilder(this);
    }


    public static final class CancelWatchingBuilder extends TraktApiBuilder<Response> {
        private static final String URI = "/show/cancelwatching/" + FIELD_API_KEY;

        private CancelWatchingBuilder(ShowService service) {
            super(service, new TypeToken<Response>() {}, URI, HttpMethod.Post);
        }
    }
    public static final class EpisodeLibraryBuilder extends TraktApiBuilder<Void> {
        private static final String POST_IMDB_ID = "imdb_id";
        private static final String POST_TVDB_ID = "tvdb_id";
        private static final String POST_TITLE = "title";
        private static final String POST_YEAR = "year";
        private static final String POST_EPISODES = "episodes";
        private static final String POST_SEASON = "season";
        private static final String POST_EPISODE = "episode";

        private static final String URI = "/show/episode/library/" + FIELD_API_KEY;

        private JsonArray episodeList;

        private EpisodeLibraryBuilder(ShowService service) {
            super(service, new TypeToken<Void>() {}, URI, HttpMethod.Post);

            this.episodeList = new JsonArray();
        }

        /**
         * Show IMDB ID.
         *
         * @param imdbId Value.
         * @return Builder instance.
         */
        public EpisodeLibraryBuilder imdbId(String imdbId) {
            this.postParameter(POST_IMDB_ID, imdbId);
            return this;
        }

        /**
         * Show TVDB ID.
         *
         * @param tvdbId Value.
         * @return Builder instance.
         */
        public EpisodeLibraryBuilder tvdbId(int tvdbId) {
            this.postParameter(POST_TVDB_ID, tvdbId);
            return this;
        }

        /**
         * Show title.
         *
         * @param title Value.
         * @return Builder instance.
         */
        public EpisodeLibraryBuilder title(String title) {
            this.postParameter(POST_TITLE, title);
            return this;
        }

        /**
         * Show year.
         *
         * @param year Value.
         * @return Builder instance.
         */
        public EpisodeLibraryBuilder year(int year) {
            this.postParameter(POST_YEAR, year);
            return this;
        }

        /**
         * Add an episode to the list.
         *
         * @param season Episode's season number.
         * @param episode Episode's number.
         * @return Builder instance.
         */
        public EpisodeLibraryBuilder episode(int season, int episode) {
            JsonObject ep = new JsonObject();
            ep.addProperty(POST_SEASON, season);
            ep.addProperty(POST_EPISODE, episode);
            this.episodeList.add(ep);
            return this;
        }

        @Override
        protected void preFireCallback() {
            //Add the assembled movie list to the JSON post body.
            this.postParameter(POST_EPISODES, this.episodeList);
        }
    }
    public static final class EpisodeSeenBuilder extends TraktApiBuilder<Void> {
        private static final String POST_IMDB_ID = "imdb_id";
        private static final String POST_TVDB_ID = "tvdb_id";
        private static final String POST_TITLE = "title";
        private static final String POST_YEAR = "year";
        private static final String POST_EPISODES = "episodes";
        private static final String POST_SEASON = "season";
        private static final String POST_EPISODE = "episode";

        private static final String URI = "/show/episode/seen/" + FIELD_API_KEY;

        private JsonArray episodeList;

        private EpisodeSeenBuilder(ShowService service) {
            super(service, new TypeToken<Void>() {}, URI, HttpMethod.Post);

            this.episodeList = new JsonArray();
        }

        /**
         * Show IMDB ID.
         *
         * @param imdbId Value.
         * @return Builder instance.
         */
        public EpisodeSeenBuilder imdbId(String imdbId) {
            this.postParameter(POST_IMDB_ID, imdbId);
            return this;
        }

        /**
         * Show TVDB ID.
         *
         * @param tvdbId Value.
         * @return Builder instance.
         */
        public EpisodeSeenBuilder tvdbId(int tvdbId) {
            this.postParameter(POST_TVDB_ID, tvdbId);
            return this;
        }

        /**
         * Show title.
         *
         * @param title Value.
         * @return Builder instance.
         */
        public EpisodeSeenBuilder title(String title) {
            this.postParameter(POST_TITLE, title);
            return this;
        }

        /**
         * Show year.
         *
         * @param year Value.
         * @return Builder instance.
         */
        public EpisodeSeenBuilder year(int year) {
            this.postParameter(POST_YEAR, year);
            return this;
        }

        /**
         * Add an episode to the list.
         *
         * @param season Episode's season number.
         * @param episode Episode's number.
         * @return Builder instance.
         */
        public EpisodeSeenBuilder episode(int season, int episode) {
            JsonObject ep = new JsonObject();
            ep.addProperty(POST_SEASON, season);
            ep.addProperty(POST_EPISODE, episode);
            this.episodeList.add(ep);
            return this;
        }

        @Override
        protected void preFireCallback() {
            //Add the assembled movie list to the JSON post body.
            this.postParameter(POST_EPISODES, this.episodeList);
        }
    }
    public static final class EpisodeSummaryBuilder extends TraktApiBuilder<TvEntity> {
        private static final String URI = "/show/episode/summary.json/" + FIELD_API_KEY + "/" + FIELD_TITLE + "/" + FIELD_SEASON + "/" + FIELD_EPISODE;

        private EpisodeSummaryBuilder(ShowService service) {
            super(service, new TypeToken<TvEntity>() {}, URI);
        }

        /**
         * Show title.
         *
         * @param title Value.
         * @return Builder instance.
         */
        public EpisodeSummaryBuilder title(String title) {
            this.field(FIELD_TITLE, title);
            return this;
        }

        /**
         * Show TVDB ID.
         *
         * @param tvdbId Value.
         * @return Builder instance.
         */
        public EpisodeSummaryBuilder tvdbId(int tvdbId) {
            this.field(FIELD_TITLE, tvdbId);
            return this;
        }

        /**
         * Show season.
         *
         * @param season Value.
         * @return Builder instance.
         */
        public EpisodeSummaryBuilder season(int season) {
            this.field(FIELD_SEASON, season);
            return this;
        }

        /**
         * Show episode.
         *
         * @param episode Value.
         * @return Builder instance.
         */
        public EpisodeSummaryBuilder episode(int episode) {
            this.field(FIELD_EPISODE, episode);
            return this;
        }
    }
    public static final class EpisodeUnlibraryBuilder extends TraktApiBuilder<Void> {
        private static final String POST_IMDB_ID = "imdb_id";
        private static final String POST_TVDB_ID = "tvdb_id";
        private static final String POST_TITLE = "title";
        private static final String POST_YEAR = "year";
        private static final String POST_EPISODES = "episodes";
        private static final String POST_SEASON = "season";
        private static final String POST_EPISODE = "episode";

        private static final String URI = "/show/episode/unlibrary/" + FIELD_API_KEY;

        private JsonArray episodeList;

        private EpisodeUnlibraryBuilder(ShowService service) {
            super(service, new TypeToken<Void>() {}, URI, HttpMethod.Post);

            this.episodeList = new JsonArray();
        }

        /**
         * Show IMDB ID.
         *
         * @param imdbId Value.
         * @return Builder instance.
         */
        public EpisodeUnlibraryBuilder imdbId(String imdbId) {
            this.postParameter(POST_IMDB_ID, imdbId);
            return this;
        }

        /**
         * Show TVDB ID.
         *
         * @param tvdbId Value.
         * @return Builder instance.
         */
        public EpisodeUnlibraryBuilder tvdbId(int tvdbId) {
            this.postParameter(POST_TVDB_ID, tvdbId);
            return this;
        }

        /**
         * Show title.
         *
         * @param title Value.
         * @return Builder instance.
         */
        public EpisodeUnlibraryBuilder title(String title) {
            this.postParameter(POST_TITLE, title);
            return this;
        }

        /**
         * Show year.
         *
         * @param year Value.
         * @return Builder instance.
         */
        public EpisodeUnlibraryBuilder year(int year) {
            this.postParameter(POST_YEAR, year);
            return this;
        }

        /**
         * Add an episode to the list.
         *
         * @param season Episode's season number.
         * @param episode Episode's number.
         * @return Builder instance.
         */
        public EpisodeUnlibraryBuilder episode(int season, int episode) {
            JsonObject ep = new JsonObject();
            ep.addProperty(POST_SEASON, season);
            ep.addProperty(POST_EPISODE, episode);
            this.episodeList.add(ep);
            return this;
        }

        @Override
        protected void preFireCallback() {
            //Add the assembled movie list to the JSON post body.
            this.postParameter(POST_EPISODES, this.episodeList);
        }
    }
    public static final class EpisodeUnseenBuilder extends TraktApiBuilder<Void> {
        private static final String POST_IMDB_ID = "imdb_id";
        private static final String POST_TVDB_ID = "tvdb_id";
        private static final String POST_TITLE = "title";
        private static final String POST_YEAR = "year";
        private static final String POST_EPISODES = "episodes";
        private static final String POST_SEASON = "season";
        private static final String POST_EPISODE = "episode";

        private static final String URI = "/show/episode/unseen/" + FIELD_API_KEY;

        private JsonArray episodeList;

        private EpisodeUnseenBuilder(ShowService service) {
            super(service, new TypeToken<Void>() {}, URI, HttpMethod.Post);

            this.episodeList = new JsonArray();
        }

        /**
         * Show IMDB ID.
         *
         * @param imdbId Value.
         * @return Builder instance.
         */
        public EpisodeUnseenBuilder imdbId(String imdbId) {
            this.postParameter(POST_IMDB_ID, imdbId);
            return this;
        }

        /**
         * Show TVDB ID.
         *
         * @param tvdbId Value.
         * @return Builder instance.
         */
        public EpisodeUnseenBuilder tvdbId(int tvdbId) {
            this.postParameter(POST_TVDB_ID, tvdbId);
            return this;
        }

        /**
         * Show title.
         *
         * @param title Value.
         * @return Builder instance.
         */
        public EpisodeUnseenBuilder title(String title) {
            this.postParameter(POST_TITLE, title);
            return this;
        }

        /**
         * Show year.
         *
         * @param year Value.
         * @return Builder instance.
         */
        public EpisodeUnseenBuilder year(int year) {
            this.postParameter(POST_YEAR, year);
            return this;
        }

        /**
         * Add an episode to the list.
         *
         * @param season Episode's season number.
         * @param episode Episode's number.
         * @return Builder instance.
         */
        public EpisodeUnseenBuilder episode(int season, int episode) {
            JsonObject ep = new JsonObject();
            ep.addProperty(POST_SEASON, season);
            ep.addProperty(POST_EPISODE, episode);
            this.episodeList.add(ep);
            return this;
        }

        @Override
        protected void preFireCallback() {
            //Add the assembled movie list to the JSON post body.
            this.postParameter(POST_EPISODES, this.episodeList);
        }
    }
    public static final class EpisodeUnwatchlistBuilder extends TraktApiBuilder<Void> {
        private static final String POST_IMDB_ID = "imdb_id";
        private static final String POST_TVDB_ID = "tvdb_id";
        private static final String POST_TITLE = "title";
        private static final String POST_YEAR = "year";
        private static final String POST_EPISODES = "episodes";
        private static final String POST_SEASON = "season";
        private static final String POST_EPISODE = "episode";

        private static final String URI = "/show/episode/unwatchlist/" + FIELD_API_KEY;

        private JsonArray episodeList;

        private EpisodeUnwatchlistBuilder(ShowService service) {
            super(service, new TypeToken<Void>() {}, URI, HttpMethod.Post);

            this.episodeList = new JsonArray();
        }

        /**
         * Show IMDB ID.
         *
         * @param imdbId Value.
         * @return Builder instance.
         */
        public EpisodeUnwatchlistBuilder imdbId(String imdbId) {
            this.postParameter(POST_IMDB_ID, imdbId);
            return this;
        }

        /**
         * Show TVDB ID.
         *
         * @param tvdbId Value.
         * @return Builder instance.
         */
        public EpisodeUnwatchlistBuilder tvdbId(int tvdbId) {
            this.postParameter(POST_TVDB_ID, tvdbId);
            return this;
        }

        /**
         * Show title.
         *
         * @param title Value.
         * @return Builder instance.
         */
        public EpisodeUnwatchlistBuilder title(String title) {
            this.postParameter(POST_TITLE, title);
            return this;
        }

        /**
         * Show year.
         *
         * @param year Value.
         * @return Builder instance.
         */
        public EpisodeUnwatchlistBuilder year(int year) {
            this.postParameter(POST_YEAR, year);
            return this;
        }

        /**
         * Add an episode to the list.
         *
         * @param season Episode's season number.
         * @param episode Episode's number.
         * @return Builder instance.
         */
        public EpisodeUnwatchlistBuilder episode(int season, int episode) {
            JsonObject ep = new JsonObject();
            ep.addProperty(POST_SEASON, season);
            ep.addProperty(POST_EPISODE, episode);
            this.episodeList.add(ep);
            return this;
        }

        @Override
        protected void preFireCallback() {
            //Add the assembled movie list to the JSON post body.
            this.postParameter(POST_EPISODES, this.episodeList);
        }
    }
    public static final class EpisodeWatchingNowBuilder extends TraktApiBuilder<List<UserProfile>> {
        private static final String URI = "/show/episode/watchingnow.json/" + FIELD_API_KEY + "/" + FIELD_TITLE + "/" + FIELD_SEASON + "/" + FIELD_EPISODE;

        private EpisodeWatchingNowBuilder(ShowService service) {
            super(service, new TypeToken<List<UserProfile>>() {}, URI);
        }

        /**
         * Either the slug (i.e. the-walking-dead) or TVDB ID. You can get a
         * show's slug by browsing the website and looking at the URL when on a
         * show summary page.
         *
         * @param title Value.
         * @return Builder instance.
         */
        public EpisodeWatchingNowBuilder title(String title) {
            this.field(FIELD_TITLE, title);
            return this;
        }

        /**
         * The season number. Use 0 if you want the specials.
         *
         * @param season Value.
         * @return Builder instance.
         */
        public EpisodeWatchingNowBuilder season(int season) {
            this.field(FIELD_SEASON, season);
            return this;
        }

        /**
         * The episode number.
         *
         * @param episode Value
         * @return Builder instance.
         */
        public EpisodeWatchingNowBuilder episode(int episode) {
            this.field(FIELD_EPISODE, episode);
            return this;
        }
    }
    public static final class EpisodeWatchlistBuilder extends TraktApiBuilder<Void> {
        private static final String POST_IMDB_ID = "imdb_id";
        private static final String POST_TVDB_ID = "tvdb_id";
        private static final String POST_TITLE = "title";
        private static final String POST_YEAR = "year";
        private static final String POST_EPISODES = "episodes";
        private static final String POST_SEASON = "season";
        private static final String POST_EPISODE = "episode";

        private static final String URI = "/show/episode/watchlist/" + FIELD_API_KEY;

        private JsonArray episodeList;

        private EpisodeWatchlistBuilder(ShowService service) {
            super(service, new TypeToken<Void>() {}, URI, HttpMethod.Post);

            this.episodeList = new JsonArray();
        }

        /**
         * Show IMDB ID.
         *
         * @param imdbId Value.
         * @return Builder instance.
         */
        public EpisodeWatchlistBuilder imdbId(String imdbId) {
            this.postParameter(POST_IMDB_ID, imdbId);
            return this;
        }

        /**
         * Show TVDB ID.
         *
         * @param tvdbId Value.
         * @return Builder instance.
         */
        public EpisodeWatchlistBuilder tvdbId(int tvdbId) {
            this.postParameter(POST_TVDB_ID, tvdbId);
            return this;
        }

        /**
         * Show title.
         *
         * @param title Value.
         * @return Builder instance.
         */
        public EpisodeWatchlistBuilder title(String title) {
            this.postParameter(POST_TITLE, title);
            return this;
        }

        /**
         * Show year.
         *
         * @param year Value.
         * @return Builder instance.
         */
        public EpisodeWatchlistBuilder year(int year) {
            this.postParameter(POST_YEAR, year);
            return this;
        }

        /**
         * Add an episode to the list.
         *
         * @param season Episode's season number.
         * @param episode Episode's number.
         * @return Builder instance.
         */
        public EpisodeWatchlistBuilder episode(int season, int episode) {
            JsonObject ep = new JsonObject();
            ep.addProperty(POST_SEASON, season);
            ep.addProperty(POST_EPISODE, episode);
            this.episodeList.add(ep);
            return this;
        }

        @Override
        protected void preFireCallback() {
            //Add the assembled movie list to the JSON post body.
            this.postParameter(POST_EPISODES, this.episodeList);
        }
    }
    public static final class ScrobbleBuilder extends TraktApiBuilder<Response> {
        private static final String POST_IMDB_ID = "imdb_id";
        private static final String POST_TVDB_ID = "tvdb_id";
        private static final String POST_TITLE = "title";
        private static final String POST_YEAR = "year";
        private static final String POST_SEASON = "season";
        private static final String POST_EPISODE = "episode";
        private static final String POST_DURATION = "duration";
        private static final String POST_PROGRESS = "progress";

        private static final String URI = "/show/scrobble/" + FIELD_API_KEY;

        private ScrobbleBuilder(ShowService service) {
            super(service, new TypeToken<Response>() {}, URI, HttpMethod.Post);
            this.includeScrobbleDebugStrings();
        }

        /**
         * IMDB ID for the show.
         *
         * @param imdbId Value.
         * @return Builder instance.
         */
        public ScrobbleBuilder imdbId(String imdbId) {
            this.postParameter(POST_IMDB_ID, imdbId);
            return this;
        }

        /**
         * TVDB ID for the show.
         *
         * @param tmdbId Value.
         * @return Builder instance.
         */
        public ScrobbleBuilder tvdbId(int tmdbId) {
            this.postParameter(POST_TVDB_ID, tmdbId);
            return this;
        }

        /**
         * Movie title.
         *
         * @param title Value.
         * @return Builder instance.
         */
        public ScrobbleBuilder title(String title) {
            this.postParameter(POST_TITLE, title);
            return this;
        }

        /**
         * Movie year.
         *
         * @param year Value.
         * @return Builder instance.
         */
        public ScrobbleBuilder year(int year) {
            this.postParameter(POST_YEAR, year);
            return this;
        }

        /**
         * Show season. Send 0 if watching a special.
         *
         * @param season Value.
         * @return Builder instance.
         */
        public ScrobbleBuilder season(int season) {
            this.postParameter(POST_SEASON, season);
            return this;
        }

        /**
         * Show episode.
         *
         * @param episode Value.
         * @return Builder instance.
         */
        public ScrobbleBuilder episode(int episode) {
            this.postParameter(POST_EPISODE, episode);
            return this;
        }

        /**
         * Duration (in minutes).
         *
         * @param duration Value.
         * @return Builder instance.
         */
        public ScrobbleBuilder duration(int duration) {
            this.postParameter(POST_DURATION, duration);
            return this;
        }

        /**
         * Percent progress (0-100). It is recommended to call the watching API
         * every 15 minutes, then call the scrobble API near the end of the
         * movie to lock it in.
         *
         * @param progress Value.
         * @return Builder instance.
         */
        public ScrobbleBuilder progress(int progress) {
            this.postParameter(POST_PROGRESS, progress);
            return this;
        }

        @Override
        protected void performValidation() {
            assert this.hasPostParameter(POST_TVDB_ID)
            || (this.hasPostParameter(POST_TITLE) && this.hasPostParameter(POST_YEAR))
            : "Either IMDB ID, TMDB ID, or both title and year is required.";
            assert this.hasPostParameter(POST_SEASON) : "Season is required.";
            assert this.hasPostParameter(POST_EPISODE) : "Episode is required.";
            assert this.hasPostParameter(POST_DURATION) : "Duration is required.";
            assert this.hasPostParameter(POST_PROGRESS) : "Progress is required.";
        }
    }
    public static final class SeasonBuilder extends TraktApiBuilder<List<TvShowEpisode>> {
        private static final String URI = "/show/season.json/" + FIELD_API_KEY + "/" + FIELD_QUERY + "/" + FIELD_SEASON;

        private SeasonBuilder(ShowService service, String query, int season) {
            super(service, new TypeToken<List<TvShowEpisode>>() {}, URI);

            this.field(FIELD_QUERY, query);
            this.field(FIELD_SEASON, season);
        }
    }
    public static final class SeasonsBuilder extends TraktApiBuilder<List<TvShowSeason>> {
        private static final String URI = "/show/seasons.json/" + FIELD_API_KEY + "/" + FIELD_QUERY;

        private SeasonsBuilder(ShowService service, String query) {
            super(service, new TypeToken<List<TvShowSeason>>() {}, URI);

            this.field(FIELD_QUERY, query);
        }
    }
    public static final class SeasonSeenBuilder extends TraktApiBuilder<Void> {
        private static final String POST_IMDB_ID = "imdb_id";
        private static final String POST_TVDB_ID = "tvdb_id";
        private static final String POST_TITLE = "title";
        private static final String POST_YEAR = "year";
        private static final String POST_SEASON = "season";
        
        private static final String URI = "/show/season/seen/" + FIELD_API_KEY;
        
        private SeasonSeenBuilder(ShowService service) {
            super(service, new TypeToken<Void>() {}, URI, HttpMethod.Post);
        }

        /**
         * Show IMDB ID.
         *
         * @param imdbId Value.
         * @return Builder instance.
         */
        public SeasonSeenBuilder imdbId(String imdbId) {
            postParameter(POST_IMDB_ID, imdbId);
            return this;
        }

        /**
         * Show TVDB ID.
         *
         * @param tvdbId Value.
         * @return Builder instance.
         */
        public SeasonSeenBuilder tvdbId(int tvdbId) {
            postParameter(POST_TVDB_ID, tvdbId);
            return this;
        }

        /**
         * Show title.
         *
         * @param title Value.
         * @return Builder instance.
         */
        public SeasonSeenBuilder title(String title) {
            postParameter(POST_TITLE, title);
            return this;
        }

        /**
         * Show year.
         *
         * @param year Value.
         * @return Builder instance.
         */
        public SeasonSeenBuilder year(int year) {
            postParameter(POST_YEAR, year);
            return this;
        }

        /**
         * Season.
         *
         * @param season Season number.
         * @return Builder instance.
         */
        public SeasonSeenBuilder season(int season) {
            postParameter(POST_SEASON, season);
            return this;
        }
    }
    public static final class ShowSeenBuilder extends TraktApiBuilder<Void> {
        private static final String POST_IMDB_ID = "imdb_id";
        private static final String POST_TVDB_ID = "tvdb_id";
        private static final String POST_TITLE = "title";
        private static final String POST_YEAR = "year";
        
        private static final String URI = "/show/seen/" + FIELD_API_KEY;
        
        private ShowSeenBuilder(ShowService service) {
            super(service, new TypeToken<Void>() {}, URI, HttpMethod.Post);
        }
    
        /**
         * Show IMDB ID.
         *
         * @param imdbId Value.
         * @return Builder instance.
         */
        public ShowSeenBuilder imdbId(String imdbId) {
            postParameter(POST_IMDB_ID, imdbId);
            return this;
        }
    
        /**
         * Show TVDB ID.
         *
         * @param tvdbId Value.
         * @return Builder instance.
         */
        public ShowSeenBuilder tvdbId(int tvdbId) {
            postParameter(POST_TVDB_ID, tvdbId);
            return this;
        }
    
        /**
         * Show title.
         *
         * @param title Value.
         * @return Builder instance.
         */
        public ShowSeenBuilder title(String title) {
            postParameter(POST_TITLE, title);
            return this;
        }
    
        /**
         * Show year.
         *
         * @param year Value.
         * @return Builder instance.
         */
        public ShowSeenBuilder year(int year) {
            postParameter(POST_YEAR, year);
            return this;
        }
    }
    public static final class SummaryBuilder extends TraktApiBuilder<TvShow> {
        private static final String EXTENDED = "extended";

        private static final String URI = "/show/summary.json/" + FIELD_API_KEY + "/" + FIELD_TITLE + "/" + FIELD_EXTENDED;

        private SummaryBuilder(ShowService service) {
            super(service, new TypeToken<TvShow>() {}, URI);
        }

        /**
         * Either the slug (i.e. the-walking-dead) or TVDB ID. You can get a
         * show's slug by browsing the website and looking at the URL when on a
         * show summary page.
         *
         * @param title Value.
         * @return Builder instance.
         */
        public SummaryBuilder title(String title) {
            this.field(FIELD_TITLE, title);
            return this;
        }

        /**
         * Returns complete season and episode info. Only send this if you
         * really need the full dump. Use the show/seasons and show/season
         * methods if you only need some of the season or episode info.
         *
         * @return Builder instance.
         */
        public SummaryBuilder extended() {
            this.field(FIELD_EXTENDED, EXTENDED);
            return this;
        }
    }
    public static final class TrendingBuilder extends TraktApiBuilder<List<TvShow>> {
        private static final String URI = "/shows/trending.json/" + FIELD_API_KEY;

        private TrendingBuilder(ShowService service) {
            super(service, new TypeToken<List<TvShow>>() {}, URI);
        }
    }
    public static final class UnlibraryBuilder extends TraktApiBuilder<Void> {
        private static final String POST_IMDB_ID = "imdb_id";
        private static final String POST_TVDB_ID = "tvdb_id";
        private static final String POST_TITLE = "title";
        private static final String POST_YEAR = "year";

        private static final String URI = "/show/unlibrary/" + FIELD_API_KEY;

        private UnlibraryBuilder(ShowService service) {
            super(service, new TypeToken<Void>() {}, URI, HttpMethod.Post);
        }

        /**
         * IMDB ID for the show.
         *
         * @param imdbId Value.
         * @return Builder instance.
         */
        public UnlibraryBuilder imdbId(String imdbId) {
            this.postParameter(POST_IMDB_ID, imdbId);
            return this;
        }

        /**
         * TVDB ID for the show.
         *
         * @param tmdbId Value.
         * @return Builder instance.
         */
        public UnlibraryBuilder tvdbId(int tmdbId) {
            this.postParameter(POST_TVDB_ID, tmdbId);
            return this;
        }

        /**
         * Movie title.
         *
         * @param title Value.
         * @return Builder instance.
         */
        public UnlibraryBuilder title(String title) {
            this.postParameter(POST_TITLE, title);
            return this;
        }

        /**
         * Movie year.
         *
         * @param year Value.
         * @return Builder instance.
         */
        public UnlibraryBuilder year(int year) {
            this.postParameter(POST_YEAR, year);
            return this;
        }
    }
    public static final class UnwatchlistBuilder extends TraktApiBuilder<Void> {
        private static final String POST_IMDB_ID = "imdb_id";
        private static final String POST_TVDB_ID = "tvdb_id";
        private static final String POST_TITLE = "title";
        private static final String POST_YEAR = "year";
        private static final String POST_SHOWS = "shows";

        private static final String URI = "/show/unwatchlist/" + FIELD_API_KEY;

        private JsonArray showList;

        private UnwatchlistBuilder(ShowService service) {
            super(service, new TypeToken<Void>() {}, URI, HttpMethod.Post);

            this.showList = new JsonArray();
        }

        /**
         * Add a show to be removed.
         *
         * @param imdbId IMDB ID for the show.
         * @return Builder instance.
         */
        public UnwatchlistBuilder imdbId(String imdbId) {
            JsonObject show = new JsonObject();
            show.addProperty(POST_IMDB_ID, imdbId);
            this.showList.add(show);
            return this;
        }

        /**
         * Add a show to be removed.
         *
         * @param tvdbId TVDB ID for the show.
         * @return Builder instance.
         */
        public UnwatchlistBuilder tvdbId(int tvdbId) {
            JsonObject show = new JsonObject();
            show.addProperty(POST_TVDB_ID, tvdbId);
            this.showList.add(show);
            return this;
        }

        /**
         * Add a show to be removed.
         *
         * @param title Title for the show.
         * @param year Year of the show.
         * @return Builder instance.
         */
        public UnwatchlistBuilder title(String title, int year) {
            JsonObject show = new JsonObject();
            show.addProperty(POST_TITLE, title);
            show.addProperty(POST_YEAR, year);
            this.showList.add(show);
            return this;
        }

        @Override
        protected void preFireCallback() {
            this.postParameter(POST_SHOWS, this.showList);
        }
    }
    public static final class WatchingBuilder extends TraktApiBuilder<Response> {
        private static final String POST_IMDB_ID = "imdb_id";
        private static final String POST_TVDB_ID = "tvdb_id";
        private static final String POST_TITLE = "title";
        private static final String POST_YEAR = "year";
        private static final String POST_SEASON = "season";
        private static final String POST_EPISODE = "episode";
        private static final String POST_DURATION = "duration";
        private static final String POST_PROGRESS = "progress";

        private static final String URI = "/show/watching/" + FIELD_API_KEY;

        private WatchingBuilder(ShowService service) {
            super(service, new TypeToken<Response>() {}, URI, HttpMethod.Post);
            this.includeScrobbleDebugStrings();
        }

        /**
         * IMDB ID for the show.
         *
         * @param imdbId Value.
         * @return Builder instance.
         */
        public WatchingBuilder imdbId(String imdbId) {
            this.postParameter(POST_IMDB_ID, imdbId);
            return this;
        }

        /**
         * TVDB ID for the show.
         *
         * @param tmdbId Value.
         * @return Builder instance.
         */
        public WatchingBuilder tvdbId(int tmdbId) {
            this.postParameter(POST_TVDB_ID, tmdbId);
            return this;
        }

        /**
         * Movie title.
         *
         * @param title Value.
         * @return Builder instance.
         */
        public WatchingBuilder title(String title) {
            this.postParameter(POST_TITLE, title);
            return this;
        }

        /**
         * Movie year.
         *
         * @param year Value.
         * @return Builder instance.
         */
        public WatchingBuilder year(int year) {
            this.postParameter(POST_YEAR, year);
            return this;
        }

        /**
         * Show season. Send 0 if watching a special.
         *
         * @param season Value.
         * @return Builder instance.
         */
        public WatchingBuilder season(int season) {
            this.postParameter(POST_SEASON, season);
            return this;
        }

        /**
         * Show episode.
         *
         * @param episode Value.
         * @return Builder instance.
         */
        public WatchingBuilder episode(int episode) {
            this.postParameter(POST_EPISODE, episode);
            return this;
        }

        /**
         * Duration (in minutes).
         *
         * @param duration Value.
         * @return Builder instance.
         */
        public WatchingBuilder duration(int duration) {
            this.postParameter(POST_DURATION, duration);
            return this;
        }

        /**
         * Percent progress (0-100). It is recommended to call the watching API
         * every 15 minutes, then call the scrobble API near the end of the
         * movie to lock it in.
         *
         * @param progress Value.
         * @return Builder instance.
         */
        public WatchingBuilder progress(int progress) {
            this.postParameter(POST_PROGRESS, progress);
            return this;
        }

        @Override
        protected void performValidation() {
            assert this.hasPostParameter(POST_TVDB_ID)
            || (this.hasPostParameter(POST_TITLE) && this.hasPostParameter(POST_YEAR))
            : "Either IMDB ID, TMDB ID, or both title and year is required.";
            assert this.hasPostParameter(POST_SEASON) : "Season is required.";
            assert this.hasPostParameter(POST_EPISODE) : "Episode is required.";
            assert this.hasPostParameter(POST_DURATION) : "Duration is required.";
            assert this.hasPostParameter(POST_PROGRESS) : "Progress is required.";
        }
    }
    public static final class WatchingNowBuilder extends TraktApiBuilder<List<UserProfile>> {
        private static final String URI = "/show/watchingnow.json/" + FIELD_API_KEY + "/" + FIELD_QUERY;

        private WatchingNowBuilder(ShowService service) {
            super(service, new TypeToken<List<UserProfile>>() {}, URI);
        }

        /**
         * Either the slug (i.e. the-walking-dead) or TVDB ID. You can get a
         * show's slug by browsing the website and looking at the URL when on
         * a show summary page.
         *
         * @param query Value.
         * @return Builder instance.
         */
        public WatchingNowBuilder query(String query) {
            this.field(FIELD_QUERY, query);
            return this;
        }
    }
    public static final class WatchlistBuilder extends TraktApiBuilder<Void> {
        private static final String POST_IMDB_ID = "imdb_id";
        private static final String POST_TVDB_ID = "tvdb_id";
        private static final String POST_TITLE = "title";
        private static final String POST_YEAR = "year";
        private static final String POST_SHOWS = "shows";

        private static final String URI = "/show/watchlist/" + FIELD_API_KEY;

        private JsonArray showList;

        private WatchlistBuilder(ShowService service) {
            super(service, new TypeToken<Void>() {}, URI, HttpMethod.Post);

            this.showList = new JsonArray();
        }

        /**
         * Add a show to be added.
         *
         * @param imdbId IMDB ID for the show.
         * @return Builder instance.
         */
        public WatchlistBuilder imdbId(String imdbId) {
            JsonObject show = new JsonObject();
            show.addProperty(POST_IMDB_ID, imdbId);
            this.showList.add(show);
            return this;
        }

        /**
         * Add a show to be added.
         *
         * @param tvdbId TVDB ID for the show.
         * @return Builder instance.
         */
        public WatchlistBuilder tvdbId(int tvdbId) {
            JsonObject show = new JsonObject();
            show.addProperty(POST_TVDB_ID, tvdbId);
            this.showList.add(show);
            return this;
        }

        /**
         * Add a show to be added.
         *
         * @param title Title for the show.
         * @param year Year of the show.
         * @return Builder instance.
         */
        public WatchlistBuilder title(String title, int year) {
            JsonObject show = new JsonObject();
            show.addProperty(POST_TITLE, title);
            show.addProperty(POST_YEAR, year);
            this.showList.add(show);
            return this;
        }

        @Override
        protected void preFireCallback() {
            this.postParameter(POST_SHOWS, this.showList);
        }
    }
    public static final class ShoutsBuilder extends TraktApiBuilder<List<Shout>> {
        private static final String URI = "/show/shouts.json/" + FIELD_API_KEY + "/" + FIELD_TITLE;

        private ShoutsBuilder(ShowService service) {
            super(service, new TypeToken<List<Shout>>() {}, URI);
        }

        /**
         * Set show title.
         *
         * @param title Value.
         * @return Builder instance.
         */
        public ShoutsBuilder title(String title) {
            this.field(FIELD_TITLE, title);
            return this;
        }

        /**
         * Set show TVDB ID
         *
         * @param tvdbId Value.
         * @return Builder instance.
         */
        public ShoutsBuilder title(int tvdbId) {
            this.field(FIELD_TITLE, tvdbId);
            return this;
        }
    }
    public static final class EpisodeShoutsBuilder extends TraktApiBuilder<List<Shout>> {
        private static final String URI = "/show/episode/shouts.json/" + FIELD_API_KEY + "/" + FIELD_TITLE + "/" + FIELD_SEASON + "/" + FIELD_EPISODE;

        private EpisodeShoutsBuilder(ShowService service) {
            super(service, new TypeToken<List<Shout>>() {}, URI);
        }

        /**
         * Set show title or IMDB ID.
         *
         * @param titleOrImdbId Value.
         * @return Builder instance.
         */
        public EpisodeShoutsBuilder title(String titleOrImdbId) {
            this.field(FIELD_TITLE, titleOrImdbId);
            return this;
        }

        /**
         * Set show TMDB ID.
         *
         * @param tmdbId Value.
         * @return Builder instance.
         */
        public EpisodeShoutsBuilder title(int tmdbId) {
            this.field(FIELD_TITLE, tmdbId);
            return this;
        }

        /**
         * Set episode season.
         *
         * @param season Value.
         * @return Builder instance.
         */
        public EpisodeShoutsBuilder season(int season) {
            this.field(FIELD_SEASON, season);
            return this;
        }

        /**
         * Set episode number.
         *
         * @param episode Value.
         * @return Builder instance.
         */
        public EpisodeShoutsBuilder episode(int episode) {
            this.field(FIELD_EPISODE, episode);
            return this;
        }
    }
    public static final class RelatedBuilder extends TraktApiBuilder<List<TvShow>> {
        private static final String EXTENDED = "extended";
        private static final String HIDE_WATCHED = "hidewatched";

        private static final String URI = "/show/related.json/" + FIELD_API_KEY + "/" + FIELD_TITLE + "/" + FIELD_EXTENDED + "/" + FIELD_HIDE_WATCHED;

        private RelatedBuilder(ShowService service) {
            super(service, new TypeToken<List<TvShow>>() {}, URI);
        }

        /**
         * Show title.
         *
         * @param slug Show title slug.
         * @return Builder instance.
         */
        public RelatedBuilder title(String slug) {
            this.field(FIELD_TITLE, slug);
            return this;
        }

        /**
         * Show ID.
         *
         * @param tvdbId TVDB ID.
         * @return Builder instance.
         */
        public RelatedBuilder title(int tvdbId) {
            this.field(FIELD_TITLE, tvdbId);
            return this;
        }

        /**
         * Returns complete season and episode info. Only send this if you
         * really need the full dump. Use the show/seasons and show/season
         * methods if you only need some of the season or episode info.
         *
         * @return Builder instance.
         */
        public RelatedBuilder extended() {
            this.field(FIELD_EXTENDED, EXTENDED);
            return this;
        }

        /**
         * If this parameter is set and valid auth is sent, shows with at least one play will be filtered out.
         *
         * @param hideWatched Value.
         * @return Builder instance.
         */
        public RelatedBuilder hideWatched(boolean hideWatched) {
            if (hideWatched) {
                this.field(FIELD_HIDE_WATCHED, HIDE_WATCHED);
            }
            return this;
        }
    }
    public static final class CheckinBuilder extends TraktApiBuilder<Response> {
        private static final String POST_TVDB_ID = "tvdb_id";
        private static final String POST_TITLE = "title";
        private static final String POST_YEAR = "year";
        private static final String POST_SEASON = "season";
        private static final String POST_EPISODE = "episode";
        private static final String POST_DURATION = "duration";
        private static final String POST_VENUE_ID = "venue_id";
        private static final String POST_VENUE_NAME = "venue_name";
        private static final String POST_MESSAGE = "message";

        private static final String URI = "/show/checkin/" + FIELD_API_KEY;

        private CheckinBuilder(ShowService service) {
            super(service, new TypeToken<Response>() {}, URI, HttpMethod.Post);
            this.includeCheckinDebugStrings();
        }

        /** TVDB ID for the show. */
        public CheckinBuilder tvdbId(int tvdbId) {
            this.postParameter(POST_TVDB_ID, tvdbId);
            return this;
        }

        /** Show title. */
        public CheckinBuilder title(String title) {
            this.postParameter(POST_TITLE, title);
            return this;
        }

        /** Show year. */
        public CheckinBuilder year(int year) {
            this.postParameter(POST_YEAR, year);
            return this;
        }

        /** Show season. Send '0' if watching a special. */
        public CheckinBuilder season(int season) {
            this.postParameter(POST_SEASON, season);
            return this;
        }

        /** Show episode. */
        public CheckinBuilder episode(int episode) {
            this.postParameter(POST_EPISODE, episode);
            return this;
        }

        /** Duration in minutes. */
        public CheckinBuilder duration(int duration) {
            this.postParameter(POST_DURATION, duration);
            return this;
        }

        /** Foursquare venue ID. */
        public CheckinBuilder venueId(int venueId) {
            this.postParameter(POST_VENUE_ID, venueId);
            return this;
        }

        /** Custom venue name for display purposes. */
        public CheckinBuilder venueName(String venueName) {
            this.postParameter(POST_VENUE_NAME, venueName);
            return this;
        }

        /**
         * The message to use for sharing. If not sent, it will use the
         * localized watching string set on the connections page. The message
         * will be truncated to 100 characters to make sure it fits in the tweet
         * with the url and hashtag.
         */
        public CheckinBuilder message(String message) {
            this.postParameter(POST_MESSAGE, message);
            return this;
        }
    }
    public static final class CancelCheckinBuilder extends TraktApiBuilder<Response> {
        private static final String URI = "/show/cancelcheckin/" + FIELD_API_KEY;

        private CancelCheckinBuilder(ShowService service) {
            super(service, new TypeToken<Response>() {}, URI, HttpMethod.Post);
        }
    }
}
