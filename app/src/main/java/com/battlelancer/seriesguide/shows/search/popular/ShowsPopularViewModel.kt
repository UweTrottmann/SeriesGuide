// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.popular

import android.app.Application
import com.battlelancer.seriesguide.shows.search.discover.BaseShowResultsDataSource
import com.battlelancer.seriesguide.shows.search.discover.BaseDiscoverShowsViewModel
import com.uwetrottmann.tmdb2.Tmdb

class ShowsPopularViewModel(application: Application) : BaseDiscoverShowsViewModel(application) {

    override fun buildDiscoverDataSource(
        tmdb: Tmdb, languageCode: String,
        firstReleaseYear: Int?,
        originalLanguageCode: String?,
        watchProviderIds: List<Int>?,
        watchRegion: String?
    ): BaseShowResultsDataSource {
        return ShowsPopularDataSource(
            getApplication(),
            tmdb,
            languageCode,
            firstReleaseYear,
            originalLanguageCode,
            watchProviderIds,
            watchRegion
        )
    }

}