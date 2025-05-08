// SPDX-License-Identifier: Apache-2.0
// Copyright 2020-2025 Uwe Trottmann

package com.battlelancer.seriesguide.shows

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.battlelancer.seriesguide.shows.ShowsActivityImpl.Tab
import kotlinx.coroutines.flow.MutableStateFlow

class ShowsActivityViewModel(application: Application) : AndroidViewModel(application) {

    data class SelectTab(
        val index: Int,
        val smoothScroll: Boolean
    )

    /**
     * The currently selected tab. Mainly exists so [ShowsFragment] can [selectDiscoverTab].
     */
    val selectedTab = MutableStateFlow(SelectTab(Tab.SHOWS.index, false))
    val scrollTabToTopLiveData = MutableLiveData<Int?>()

    // Used by fragments, tie to ShowsActivity lifecycle to keep instance as short as possible
    val showsDistillationSettings = ShowsDistillationSettings(application)

    /**
     * For [tabPosition] use ShowsActivity.InitBundle.
     */
    fun scrollTabToTop(tabPosition: Int) {
        scrollTabToTopLiveData.value = tabPosition
        // Clear value so on config change tab does not scroll up again.
        scrollTabToTopLiveData.postValue(null)
    }

    fun setInitialTab(tabIndex: Int) {
        selectedTab.value = SelectTab(tabIndex, false)
    }

    fun selectDiscoverTab() {
        selectedTab.value = SelectTab(Tab.DISCOVER.index, true)
    }

}