package com.battlelancer.seriesguide.ui.search

import android.app.Application
import android.arch.lifecycle.AndroidViewModel

class ShowsDiscoverViewModel(application: Application) : AndroidViewModel(application) {

    val data = ShowsDiscoverLiveData()

}