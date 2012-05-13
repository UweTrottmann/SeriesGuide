
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
        LATEST_FIRST(0, "latestfirst", Episodes.NUMBER + " desc"),

        OLDEST_FIRST(1, "oldestfirst", Episodes.NUMBER + " asc"),

        UNWATCHED_FIRST(2, "unwatchedfirst", Episodes.WATCHED + " asc," + Episodes.NUMBER + " asc"),

        ALPHABETICAL_ASC(3, "atoz", Episodes.TITLE + " asc"),

        ALPHABETICAL_DESC(4, "ztoa", Episodes.TITLE + " desc"),

        DVDLATEST_FIRST(5, "dvdlatestfirst", Episodes.DVDNUMBER + " desc," + Episodes.NUMBER
                + " desc"),

        DVDOLDEST_FIRST(6, "dvdoldestfirst", Episodes.DVDNUMBER + " asc," + Episodes.NUMBER
                + " asc");

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
        LATEST_FIRST(0, "latestfirst", Seasons.COMBINED + " desc"),

        OLDEST_FIRST(1, "oldestfirst", Seasons.COMBINED + " asc");

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
        ALPHABETIC(0, "alphabetic", Shows.TITLE + " asc"),

        UPCOMING(1, "upcoming", Shows.NEXTAIRDATEMS + " asc," + Shows.AIRSTIME + " asc,"
                + Shows.TITLE + " asc"),

        FAVORITES_FIRST(2, "favorites", Shows.FAVORITE + " desc," + Shows.TITLE + " asc"),

        FAVORITES_UPCOMING(3, "favoritesupcoming", Shows.FAVORITE + " desc," + Shows.NEXTAIRDATEMS
                + " asc," + Shows.AIRSTIME + " asc," + Shows.TITLE + " asc");

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
