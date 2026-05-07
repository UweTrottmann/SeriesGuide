// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2021 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.lists

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Uses raw query to get list items with show details.
 */
class SgListItemViewModel(
    private val listId: String,
    application: Application
) : AndroidViewModel(application) {

    private val queryString = MutableStateFlow("")
    val items: StateFlow<List<UiListItem>> = queryString
        .flatMapLatest { queryString ->
            SgRoomDatabase.getInstance(getApplication()).sgListHelper()
                .getListItemsWithDetails(SimpleSQLiteQuery(queryString, arrayOf(listId)))
        }
        .map { items ->
            val builder = UiListItemBuilder(getApplication())
            items.map { builder.buildFrom(it) }
        }
        // UiListItemBuilder may do database queries for legacy season and episode items, so use
        // IO dispatcher for both.
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            // Note: the associated fragment is shown in a RecyclerView-based ViewPager2 and
            // SgListFragment uses repeatOnLifecycle(Lifecycle.State.STARTED)
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        updateQuery()
    }

    fun updateQuery() {
        val orderClause = ListsDistillationSettings.getSortQuery(getApplication())

        // items of this list, but exclude any if show was removed from the database
        // (the join on show data will fail, hence the show id will be 0/null)
        queryString.value =
            "SELECT * FROM ${Tables.LIST_ITEMS_WITH_DETAILS}" +
                    " WHERE ${Lists.LIST_ID}=? AND ${SgShow2Columns.REF_SHOW_ID}>0" +
                    " ORDER BY $orderClause"
    }

}

class SgListItemViewModelFactory(
    private val listId: String,
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SgListItemViewModel(listId, application) as T
    }

}
