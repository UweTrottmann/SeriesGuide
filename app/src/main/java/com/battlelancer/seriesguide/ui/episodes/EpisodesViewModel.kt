package com.battlelancer.seriesguide.ui.episodes

import android.app.Application
import android.arch.lifecycle.AndroidViewModel

class EpisodesViewModel(application: Application) : AndroidViewModel(application) {

    val episodeCountLiveData = EpisodeCountLiveData(application)

}