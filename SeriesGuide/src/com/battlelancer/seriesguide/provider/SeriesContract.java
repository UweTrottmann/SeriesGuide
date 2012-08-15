/*
 * Copyright 2011 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class SeriesContract {

    interface ShowsColumns {
        /** This column is NOT in this table, it is for reference purposes only. */
        String REF_SHOW_ID = "series_id";

        String NEXTEPISODE = "next";

        /** Deprecated, use {@link NEXTAIRDATEMS} instead. **/
        String NEXTAIRDATE = "nextairdate";

        String NEXTTEXT = "nexttext";

        String POSTER = "poster";

        String CONTENTRATING = "contentrating";

        String STATUS = "status";

        String RUNTIME = "runtime";

        String RATING = "rating";

        String NETWORK = "network";

        String GENRES = "genres";

        String FIRSTAIRED = "firstaired";

        String AIRSTIME = "airstime";

        String AIRSDAYOFWEEK = "airsdayofweek";

        String ACTORS = "actors";

        String OVERVIEW = "overview";

        String TITLE = "seriestitle";

        String IMDBID = "imdbid";

        String FAVORITE = "series_favorite";

        /**
         * Added in db version 23 to allow hiding of shows.
         */
        String HIDDEN = "series_hidden";

        String NEXTAIRDATETEXT = "series_nextairdatetext";

        String SYNCENABLED = "series_syncenabled";

        /**
         * Added in db version 21 to store the airtime in pure text.
         */
        String AIRTIME = "series_airtime";

        /**
         * Added in db version 22 to store the last time a show was updated.
         */
        String LASTUPDATED = "series_lastupdate";

        /**
         * Added in db version 25 to allow correct sorting by next air date.
         */
        String NEXTAIRDATEMS = "series_nextairdate";

    }

    interface SeasonsColumns {
        /** This column is NOT in this table, it is for reference purposes only. */
        String REF_SEASON_ID = "season_id";

        String POSTER = "seasonposter";

        String UNAIREDCOUNT = "willaircount";

        String WATCHCOUNT = "watchcount";

        String NOAIRDATECOUNT = "noairdatecount";

        String COMBINED = "combinednr";

        String TOTALCOUNT = "season_totalcount";
    }

    interface EpisodesColumns {
        String SEASON = "season";

        String NUMBER = "episodenumber";

        String DVDNUMBER = "dvdnumber";

        String IMAGE = "episodeimage";

        String WRITERS = "writers";

        String GUESTSTARS = "gueststars";

        String DIRECTORS = "directors";

        String RATING = "rating";

        String FIRSTAIRED = "epfirstaired";

        String WATCHED = "watched";

        String OVERVIEW = "episodedescription";

        String TITLE = "episodetitle";

        String FIRSTAIREDMS = "episode_firstairedms";

        /**
         * Whether an episode has been collected in digital, physical form.
         */
        String COLLECTED = "episode_collected";
    }

    interface EpisodeSearchColumns {
        String _DOCID = "docid";

        String TITLE = Episodes.TITLE;

        String OVERVIEW = Episodes.OVERVIEW;
    }

    public static final String CONTENT_AUTHORITY = "com.battlelancer.seriesguide";

    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

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

    public static class Shows implements ShowsColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_SHOWS)
                .build();

        public static final Uri CONTENT_FILTER_URI = Uri.withAppendedPath(CONTENT_URI, "filter");

        /** Use if multiple items get returned */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.seriesguide.show";

        /** Use if a single item is returned */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.seriesguide.show";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = ShowsColumns.TITLE + " ASC";

        public static Uri buildShowUri(String showId) {
            return CONTENT_URI.buildUpon().appendPath(showId).build();
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

        /** Use if multiple items get returned */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.seriesguide.episode";

        /** Use if a single item is returned */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.seriesguide.episode";

        public static Uri buildEpisodeUri(String episodeId) {
            return CONTENT_URI.buildUpon().appendPath(episodeId).build();
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

        public static Uri buildEpisodesOfShowUri(String showId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_OFSHOW).appendPath(showId).build();
        }

        public static Uri buildEpisodeWithShowUri(String episodeId) {
            return CONTENT_URI_WITHSHOW.buildUpon().appendPath(episodeId).build();
        }
    }

    public static class Seasons implements SeasonsColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_SEASONS)
                .build();

        /** Use if multiple items get returned */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.seriesguide.season";

        /** Use if a single item is returned */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.seriesguide.season";

        public static Uri buildSeasonUri(String seasonId) {
            return CONTENT_URI.buildUpon().appendPath(seasonId).build();
        }

        public static String getSeasonId(Uri uri) {
            return uri.getLastPathSegment();
        }

        public static Uri buildSeasonsOfShowUri(String showId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_OFSHOW).appendPath(showId).build();
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

    private SeriesContract() {
    }
}
