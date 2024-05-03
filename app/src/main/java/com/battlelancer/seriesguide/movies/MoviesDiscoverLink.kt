// SPDX-License-Identifier: Apache-2.0
// Copyright 2017-2024 Uwe Trottmann
package com.battlelancer.seriesguide.movies

import androidx.annotation.StringRes
import com.battlelancer.seriesguide.R

enum class MoviesDiscoverLink(
    val id: Int,
    @param:StringRes val titleRes: Int
) {
    IN_THEATERS(0, R.string.movies_in_theatres),
    POPULAR(1, R.string.title_popular),
    DIGITAL(2, R.string.title_digital_releases),
    DISC(3, R.string.title_disc_releases),
    UPCOMING(4, R.string.upcoming);

    companion object {
        /** Guaranteed to not be used as an ID so [fromId] will return `null`. */
        const val NO_LINK_ID = -1

        fun fromId(id: Int): MoviesDiscoverLink? = entries.find { it.id == id }
    }
}
