// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2022, 2024 Uwe Trottmann

package com.battlelancer.seriesguide.streaming

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.streaming.SgWatchProvider.Type

class DiscoverFilterViewModel(
    application: Application,
    private val type: Type
) : AndroidViewModel(application) {

    val allWatchProvidersFlow = Pager(
        // Configure how data is loaded by passing additional properties to
        // PagingConfig, such as prefetchDistance.
        PagingConfig(pageSize = 20)
    ) {
        SgRoomDatabase.getInstance(getApplication()).sgWatchProviderHelper()
            .allWatchProvidersPagingSource(type.id)
    }.flow
        .cachedIn(viewModelScope)

}


class DiscoverFilterViewModelFactory(
    private val application: Application,
    private val type: Type
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DiscoverFilterViewModel(application, type) as T
    }
}
