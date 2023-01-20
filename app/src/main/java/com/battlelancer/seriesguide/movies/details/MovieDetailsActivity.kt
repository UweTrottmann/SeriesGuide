package com.battlelancer.seriesguide.movies.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import com.battlelancer.seriesguide.util.ThemeUtils
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
            val f = MovieDetailsFragment.newInstance(tmdbId)
            supportFragmentManager.beginTransaction().add(R.id.content_frame, f).commit()
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
        // loader ids for this activity (mostly used by fragments)
        var LOADER_ID_MOVIE = 100
        var LOADER_ID_MOVIE_TRAILERS = 101

        fun intentMovie(context: Context, movieTmdbId: Int): Intent {
            return Intent(context, MovieDetailsActivity::class.java)
                .putExtra(MovieDetailsFragment.ARG_TMDB_ID, movieTmdbId)
        }
    }
}