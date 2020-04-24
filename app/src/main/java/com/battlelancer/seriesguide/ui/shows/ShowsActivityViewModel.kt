package com.battlelancer.seriesguide.ui.shows

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ShowsActivityViewModel : ViewModel() {

    val scrollTabToTopLiveData = MutableLiveData<Int?>()

    /**
     * For [tabPosition] use ShowsActivity.InitBundle.
     */
    fun scrollTabToTop(tabPosition: Int) {
        scrollTabToTopLiveData.value = tabPosition
        // Clear value so on config change tab does not scroll up again.
        scrollTabToTopLiveData.postValue(null)
    }

}