package com.jakewharton.trakt.services;

import com.google.myjson.reflect.TypeToken;
import com.jakewharton.trakt.TraktApiBuilder;
import com.jakewharton.trakt.TraktApiService;
import com.jakewharton.trakt.entities.Activity;
import com.jakewharton.trakt.enumerations.ActivityAction;
import com.jakewharton.trakt.enumerations.ActivityType;

import java.util.Date;

public class ActivityService extends TraktApiService {
    /**
     * Get a list of all public activity for the entire Trakt community. The
     * most recent 100 activities are returned for all types and actions. You
     * can customize the activity stream with only the types and actions you
     * need.
     */
    public CommunityBuilder community() {
        return new CommunityBuilder(this);
    }

    /**
     * <p>Get a list of all activity for one or more episodes for one or more
     * seasons for one or more shows. The most recent 100 activities are
     * returned for all actions. You can customize the activity stream with
     * only the actions you need.</p>
     *
     * <p>You <strong>must</strong> specify one or more values for each of
     * {@link EpisodesBuilder#title(String...)},
     * {@link EpisodesBuilder#season(int...)}, and
     * {@link EpisodesBuilder#episode(int...)} when using this method. If you
     * only want information on a single episode, use
     * {@link #episodes(String, int, int)} or
     * {@link #episodes(int, int, int)} for quicker access.</p>
     */
    public EpisodesBuilder episodes() {
        return new EpisodesBuilder(this);
    }

    /**
     * Get a list of all activity for one or more episodes for one or more
     * seasons for one or more shows. The most recent 100 activities are
     * returned for all actions. You can customize the activity stream with
     * only the actions you need.
     */
    public EpisodesBuilder episodes(String title, int season, int episode) {
        return new EpisodesBuilder(this).title(title).season(season).episode(episode);
    }

    /**
     * Get a list of all activity for one or more episodes for one or more
     * seasons for one or more shows. The most recent 100 activities are
     * returned for all actions. You can customize the activity stream with
     * only the actions you need.
     */
    public EpisodesBuilder episodes(int tvdbId, int season, int episode) {
        return new EpisodesBuilder(this).title(tvdbId).season(season).episode(episode);
    }

    /**
     * Get a list of all activity for your friends. The most recent 100
     * activities are returned for all types and actions. You can customize the
     * activity stream with only the types and actions you need.
     */
    public FriendsBuilder friends() {
        return new FriendsBuilder(this);
    }

    /**
     * Get a list of all activity for one or more movies. The most recent 100
     * activities are returned for all actions. You can customize the activity
     * stream with only the actions you need.
     */
    public MoviesBuilder movies(String... titlesOrImdbIds) {
        return new MoviesBuilder(this).titles(titlesOrImdbIds);
    }

    /**
     * Get a list of all activity for one or more movies. The most recent 100
     * activities are returned for all actions. You can customize the activity
     * stream with only the actions you need.
     */
    public MoviesBuilder movies(int... tmdbIds) {
        return new MoviesBuilder(this).titles(tmdbIds);
    }

    /**
     * <p>Get a list of all activity for one or more seasons for one or more
     * shows. The most recent 100 activities are returned for all actions. You
     * can customize the activity stream with only the actions you need.</p>
     *
     * <p>You <strong>must</strong> specify one or more values for each of
     * {@link SeasonsBuilder#title(String...)} and
     * {@link SeasonsBuilder#season(int...)} when using this method. If you
     * only want information on a single season, use
     * {@link #seasons(String, int)} or {@link #seasons(int, int)} for
     * quicker access.</p>
     */
    public SeasonsBuilder seasons() {
        return new SeasonsBuilder(this);
    }

    /**
     * Get a list of all activity for one or more seasons for one or more
     * shows. The most recent 100 activities are returned for all actions. You
     * can customize the activity stream with only the actions you need.
     */
    public SeasonsBuilder seasons(String title, int season) {
        return new SeasonsBuilder(this).title(title).season(season);
    }

    /**
     * Get a list of all activity for one or more seasons for one or more
     * shows. The most recent 100 activities are returned for all actions. You
     * can customize the activity stream with only the actions you need.
     */
    public SeasonsBuilder seasons(int tvdbId, int season) {
        return new SeasonsBuilder(this).title(tvdbId).season(season);
    }

    /**
     * Get a list of all activity for one or more shows. The most recent 100
     * activities are returned for all actions. You can customize the activity
     * stream with only the actions you need.
     */
    public ShowsBuilder shows(String... titles) {
        return new ShowsBuilder(this).title(titles);
    }

    /**
     * Get a list of all activity for one or more shows. The most recent 100
     * activities are returned for all actions. You can customize the activity
     * stream with only the actions you need.
     */
    public ShowsBuilder shows(int... tvdbIds) {
        return new ShowsBuilder(this).title(tvdbIds);
    }

    /**
     * Get a list of all activity for a single user. The most recent 100
     * activities are returned for all types and actions. You can customize the
     * activity stream with only the types and actions you need.
     */
    public UserBuilder user(String username) {
        return new UserBuilder(this).username(username);
    }


    public static class CommunityBuilder extends TraktApiBuilder<Activity> {
        private static final String URI = "/activity/community.json/" + FIELD_API_KEY + "/" + FIELD_TYPES + "/" + FIELD_ACTIONS + "/" + FIELD_TIMESTAMP;

        private CommunityBuilder(ActivityService service) {
            super(service, new TypeToken<Activity>() {}, URI);
        }

        /**
         * Defaults to all, but you can instead send a list of types.
         */
        public CommunityBuilder types(ActivityType... types) {
            this.field(FIELD_TYPES, types);
            return this;
        }

        /**
         * Defaults to all, but you can instead send a list of actions.
         */
        public CommunityBuilder actions(ActivityAction... actions) {
            if (!this.hasField(FIELD_TYPES)) {
                this.types(ActivityType.All);
            }
            this.field(FIELD_ACTIONS, actions);
            return this;
        }

        /**
         * Specify the start timestamp in PST. Only activity from this
         * timestamp forward will be returned.
         */
        public CommunityBuilder timestamp(Date timestamp) {
            return this.timestamp(timestamp.getTime() / MILLISECONDS_IN_SECOND);
        }

        /**
         * Specify the start timestamp in PST. Only activity from this
         * timestamp forward will be returned.
         */
        public CommunityBuilder timestamp(long timestamp) {
            if (!this.hasField(FIELD_ACTIONS)) {
                this.actions(ActivityAction.All);
            }
            this.field(FIELD_TIMESTAMP, timestamp);
            return this;
        }
    }
    public static class EpisodesBuilder extends TraktApiBuilder<Activity> {
        private static final String URI = "/activity/episodes.json/" + FIELD_API_KEY + "/" + FIELD_TITLE + "/" + FIELD_SEASON + "/" + FIELD_EPISODE + "/" + FIELD_ACTIONS + "/" + FIELD_TIMESTAMP;

        private EpisodesBuilder(ActivityService service) {
            super(service, new TypeToken<Activity>() {}, URI);
        }

        /**
         * The slug (i.e. the-walking-dead). You can get a show's slug by
         * browsing the website and looking at the URL when on a show summary
         * page. You can also send a list of titles to get activity for
         * multiple shows.
         */
        public EpisodesBuilder title(String... titles) {
            this.field(FIELD_TITLE, titles);
            return this;
        }

        /**
         * The TVDB ID. You can also send a list of IDs to get activity for
         * multiple shows.
         */
        public EpisodesBuilder title(int... tvdbIds) {
            this.field(FIELD_TITLE, tvdbIds);
            return this;
        }

        /**
         * The season number. Use 0 if you want the specials. You can also send
         * a list of seasons to get activity for multiple seasons.
         */
        public EpisodesBuilder season(int... seasons) {
            return this;
        }

        /**
         * The episode number. You can also send a list of episodes to get
         * activity for multiple episodes.
         */
        public EpisodesBuilder episode(int... episodes) {
            return this;
        }

        /**
         * Defaults to all, but you can instead send a list of actions.
         */
        public EpisodesBuilder actions(ActivityAction... actions) {
            return this;
        }

        /**
         * Specify the start timestamp in PST. Only activity from this
         * timestamp forward will be returned.
         */
        public EpisodesBuilder timestamp(Date timestamp) {
            return this.timestamp(timestamp.getTime() / MILLISECONDS_IN_SECOND);
        }

        /**
         * Specify the start timestamp in PST. Only activity from this
         * timestamp forward will be returned.
         */
        public EpisodesBuilder timestamp(long timestamp) {
            if (!this.hasField(FIELD_ACTIONS)) {
                this.actions(ActivityAction.All);
            }
            this.field(FIELD_TIMESTAMP, timestamp);
            return this;
        }
    }
    public static class FriendsBuilder extends TraktApiBuilder<Activity> {
        private static final String URI = "/activity/friends.json/" + FIELD_API_KEY + "/" + FIELD_TYPES + "/" + FIELD_ACTIONS + "/" + FIELD_TIMESTAMP;

        private FriendsBuilder(ActivityService service) {
            super(service, new TypeToken<Activity>() {}, URI, HttpMethod.Post);
        }

        /**
         * Defaults to all, but you can instead send a list of types.
         */
        public FriendsBuilder types(ActivityType... types) {
            this.field(FIELD_TYPES, types);
            return this;
        }

        /**
         * Defaults to all, but you can instead send a list of actions.
         */
        public FriendsBuilder actions(ActivityAction... actions) {
            if (!this.hasField(FIELD_TYPES)) {
                this.types(ActivityType.All);
            }
            this.field(FIELD_ACTIONS, actions);
            return this;
        }

        /**
         * Specify the start timestamp in PST. Only activity from this
         * timestamp forward will be returned.
         */
        public FriendsBuilder timestamp(Date timestamp) {
            return this.timestamp(timestamp.getTime() / MILLISECONDS_IN_SECOND);
        }

        /**
         * Specify the start timestamp in PST. Only activity from this
         * timestamp forward will be returned.
         */
        public FriendsBuilder timestamp(long timestamp) {
            if (!this.hasField(FIELD_ACTIONS)) {
                this.actions(ActivityAction.All);
            }
            this.field(FIELD_TIMESTAMP, timestamp);
            return this;
        }
    }
    public static class MoviesBuilder extends TraktApiBuilder<Activity> {
        private static final String URI = "/activity/movies.json/" + FIELD_API_KEY + "/" + FIELD_TITLE + "/" + FIELD_ACTIONS + "/" + FIELD_TIMESTAMP;

        private MoviesBuilder(ActivityService service) {
            super(service, new TypeToken<Activity>() {}, URI);
        }


        /**
         * Either the slug (i.e. the-social-network-2010) or IMDB ID. You can
         * get a movie's slug by browsing the website and looking at the URL
         * when on a movie summary page. You can also send a list of titles to
         * get activity for multiple movies.
         */
        public MoviesBuilder titles(String... titles) {
            this.field(FIELD_TITLE, titles);
            return this;
        }

        /**
         * The TMDB ID. You can also send a list of titles to get activity for
         * multiple movies.
         */
        public MoviesBuilder titles(int... tmdbIds) {
            this.field(FIELD_TITLE, tmdbIds);
            return this;
        }

        /**
         * Defaults to all, but you can instead send a list of actions.
         */
        public MoviesBuilder actions(ActivityAction... actions) {
            this.field(FIELD_ACTIONS, actions);
            return this;
        }

        /**
         * Specify the start timestamp in PST. Only activity from this
         * timestamp forward will be returned.
         */
        public MoviesBuilder timestamp(Date timestamp) {
            return this.timestamp(timestamp.getTime() / MILLISECONDS_IN_SECOND);
        }

        /**
         * Specify the start timestamp in PST. Only activity from this
         * timestamp forward will be returned.
         */
        public MoviesBuilder timestamp(long timestamp) {
            if (!this.hasField(FIELD_ACTIONS)) {
                this.actions(ActivityAction.All);
            }
            this.field(FIELD_TIMESTAMP, timestamp);
            return this;
        }
    }
    public static class SeasonsBuilder extends TraktApiBuilder<Activity> {
        private static final String URI = "/activity/seasons.json/" + FIELD_API_KEY + "/" + FIELD_TITLE + "/" + FIELD_SEASON + "/" + FIELD_ACTIONS + "/" + FIELD_TIMESTAMP;

        private SeasonsBuilder(ActivityService service) {
            super(service, new TypeToken<Activity>() {}, URI);
        }

        /**
         * The slug (i.e. the-walking-dead). You can get a show's slug by
         * browsing the website and looking at the URL when on a show summary
         * page. You can also send a list of titles to get activity for
         * multiple shows.
         */
        public SeasonsBuilder title(String... titles) {
            this.field(FIELD_TITLE, titles);
            return this;
        }

        /**
         * The TVDB ID. You can also send a list of IDs to get activity for
         * multiple shows.
         */
        public SeasonsBuilder title(int... tvdbIds) {
            this.field(FIELD_TITLE, tvdbIds);
            return this;
        }

        /**
         * The season number. Use 0 if you want the specials. You can also send
         * a list of seasons to get activity for multiple seasons.
         */
        public SeasonsBuilder season(int... seasons) {
            return this;
        }

        /**
         * Defaults to all, but you can instead send a list of actions.
         */
        public SeasonsBuilder actions(ActivityAction... actions) {
            return this;
        }

        /**
         * Specify the start timestamp in PST. Only activity from this
         * timestamp forward will be returned.
         */
        public SeasonsBuilder timestamp(Date timestamp) {
            return this.timestamp(timestamp.getTime() / MILLISECONDS_IN_SECOND);
        }

        /**
         * Specify the start timestamp in PST. Only activity from this
         * timestamp forward will be returned.
         */
        public SeasonsBuilder timestamp(long timestamp) {
            if (!this.hasField(FIELD_ACTIONS)) {
                this.actions(ActivityAction.All);
            }
            this.field(FIELD_TIMESTAMP, timestamp);
            return this;
        }
    }
    public static class ShowsBuilder extends TraktApiBuilder<Activity> {
        private static final String URI = "/activity/shows.json/" + FIELD_API_KEY + "/" + FIELD_TITLE + "/" + FIELD_ACTIONS + "/" + FIELD_TIMESTAMP;

        private ShowsBuilder(ActivityService service) {
            super(service, new TypeToken<Activity>() {}, URI);
        }

        /**
         * The slug (i.e. the-walking-dead). You can get a show's slug by
         * browsing the website and looking at the URL when on a show summary
         * page. You can also send a list of titles to get activity for
         * multiple shows.
         */
        public ShowsBuilder title(String... titles) {
            this.field(FIELD_TITLE, titles);
            return this;
        }

        /**
         * The TVDB ID. You can also send a list of IDs to get activity for
         * multiple shows.
         */
        public ShowsBuilder title(int... tvdbIds) {
            this.field(FIELD_TITLE, tvdbIds);
            return this;
        }

        /**
         * Defaults to all, but you can instead send a list of actions.
         */
        public ShowsBuilder actions(ActivityAction... actions) {
            return this;
        }

        /**
         * Specify the start timestamp in PST. Only activity from this
         * timestamp forward will be returned.
         */
        public ShowsBuilder timestamp(Date timestamp) {
            return this.timestamp(timestamp.getTime() / MILLISECONDS_IN_SECOND);
        }

        /**
         * Specify the start timestamp in PST. Only activity from this
         * timestamp forward will be returned.
         */
        public ShowsBuilder timestamp(long timestamp) {
            if (!this.hasField(FIELD_ACTIONS)) {
                this.actions(ActivityAction.All);
            }
            this.field(FIELD_TIMESTAMP, timestamp);
            return this;
        }
    }
    public static class UserBuilder extends TraktApiBuilder<Activity> {
        private static final String URI = "/activity/user.json/" + FIELD_API_KEY + "/" + FIELD_USERNAME + "/" + FIELD_TYPES + "/" + FIELD_ACTIONS + "/" + FIELD_TIMESTAMP;

        private UserBuilder(ActivityService service) {
            super(service, new TypeToken<Activity>() {}, URI);
        }


        /**
         * You can get a username by browsing the website and looking at the
         * URL when on a profile page.
         */
        public UserBuilder username(String username) {
            this.field(FIELD_USERNAME, username, false);
            return this;
        }

        /**
         * Defaults to all, but you can instead send a list of types.
         */
        public UserBuilder types(ActivityType... types) {
            this.field(FIELD_TYPES, types);
            return this;
        }

        /**
         * Defaults to all, but you can instead send a list of actions.
         */
        public UserBuilder actions(ActivityAction... actions) {
            if (!this.hasField(FIELD_TYPES)) {
                this.types(ActivityType.All);
            }
            this.field(FIELD_ACTIONS, actions);
            return this;
        }

        /**
         * Specify the start timestamp in PST. Only activity from this
         * timestamp forward will be returned.
         */
        public UserBuilder timestamp(Date timestamp) {
            return this.timestamp(timestamp.getTime() / MILLISECONDS_IN_SECOND);
        }

        /**
         * Specify the start timestamp in PST. Only activity from this
         * timestamp forward will be returned.
         */
        public UserBuilder timestamp(long timestamp) {
            if (!this.hasField(FIELD_ACTIONS)) {
                this.actions(ActivityAction.All);
            }
            this.field(FIELD_TIMESTAMP, timestamp);
            return this;
        }
    }
}
