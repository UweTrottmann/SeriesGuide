package com.battlelancer.seriesguide.ui.lists

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ListsActivityViewModel : ViewModel() {

    val scrollTabToTopLiveData = MutableLiveData<Int?>()

    fun scrollTabToTop(tabPosition: Int) {
        scrollTabToTopLiveData.value = tabPosition
        // Clear value so on config change tab does not scroll up again.
        scrollTabToTopLiveData.postValue(null)
    }

}