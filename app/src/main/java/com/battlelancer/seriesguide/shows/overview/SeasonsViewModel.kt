// SPDX-License-Identifier: Apache-2.0
// Copyright 2018, 2020-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgSeason2
import com.battlelancer.seriesguide.util.TimeTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SeasonsViewModel(
    application: Application,
    private val showId: Long
) : AndroidViewModel(application) {

    private val order = MutableStateFlow(SeasonsSettings.getSeasonSortOrder(getApplication()))

    data class SeasonStats(
        val total: Int,
        val notWatchedReleased: Int,
        val notWatchedToBeReleased: Int,
        val notWatchedNoRelease: Int,
        val skipped: Int,
        val collected: Int
    ) {
        constructor() : this(0, 0, 0, 0, 0, 0)
    }

    data class SgSeasonWithStats(
        val season: SgSeason2,
        val stats: SeasonStats
    )

    private val seasonStats = MutableStateFlow(mapOf<Long, SeasonStats>())

    val seasonsWithStats = order
        .flatMapLatest {
            val helper = SgRoomDatabase.getInstance(application).sgSeason2Helper()
            if (it == SeasonsSettings.SeasonSorting.LATEST_FIRST) {
                helper.getSeasonsOfShowLatestFirst(showId)
            } else {
                helper.getSeasonsOfShowOldestFirst(showId)
            }
        }
        .combine(seasonStats) { seasons, stats ->
            seasons.map { SgSeasonWithStats(it, stats[it.id] ?: SeasonStats()) }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf()
        )

    val remainingCountData = RemainingCountLiveData(application, viewModelScope)

    fun updateOrder() {
        order.value = SeasonsSettings.getSeasonSortOrder(getApplication())
    }

    /**
     * Updates episode counts for a specific [seasonIdToUpdate] or all seasons if null.
     */
    fun updateSeasonStats(seasonIdToUpdate: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            val database = SgRoomDatabase.getInstance(getApplication())

            val seasonIds = if (seasonIdToUpdate != null) {
                listOf(seasonIdToUpdate)
            } else {
                database.sgSeason2Helper().getSeasonIdsOfShow(showId)
            }

            val newMap = seasonStats.value.toMutableMap()
            val helper = database.sgEpisode2Helper()
            val currentTime = TimeTools.getCurrentTime(getApplication())
            for (seasonId in seasonIds) {
                val total = helper.countEpisodesOfSeason(seasonId)
                newMap[seasonId] = SeasonStats(
                    total = total,
                    notWatchedReleased = helper
                        .countNotWatchedReleasedEpisodesOfSeason(seasonId, currentTime),
                    notWatchedToBeReleased = helper
                        .countNotWatchedToBeReleasedEpisodesOfSeason(seasonId, currentTime),
                    notWatchedNoRelease = helper.countNotWatchedNoReleaseEpisodesOfSeason(seasonId),
                    skipped = helper.countSkippedEpisodesOfSeason(seasonId),
                    collected = total - helper.countNotCollectedEpisodesOfSeason(seasonId)
                )
            }
            seasonStats.value = newMap
        }
    }

}

class SeasonsViewModelFactory(
    private val application: Application,
    private val showId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SeasonsViewModel(application, showId) as T
    }
}