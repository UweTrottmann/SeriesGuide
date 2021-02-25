package com.battlelancer.seriesguide.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import com.battlelancer.seriesguide.util.TaskManager

class TraktShowsActivity : BaseMessageActivity(), AddShowDialogFragment.OnAddShowListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trakt_shows)

        val link = TraktShowsLink.fromId(intent.getIntExtra(EXTRA_LINK, -1))

        setupActionBar(link)

        if (savedInstanceState == null) {
            val fragment = when (link) {
                TraktShowsLink.POPULAR -> ShowsPopularFragment()
                else -> TraktAddFragment.newInstance(link)
            }
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.containerTraktShowsFragment, fragment)
                    .commit()
        }

        SimilarShowsFragment.displaySimilarShowsEventLiveData.observe(this, {
            startActivity(SimilarShowsActivity.intent(this, it.tmdbId, it.title))
        })
    }

    fun setupActionBar(link: TraktShowsLink) {
        setupActionBar()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(link.titleRes)
    }

    override fun onAddShow(show: SearchResult) {
        TaskManager.getInstance().performAddTask(this, show)
    }

    companion object {
        const val EXTRA_LINK = "LINK"

        @JvmStatic
        fun intent(context: Context, link: TraktShowsLink): Intent {
            return Intent(context, TraktShowsActivity::class.java).putExtra(EXTRA_LINK, link.id)
        }
    }

}