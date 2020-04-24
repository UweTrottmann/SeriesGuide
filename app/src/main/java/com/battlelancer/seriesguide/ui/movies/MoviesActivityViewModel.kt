package com.battlelancer.seriesguide.ui.movies

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MoviesActivityViewModel : ViewModel() {

    data class ScrollTabToTopEvent(val tabPosition: Int, val isShowingNowTab: Boolean)

    val scrollTabToTopLiveData = MutableLiveData<ScrollTabToTopEvent?>()

    fun scrollTabToTop(tabPosition: Int, isShowingNowTab: Boolean) {
        scrollTabToTopLiveData.value = ScrollTabToTopEvent(tabPosition, isShowingNowTab)
        // Clear value so on config change tab does not scroll up again.
        scrollTabToTopLiveData.postValue(null)
    }
}