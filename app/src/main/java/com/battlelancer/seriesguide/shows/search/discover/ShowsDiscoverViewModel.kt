// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.search.discover

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.streaming.SgWatchProvider
import com.battlelancer.seriesguide.provider.SgRoomDatabase

class ShowsDiscoverViewModel(application: Application) : AndroidViewModel(application) {

    val watchProviderIds = SgRoomDatabase.getInstance(getApplication()).sgWatchProviderHelper()
        .getEnabledWatchProviderIds(SgWatchProvider.Type.SHOWS.id)
    val data = ShowsDiscoverLiveData(application, viewModelScope)

}