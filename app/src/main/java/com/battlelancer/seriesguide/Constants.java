package com.battlelancer.seriesguide;

import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Constants {

    /**
     * See {@link Episodes#FIRSTAIREDMS}.
     */
    public static final int EPISODE_UNKNOWN_RELEASE = -1;

    public enum EpisodeSorting {
        LATEST_FIRST(0, "latestfirst", Episodes.NUMBER + " DESC"),

        OLDEST_FIRST(1, "oldestfirst", Episodes.NUMBER + " ASC"),

        UNWATCHED_FIRST(2, "unwatchedfirst", Episodes.WATCHED + " ASC," + Episodes.NUMBER + " ASC"),

        ALPHABETICAL_ASC(3, "atoz", Episodes.TITLE + " COLLATE NOCASE ASC"),

        TOP_RATED(4, "toprated", Tables.EPISODES + "." + Episodes.RATING_GLOBAL + " COLLATE NOCASE DESC"),

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

        @NonNull
        @Override
        public String toString() {
            return this.value;
        }

        private static final Map<String, EpisodeSorting> STRING_MAPPING = new HashMap<>();

        static {
            for (EpisodeSorting via : EpisodeSorting.values()) {
                STRING_MAPPING.put(via.toString().toUpperCase(Locale.US), via);
            }
        }

        public static EpisodeSorting fromValue(String value) {
            return STRING_MAPPING.get(value.toUpperCase(Locale.US));
        }
    }

    public enum SeasonSorting {
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

        @NonNull
        @Override
        public String toString() {
            return this.value;
        }

        private static final Map<String, SeasonSorting> STRING_MAPPING = new HashMap<>();

        static {
            for (SeasonSorting via : SeasonSorting.values()) {
                STRING_MAPPING.put(via.toString().toUpperCase(Locale.US), via);
            }
        }

        public static SeasonSorting fromValue(String value) {
            return STRING_MAPPING.get(value.toUpperCase(Locale.US));
        }
    }


}
