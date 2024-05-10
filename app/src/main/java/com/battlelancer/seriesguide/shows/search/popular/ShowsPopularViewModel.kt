// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.popular

import android.app.Application
import com.uwetrottmann.tmdb2.Tmdb

class ShowsPopularViewModel(application: Application) : BaseDiscoverShowsViewModel(application) {

    override fun buildDataSource(
        tmdb: Tmdb, languageCode: String,
        firstReleaseYear: Int?,
        originalLanguageCode: String?,
        watchProviderIds: List<Int>?,
        watchRegion: String?
    ): BaseDiscoverShowDataSource {
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