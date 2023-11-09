// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.overview

import android.content.Context
import com.battlelancer.seriesguide.R

object SeasonTools {

    fun hasSkippedTag(tags: String?): Boolean {
        return SeasonTags.SKIPPED == tags
    }

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