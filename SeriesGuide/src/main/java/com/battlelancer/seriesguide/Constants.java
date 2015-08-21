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

package com.battlelancer.seriesguide;

import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Constants {

    // Use non-application id-based type as beaming is meant to be cross-flavor compatible
    public static final String ANDROID_BEAM_NDEF_MIME_TYPE = "application/vnd.com.uwetrottmann.seriesguide";

    /**
     * See {@link Episodes#FIRSTAIREDMS}.
     */
    public static final int EPISODE_UNKNOWN_RELEASE = -1;

    public static enum EpisodeSorting {
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
                STRING_MAPPING.put(via.toString().toUpperCase(Locale.US), via);
            }
        }

        public static SeasonSorting fromValue(String value) {
            return STRING_MAPPING.get(value.toUpperCase(Locale.US));
        }
    }


}
