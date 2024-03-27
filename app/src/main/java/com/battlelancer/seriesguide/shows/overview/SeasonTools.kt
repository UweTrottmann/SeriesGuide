// SPDX-License-Identifier: Apache-2.0
// Copyright 2013, 2014, 2016, 2018, 2022-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.overview

import android.content.Context
import com.battlelancer.seriesguide.R

object SeasonTools {

    /**
     * Builds a localized string like "Season 5" or if the number is 0 "Special Episodes".
     */
    fun getSeasonString(context: Context, seasonNumber: Int): String {
        return if (seasonNumber == 0) {
            context.getString(R.string.specialseason)
        } else {
            context.getString(R.string.season_number, seasonNumber)
        }
    }

}