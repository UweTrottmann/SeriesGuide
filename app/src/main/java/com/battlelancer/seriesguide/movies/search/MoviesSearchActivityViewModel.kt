// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.movies.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.streaming.SgWatchProvider
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Keeps state for [MoviesSearchActivity] and state sharing with [MoviesSearchFragment].
 */
class MoviesSearchActivityViewModel(application: Application) : AndroidViewModel(application) {

    val queryString = MutableStateFlow<String?>(null)
    val releaseYear = MutableStateFlow<Int?>(null)
    val originalLanguage = MutableStateFlow<String?>(null)
    val watchProviderIds =
        SgRoomDatabase.getInstance(getApplication()).sgWatchProviderHelper()
            .getEnabledWatchProviderIdsFlow(SgWatchProvider.Type.MOVIES.id)

}
