package com.battlelancer.seriesguide.shows.episodes

import android.content.Context
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns

object EpisodesSettings {

    const val KEY_EPISODE_SORT_ORDER = "episodeSorting"

    fun getEpisodeSortOrder(context: Context): EpisodeSorting {
        val orderId = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_EPISODE_SORT_ORDER, null)
        return EpisodeSorting.fromValue(orderId)
    }

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

}