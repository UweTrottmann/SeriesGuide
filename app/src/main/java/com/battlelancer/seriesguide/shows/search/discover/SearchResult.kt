// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

/**
 * Holds a search result. Supplying a poster URL is optional.
 */
data class SearchResult(
    val tmdbId: Int,
    /** Two-letter ISO 639-1 language code plus ISO-3166-1 region tag. */
    val languageCode: String,
    val title: String,
    val overview: String,
    var posterUrl: String?,
    var state: Int = STATE_ADD
) {
    companion object {
        const val STATE_ADD: Int = 0
        const val STATE_ADDING: Int = 1
        const val STATE_ADDED: Int = 2
    }
}
