package com.battlelancer.seriesguide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns;

public class Constants {

    /**
     * See {@link Episodes#FIRSTAIREDMS}.
     */
    public static final int EPISODE_UNKNOWN_RELEASE = -1;

    public enum EpisodeSorting {
        LATEST_FIRST(0, "latestfirst", SgEpisode2Columns.NUMBER + " DESC"),

        OLDEST_FIRST(1, "oldestfirst", SgEpisode2Columns.NUMBER + " ASC"),

        UNWATCHED_FIRST(2, "unwatchedfirst",
                SgEpisode2Columns.WATCHED + " ASC," + SgEpisode2Columns.NUMBER + " ASC"),

        ALPHABETICAL_ASC(3, "atoz", SgEpisode2Columns.TITLE + " COLLATE NOCASE ASC"),

        TOP_RATED(4, "toprated", SgEpisode2Columns.RATING_GLOBAL + " COLLATE NOCASE DESC"),

        DVDLATEST_FIRST(5, "dvdlatestfirst",
                SgEpisode2Columns.DVDNUMBER + " DESC," + SgEpisode2Columns.NUMBER + " DESC"),

        DVDOLDEST_FIRST(6, "dvdoldestfirst",
                SgEpisode2Columns.DVDNUMBER + " ASC," + SgEpisode2Columns.NUMBER + " ASC");

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

        public static EpisodeSorting fromValue(@Nullable  String value) {
            if (value != null) {
                for (EpisodeSorting sorting : values()) {
                    if (sorting.value.equals(value)) {
                        return sorting;
                    }
                }
            }
            return OLDEST_FIRST;
        }
    }

    public enum SeasonSorting {
        LATEST_FIRST(0, "latestfirst"),

        OLDEST_FIRST(1, "oldestfirst");

        public final int index;
        private final String value;

        SeasonSorting(int index, String value) {
            this.index = index;
            this.value = value;
        }

        @NonNull
        @Override
        public String toString() {
            return this.value;
        }

        public static SeasonSorting fromValue(@Nullable String value) {
            if (value != null) {
                for (SeasonSorting sorting : values()) {
                    if (sorting.value.equals(value)) {
                        return sorting;
                    }
                }
            }
            return LATEST_FIRST;
        }
    }


}
