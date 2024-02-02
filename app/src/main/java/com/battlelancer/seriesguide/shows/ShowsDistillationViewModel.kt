// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.streaming.SgWatchProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ShowsDistillationViewModel(application: Application) : AndroidViewModel(application) {

    val showsDistillationUiState: StateFlow<ShowsDistillationUiState> =
        SgRoomDatabase.getInstance(application).sgWatchProviderHelper()
            .usedShowWatchProvidersFlow(SgWatchProvider.Type.SHOWS.id)
            .map { ShowsDistillationUiState(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = ShowsDistillationUiState()
            )

}

data class ShowsDistillationUiState(
    val watchProviders: List<SgWatchProvider> = listOf()
)