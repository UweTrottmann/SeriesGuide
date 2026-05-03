// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2023 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.lists

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Loads show or movie details and lists with the initial is-on-list state for the item,
 * allows to modify the state without changing the database.
 */
class ManageListsDialogFragmentViewModel(
    application: Application,
    private val listItem: ListItem
) : AndroidViewModel(application) {

    @ListItemTypes
    private val listItemType: Int = when (listItem) {
        is ListItem.Show -> ListItemTypes.TMDB_SHOW
        is ListItem.Movie -> ListItemTypes.TMDB_MOVIE
    }

    sealed class ListItem {
        /** A show identified by its local database row ID. */
        data class Show(val showId: Long) : ListItem()

        /** A movie identified by its TMDB ID. */
        data class Movie(val movieTmdbId: Int) : ListItem()
    }

    /**
     * Minimal details needed to display the item in the dialog.
     * [tmdbId] is 0 for shows that have not yet been migrated to TMDB data.
     */
    data class ItemDetails(
        val title: String,
        val tmdbId: Int
    )

    val listItemDetails = liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
        val db = SgRoomDatabase.getInstance(application)
        val details: ItemDetails? = when (listItem) {
            is ListItem.Show -> {
                db.sgShow2Helper().getShowMinimal(listItem.showId)
                    ?.let { ItemDetails(it.title, it.tmdbId ?: 0) }
            }

            is ListItem.Movie -> {
                db.movieHelper().getMovieTitle(listItem.movieTmdbId)
                    ?.let { ItemDetails(it, listItem.movieTmdbId) }
            }
        }
        emit(details)
        if (details != null && details.tmdbId > 0) {
            loadLists(details.tmdbId)
        }
    }

    data class ListWithItem(
        val listId: String,
        val listName: String,
        val isItemOnListOriginal: Boolean,
        val isItemOnList: Boolean
    )

    val listsWithItem = MutableLiveData<List<ListWithItem>>()

    private fun loadLists(tmdbId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val listHelper = SgRoomDatabase.getInstance(getApplication()).sgListHelper()
            val listItemsForItem = listHelper.getListItemsWithTmdbId(tmdbId, listItemType)
            val initialListsWithItem = listHelper
                .getListsForExport()
                .map { list ->
                    val isItemOnList = listItemsForItem.find { list.listId == it.listId } != null
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

    fun setIsItemOnList(listId: String, value: Boolean) {
        val listsWithItem = listsWithItem.value ?: return
        val indexToReplace = listsWithItem.indexOfFirst { it.listId == listId }
        if (indexToReplace >= 0) {
            val updatedLists = listsWithItem.toMutableList()
            updatedLists[indexToReplace] = listsWithItem[indexToReplace].copy(isItemOnList = value)
            this.listsWithItem.postValue(updatedLists)
        }
    }

    /**
     * Adds the item to selected lists, removes it from previously selected lists.
     *
     * Does nothing if missing required data.
     */
    suspend fun saveChanges() = withContext(Dispatchers.IO) {
        val tmdbId = listItemDetails.value?.tmdbId ?: 0
        if (tmdbId <= 0) {
            return@withContext // Show has no TMDB ID or failed to load it from database
        }

        val lists = listsWithItem.value
            ?: return@withContext // Do nothing, lists not loaded, yet


        val addToTheseLists = mutableListOf<String>()
        val removeFromTheseLists = mutableListOf<String>()
        lists
            .filter { it.isItemOnList != it.isItemOnListOriginal }
            .forEach {
                if (it.isItemOnListOriginal) {
                    // Remove from list
                    removeFromTheseLists.add(it.listId)
                } else {
                    // Add to list
                    addToTheseLists.add(it.listId)
                }
            }
        ListsTools.changeListsOfItem(
            getApplication(),
            tmdbId,
            listItemType,
            addToTheseLists,
            removeFromTheseLists
        )
    }

}

class ManageListsDialogFragmentViewModelFactory(
    private val application: Application,
    private val listItem: ManageListsDialogFragmentViewModel.ListItem
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManageListsDialogFragmentViewModel(application, listItem) as T
    }

}
