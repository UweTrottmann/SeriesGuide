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

package com.battlelancer.seriesguide;

import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class Constants {

    public static final SimpleDateFormat theTVDBDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public static enum EpisodeSorting {
        LATEST_FIRST(0, "latestfirst", Episodes.NUMBER + " DESC"),

        OLDEST_FIRST(1, "oldestfirst", Episodes.NUMBER + " ASC"),

        UNWATCHED_FIRST(2, "unwatchedfirst", Episodes.WATCHED + " ASC," + Episodes.NUMBER + " ASC"),

        ALPHABETICAL_ASC(3, "atoz", Episodes.TITLE + " COLLATE NOCASE ASC"),

        ALPHABETICAL_DESC(4, "ztoa", Episodes.TITLE + " COLLATE NOCASE DESC"),

        DVDLATEST_FIRST(5, "dvdlatestfirst", Episodes.DVDNUMBER + " DESC," + Episodes.NUMBER
                + " DESC"),

        DVDOLDEST_FIRST(6, "dvdoldestfirst", Episodes.DVDNUMBER + " ASC," + Episodes.NUMBER
                + " ASC");

        private final int index;

        private final String value;

        private final String query;

        EpisodeSorting(int index, String value, String query) {
            this.index = index;
            this.value = value;
            this.query = query;
        }

        public int index() {
            return index;
        }

        public String value() {
            return value;
        }

        public String query() {
            return query;
        }

        @Override
        public String toString() {
            return this.value;
        }

        private static final Map<String, EpisodeSorting> STRING_MAPPING = new HashMap<String, EpisodeSorting>();

        static {
            for (EpisodeSorting via : EpisodeSorting.values()) {
                STRING_MAPPING.put(via.toString().toUpperCase(), via);
            }
        }

        public static EpisodeSorting fromValue(String value) {
            return STRING_MAPPING.get(value.toUpperCase());
        }
    }

    public static enum SeasonSorting {
        LATEST_FIRST(0, "latestfirst", Seasons.COMBINED + " DESC"),

        OLDEST_FIRST(1, "oldestfirst", Seasons.COMBINED + " ASC");

        private final int index;

        private final String value;

        private final String query;

        SeasonSorting(int index, String value, String query) {
            this.index = index;
            this.value = value;
            this.query = query;
        }

        public int index() {
            return index;
        }

        public String value() {
            return value;
        }

        public String query() {
            return query;
        }

        @Override
        public String toString() {
            return this.value;
        }

        private static final Map<String, SeasonSorting> STRING_MAPPING = new HashMap<String, SeasonSorting>();

        static {
            for (SeasonSorting via : SeasonSorting.values()) {
                STRING_MAPPING.put(via.toString().toUpperCase(), via);
            }
        }

        public static SeasonSorting fromValue(String value) {
            return STRING_MAPPING.get(value.toUpperCase());
        }
    }

    public static enum ShowSorting {
        ALPHABETIC(0, "alphabetic", Shows.TITLE + " COLLATE NOCASE ASC"),

        UPCOMING(1, "upcoming", Shows.NEXTAIRDATEMS + " ASC," + Shows.AIRSTIME + " ASC,"
                + Shows.TITLE + " COLLATE NOCASE ASC"),

        FAVORITES_FIRST(2, "favorites", Shows.FAVORITE + " DESC," + Shows.TITLE
                + " COLLATE NOCASE ASC"),

        FAVORITES_UPCOMING(3, "favoritesupcoming", Shows.FAVORITE + " DESC," + Shows.NEXTAIRDATEMS
                + " ASC," + Shows.AIRSTIME + " ASC," + Shows.TITLE + " COLLATE NOCASE ASC");

        private final int index;

        private final String value;

        private final String query;

        ShowSorting(int index, String value, String query) {
            this.index = index;
            this.value = value;
            this.query = query;
        }

        public int index() {
            return index;
        }

        public String value() {
            return value;
        }

        public String query() {
            return query;
        }

        @Override
        public String toString() {
            return this.value;
        }

        private static final Map<String, ShowSorting> STRING_MAPPING = new HashMap<String, ShowSorting>();

        static {
            for (ShowSorting via : ShowSorting.values()) {
                STRING_MAPPING.put(via.toString().toUpperCase(), via);
            }
        }

        public static ShowSorting fromValue(String value) {
            return STRING_MAPPING.get(value.toUpperCase());
        }
    }

    public static final String TVDB_SHOW_URL = "http://thetvdb.com/?tab=series&id=";

    public static final String TVDB_EPISODE_URL_1 = "http://thetvdb.com/?tab=episode&seriesid=";

    public static final String TVDB_EPISODE_URL_2 = "&seasonid=";

    public static final String TVDB_EPISODE_URL_3 = "&id=";
}
