package com.battlelancer.seriesguide.ui.episodes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.provider.SgEpisode2Numbers
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.provider.SgShow2Minimal
import com.battlelancer.seriesguide.settings.DisplaySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Based on either an episode or a season id loads season and show info
 * as well as the list of episodes of that season.
 */
class EpisodesActivityViewModel(
    application: Application,
    episodeTvdbId: Int,
    episodeRowId: Long,
    seasonRowId: Long
) : AndroidViewModel(application) {

    val seasonAndShowInfoLiveData = MutableLiveData<EpisodeSeasonAndShowInfo?>()

    init {
        updateEpisodesData(episodeTvdbId, episodeRowId, seasonRowId)
    }

    fun updateEpisodesData(
        episodeTvdbId: Int,
        episodeRowId: Long,
        seasonRowId: Long
    ) = viewModelScope.launch(Dispatchers.IO) {
        val database = SgRoomDatabase.getInstance(getApplication())

        val initialEpisodeId = if (episodeRowId == 0L && episodeTvdbId > 0) {
            database.sgEpisode2Helper().getEpisodeIdByTvdbId(episodeTvdbId)
        } else {
            episodeRowId
        }

        // Determine required data.
        val seasonInfo = when {
            initialEpisodeId > 0 -> {
                database.sgEpisode2Helper().getEpisodeInfo(initialEpisodeId)?.let {
                    SeasonInfo(it.seasonId, it.season, it.showId)
                }
            }
            seasonRowId > 0 -> {
                database.sgSeason2Helper().getSeasonNumbers(seasonRowId)?.let {
                    SeasonInfo(seasonRowId, it.numberOrNull, it.showId)
                }
            }
            else -> null
        }

        // Check for all required data.
        if (seasonInfo?.seasonId == null || seasonInfo.seasonId <= 0
            || seasonInfo.seasonNumber == null
            || seasonInfo.showId <= 0) {
            seasonAndShowInfoLiveData.postValue(null)
            return@launch
        }

        // Get show info.
        val show = database.sgShow2Helper().getShowMinimal(seasonInfo.showId)
        if (show == null) {
            seasonAndShowInfoLiveData.postValue(null)
            return@launch
        }

        // Get episode list.
        val seasonId = seasonInfo.seasonId
        val sortOrder = DisplaySettings.getEpisodeSortOrder(getApplication())
        val episodes = database.sgEpisode2Helper()
            .getEpisodeNumbersOfSeason(SgEpisode2Numbers.buildQuery(seasonId, sortOrder))

        val episodeIndexOrMinus1 = episodes.indexOfFirst { it.id == initialEpisodeId }

        // Post gathered info.
        seasonAndShowInfoLiveData.postValue(
            EpisodeSeasonAndShowInfo(
                SeasonAndShowInfo(
                    seasonInfo.seasonId,
                    seasonInfo.seasonNumber,
                    seasonInfo.showId,
                    show
                ),
                startPosition = if (episodeIndexOrMinus1 != -1) episodeIndexOrMinus1 else 0,
                episodes
            )
        )
    }

    data class SeasonInfo(
        val seasonId: Long?,
        val seasonNumber: Int?,
        val showId: Long
    )

    data class SeasonAndShowInfo(
        val seasonId: Long,
        val seasonNumber: Int,
        val showId: Long,
        val show: SgShow2Minimal
    )

    data class EpisodeSeasonAndShowInfo(
        val seasonAndShowInfo: SeasonAndShowInfo,
        val startPosition: Int,
        val episodes: List<SgEpisode2Numbers>
    )

}

class EpisodesActivityViewModelFactory(
    private val application: Application,
    private val episodeTvdbId: Int,
    private val episodeRowId: Long,
    private val seasonRowId: Long
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EpisodesActivityViewModel(application, episodeTvdbId, episodeRowId, seasonRowId) as T
    }

}
