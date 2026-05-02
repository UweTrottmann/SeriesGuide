// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.movies

import android.app.Application
import com.battlelancer.seriesguide.provider.SeriesGuideContract

class MoviesCollectionViewModel(application: Application) : MoviesWatchedViewModel(application) {

    override val selection: String
        get() = SeriesGuideContract.Movies.SELECTION_COLLECTION

}