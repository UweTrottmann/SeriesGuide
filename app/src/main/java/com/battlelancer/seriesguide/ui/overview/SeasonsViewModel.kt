package com.battlelancer.seriesguide.ui.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

class SeasonsViewModel(application: Application): AndroidViewModel(application) {

    val remainingCountData = RemainingCountLiveData(application, viewModelScope)

}