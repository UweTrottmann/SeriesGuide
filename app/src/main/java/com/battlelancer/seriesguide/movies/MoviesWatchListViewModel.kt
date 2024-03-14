// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.movies

import android.app.Application
import com.battlelancer.seriesguide.provider.SeriesGuideContract

class MoviesWatchListViewModel(application: Application) : MoviesWatchedViewModel(application) {

    override val selection: String
        get() = SeriesGuideContract.Movies.SELECTION_WATCHLIST

}