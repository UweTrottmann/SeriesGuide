// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.movies.similar

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.shows.search.similar.SimilarShowsFragment
import com.battlelancer.seriesguide.ui.BaseSimilarActivity

class SimilarMoviesActivity : BaseSimilarActivity() {

    override val liftOnScrollTargetViewId: Int = SimilarShowsFragment.liftOnScrollTargetViewId
    override val titleStringRes: Int = R.string.title_similar_movies
    override fun createFragment(tmdbId: Int, title: String?): Fragment =
        SimilarMoviesFragment.newInstance(tmdbId, title)

    companion object {
        fun intent(context: Context, movieTmdbId: Int, title: String?): Intent {
            return Intent(context, SimilarMoviesActivity::class.java)
                .putExtras(movieTmdbId, title)
        }
    }

}