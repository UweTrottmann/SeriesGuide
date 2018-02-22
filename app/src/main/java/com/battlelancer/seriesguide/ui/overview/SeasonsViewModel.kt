package com.battlelancer.seriesguide.ui.overview

import android.app.Application
import android.arch.lifecycle.AndroidViewModel

class SeasonsViewModel(application: Application): AndroidViewModel(application) {

    val remainingCountData = RemainingCountLiveData(application)

}