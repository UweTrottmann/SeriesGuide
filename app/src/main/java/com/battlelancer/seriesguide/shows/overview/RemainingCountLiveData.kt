// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.overview

import android.content.Context
import androidx.lifecycle.LiveData
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.util.TimeTools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Calculates the number of unwatched episodes and if all episodes are collected
 * (excluding specials) for a show.
 */
class RemainingCountLiveData(
    val context: Context,
    val scope: CoroutineScope
) : LiveData<RemainingCountLiveData.Result>() {

    private val semaphore = Semaphore(1)

    data class Result(
        val unwatchedEpisodes: Int,
        val collectedAllEpisodes: Boolean
    )

    fun load(showRowId: Long) {
        if (showRowId > 0) {
            scope.launch(Dispatchers.IO) {
                // Use Semaphore with 1 permit to only run one calculation at a time and to
                // guarantee results are delivered in order.
                semaphore.withPermit {
                    calcRemainingCounts(showRowId)
                }
            }
        }
    }

    private suspend fun calcRemainingCounts(showRowId: Long) = withContext(Dispatchers.IO) {
        val helper = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
        val currentTime = TimeTools.getCurrentTime(context)
        val unwatchedEpisodes = helper.countNotWatchedEpisodesOfShow(showRowId, currentTime)
        // Only consider all collected if there is at least one collected
        val collectedAllEpisodes = !helper.hasEpisodesToCollect(showRowId)
                && helper.hasCollectedEpisodes(showRowId)
        postValue(Result(unwatchedEpisodes, collectedAllEpisodes))
    }

}