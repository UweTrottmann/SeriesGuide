package com.battlelancer.seriesguide.ui.episodes

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class EpisodesActivityViewModelFactory(
    private val application: Application,
    private val episodeTvdbId: Int,
    private val seasonTvdbId: Int
): ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EpisodesActivityViewModel(application, episodeTvdbId, seasonTvdbId) as T
    }

}