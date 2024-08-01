// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.movies.collection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.replace
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseActivity
import com.battlelancer.seriesguide.ui.SinglePaneActivity
import com.battlelancer.seriesguide.util.commitReorderingAllowed

/**
 * Hosts a [MovieCollectionFragment], contains a large top app bar that lifts on scroll,
 * displays close navigation indicator.
 */
class MovieCollectionActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = SinglePaneActivity.onCreateFor(this)
        binding.sgAppBarLayout.sgAppBarLayout.liftOnScrollTargetViewId =
            MovieCollectionFragment.liftOnScrollTargetViewId
        setupActionBar()

        val title = intent.getStringExtra(EXTRA_TITLE)
            ?: getString(R.string.title_similar_movies)
        setTitle(title)

        val collectionId = intent.getIntExtra(EXTRA_COLLECTION_ID, 0)
        if (collectionId <= 0) {
            throw IllegalArgumentException("EXTRA_COLLECTION_ID must be positive, but was $collectionId")
        }

        if (savedInstanceState == null) {
            supportFragmentManager.commitReorderingAllowed {
                replace<MovieCollectionFragment>(
                    R.id.content_frame,
                    args = MovieCollectionFragment.buildArgs(collectionId)
                )
            }
        }
    }

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_clear_24dp)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    companion object {
        private const val EXTRA_COLLECTION_ID = "COLLECTION_ID"
        private const val EXTRA_TITLE = "ARG_TITLE"

        fun intent(context: Context, collectionId: Int, title: String): Intent =
            Intent(context, MovieCollectionActivity::class.java)
                .putExtra(EXTRA_COLLECTION_ID, collectionId)
                .putExtra(EXTRA_TITLE, title)
    }
}
