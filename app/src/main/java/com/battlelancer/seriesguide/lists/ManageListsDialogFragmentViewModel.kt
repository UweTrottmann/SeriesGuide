// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.lists

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgShow2Minimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Loads show details and lists with the initial is on list state for the item,
 * allows to modify the state without changing the database.
 */
class ManageListsDialogFragmentViewModel(
    application: Application,
    showId: Long
) : AndroidViewModel(application) {

    val showDetails = liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
        val showDetails = SgRoomDatabase.getInstance(application)
            .sgShow2Helper()
            .getShowMinimal(showId)
        emit(showDetails)
        if (showDetails != null) {
            loadLists(showDetails)
        }
    }

    data class ListWithItem(
        val listId: String,
        val listName: String,
        val isItemOnListOriginal: Boolean,
        val isItemOnList: Boolean
    )

    val listsWithItem = MutableLiveData<List<ListWithItem>>()

    private fun loadLists(showDetails: SgShow2Minimal) {
        viewModelScope.launch(Dispatchers.IO) {
            val showTmdbId = showDetails.tmdbId ?: 0
            if (showTmdbId > 0) {
                val listHelper = SgRoomDatabase.getInstance(getApplication()).sgListHelper()
                val listItemsForShow = listHelper.getListItemsWithTmdbId(showTmdbId)
                val initialListsWithItem = listHelper
                    .getListsForExport()
                    .map { list ->
                        val isItemOnList = listItemsForShow.find { list.listId == it.listId } != null
                        ListWithItem(
                            listId = list.listId,
                            listName = list.name,
                            isItemOnListOriginal = isItemOnList,
                            isItemOnList = isItemOnList
                        )
                    }
                listsWithItem.postValue(initialListsWithItem)
            }
        }
    }

    fun setIsItemOnList(listId: String, value: Boolean) {
        val listsWithItem = listsWithItem.value ?: return
        val indexToReplace = listsWithItem.indexOfFirst { it.listId == listId }
        if (indexToReplace >= 0) {
            val updatedLists = listsWithItem.toMutableList()
            updatedLists[indexToReplace] = listsWithItem[indexToReplace].copy(isItemOnList = value)
            this.listsWithItem.postValue(updatedLists)
        }
    }

}

class ManageListsDialogFragmentViewModelFactory(
    private val application: Application,
    private val showId: Long
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManageListsDialogFragmentViewModel(application, showId) as T
    }

}
