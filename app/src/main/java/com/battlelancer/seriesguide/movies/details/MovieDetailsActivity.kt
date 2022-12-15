package com.battlelancer.seriesguide.movies.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import com.readystatesoftware.systembartint.SystemBarTintManager
import com.uwetrottmann.androidutils.AndroidUtils

/**
 * Hosts a [MovieDetailsFragment] displaying details about the movie defined by the given TMDb
 * id intent extra.
 */
class MovieDetailsActivity : BaseMessageActivity() {

    lateinit var systemBarTintManager: SystemBarTintManager

    override fun getCustomTheme(): Int {
        return R.style.Theme_SeriesGuide_DayNight_Immersive
    }

    override fun configureEdgeToEdge() {
        // Do nothing.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // support transparent status bar
        if (AndroidUtils.isMarshmallowOrHigher) {
            findViewById<View>(android.R.id.content).systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }

        setContentView(R.layout.activity_movie)
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

        setupViews()

        if (savedInstanceState == null) {
            val f = MovieDetailsFragment.newInstance(tmdbId)
            supportFragmentManager.beginTransaction().add(R.id.content_frame, f).commit()
        }
    }

    private fun setupViews() {
        // fix padding with translucent (K+)/transparent (M+) status bar
        // warning: pre-M status bar not always translucent (e.g. Nexus 10)
        // (using fitsSystemWindows would not work correctly with multiple views)
        val systemBarTintManager = SystemBarTintManager(this)
            .also { systemBarTintManager = it }
        val config = systemBarTintManager.config
        val insetTop = if (AndroidUtils.isMarshmallowOrHigher) {
            config.statusBarHeight // transparent status bar
        } else {
            config.getPixelInsetTop(false) // translucent status bar
        }
        val actionBarToolbar = findViewById<ViewGroup>(R.id.sgToolbar)
        val layoutParams = actionBarToolbar.layoutParams as MarginLayoutParams
        layoutParams.setMargins(
            layoutParams.leftMargin, layoutParams.topMargin + insetTop,
            layoutParams.rightMargin, layoutParams.bottomMargin
        )
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