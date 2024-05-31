// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.popular

import android.content.Context
import com.battlelancer.seriesguide.shows.search.discover.BaseShowResultsDataSource
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.uwetrottmann.tmdb2.Tmdb
import com.uwetrottmann.tmdb2.entities.TvShowResultsPage

/**
 * Loads popular shows in pages from TMDB.
 */
class ShowsPopularDataSource(
    context: Context,
    tmdb: Tmdb,
    languageCode: String,
    private val firstReleaseYear: Int?,
    private val originalLanguageCode: String?,
    private val watchProviderIds: List<Int>?,
    private val watchRegion: String?
) : BaseShowResultsDataSource(
    context,
    tmdb,
    languageCode
) {

    override val action: String
        get() = "load popular shows"

    override suspend fun loadShows(
        tmdb: Tmdb,
        language: String,
        page: Int,
    ): TvShowResultsPage? = TmdbTools2().getPopularShows(
        tmdb,
        language,
        page,
        firstReleaseYear,
        originalLanguageCode,
        watchProviderIds,
        watchRegion
    )
}
