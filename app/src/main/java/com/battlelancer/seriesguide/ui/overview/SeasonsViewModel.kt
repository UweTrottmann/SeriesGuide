package com.battlelancer.seriesguide.ui.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class SeasonsViewModel(application: Application): AndroidViewModel(application) {

    val remainingCountData = RemainingCountLiveData(application)

}