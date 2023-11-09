// Copyright 2021-2023 Uwe Trottmann
// Copyright 2018 Thouseef Hameed
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.search

import android.app.Application
import android.text.format.DateUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.sqlite.db.SimpleSQLiteQuery
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.util.TimeTools

class ShowSearchViewModel(application: Application) : AndroidViewModel(application) {

    val searchTerm = MutableLiveData<String?>()
    val shows = searchTerm.switchMap { searchTerm ->
        val database = SgRoomDatabase.getInstance(getApplication())
        val query = if (searchTerm.isNullOrBlank()) {
            SimpleSQLiteQuery(
                "SELECT * FROM ${Tables.SG_SHOW} " +
                        "WHERE ${SgShow2Columns.NEXTEPISODE} != '' AND ${SgShow2Columns.HIDDEN} = 0 " +
                        "AND ${SgShow2Columns.NEXTAIRDATEMS} < ? " +
                        "ORDER BY ${SgShow2Columns.SORT_LATEST_EPISODE_THEN_STATUS}",
                arrayOf((TimeTools.getCurrentTime(getApplication()) + DateUtils.HOUR_IN_MILLIS))
            )
        } else {
            // Query uses % at the beginning and end of the given string
            // so remove trailing whitespace and replace inner whitespaces with %
            // this improves matching if characters are left out, like "Mr Robot" vs "Mr. Robot"
            val filter = searchTerm.trim().replace("\\s".toRegex(), "%")
            SimpleSQLiteQuery("SELECT * FROM ${Tables.SG_SHOW} " +
                    "WHERE ${SgShow2Columns.TITLE} LIKE ?", arrayOf("%$filter%"))
        }
        database.sgShow2Helper().getShowsLiveData(query)
    }

}