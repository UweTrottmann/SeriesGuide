// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.comments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TraktCommentsViewModel(application: Application) : AndroidViewModel(application) {

    fun postShowComment(showId: Long, comment: String, isSpoiler: Boolean) {
        SgApp.coroutineScope.launch(Dispatchers.IO) {
            val showTraktId = SgApp.getServicesComponent(getApplication())
                .showTools()
                .getShowTraktId(showId)
            if (showTraktId != null) {
                SgApp.getServicesComponent(getApplication()).trakt().sgComments()
                    .postShowComment(showTraktId, comment, isSpoiler)
            }
        }
    }

    fun postEpisodeComment(episodeId: Long, comment: String, isSpoiler: Boolean) {
        SgApp.coroutineScope.launch(Dispatchers.IO) {
            val episodeTmdbId = SgRoomDatabase.getInstance(getApplication())
                .sgEpisode2Helper().getEpisodeTmdbId(episodeId)
            if (episodeTmdbId > 0) {
                SgApp.getServicesComponent(getApplication()).trakt().sgComments()
                    .postEpisodeComment(episodeTmdbId, comment, isSpoiler)
            }
        }
    }

    fun postMovieComment(movieTmdbId: Int, comment: String, isSpoiler: Boolean) {
        SgApp.coroutineScope.launch(Dispatchers.IO) {
            SgApp.getServicesComponent(getApplication()).trakt().sgComments()
                .postMovieComment(movieTmdbId, comment, isSpoiler)
        }
    }

}