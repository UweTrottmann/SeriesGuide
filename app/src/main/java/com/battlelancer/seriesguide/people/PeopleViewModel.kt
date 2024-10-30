// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2024 Uwe Trottmann

package com.battlelancer.seriesguide.people

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PeopleViewModel(
    application: Application,
    private val tmdbId: Int,
    private val mediaType: PeopleActivity.MediaType,
    private val peopleType: PeopleActivity.PeopleType
) : AndroidViewModel(application) {

    val credits = MutableLiveData<List<Person>>()

    init {
        loadCredits()
    }

    fun loadCredits() {
        viewModelScope.launch(Dispatchers.IO) {
            val newCredits = if (mediaType == PeopleActivity.MediaType.MOVIE) {
                TmdbTools2().getCreditsForMovie(getApplication(), tmdbId)
            } else {
                TmdbTools2().getCreditsForShow(getApplication(), tmdbId)
            }

            val castOrCrewOrNull = if (peopleType == PeopleActivity.PeopleType.CAST) {
                newCredits?.cast
            } else {
                newCredits?.crew
            }

            credits.postValue(castOrCrewOrNull ?: emptyList())
        }
    }

}

class PeopleViewModelFactory(
    private val application: Application,
    private val tmdbId: Int,
    private val mediaType: PeopleActivity.MediaType,
    private val peopleType: PeopleActivity.PeopleType
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PeopleViewModel(application, tmdbId, mediaType, peopleType) as T
    }

}
