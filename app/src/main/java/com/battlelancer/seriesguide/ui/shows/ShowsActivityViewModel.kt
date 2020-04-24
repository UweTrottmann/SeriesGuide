package com.battlelancer.seriesguide.ui.shows

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ShowsActivityViewModel : ViewModel() {

    val scrollTabToTopLiveData = MutableLiveData<Int>()

    /**
     * For [tabIndex] use ShowsActivity.InitBundle.
     */
    fun scrollTabToTop(tabIndex: Int) {
        scrollTabToTopLiveData.value = tabIndex
        // Clear value so on config change tab does not scroll up again.
        scrollTabToTopLiveData.postValue(null)
    }

}