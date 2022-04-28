package com.battlelancer.seriesguide.shows.overview

import android.content.Context
import androidx.preference.PreferenceManager

object SeasonsSettings {

    const val KEY_SEASON_SORT_ORDER = "seasonSorting"

    fun getSeasonSortOrder(context: Context): SeasonSorting {
        val orderId = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_SEASON_SORT_ORDER, null)
        return SeasonSorting.fromValue(orderId)
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