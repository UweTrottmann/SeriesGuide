
package com.battlelancer.seriesguide;

import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;

import java.text.SimpleDateFormat;

public class Constants {

    public static final SimpleDateFormat theTVDBDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public static enum EpisodeSorting {
        LATEST_FIRST(0, Episodes.NUMBER + " desc"), OLDEST_FIRST(1, Episodes.NUMBER + " asc"), UNWATCHED_FIRST(
                2, Episodes.WATCHED + " asc," + Episodes.NUMBER + " asc"), ALPHABETICAL_ASC(3,
                Episodes.TITLE + " asc"), ALPHABETICAL_DESC(4, Episodes.TITLE + " desc"), DVDLATEST_FIRST(
                5, Episodes.DVDNUMBER + " desc," + Episodes.NUMBER + " desc"), DVDOLDEST_FIRST(6,
                Episodes.DVDNUMBER + " asc," + Episodes.NUMBER + " asc");

        private final int index;

        private final String query;

        EpisodeSorting(int index, String query) {
            this.index = index;
            this.query = query;
        }

        public int index() {
            return index;
        }

        public String query() {
            return query;
        }
    }

    public static enum SeasonSorting {
        LATEST_FIRST(0, Seasons.COMBINED + " desc"), OLDEST_FIRST(1, Seasons.COMBINED + " asc");

        private final int index;

        private final String query;

        SeasonSorting(int index, String query) {
            this.index = index;
            this.query = query;
        }

        public int index() {
            return index;
        }

        public String query() {
            return query;
        }
    }

    public static enum ShowSorting {
        ALPHABETIC(0, Shows.TITLE + " asc"), UPCOMING(1, Shows.NEXTAIRDATEMS + " asc,"
                + Shows.AIRSTIME + " asc," + Shows.TITLE + " asc"), FAVORITES_FIRST(2,
                Shows.FAVORITE + " desc," + Shows.TITLE + " asc"), FAVORITES_UPCOMING(3,
                Shows.FAVORITE + " desc," + Shows.NEXTAIRDATEMS + " asc," + Shows.AIRSTIME
                        + " asc," + Shows.TITLE + " asc");

        private final int index;

        private final String query;

        ShowSorting(int index, String query) {
            this.index = index;
            this.query = query;
        }

        public int index() {
            return index;
        }

        public String query() {
            return query;
        }
    }

    public static final String TVDB_SHOW_URL = "http://thetvdb.com/?tab=series&id=";

    public static final String TVDB_EPISODE_URL_1 = "http://thetvdb.com/?tab=episode&seriesid=";

    public static final String TVDB_EPISODE_URL_2 = "&seasonid=";

    public static final String TVDB_EPISODE_URL_3 = "&id=";
}
