// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2025 Uwe Trottmann

package com.battlelancer.seriesguide.shows.overview

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.billing.BillingTools
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgShow2
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.battlelancer.seriesguide.tmdbapi.TmdbTools3
import com.battlelancer.seriesguide.traktapi.TraktTools
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.RatingsTools
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.github.michaelbull.result.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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
        val userNote: String,
        val overview: String?,
        val languageData: LanguageTools.LanguageData?,
        val country: String,
        val releaseYear: String?,
        val lastUpdated: String,
        val genres: String,
        val tmdbRating: String,
        val tmdbVotes: String,
        val traktRating: String,
        val traktVotes: String,
        val traktUserRating: String,
        val trailerVideoId: String?
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
                    val runtime = show.runtime?.let {
                        TimeTools.formatToHoursAndMinutes(application.resources, it)
                    } ?: ""
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

                    // country for release time calculation
                    // show "unknown" if country is not supported
                    val country = TimeTools.getCountry(application, show.releaseCountry)

                    val releaseYear = TimeTools.getShowReleaseYear(show.firstRelease)

                    // When the show was last updated by this app
                    val lastUpdated =
                        TimeTools.formatToLocalDateAndTime(application, show.lastUpdatedMs)

                    val databaseValues = ShowForUi(
                        show,
                        timeOrNull,
                        baseInfo,
                        show.userNoteOrEmpty,
                        overview,
                        languageData,
                        country,
                        releaseYear,
                        lastUpdated,
                        TextTools.splitPipeSeparatedStrings(show.genres),
                        RatingsTools.buildRatingString(show.ratingTmdb),
                        RatingsTools.buildRatingVotesString(application, show.ratingTmdbVotes),
                        RatingsTools.buildRatingString(show.ratingTrakt),
                        RatingsTools.buildRatingVotesString(application, show.ratingTraktVotes),
                        TraktTools.buildUserRatingString(application, show.ratingUser),
                        null
                    )

                    withContext(Dispatchers.Main) {
                        showForUi.value = databaseValues
                    }

                    // Do network request after returning data from the database
                    val showTmdbId = show.tmdbId
                    if (showTmdbId != null && languageData != null) {
                        TmdbTools3.getShowTrailerYoutubeId(
                            application,
                            show.tmdbId,
                            languageData.languageCode
                        ).onSuccess {
                            if (it != null) {
                                withContext(Dispatchers.Main) {
                                    showForUi.value = databaseValues.copy(trailerVideoId = it)
                                }
                            }
                        }
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
     * Observe unlock state. This is useful if a user is advertised the subscription, purchases it
     * and comes back to the screen using this to find features unlocked.
     */
    val hasAllFeatures = MutableStateFlow(BillingTools.unlockStateReadOnly.value.isUnlockAll)

    init {
        viewModelScope.launch {
            BillingTools.unlockStateReadOnly.collect {
                hasAllFeatures.value = it.isUnlockAll
            }
        }
    }

    fun setShowId(showId: Long) {
        this.showId.value = showId
    }

}