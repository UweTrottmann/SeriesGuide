// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.newepisodes

import android.app.Application
import com.battlelancer.seriesguide.shows.search.popular.BaseDiscoverShowDataSource
import com.battlelancer.seriesguide.shows.search.popular.BaseDiscoverShowsViewModel
import com.uwetrottmann.tmdb2.Tmdb

class ShowsNewEpisodesViewModel(application: Application) : BaseDiscoverShowsViewModel(application) {

    override fun buildDataSource(
        tmdb: Tmdb, languageCode: String,
        firstReleaseYear: Int?,
        originalLanguageCode: String?,
        watchProviderIds: List<Int>?,
        watchRegion: String?
    ): BaseDiscoverShowDataSource {
        return ShowsNewEpisodesDataSource(
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