package com.battlelancer.seriesguide.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.Observer
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseActivity
import com.battlelancer.seriesguide.util.TaskManager

class SimilarShowsActivity : BaseActivity(), AddShowDialogFragment.OnAddShowListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_singlepane)
        setupActionBar()

        val showTvdbId = intent.getIntExtra(EXTRA_SHOW_THETVDB_ID, 0)
        if (showTvdbId <= 0) {
            finish()
            return
        }

        if (savedInstanceState == null) {
            addFragmentWithSimilarShows(showTvdbId)
        }

        SimilarShowsFragment.displaySimilarShowsEventLiveData.observe(this, Observer {
            addFragmentWithSimilarShows(it, true)
        })
    }

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_clear_24dp)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.title_similar_shows)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                // Go to the last activity.
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onAddShow(show: SearchResult) {
        TaskManager.getInstance().performAddTask(this, show)
    }

    private fun addFragmentWithSimilarShows(showTvdbId: Int, addToBackStack: Boolean = false) {
        val fragment = SimilarShowsFragment.newInstance(showTvdbId)
        supportFragmentManager.beginTransaction().apply {
            if (addToBackStack) {
                replace(R.id.content_frame, fragment)
                addToBackStack(null)
            } else {
                add(R.id.content_frame, fragment)
            }
        }.commit()
    }

    companion object {
        private const val EXTRA_SHOW_THETVDB_ID = "EXTRA_SHOW_THETVDB_ID"

        @JvmStatic
        fun intent(context: Context, showTvdbId: Int): Intent {
            return Intent(context, SimilarShowsActivity::class.java).putExtra(
                EXTRA_SHOW_THETVDB_ID,
                showTvdbId
            )
        }
    }

}