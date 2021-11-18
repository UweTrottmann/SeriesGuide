package com.battlelancer.seriesguide.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.provider.SgRoomDatabase

class ShowsDiscoverViewModel(application: Application) : AndroidViewModel(application) {

    val watchProviderIds = SgRoomDatabase.getInstance(getApplication()).sgWatchProviderHelper()
        .getEnabledWatchProviderIds()
    val data = ShowsDiscoverLiveData(application, viewModelScope)

}