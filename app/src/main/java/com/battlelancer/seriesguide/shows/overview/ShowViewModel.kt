// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.overview

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgShow2
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.battlelancer.seriesguide.traktapi.TraktTools
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShowViewModel(application: Application) : AndroidViewModel(application) {

    private val showId = MutableLiveData<Long>()
    private val show by lazy {
        showId.switchMap {
            SgRoomDatabase.getInstance(getApplication()).sgShow2Helper().getShowLiveData(it)
        }
    }

    data class ShowForUi(
        val show: SgShow2,
        val releaseTime: String?,
        val baseInfo: String,
        val overview: CharSequence,
        val languageData: LanguageTools.LanguageData?,
        val country: String,
        val releaseYear: String?,
        val lastUpdated: String,
        val genres: String,
        val traktRating: String,
        val traktVotes: String,
        val traktUserRating: String
    )

    // Mediator to compute some additional data for the UI in the background.
    val showForUi: MediatorLiveData<ShowForUi?> by lazy {
        MediatorLiveData<ShowForUi?>().apply {
            addSource(show) { show ->
                if (show == null) {
                    showForUi.value = null
                    return@addSource
                }
                viewModelScope.launch(Dispatchers.IO) {
                    // Release time
                    val timeOrNull = TimeTools.getLocalReleaseDayAndTime(application, show)

                    // Basic info
                    val network = show.network
                    val runtime = application.resources.getString(
                        R.string.runtime_minutes,
                        show.runtime.toString()
                    )
                    val baseInfo = "$network\n$runtime"

                    // Language data
                    val languageCode = show.language?.let { LanguageTools.mapLegacyShowCode(it) }
                    val languageData =
                        LanguageTools.getShowLanguageDataFor(application, languageCode)

                    // Overview
                    var overview = show.overview
                    if (TextUtils.isEmpty(overview)) {
                        // no description available, show no translation available message
                        overview = TextTools.textNoTranslation(application, languageCode)
                    }
                    val overviewStyled = TextTools.textWithTmdbSource(application, overview)

                    // country for release time calculation
                    // show "unknown" if country is not supported
                    val country = TimeTools.getCountry(application, show.releaseCountry)

                    val releaseYear = TimeTools.getShowReleaseYear(show.firstRelease)

                    // When the show was last updated by this app
                    val lastUpdated =
                        TimeTools.formatToLocalDateAndTime(application, show.lastUpdatedMs)

                    val genres = TextTools.splitPipeSeparatedStrings(show.genres)

                    val traktRating = TraktTools.buildRatingString(show.ratingGlobal)
                    val traktVotes =
                        TraktTools.buildRatingVotesString(application, show.ratingVotes)
                    val traktUserRating =
                        TraktTools.buildUserRatingString(application, show.ratingUser)

                    withContext(Dispatchers.Main) {
                        showForUi.value =
                            ShowForUi(
                                show,
                                timeOrNull,
                                baseInfo,
                                overviewStyled,
                                languageData,
                                country,
                                releaseYear,
                                lastUpdated,
                                genres,
                                traktRating, traktVotes, traktUserRating
                            )
                    }
                }
            }
        }
    }

    val credits by lazy {
        showId.switchMap {
            liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                emit(TmdbTools2().loadCreditsForShow(application, it))
            }
        }
    }

    /**
     * This currently does not auto-update, it maybe should at some point (add global LiveData).
     */
    val hasAccessToX = MutableLiveData<Boolean>()

    init {
        updateUserStatus()
    }

    fun setShowId(showId: Long) {
        this.showId.value = showId
    }

    fun updateUserStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = hasAccessToX.value
            val newState = Utils.hasAccessToX(getApplication())
            if (currentState != newState) {
                hasAccessToX.postValue(newState)
            }
        }
    }

}