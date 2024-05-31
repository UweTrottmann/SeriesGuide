// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2024 Uwe Trottmann
package com.battlelancer.seriesguide.shows.search.discover

import androidx.annotation.StringRes
import com.battlelancer.seriesguide.R

enum class DiscoverShowsLink(
    val id: Int,
    @param:StringRes val titleRes: Int
) {
    POPULAR(0, R.string.title_popular),
    NEW_EPISODES(1, R.string.title_new_episodes),
    WATCHED(2, R.string.watched_shows),
    COLLECTION(3, R.string.shows_collection),
    WATCHLIST(4, R.string.watchlist);

    companion object {
        fun fromId(id: Int): DiscoverShowsLink = entries.find { it.id == id } ?: POPULAR
    }
}
