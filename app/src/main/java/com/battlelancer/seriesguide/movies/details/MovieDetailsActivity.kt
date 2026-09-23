// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2013 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.movies.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.commitReorderingAllowed
import com.google.android.material.appbar.AppBarLayout

/**
 * Hosts a [MovieDetailsFragment] displaying details about the movie defined by the given TMDb
 * id intent extra.
 */
class MovieDetailsActivity : BaseMessageActivity() {

    lateinit var sgAppBarLayout: AppBarLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie)
        ThemeUtils.configureForEdgeToEdge(findViewById(R.id.rootLayoutMovieActivity))
        sgAppBarLayout = findViewById(R.id.sgAppBarLayout)
        setupActionBar()

        if (intent.extras == null) {
            finish()
            return
        }
        val tmdbId = intent.extras!!.getInt(MovieDetailsFragment.ARG_TMDB_ID)
        if (tmdbId == 0) {
            finish()
            return
        }

        if (savedInstanceState == null) {
            supportFragmentManager.commitReorderingAllowed {
                add(R.id.content_frame, MovieDetailsFragment.newInstance(tmdbId))
            }
        }
    }

    override fun setupActionBar() {
        super.setupActionBar()
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowTitleEnabled(false)
        }
    }

    companion object {
        fun intentMovie(context: Context, movieTmdbId: Int): Intent {
            return Intent(context, MovieDetailsActivity::class.java)
                .putExtra(MovieDetailsFragment.ARG_TMDB_ID, movieTmdbId)
        }
    }
}