package com.jakewharton.trakt.services;

import com.google.myjson.reflect.TypeToken;
import com.jakewharton.trakt.TraktApiBuilder;
import com.jakewharton.trakt.TraktApiService;
import com.jakewharton.trakt.entities.Movie;
import com.jakewharton.trakt.entities.Person;
import com.jakewharton.trakt.entities.TvEntity;
import com.jakewharton.trakt.entities.TvShow;
import com.jakewharton.trakt.entities.UserProfile;

import java.util.List;

public class SearchService extends TraktApiService {
    /**
     * Search for TV show episodes.
     *
     * @param query The search query that should be used.
     * @return Builder instance.
     */
    public EpisodesBuilder episodes(String query) {
        return new EpisodesBuilder(this, query);
    }

    /**
     * Search for movies.
     *
     * @param query The search query that should be used.
     * @return Builder instance.
     */
    public MoviesBuilder movies(String query) {
        return new MoviesBuilder(this, query);
    }

    /**
     * Search for people including actors, directors, producers, and writers.
     *
     * @param query The search query that should be used.
     * @return Builder instance.
     */
    public PeopleBuilder people(String query) {
        return new PeopleBuilder(this, query);
    }

    /**
     * Search for TV shows.
     *
     * @param query The search query that should be used.
     * @return Builder instance.
     */
    public ShowsBuilder shows(String query) {
        return new ShowsBuilder(this, query);
    }

    /**
     * Search for Trakt users.
     *
     * @param query The search query that should be used.
     * @return Builder instance.
     */
    public UsersBuilder users(String query) {
        return new UsersBuilder(this, query);
    }


    public static final class EpisodesBuilder extends TraktApiBuilder<List<TvEntity>> {
        private static final String URI = "/search/episodes.json/" + FIELD_API_KEY + "/" + FIELD_QUERY;

        private EpisodesBuilder(SearchService service, String query) {
            super(service, new TypeToken<List<TvEntity>>() {}, URI);

            this.field(FIELD_QUERY, query);
        }
    }
    public static final class MoviesBuilder extends TraktApiBuilder<List<Movie>> {
        private static final String URI = "/search/movies.json/" + FIELD_API_KEY + "/" + FIELD_QUERY;

        private MoviesBuilder(SearchService service, String query) {
            super(service, new TypeToken<List<Movie>>() {}, URI);

            this.field(FIELD_QUERY, query);
        }
    }
    public static final class PeopleBuilder extends TraktApiBuilder<List<Person>> {
        private static final String URI = "/search/people.json/" + FIELD_API_KEY + "/" + FIELD_QUERY;

        private PeopleBuilder(SearchService service, String query) {
            super(service, new TypeToken<List<Person>>() {}, URI);

            this.field(FIELD_QUERY, query);
        }
    }
    public static final class ShowsBuilder extends TraktApiBuilder<List<TvShow>> {
        private static final String URI = "/search/shows.json/" + FIELD_API_KEY + "/" + FIELD_QUERY;

        private ShowsBuilder(SearchService service, String query) {
            super(service, new TypeToken<List<TvShow>>() {}, URI);

            this.field(FIELD_QUERY, query);
        }
    }
    public static final class UsersBuilder extends TraktApiBuilder<List<UserProfile>> {
        private static final String URI = "/search/users.json/" + FIELD_API_KEY + "/" + FIELD_QUERY;

        private UsersBuilder(SearchService service, String query) {
            super(service, new TypeToken<List<UserProfile>>() {}, URI);

            this.field(FIELD_QUERY, query);
        }
    }
}
