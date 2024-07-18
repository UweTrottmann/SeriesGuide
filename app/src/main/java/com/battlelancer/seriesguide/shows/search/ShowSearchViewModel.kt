// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2024 Uwe Trottmann
// Copyright 2018 Thouseef Hameed

package com.battlelancer.seriesguide.shows.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.sqlite.db.SimpleSQLiteQuery
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables
import com.battlelancer.seriesguide.provider.SgRoomDatabase

class ShowSearchViewModel(application: Application) : AndroidViewModel(application) {

    // Set initial value to show results before typing a query,
    // might already display what the user looks for.
    val searchTerm = MutableLiveData("")
    val shows = searchTerm.switchMap { searchTerm ->
        val database = SgRoomDatabase.getInstance(getApplication())
        val query = if (searchTerm.isNullOrBlank()) {
            SimpleSQLiteQuery(
                "SELECT * FROM ${Tables.SG_SHOW} " +
                        "ORDER BY ${SgShow2Columns.SORT_LAST_WATCHED}, ${SgShow2Columns.SORT_TITLE}"
            )
        } else {
            // Query uses % at the beginning and end of the given string
            // so remove trailing whitespace and replace inner whitespaces with %
            // this improves matching if characters are left out, like "Mr Robot" vs "Mr. Robot"
            val filter = searchTerm.trim().replace("\\s".toRegex(), "%")
            SimpleSQLiteQuery(
                "SELECT * FROM ${Tables.SG_SHOW} " +
                        "WHERE ${SgShow2Columns.TITLE} LIKE ?", arrayOf("%$filter%")
            )
        }
        database.sgShow2Helper().getShowsLiveData(query)
    }

}