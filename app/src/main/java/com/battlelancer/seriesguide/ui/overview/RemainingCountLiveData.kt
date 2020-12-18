package com.battlelancer.seriesguide.ui.overview

import android.content.Context
import androidx.lifecycle.LiveData
import com.battlelancer.seriesguide.util.DBUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Calculates the number of unwatched and not collected episodes of a show.
 */
class RemainingCountLiveData(
    private val context: Context,
    private val scope: CoroutineScope
) : LiveData<RemainingCountLiveData.Result>() {

    private val semaphore = Semaphore(1)

    data class Result(
            val unwatchedEpisodes: Int,
            val uncollectedEpisodes: Int
    )

    fun load(showId: Long) {
        if (showId > 0) {
            scope.launch(Dispatchers.IO) {
                // Use Semaphore with 1 permit to only run one calculation at a time and to
                // guarantee results are delivered in order.
                semaphore.withPermit {
                    calcRemainingCounts(showId)
                }
            }
        }
    }

    private suspend fun calcRemainingCounts(showId: Long) = withContext(Dispatchers.IO) {
        val unwatchedEpisodes = DBUtils.getUnwatchedEpisodesOfShow(context, showId)
        val uncollectedEpisodes = DBUtils.getUncollectedEpisodesOfShow(context, showId)
        postValue(Result(unwatchedEpisodes, uncollectedEpisodes))
    }

}