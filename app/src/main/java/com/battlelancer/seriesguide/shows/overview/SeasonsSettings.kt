package com.battlelancer.seriesguide.shows.overview

import android.content.Context
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.Constants

object SeasonsSettings {

    const val KEY_SEASON_SORT_ORDER = "seasonSorting"

    fun getSeasonSortOrder(context: Context): Constants.SeasonSorting {
        val orderId = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_SEASON_SORT_ORDER, null)
        return Constants.SeasonSorting.fromValue(orderId)
    }

}