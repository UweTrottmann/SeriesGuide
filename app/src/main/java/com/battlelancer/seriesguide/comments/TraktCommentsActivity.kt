// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.comments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseActivity
import com.battlelancer.seriesguide.ui.SinglePaneActivity
import timber.log.Timber

/**
 * Hosts a [TraktCommentsFragment].
 */
class TraktCommentsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = SinglePaneActivity.onCreateFor(this)
        binding.sgAppBarLayout.sgAppBarLayout.liftOnScrollTargetViewId =
            TraktCommentsFragment.liftOnScrollTargetViewId

        if (intent.extras == null) {
            finish()
            Timber.e("Finishing, missing required extras.")
            return
        }

        setupActionBar()

        if (savedInstanceState == null) {
            val f = TraktCommentsFragment()
                .apply { arguments = intent.extras }
            supportFragmentManager.beginTransaction()
                .add(R.id.content_frame, f)
                .commit()
        }
    }

    override fun setupActionBar() {
        super.setupActionBar()
        val commentsTitle: String? = intent.getStringExtra(EXTRA_TITLE)
        title = "${getString(R.string.comments)} $commentsTitle"
        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_clear_24dp)
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.comments)
            subtitle = commentsTitle
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {

        const val LOADER_ID_COMMENTS = 100
        private const val EXTRA_TITLE = "title"

        /**
         * Display comments of an episode.
         */
        fun intentEpisode(context: Context, title: String?, episodeId: Long): Intent {
            return Intent(context, TraktCommentsActivity::class.java)
                .putExtra(TraktCommentsFragment.InitBundle.EPISODE_ID, episodeId)
                .putExtra(EXTRA_TITLE, title)
        }

        /**
         * Display comments of a show.
         */
        fun intentShow(context: Context, title: String?, showId: Long): Intent {
            return Intent(context, TraktCommentsActivity::class.java)
                .putExtra(TraktCommentsFragment.InitBundle.SHOW_ID, showId)
                .putExtra(EXTRA_TITLE, title)
        }

        /**
         * Display comments of a movie.
         */
        fun intentMovie(context: Context, title: String?, movieTmdbId: Int): Intent {
            return Intent(context, TraktCommentsActivity::class.java)
                .putExtra(TraktCommentsFragment.InitBundle.MOVIE_TMDB_ID, movieTmdbId)
                .putExtra(EXTRA_TITLE, title)
        }
    }
}
