package com.battlelancer.seriesguide.ui.episodes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.model.SgShowMinimal
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.DisplaySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.ArrayList

/**
 * Based on either an episode or a season id loads season and show info
 * as well as the list of episodes of that season.
 */
class EpisodesActivityViewModel(
    application: Application,
    episodeTvdbId: Int,
    seasonTvdbId: Int
) : AndroidViewModel(application) {

    val seasonAndShowInfoLiveData = MutableLiveData<EpisodeSeasonAndShowInfo>()

    init {
        updateEpisodesData(episodeTvdbId, seasonTvdbId)
    }

    fun updateEpisodesData(episodeTvdbId: Int, seasonTvdbId: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            val database = SgRoomDatabase.getInstance(getApplication())

            // Determine required data.
            val seasonInfo = when {
                episodeTvdbId > 0 -> {
                    database.episodeHelper().getEpisodeMinimal(episodeTvdbId)?.let {
                        SeasonInfo(
                            it.seasonTvdbId,
                            it.seasonNumber,
                            it.showTvdbId
                        )
                    }
                }
                seasonTvdbId > 0 -> {
                    database.seasonHelper().getSeasonMinimal(seasonTvdbId)?.let {
                        SeasonInfo(
                            seasonTvdbId,
                            it.number,
                            it.showTvdbId?.toIntOrNull()
                        )
                    }
                }
                else -> null
            }

            // Check for all required data.
            @Suppress("NullChecksToSafeCall")
            if (seasonInfo == null
                || seasonInfo.seasonTvdbId == null || seasonInfo.seasonTvdbId <= 0
                || seasonInfo.seasonNumber == null
                || seasonInfo.showTvdbId == null || seasonInfo.showTvdbId <= 0) {
                seasonAndShowInfoLiveData.postValue(null)
                return@launch
            }

            // Get show info.
            val show = database.showHelper().getShowMinimal(seasonInfo.showTvdbId.toLong())
            if (show == null) {
                seasonAndShowInfoLiveData.postValue(null)
                return@launch
            }

            updateEpisodeList(
                SeasonAndShowInfo(
                    seasonInfo.seasonTvdbId,
                    seasonInfo.seasonNumber,
                    seasonInfo.showTvdbId,
                    show
                ), episodeTvdbId
            )
        }

    private fun updateEpisodeList(seasonAndShowInfo: SeasonAndShowInfo, initialEpisodeTvdbId: Int) {
        val sortOrder = DisplaySettings.getEpisodeSortOrder(getApplication())

        val episodeCursor = getApplication<Application>().contentResolver.query(
            SeriesGuideContract.Episodes.buildEpisodesOfSeasonWithShowUri(seasonAndShowInfo.seasonTvdbId.toString()),
            arrayOf(SeriesGuideContract.Episodes._ID, SeriesGuideContract.Episodes.NUMBER),
            null,
            null,
            sortOrder.query()
        )

        val episodeList = ArrayList<Episode>()
        var startPosition = 0
        if (episodeCursor != null) {
            while (episodeCursor.moveToNext()) {
                val ep = Episode()
                ep.episodeId = episodeCursor.getInt(0)
                if (ep.episodeId == initialEpisodeTvdbId) {
                    startPosition = episodeCursor.position
                }
                ep.episodeNumber = episodeCursor.getInt(1)
                ep.seasonNumber = seasonAndShowInfo.seasonNumber
                episodeList.add(ep)
            }

            episodeCursor.close()
        }

        seasonAndShowInfoLiveData.postValue(
            EpisodeSeasonAndShowInfo(
                seasonAndShowInfo,
                startPosition,
                episodeList
            )
        )
    }

    data class EpisodeSeasonAndShowInfo(
        val seasonAndShowInfo: SeasonAndShowInfo,
        val startPosition: Int,
        val episodes: ArrayList<Episode>
    )

    data class SeasonAndShowInfo(
        val seasonTvdbId: Int,
        val seasonNumber: Int,
        val showTvdbId: Int,
        val show: SgShowMinimal
    )

    data class SeasonInfo(
        val seasonTvdbId: Int?,
        val seasonNumber: Int?,
        val showTvdbId: Int?
    )

}