package com.battlelancer.seriesguide

import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns

object Constants {
    /**
     * See [com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes.FIRSTAIREDMS].
     */
    const val EPISODE_UNKNOWN_RELEASE = -1

    enum class EpisodeSorting(
        private val index: Int,
        private val value: String,
        private val query: String
    ) {
        LATEST_FIRST(
            0, "latestfirst",
            "${SgEpisode2Columns.NUMBER} DESC"
        ),
        OLDEST_FIRST(
            1, "oldestfirst",
            "${SgEpisode2Columns.NUMBER} ASC"
        ),
        UNWATCHED_FIRST(
            2, "unwatchedfirst",
            "${SgEpisode2Columns.WATCHED} ASC,${SgEpisode2Columns.NUMBER} ASC"
        ),
        ALPHABETICAL_ASC(
            3, "atoz",
            "${SgEpisode2Columns.TITLE} COLLATE UNICODE ASC"
        ),
        TOP_RATED(
            4, "toprated",
            "${SgEpisode2Columns.RATING_GLOBAL} COLLATE UNICODE DESC"
        ),
        DVDLATEST_FIRST(
            5, "dvdlatestfirst",
            "${SgEpisode2Columns.DVDNUMBER} DESC,${SgEpisode2Columns.NUMBER} DESC"
        ),
        DVDOLDEST_FIRST(
            6, "dvdoldestfirst",
            "${SgEpisode2Columns.DVDNUMBER} ASC,${SgEpisode2Columns.NUMBER} ASC"
        );

        fun index(): Int {
            return index
        }

        fun value(): String {
            return value
        }

        fun query(): String {
            return query
        }

        override fun toString(): String {
            return value
        }

        companion object {
            fun fromValue(value: String?): EpisodeSorting {
                if (value != null) {
                    for (sorting in values()) {
                        if (sorting.value == value) {
                            return sorting
                        }
                    }
                }
                return OLDEST_FIRST
            }
        }
    }

    enum class SeasonSorting(val index: Int, private val value: String) {
        LATEST_FIRST(0, "latestfirst"),
        OLDEST_FIRST(1, "oldestfirst");

        override fun toString(): String {
            return value
        }

        companion object {
            fun fromValue(value: String?): SeasonSorting {
                if (value != null) {
                    for (sorting in values()) {
                        if (sorting.value == value) {
                            return sorting
                        }
                    }
                }
                return LATEST_FIRST
            }
        }
    }
}