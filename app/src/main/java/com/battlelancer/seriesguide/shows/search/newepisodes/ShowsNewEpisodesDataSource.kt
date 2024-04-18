// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.newepisodes

import android.content.Context
import com.battlelancer.seriesguide.shows.search.popular.BaseDiscoverShowDataSource
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.uwetrottmann.tmdb2.Tmdb
import com.uwetrottmann.tmdb2.entities.TvShowResultsPage

/**
 * Loads shows with new episodes in pages from TMDB.
 */
class ShowsNewEpisodesDataSource(
    context: Context,
    tmdb: Tmdb,
    languageCode: String,
    firstReleaseYear: Int?,
    originalLanguageCode: String?,
    watchProviderIds: List<Int>?,
    watchRegion: String?
) : BaseDiscoverShowDataSource(
    context,
    tmdb,
    languageCode,
    firstReleaseYear,
    originalLanguageCode,
    watchProviderIds,
    watchRegion
) {

    override val action: String
        get() = "get shows w new episodes"

    override suspend fun loadShows(
        tmdb: Tmdb,
        language: String,
        page: Int,
        firstReleaseYear: Int?,
        originalLanguageCode: String?,
        watchProviderIds: List<Int>?,
        watchRegion: String?
    ): TvShowResultsPage? = TmdbTools2().getShowsWithNewEpisodes(
        tmdb,
        language,
        page,
        firstReleaseYear,
        originalLanguageCode,
        watchProviderIds,
        watchRegion
    )
}
