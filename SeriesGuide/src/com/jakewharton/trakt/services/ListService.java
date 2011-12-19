package com.jakewharton.trakt.services;

import com.google.myjson.JsonArray;
import com.google.myjson.JsonObject;
import com.google.myjson.reflect.TypeToken;
import com.jakewharton.trakt.TraktApiBuilder;
import com.jakewharton.trakt.TraktApiService;
import com.jakewharton.trakt.entities.ListItemsResponse;
import com.jakewharton.trakt.entities.ListResponse;
import com.jakewharton.trakt.entities.Response;
import com.jakewharton.trakt.enumerations.ListItemType;
import com.jakewharton.trakt.enumerations.ListPrivacy;

public class ListService extends TraktApiService {
    /**
     * Add a new custom list.
     *
     * @param name The list name. This must be unique.
     * @param privacy Privacy level.
     * @return Builder instance.
     */
    public AddBuilder add(String name, ListPrivacy privacy) {
        return new AddBuilder(this).name(name).privacy(privacy);
    }

    /**
     * Delete a custom list including all items it contains.
     *
     * @param slug Slug to identify what list is being deleted.
     * @return Builder instance.
     */
    public DeleteBuilder delete(String slug) {
        return new DeleteBuilder(this).slug(slug);
    }

    /**
     * Add one or more items to an existing list. Items can be movies, shows,
     * season, or episodes.
     *
     * @param slug Slug to identify what list is being added to.
     * @return Builder instance.
     */
    public ItemsAddBuilder itemsAdd(String slug) {
        return new ItemsAddBuilder(this).slug(slug);
    }

    /**
     * Delete one or more items from an existing list. Items can be movies,
     * shows, season, or episodes.
     *
     * @param slug Slug to identify what list is being deleted.
     * @return Builder instance.
     */
    public ItemsDeleteBuilder itemsDelete(String slug) {
        return new ItemsDeleteBuilder(this).slug(slug);
    }

    /**
     * Update a custom list.
     *
     * @param slug Slug to identify what list is being updated.
     * @return Builder instance.
     */
    public UpdateBuilder update(String slug) {
        return new UpdateBuilder(this).slug(slug);
    }


    public static final class AddBuilder extends TraktApiBuilder<ListResponse> {
        private static final String POST_NAME = "name";
        private static final String POST_DESCRIPTION = "description";
        private static final String POST_PRIVACY = "privacy";

        private static final String URI = "/lists/add/" + FIELD_API_KEY;

        private AddBuilder(ListService service) {
            super(service, new TypeToken<ListResponse>() {}, URI, HttpMethod.Post);
        }

        /**
         * The list name. This must be unique.
         *
         * @param name Value.
         * @return Builder instance.
         */
        public AddBuilder name(String name) {
            super.postParameter(POST_NAME, name);
            return this;
        }

        /**
         * Optional but recommended description of what the list contains.
         *
         * @param description Value.
         * @return Builder instance.
         */
        public AddBuilder description(String description) {
            super.postParameter(POST_DESCRIPTION, description);
            return this;
        }

        /**
         * Privacy level.
         *
         * @param privacy Value.
         * @return Builder instance.
         */
        public AddBuilder privacy(ListPrivacy privacy) {
            super.postParameter(POST_PRIVACY, privacy);
            return this;
        }
    }
    public static final class DeleteBuilder extends TraktApiBuilder<Response> {
        private static final String POST_SLUG = "slug";

        private static final String URI = "/lists/delete/" + FIELD_API_KEY;

        private DeleteBuilder(ListService service) {
            super(service, new TypeToken<Response>() {}, URI, HttpMethod.Post);
        }

        /**
         * Slug to identify what list is being deleted.
         *
         * @param slug Value.
         * @return Builder instance.
         */
        public DeleteBuilder slug(String slug) {
            super.postParameter(POST_SLUG, slug);
            return this;
        }
    }
    public static final class ItemsAddBuilder extends TraktApiBuilder<ListItemsResponse> {
        private static final String POST_SLUG = "slug";
        private static final String POST_ITEMS = "items";

        private static final String URI = "/lists/items/add/" + FIELD_API_KEY;

        private final JsonArray items = new JsonArray();

        private ItemsAddBuilder(ListService service) {
            super(service, new TypeToken<ListItemsResponse>() {}, URI, HttpMethod.Post);
        }

        /**
         * Slug to identify what list is being added to.
         *
         * @param slug Value.
         * @return Builder instance.
         */
        public ItemsAddBuilder slug(String slug) {
            super.postParameter(POST_SLUG, slug);
            return this;
        }

        public ItemsAddBuilder addMovie(String imdbId) {
            return this.addMovie(imdbId, null, 0);
        }
        public ItemsAddBuilder addMovie(String title, int year) {
            return this.addMovie(null, title, year);
        }
        public ItemsAddBuilder addMovie(String imdbId, String title, int year) {
            JsonObject item = new JsonObject();
            item.addProperty("type", ListItemType.Movie.toString());
            if (imdbId != null) {
                item.addProperty("imdb_id", imdbId);
            }
            if (title != null) {
                item.addProperty("title", title);
            }
            if (year > 0) {
                item.addProperty("year", year);
            }
            this.items.add(item);
            return this;
        }
        public ItemsAddBuilder addShow(String tvdbId) {
            return this.addShow(tvdbId, null);
        }
        public ItemsAddBuilder addShow(String tvdbId, String title) {
            JsonObject item = new JsonObject();
            item.addProperty("type", ListItemType.TvShow.toString());
            item.addProperty("tvdb_id", tvdbId);
            if (title != null) {
                item.addProperty("title", title);
            }
            this.items.add(item);
            return this;
        }
        public ItemsAddBuilder addShowSeason(String tvdbId, int season) {
            return this.addShowSeason(tvdbId, null, season);
        }
        public ItemsAddBuilder addShowSeason(String tvdbId, String title, int season) {
            JsonObject item = new JsonObject();
            item.addProperty("type", ListItemType.TvShowSeason.toString());
            item.addProperty("tvdb_id", tvdbId);
            item.addProperty("season", season);
            if (title != null) {
                item.addProperty("title", title);
            }
            this.items.add(item);
            return this;
        }
        public ItemsAddBuilder addShowEpisode(String tvdbId, int season, int episode) {
            return this.addShowEpisode(tvdbId, null, season, episode);
        }
        public ItemsAddBuilder addShowEpisode(String tvdbId, String title, int season, int episode) {
            JsonObject item = new JsonObject();
            item.addProperty("type", ListItemType.TvShowEpisode.toString());
            item.addProperty("tvdb_id", tvdbId);
            item.addProperty("season", season);
            item.addProperty("episode", episode);
            if (title != null) {
                item.addProperty("title", title);
            }
            this.items.add(item);
            return this;
        }

        @Override
        protected void preFireCallback() {
            super.postParameter(POST_ITEMS, this.items);
        }
    }
    public static final class ItemsDeleteBuilder extends TraktApiBuilder<Response> {
        private static final String POST_SLUG = "slug";
        private static final String POST_ITEMS = "items";

        private static final String URI = "/lists/items/delete/" + FIELD_API_KEY;

        private final JsonArray items = new JsonArray();

        private ItemsDeleteBuilder(ListService service) {
            super(service, new TypeToken<Response>() {}, URI, HttpMethod.Post);
        }

        /**
         * Slug to identify what list is being added to.
         *
         * @param slug Value.
         * @return Builder instance.
         */
        public ItemsDeleteBuilder slug(String slug) {
            super.postParameter(POST_SLUG, slug);
            return this;
        }

        public ItemsDeleteBuilder addMovie(String imdbId) {
            return this.addMovie(imdbId, null, 0);
        }
        public ItemsDeleteBuilder addMovie(String title, int year) {
            return this.addMovie(null, title, year);
        }
        public ItemsDeleteBuilder addMovie(String imdbId, String title, int year) {
            JsonObject item = new JsonObject();
            item.addProperty("type", ListItemType.Movie.toString());
            if (imdbId != null) {
                item.addProperty("imdb_id", imdbId);
            }
            if (title != null) {
                item.addProperty("title", title);
            }
            if (year > 0) {
                item.addProperty("year", year);
            }
            this.items.add(item);
            return this;
        }
        public ItemsDeleteBuilder addShow(String tvdbId) {
            return this.addShow(tvdbId, null);
        }
        public ItemsDeleteBuilder addShow(String tvdbId, String title) {
            JsonObject item = new JsonObject();
            item.addProperty("type", ListItemType.TvShow.toString());
            item.addProperty("tvdb_id", tvdbId);
            if (title != null) {
                item.addProperty("title", title);
            }
            this.items.add(item);
            return this;
        }
        public ItemsDeleteBuilder addShowSeason(String tvdbId, int season) {
            return this.addShowSeason(tvdbId, null, season);
        }
        public ItemsDeleteBuilder addShowSeason(String tvdbId, String title, int season) {
            JsonObject item = new JsonObject();
            item.addProperty("type", ListItemType.TvShowSeason.toString());
            item.addProperty("tvdb_id", tvdbId);
            item.addProperty("season", season);
            if (title != null) {
                item.addProperty("title", title);
            }
            this.items.add(item);
            return this;
        }
        public ItemsDeleteBuilder addShowEpisode(String tvdbId, int season, int episode) {
            return this.addShowEpisode(tvdbId, null, season, episode);
        }
        public ItemsDeleteBuilder addShowEpisode(String tvdbId, String title, int season, int episode) {
            JsonObject item = new JsonObject();
            item.addProperty("type", ListItemType.TvShowEpisode.toString());
            item.addProperty("tvdb_id", tvdbId);
            item.addProperty("season", season);
            item.addProperty("episode", episode);
            if (title != null) {
                item.addProperty("title", title);
            }
            this.items.add(item);
            return this;
        }

        @Override
        protected void preFireCallback() {
            super.postParameter(POST_ITEMS, this.items);
        }
    }
    public static final class UpdateBuilder extends TraktApiBuilder<ListResponse> {
        private static final String POST_SLUG = "slug";
        private static final String POST_NAME = "name";
        private static final String POST_DESCRIPTION = "description";
        private static final String POST_PRIVACY = "privacy";

        private static final String URI = "/lists/update/" + FIELD_API_KEY;

        private UpdateBuilder(ListService service) {
            super(service, new TypeToken<ListResponse>() {}, URI, HttpMethod.Post);
        }

        /**
         * Slug to identify what list is being updated.
         *
         * @param slug Value.
         * @return Builder instance.
         */
        public UpdateBuilder slug(String slug) {
            super.postParameter(POST_SLUG, slug);
            return this;
        }

        /**
         * The list name. This must be unique.
         *
         * @param name Value.
         * @return Builder instance.
         */
        public UpdateBuilder name(String name) {
            super.postParameter(POST_NAME, name);
            return this;
        }

        /**
         * Optional but recommended description of what the list contains.
         *
         * @param description Value.
         * @return Builder instance.
         */
        public UpdateBuilder description(String description) {
            super.postParameter(POST_DESCRIPTION, description);
            return this;
        }

        /**
         * Privacy level.
         *
         * @param privacy Value.
         * @return Builder instance.
         */
        public UpdateBuilder privacy(ListPrivacy privacy) {
            super.postParameter(POST_PRIVACY, privacy);
            return this;
        }
    }
}
