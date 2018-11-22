package com.battlelancer.seriesguide.ui.episodes

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class EpisodesViewModel(application: Application) : AndroidViewModel(application) {

    val episodeCountLiveData = EpisodeCountLiveData(application)

}