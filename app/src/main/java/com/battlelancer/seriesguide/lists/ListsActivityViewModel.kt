// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.lists

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.battlelancer.seriesguide.provider.SgRoomDatabase

class ListsActivityViewModel(application: Application) : AndroidViewModel(application) {

    var hasRestoredLastListsTabPosition = false
    val listsLiveData by lazy {
        SgRoomDatabase.getInstance(application).sgListHelper().getListsForDisplay()
    }
    val scrollTabToTopLiveData = MutableLiveData<Int?>()

    fun scrollTabToTop(tabPosition: Int) {
        scrollTabToTopLiveData.value = tabPosition
        // Clear value so on config change tab does not scroll up again.
        scrollTabToTopLiveData.postValue(null)
    }

}