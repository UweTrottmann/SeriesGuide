package com.battlelancer.seriesguide.ui.lists

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.sqlite.db.SimpleSQLiteQuery
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables
import com.battlelancer.seriesguide.provider.SgListItemWithDetails
import com.battlelancer.seriesguide.provider.SgRoomDatabase

/**
 * Uses raw query to get list items with show details.
 */
class SgListItemViewModel(
    private val listId: String,
    application: Application
) : AndroidViewModel(application) {

    private val queryString = MutableLiveData<String>()
    val sgListItemLiveData: LiveData<List<SgListItemWithDetails>> =
        Transformations.switchMap(queryString) { queryString ->
            SgRoomDatabase.getInstance(getApplication()).sgListHelper()
                .getListItemsWithDetails(SimpleSQLiteQuery(queryString, arrayOf(listId)))
        }

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
