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

package com.battlelancer.seriesguide.provider;

import android.net.Uri;
import android.provider.BaseColumns;
import com.battlelancer.seriesguide.SeriesGuideApplication;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.util.ParserUtils;

public class SeriesGuideContract {

    interface ShowsColumns {

        /**
         * This column is NOT in this table, it is for reference purposes only.
         */
        String REF_SHOW_ID = "series_id";

        String TITLE = "seriestitle";

        /**
         * The title without any articles (e.g. 'the' or 'an'). Added with db version 33.
         */
        String TITLE_NOARTICLE = "series_title_noarticle";

        String OVERVIEW = "overview";

        String POSTER = "poster";

        String CONTENTRATING = "contentrating";

        String STATUS = "status";

        String RUNTIME = "runtime";

        /**
         * Global rating. Encoded as double.
         * <pre>
         * Range:   0.0-10.0
         * Default: 0.0
         * </pre>
         */
        String RATING_GLOBAL = "rating";

        /**
         * Global rating votes. Encoded as integer.
         * <pre>
         * Example: 42
         * Default: 0
         * </pre>
         */
        String RATING_VOTES = "series_rating_votes";

        /**
         * User rating. Encoded as integer.
         * <pre>
         * Range:   1-10
         * Default: 0
         * </pre>
         */
        String RATING_USER = "series_rating_user";

        String NETWORK = "network";

        String GENRES = "genres";

        /**
         * Release date of the first episode. Encoded as ISO 8601 datetime string. Might be a legacy
         * value which only includes the date.
         *
         * <pre>
         * Example: "2008-01-20T02:00:00.000Z"
         * Default: ""
         * </pre>
         */
        String FIRST_RELEASE = "firstaired";

        /**
         * Local release time. Encoded as integer (hhmm).
         *
         * <pre>
         * Example: 2035
         * Default: -1
         * </pre>
         */
        String RELEASE_TIME = "airstime";

        /**
         * Local release week day. Encoded as integer.
         * <pre>
         * Range:   1-7
         * Daily:   0
         * Default: -1
         * </pre>
         */
        String RELEASE_WEEKDAY = "airsdayofweek";

        /**
         * Release time zone. Encoded as tzdata "Area/Location" string.
         *
         * <pre>
         * Example: "America/New_York"
         * Default: ""
         * </pre>
         *
         * <p> Added with {@link com.battlelancer.seriesguide.provider.SeriesGuideDatabase#DBVER_34_TRAKT_V2}.
         */
        String RELEASE_TIMEZONE = "series_timezone";

        /**
         * Release country. Encoded as ISO3166-1 alpha-2 string.
         *
         * <pre>
         * Example: "us"
         * Default: ""
         * </pre>
         *
         * <p> Previous use: Was added in db version 21 to store the air time in pure text.
         */
        String RELEASE_COUNTRY = "series_airtime";

        String ACTORS = "actors";

        String IMDBID = "imdbid";

        /**
         * Whether this show has been favorited.
         */
        String FAVORITE = "series_favorite";

        /**
         * Whether this show has been hidden. Added in db version 23.
         */
        String HIDDEN = "series_hidden";

        /**
         * Whether this show was merged with data on Hexagon after signing in the last time.
         *
         * <pre>
         * Range: 0-1
         * Default: 1
         * </pre>
         */
        String HEXAGON_MERGE_COMPLETE = "series_syncenabled";

        /**
         * Next episode TheTVDB id.
         *
         * <pre>
         * Example: "42"
         * Default: ""
         * </pre>
         */
        String NEXTEPISODE = "next";

        /**
         * Next episode text.
         *
         * <pre>
         * Example: "0x12 Episode Name"
         * Default: ""
         * </pre>
         */
        String NEXTTEXT = "nexttext";

        /**
         * DEPRECATED. Use {@link #NEXTAIRDATEMS} instead.
         */
        String NEXTAIRDATE = "nextairdate";

        /**
         * Next episode release time instant.
         *
         * <pre>
         * Range:   long
         * Default: DBUtils.UNKNOWN_RELEASE_DATE (Long.MAX_VALUE)
         * </pre>
         *
         * <p> Added in db version 25 to allow correct sorting by next air date.
         */
        String NEXTAIRDATEMS = "series_nextairdate";

        /**
         * Next episode release time formatted as text.
         *
         * <pre>
         * Example: "Apr 2 (Mon)"
         * Default: ""
         * </pre>
         */
        String NEXTAIRDATETEXT = "series_nextairdatetext";

        /**
         * Added in db version 22 to store the last time a show was updated.
         */
        String LASTUPDATED = "series_lastupdate";

        /**
         * Last time show was edited on theTVDb.com (lastupdated field). Added in db version 27.
         */
        String LASTEDIT = "series_lastedit";

        /**
         * @deprecated Removed after tvtag (formerly GetGlue) shutdown end of 2014.
         *
         * GetGlue object id, added in version 29 to support checking into shows without IMDb id.
         */
        String GETGLUEID = "series_getglueid";

        /**
         * Id of the last watched episode, used to calculate the next episode to watch. Added in db
         * version 31.
         */
        String LASTWATCHEDID = "series_lastwatchedid";
    }

    interface SeasonsColumns {

        /**
         * This column is NOT in this table, it is for reference purposes only.
         */
        String REF_SEASON_ID = "season_id";

        /**
         * The number of a season. Starting from 0 for Special Episodes.
         */
        String COMBINED = "combinednr";

        /**
         * Number of all episodes in a season.
         */
        String TOTALCOUNT = "season_totalcount";

        /**
         * Number of unwatched, aired episodes.
         */
        String WATCHCOUNT = "watchcount";

        /**
         * Number of unwatched, future episodes (not aired yet).
         */
        String UNAIREDCOUNT = "willaircount";

        /**
         * Number of unwatched episodes with no air date.
         */
        String NOAIRDATECOUNT = "noairdatecount";

        /**
         * Text tags for this season. <em>Repurposed.</em>.
         */
        String TAGS = "seasonposter";
    }

    interface EpisodesColumns {

        /**
         * Season number. A reference to the season id is stored separately.
         */
        String SEASON = "season";

        /**
         * Number of an episode within its season.
         */
        String NUMBER = "episodenumber";

        /**
         * Sometimes episodes are ordered differently when released on DVD. Uses decimal point
         * notation, e.g. 1.0, 1.5.
         */
        String DVDNUMBER = "dvdnumber";

        /**
         * Some shows, mainly anime, use absolute episode numbers instead of the season/episode
         * grouping. Added in db version 30.
         */
        String ABSOLUTE_NUMBER = "absolute_number";

        String TITLE = "episodetitle";

        String OVERVIEW = "episodedescription";

        String IMAGE = "episodeimage";

        String WRITERS = "writers";

        String GUESTSTARS = "gueststars";

        String DIRECTORS = "directors";

        /** See {@link ShowsColumns#RATING_GLOBAL}. */
        String RATING_GLOBAL = "rating";

        /** See {@link ShowsColumns#RATING_VOTES}. */
        String RATING_VOTES = "episode_rating_votes";

        /** See {@link ShowsColumns#RATING_USER}. */
        String RATING_USER = "episode_rating_user";

        /**
         * First aired date in text as given by TVDb.com
         */
        String FIRSTAIRED = "epfirstaired";

        /**
         * First aired date in ms.
         */
        String FIRSTAIREDMS = "episode_firstairedms";

        /**
         * Whether an episode has been watched.
         */
        String WATCHED = "watched";

        /**
         * Whether an episode has been collected in digital, physical form.
         */
        String COLLECTED = "episode_collected";

        /**
         * IMDb id for a single episode. Added in db version 27.
         */
        String IMDBID = "episode_imdbid";

        /**
         * Last time episode was edited on theTVDb.com (lastupdated field) in Unix time (seconds).
         * Added in db version 27.
         */
        String LAST_EDITED = "episode_lastedit";
    }

    interface EpisodeSearchColumns {

        String _DOCID = "docid";

        String TITLE = Episodes.TITLE;

        String OVERVIEW = Episodes.OVERVIEW;
    }

    interface ListsColumns {

        /**
         * Unique string identifier.
         */
        String LIST_ID = "list_id";

        String NAME = "list_name";
    }

    interface ListItemsColumns {

        /**
         * Unique string identifier.
         */
        String LIST_ITEM_ID = "list_item_id";

        /**
         * NON-unique string identifier referencing internal ids of other tables.
         */
        String ITEM_REF_ID = "item_ref_id";

        /**
         * Type of item: show, season or episode.
         */
        String TYPE = "item_type";
    }

    public interface ListItemTypes {

        int SHOW = 1;
        int SEASON = 2;
        int EPISODE = 3;
    }

    interface MoviesColumns {

        String TITLE = "movies_title";

        /**
         * The title without any articles (e.g. 'the' or 'an'). Added with db version 33.
         */
        String TITLE_NOARTICLE = "movies_title_noarticle";

        String IMDB_ID = "movies_imdbid";

        String TMDB_ID = "movies_tmdbid";

        String POSTER = "movies_poster";

        String GENRES = "movies_genres";

        String OVERVIEW = "movies_overview";

        String RELEASED_UTC_MS = "movies_released";

        String RUNTIME_MIN = "movies_runtime";

        String TRAILER = "movies_trailer";

        String CERTIFICATION = "movies_certification";

        String IN_COLLECTION = "movies_incollection";

        String IN_WATCHLIST = "movies_inwatchlist";

        String PLAYS = "movies_plays";

        String WATCHED = "movies_watched";

        String RATING_TMDB = "movies_rating_tmdb";

        String RATING_VOTES_TMDB = "movies_rating_votes_tmdb";

        String RATING_TRAKT = "movies_rating_trakt";

        String RATING_VOTES_TRAKT = "movies_rating_votes_trakt";

        /** See {@link ShowsColumns#RATING_USER}. */
        String RATING_USER = "movies_rating_user";

        String LAST_UPDATED = "movies_last_updated";
    }

    private static final Uri BASE_CONTENT_URI = Uri.parse("content://"
            + SeriesGuideApplication.CONTENT_AUTHORITY);

    public static final String PATH_SHOWS = "shows";

    public static final String PATH_SEASONS = "seasons";

    public static final String PATH_EPISODES = "episodes";

    public static final String PATH_OFSHOW = "ofshow";

    public static final String PATH_OFSEASON = "ofseason";

    public static final String PATH_EPISODESEARCH = "episodesearch";

    public static final String PATH_WITHSHOW = "withshow";

    public static final String PATH_RENEWFTSTABLE = "renewftstable";

    public static final String PATH_SEARCH = "search";

    public static final String PATH_FILTER = "filter";

    public static final String PATH_LISTS = "lists";

    public static final String PATH_WITH_LIST_ITEM_ID = "with_list_item";

    public static final String PATH_LIST_ITEMS = "listitems";

    public static final String PATH_WITH_DETAILS = "with_details";

    public static final String PATH_WITH_NEXT_EPISODE = "with-next-episode";

    public static final String PATH_WITH_LAST_EPISODE = "with-last-episode";

    public static final String PATH_MOVIES = "movies";

    public static class Shows implements ShowsColumns, BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_SHOWS)
                .build();

        public static final Uri CONTENT_URI_WITH_NEXT_EPISODE = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_SHOWS)
                .appendPath(PATH_WITH_NEXT_EPISODE)
                .build();

        public static final Uri CONTENT_URI_WITH_LAST_EPISODE = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_SHOWS)
                .appendPath(PATH_WITH_LAST_EPISODE)
                .build();

        public static final Uri CONTENT_FILTER_URI = Uri.withAppendedPath(CONTENT_URI, "filter");

        /**
         * Use if multiple items get returned
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.seriesguide.show";

        /**
         * Use if a single item is returned
         */
        public static final String CONTENT_ITEM_TYPE
                = "vnd.android.cursor.item/vnd.seriesguide.show";

        /**
         * Default "ORDER BY" clause.
         */
        public static final String DEFAULT_SORT = ShowsColumns.TITLE + " ASC";

        public static final String SELECTION_FAVORITES = Shows.FAVORITE + "=1";

        public static final String SELECTION_WITH_NEXT_EPISODE = Shows.NEXTEPISODE + "!=''";

        public static final String SELECTION_NO_HIDDEN = Shows.HIDDEN + "=0";

        public static Uri buildShowUri(String showId) {
            return CONTENT_URI.buildUpon().appendPath(showId).build();
        }

        public static Uri buildShowUri(int showTvdbId) {
            return buildShowUri(String.valueOf(showTvdbId));
        }

        public static String getShowId(Uri uri) {
            return uri.getLastPathSegment();
        }
    }

    public static class Episodes implements EpisodesColumns, BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_EPISODES).build();

        public static final Uri CONTENT_URI_WITHSHOW = CONTENT_URI.buildUpon()
                .appendPath(PATH_WITHSHOW).build();

        /**
         * Use if multiple items get returned
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.seriesguide.episode";

        /**
         * Use if a single item is returned
         */
        public static final String CONTENT_ITEM_TYPE
                = "vnd.android.cursor.item/vnd.seriesguide.episode";

        public static final String SELECTION_UNWATCHED = Episodes.WATCHED + "=0";

        public static final String SELECTION_WATCHED = Episodes.WATCHED + "="
                + EpisodeFlags.WATCHED;

        public static final String SELECTION_COLLECTED = Episodes.COLLECTED + "=1";

        public static final String SELECTION_NOT_COLLECTED = Episodes.COLLECTED + "=0";

        public static final String SELECTION_NO_SPECIALS = Episodes.SEASON + "!=0";

        public static final String SELECTION_HAS_RELEASE_DATE = Episodes.FIRSTAIREDMS + "!=-1";

        public static final String SELECTION_RELEASED_BEFORE_X = Episodes.FIRSTAIREDMS + "<=?";

        public static final String SORT_SEASON_ASC = Episodes.SEASON + " ASC";

        public static final String SORT_NUMBER_ASC = Episodes.NUMBER + " ASC";

        public static Uri buildEpisodeUri(String episodeId) {
            return CONTENT_URI.buildUpon().appendPath(episodeId).build();
        }

        public static Uri buildEpisodeUri(int episodeId) {
            return buildEpisodeUri(String.valueOf(episodeId));
        }

        public static String getEpisodeId(Uri uri) {
            return uri.getLastPathSegment();
        }

        public static Uri buildEpisodesOfSeasonUri(String seasonId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_OFSEASON).appendPath(seasonId).build();
        }

        public static Uri buildEpisodesOfSeasonWithShowUri(String seasonId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_OFSEASON).appendPath(PATH_WITHSHOW)
                    .appendPath(seasonId).build();
        }

        public static Uri buildEpisodesOfShowUri(String showTvdbId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_OFSHOW).appendPath(showTvdbId).build();
        }

        public static Uri buildEpisodesOfShowUri(int showTvdbId) {
            return buildEpisodesOfShowUri(String.valueOf(showTvdbId));
        }

        public static Uri buildEpisodeWithShowUri(String episodeId) {
            return CONTENT_URI_WITHSHOW.buildUpon().appendPath(episodeId).build();
        }

        public static Uri buildEpisodeWithShowUri(int episodeTvdbId) {
            return buildEpisodeWithShowUri(String.valueOf(episodeTvdbId));
        }
    }

    public static class Seasons implements SeasonsColumns, BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_SEASONS)
                .build();

        /**
         * Use if multiple items get returned
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.seriesguide.season";

        /**
         * Use if a single item is returned
         */
        public static final String CONTENT_ITEM_TYPE
                = "vnd.android.cursor.item/vnd.seriesguide.season";

        public static Uri buildSeasonUri(String seasonTvdbId) {
            return CONTENT_URI.buildUpon().appendPath(seasonTvdbId).build();
        }

        public static Uri buildSeasonUri(int seasonTvdbId) {
            return buildSeasonUri(String.valueOf(seasonTvdbId));
        }

        public static String getSeasonId(Uri uri) {
            return uri.getLastPathSegment();
        }

        public static Uri buildSeasonsOfShowUri(String showTvdbId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_OFSHOW).appendPath(showTvdbId).build();
        }

        public static Uri buildSeasonsOfShowUri(int showTvdbId) {
            return buildSeasonsOfShowUri(String.valueOf(showTvdbId));
        }
    }

    public static class EpisodeSearch implements EpisodeSearchColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_EPISODESEARCH).build();

        public static final Uri CONTENT_URI_SEARCH = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_EPISODESEARCH).appendPath(PATH_SEARCH).build();

        public static final Uri CONTENT_URI_RENEWFTSTABLE = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_RENEWFTSTABLE).build();

        public static Uri buildDocIdUri(String rowId) {
            return CONTENT_URI.buildUpon().appendPath(rowId).build();
        }

        public static String getDocId(Uri uri) {
            return uri.getLastPathSegment();
        }
    }

    public static class Lists implements ListsColumns, BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_LISTS)
                .build();

        public static final Uri CONTENT_WITH_LIST_ITEM_URI = CONTENT_URI.buildUpon()
                .appendPath(PATH_WITH_LIST_ITEM_ID)
                .build();

        /**
         * Use if multiple items get returned
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.seriesguide.list";

        /**
         * Use if a single item is returned
         */
        public static final String CONTENT_ITEM_TYPE
                = "vnd.android.cursor.item/vnd.seriesguide.list";

        public static Uri buildListUri(String id) {
            return CONTENT_URI.buildUpon().appendPath(id).build();
        }

        public static Uri buildListsWithListItemUri(String itemId) {
            return CONTENT_WITH_LIST_ITEM_URI.buildUpon().appendPath(itemId).build();
        }

        public static String getId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String generateListId(String name) {
            return ParserUtils.sanitizeId(name);
        }
    }

    public static class ListItems implements ListItemsColumns, BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_LIST_ITEMS)
                .build();

        public static final Uri CONTENT_WITH_DETAILS_URI = CONTENT_URI.buildUpon()
                .appendPath(PATH_WITH_DETAILS)
                .build();

        /**
         * Use if multiple items get returned
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.seriesguide.listitem";

        /**
         * Use if a single item is returned
         */
        public static final String CONTENT_ITEM_TYPE
                = "vnd.android.cursor.item/vnd.seriesguide.listitem";

        public static Uri buildListItemUri(String id) {
            return CONTENT_URI.buildUpon().appendPath(id).build();
        }

        public static String getId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String generateListItemId(int itemTvdbId, int type, String listId) {
            return ParserUtils.sanitizeId(itemTvdbId + "-" + type + "-" + listId);
        }

        public static String generateListItemIdWildcard(int itemTvdbId, int type) {
            // The SQL % wildcard is added by the content provider
            return ParserUtils.sanitizeId(itemTvdbId + "-" + type + "-");
        }
    }

    public static class Movies implements MoviesColumns, BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_MOVIES)
                .build();

        public static final String SELECTION_COLLECTION = Movies.IN_COLLECTION + "=1";

        public static final String SELECTION_WATCHLIST = Movies.IN_WATCHLIST + "=1";

        public static final String SELECTION_WATCHED = Movies.WATCHED + "=1";

        /** Default sort order. */
        public static final String SORT_TITLE_ALPHABETICAL = Movies.TITLE + " COLLATE NOCASE ASC";

        public static final String SORT_TITLE_ALPHABETICAL_NO_ARTICLE = Movies.TITLE_NOARTICLE
                + " COLLATE NOCASE ASC";

        public static final String SORT_TITLE_REVERSE_ALPHACETICAL = Movies.TITLE
                + " COLLATE NOCASE DESC";

        public static final String SORT_TITLE_REVERSE_ALPHACETICAL_NO_ARTICLE =
                Movies.TITLE_NOARTICLE + " COLLATE NOCASE DESC";

        public static final String SORT_RELEASE_DATE_NEWEST_FIRST = Movies.RELEASED_UTC_MS
                + " DESC," + SORT_TITLE_ALPHABETICAL;

        public static final String SORT_RELEASE_DATE_OLDEST_FIRST = Movies.RELEASED_UTC_MS + " ASC,"
                + SORT_TITLE_ALPHABETICAL;

        /**
         * Use if multiple items get returned
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.seriesguide.movie";

        /**
         * Use if a single item is returned
         */
        public static final String CONTENT_ITEM_TYPE
                = "vnd.android.cursor.item/vnd.seriesguide.movie";

        public static Uri buildMovieUri(Integer tmdbId) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(tmdbId)).build();
        }

        public static String getId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    private SeriesGuideContract() {
    }
}
