// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.similar

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseSimilarActivity

class SimilarShowsActivity : BaseSimilarActivity() {

    override val liftOnScrollTargetViewId: Int = SimilarShowsFragment.liftOnScrollTargetViewId
    override val titleStringRes: Int = R.string.title_similar_shows
    override fun createFragment(tmdbId: Int, title: String): Fragment =
        SimilarShowsFragment.newInstance(tmdbId, title)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Can display similar shows from the add dialog, so build a stack of them.
        SimilarShowsFragment.displaySimilarShowsEventLiveData.observe(this) {
            addFragment(it.tmdbId, it.title, true)
        }
    }

    companion object {
        fun intent(context: Context, showTmdbId: Int, showTitle: String): Intent {
            return Intent(context, SimilarShowsActivity::class.java)
                .putExtras(showTmdbId, showTitle)
        }
    }

}