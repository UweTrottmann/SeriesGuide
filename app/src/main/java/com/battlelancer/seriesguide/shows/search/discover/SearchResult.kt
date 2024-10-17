// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2024 Uwe Trottmann
package com.battlelancer.seriesguide.shows.search.discover

/**
 * Holds a search result, used later for adding this show. Supplying a poster URL is optional.
 */
class SearchResult {
    var tmdbId: Int = 0

    /** Two-letter ISO 639-1 language code plus ISO-3166-1 region tag.  */
    var language: String? = null
    var title: String = ""
    var overview: String? = null
    var posterPath: String? = null
    var state: Int = 0

    fun copy(): SearchResult {
        val copy = SearchResult()
        copy.tmdbId = tmdbId
        copy.language = language
        copy.title = title
        copy.overview = overview
        copy.posterPath = posterPath
        copy.state = state
        return copy
    }

    companion object {
        const val STATE_ADD: Int = 0
        const val STATE_ADDING: Int = 1
        const val STATE_ADDED: Int = 2
    }
}
