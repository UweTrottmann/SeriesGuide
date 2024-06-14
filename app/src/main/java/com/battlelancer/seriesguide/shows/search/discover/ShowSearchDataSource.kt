// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.content.Context
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.uwetrottmann.tmdb2.Tmdb
import com.uwetrottmann.tmdb2.entities.TvShowResultsPage

/**
 * Loads shows matching the [query] in pages from TMDB.
 */
class ShowSearchDataSource(
    context: Context,
    tmdb: Tmdb,
    languageCode: String,
    private val query: String,
    private val firstReleaseYear: Int?
) : BaseShowResultsDataSource(
    context,
    tmdb,
    languageCode
) {

    override val action: String
        get() = "search shows"

    override suspend fun loadShows(
        tmdb: Tmdb,
        language: String,
        page: Int,
    ): TvShowResultsPage? = TmdbTools2().searchShows(
        tmdb,
        query,
        language,
        firstReleaseYear,
        page
    )
}