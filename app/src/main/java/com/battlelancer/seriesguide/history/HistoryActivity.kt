package com.battlelancer.seriesguide.history

import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.shows.search.discover.AddShowDialogFragment.OnAddShowListener
import com.battlelancer.seriesguide.shows.search.discover.SearchResult
import com.battlelancer.seriesguide.ui.BaseActivity
import com.battlelancer.seriesguide.util.TaskManager
import timber.log.Timber

/**
 * Displays history of watched episodes or movies.
 */
class HistoryActivity : BaseActivity(), OnAddShowListener {

    interface InitBundle {
        companion object {
            const val HISTORY_TYPE = BuildConfig.APPLICATION_ID + ".historytype"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_singlepane)
        setupActionBar()

        if (savedInstanceState == null) {
            val historyType = intent.getIntExtra(InitBundle.HISTORY_TYPE, -1)
            val f: Fragment = when (historyType) {
                DISPLAY_EPISODE_HISTORY -> {
                    UserEpisodeStreamFragment()
                }
                DISPLAY_MOVIE_HISTORY -> {
                    UserMovieStreamFragment()
                }
                else -> {
                    // default to episode history
                    Timber.w("onCreate: did not specify a valid HistoryType in the launch intent.")
                    UserEpisodeStreamFragment()
                }
            }
            f.arguments = intent.extras
            supportFragmentManager.beginTransaction()
                .add(R.id.content_frame, f)
                .commit()
        }
    }

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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

    /**
     * Called if the user adds a show from a trakt stream fragment.
     */
    override fun onAddShow(show: SearchResult) {
        TaskManager.getInstance().performAddTask(this, show)
    }

    companion object {
        const val EPISODES_LOADER_ID = 100
        const val MOVIES_LOADER_ID = 101
        const val DISPLAY_EPISODE_HISTORY = 0
        const val DISPLAY_MOVIE_HISTORY = 1
    }
}