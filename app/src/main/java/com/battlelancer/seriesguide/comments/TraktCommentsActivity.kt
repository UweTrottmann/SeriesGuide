// SPDX-License-Identifier: Apache-2.0
// Copyright 2012-2024 Uwe Trottmann

package com.battlelancer.seriesguide.comments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.add
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseActivity
import com.battlelancer.seriesguide.ui.SinglePaneActivity
import com.battlelancer.seriesguide.util.commitReorderingAllowed
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
            supportFragmentManager.commitReorderingAllowed {
                add<TraktCommentsFragment>(R.id.content_frame, args = intent.extras)
            }
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

    companion object {

        const val LOADER_ID_COMMENTS = 100
        private const val EXTRA_TITLE = "title"

        /**
         * Display comments of an episode.
         */
        fun intentEpisode(context: Context, title: String?, episodeId: Long): Intent {
            check(episodeId > 0)
            return Intent(context, TraktCommentsActivity::class.java)
                .putExtra(TraktCommentsFragment.InitBundle.EPISODE_ID, episodeId)
                .putExtra(EXTRA_TITLE, title)
        }

        /**
         * Display comments of a show.
         */
        fun intentShow(context: Context, title: String?, showId: Long): Intent {
            check(showId > 0)
            return Intent(context, TraktCommentsActivity::class.java)
                .putExtra(TraktCommentsFragment.InitBundle.SHOW_ID, showId)
                .putExtra(EXTRA_TITLE, title)
        }

        /**
         * Display comments of a movie.
         */
        fun intentMovie(context: Context, title: String?, movieTmdbId: Int): Intent {
            check(movieTmdbId > 0)
            return Intent(context, TraktCommentsActivity::class.java)
                .putExtra(TraktCommentsFragment.InitBundle.MOVIE_TMDB_ID, movieTmdbId)
                .putExtra(EXTRA_TITLE, title)
        }
    }
}
