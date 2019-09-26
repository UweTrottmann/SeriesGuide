package com.battlelancer.seriesguide.ui.comments

import android.os.Bundle
import android.view.MenuItem
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseActivity
import timber.log.Timber

/**
 * Hosts a [TraktCommentsFragment].
 */
class TraktCommentsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_singlepane)

        if (intent.extras == null) {
            finish()
            Timber.e("Finishing, missing required extras.")
            return
        }

        setupActionBar()

        if (savedInstanceState == null) {
            val f = TraktCommentsFragment().apply {
                arguments = intent.extras
            }
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
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.comments)
            subtitle = commentsTitle
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
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
        @JvmStatic
        fun createInitBundleEpisode(title: String?, episodeTvdbId: Int): Bundle {
            val extras = Bundle()
            extras.putInt(TraktCommentsFragment.InitBundle.EPISODE_TVDB_ID, episodeTvdbId)
            extras.putString(EXTRA_TITLE, title)
            return extras
        }

        /**
         * Display comments of a show.
         */
        fun createInitBundleShow(title: String?, showTvdbId: Int): Bundle {
            val extras = Bundle()
            extras.putInt(TraktCommentsFragment.InitBundle.SHOW_TVDB_ID, showTvdbId)
            extras.putString(EXTRA_TITLE, title)
            return extras
        }

        /**
         * Display comments of a movie.
         */
        fun createInitBundleMovie(title: String?, movieTmdbId: Int): Bundle {
            val extras = Bundle()
            extras.putInt(TraktCommentsFragment.InitBundle.MOVIE_TMDB_ID, movieTmdbId)
            extras.putString(EXTRA_TITLE, title)
            return extras
        }
    }
}
