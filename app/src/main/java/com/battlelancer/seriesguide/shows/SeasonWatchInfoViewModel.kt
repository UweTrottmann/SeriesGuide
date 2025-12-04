// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide.shows

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import timber.log.Timber

/**
 * [AndroidViewModel] that provides a [seasonWatchInfo] flow for watch providers of a season
 * determined by [setShowTmdbId] and [setSeasonNumber].
 */
open class SeasonWatchInfoViewModel(application: Application) : AndroidViewModel(application) {

    init {
        // Set original value for region.
        StreamingSearch.initRegionLiveData(application)
    }

    data class SeasonInfo(
        val showTmdbId: Int,
        val seasonNumber: Int
    )

    private var showTmdbId: Int? = null
    private var seasonNumber: Int? = null
    private val seasonInfo = MutableSharedFlow<SeasonInfo>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    data class SeasonWatchInfo(
        val showTmdbId: Int,
        val seasonNumber: Int,
        val region: String
    )

    val seasonWatchInfo: SharedFlow<TmdbTools2.WatchInfo> = seasonInfo
        .distinctUntilChanged()
        .combine(StreamingSearch.regionCode) { seasonInfo, regionCode ->
            SeasonWatchInfo(
                seasonInfo.showTmdbId,
                seasonInfo.seasonNumber,
                regionCode
            )
        }
        .map { seasonWatchInfo ->
            Timber.d("Loading watch providers for $seasonWatchInfo")
            val tmdb = SgApp.getServicesComponent(getApplication()).tmdb()
            TmdbTools2().getWatchProvidersForSeason(
                tmdb,
                seasonWatchInfo.showTmdbId,
                seasonWatchInfo.seasonNumber,
                seasonWatchInfo.region
            )
        }
        .flowOn(Dispatchers.Default)
        // OverviewFragment and EpisodeDetailsFragment briefly unsubscribe if they are paused or
        // reconfigured, so cache and replay the latest value.
        // Also stop sharing after a short timeout (long enough for the slowest device to complete a
        // config change) to avoid a parallel network request as OverviewFragment and
        // EpisodeDetailsFragment cache this model in the activity, so it would react to changes to
        // the region.
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            replay = 1
        )

    fun setShowTmdbId(showTmdbId: Int) {
        this.showTmdbId = showTmdbId
        tryUpdateSeasonInfo()
    }

    fun setSeasonNumber(seasonNumber: Int) {
        this.seasonNumber = seasonNumber
        tryUpdateSeasonInfo()
    }

    /**
     * Only emits a new [SeasonInfo] if both [showTmdbId] and [seasonNumber] are available.
     */
    private fun tryUpdateSeasonInfo() {
        val showTmdbId = this.showTmdbId
        val seasonNumber = this.seasonNumber
        if (showTmdbId != null && seasonNumber != null) {
            seasonInfo.tryEmit(SeasonInfo(showTmdbId, seasonNumber))
        }
    }

}