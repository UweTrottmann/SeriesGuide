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

import com.battlelancer.seriesguide.SeriesGuideApplication;
import com.battlelancer.seriesguide.util.ParserUtils;

import android.net.Uri;
import android.provider.BaseColumns;

public class SeriesGuideContract {

    interface ShowsColumns {

        /**
         * This column is NOT in this table, it is for reference purposes only.
         */
        String REF_SHOW_ID = "series_id";

        String TITLE = "seriestitle";

        String OVERVIEW = "overview";

        String POSTER = "poster";

        String CONTENTRATING = "contentrating";

        String STATUS = "status";

        String RUNTIME = "runtime";

        /**
         * Rating value of TVDb as double ranging from 0.0 to 10.0.
         */
        String RATING = "rating";

        String NETWORK = "network";

        String GENRES = "genres";

        /**
         * Air date of first episode, e.g. 2009-01-25.
         */
        String FIRSTAIRED = "firstaired";

        /**
         * Release time (e.g. 08:00pm), but encoded in ms. See {@link com.battlelancer.seriesguide.util.TimeTools#parseShowReleaseTime(String)}.
         */
        String AIRSTIME = "airstime";

        /** CURRENT: Stores the country episodes release originally in.<br/>
         * ORIGINALLY: Was added in db version 21 to store the air time in pure text. */
        String RELEASE_COUNTRY = "series_airtime";

        String AIRSDAYOFWEEK = "airsdayofweek";

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
         * Whether this show is included in manual trakt upload.
         */
        String SYNCENABLED = "series_syncenabled";

        /**
         * Next episode ID.
         */
        String NEXTEPISODE = "next";

        /**
         * Next episode text, e.g. '0x12 Episode Name'.
         */
        String NEXTTEXT = "nexttext";

        /**
         * DEPRECATED. Use {@link #NEXTAIRDATEMS} instead.
         */
        String NEXTAIRDATE = "nextairdate";

        /**
         * Added in db version 25 to allow correct sorting by next air date.
         */
        String NEXTAIRDATEMS = "series_nextairdate";

        /**
         * Next air date text, e.g. 'Apr 2 (Mon)'.
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

        /**
         * Rating value of TVDb as double ranging from 0.0 to 10.0.
         */
        String RATING = "rating";

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
         * Last time episode was edited on theTVDb.com (lastupdated field). Added in db version 27.
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

    public static final String PATH_WITH_EPISODE = "with_episode";

    public static final String PATH_MOVIES = "movies";

    public static class Shows implements ShowsColumns, BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_SHOWS)
                .build();

        public static final Uri CONTENT_URI_WITH_EPISODE = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_SHOWS)
                .appendPath(PATH_WITH_EPISODE)
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

        public static final String SELECTION_FAVORITES = " AND " + Shows.FAVORITE + "=1";

        public static final String SELECTION_WITH_NEXT_EPISODE = " AND " + Shows.NEXTEPISODE
                + "!=''";

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

        public static final String SELECTION_NOWATCHED = " AND " + Episodes.WATCHED + "=0";

        public static final String SELECTION_NOSPECIALS = " AND " + Episodes.SEASON + "!=0";

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

        public static Uri buildSeasonUri(String seasonId) {
            return CONTENT_URI.buildUpon().appendPath(seasonId).build();
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

        public static String generateListItemId(String id, int type, String listId) {
            return ParserUtils.sanitizeId(id + "-" + type + "-" + listId);
        }

        public static String generateListItemIdWildcard(String id, int type) {
            // The SQL % wildcard is added by the content provider
            return ParserUtils.sanitizeId(id + "-" + type + "-");
        }
    }

    public static class Movies implements MoviesColumns, BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_MOVIES)
                .build();

        public static final String SELECTION_COLLECTION = Movies.IN_COLLECTION + "=1";

        public static final String SELECTION_WATCHLIST = Movies.IN_WATCHLIST + "=1";

        public static final String DEFAULT_SORT = Movies.TITLE + " ASC";

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
