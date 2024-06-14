// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ActivityTraktShowsBinding
import com.battlelancer.seriesguide.shows.search.similar.SimilarShowsActivity
import com.battlelancer.seriesguide.shows.search.similar.SimilarShowsFragment
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import com.battlelancer.seriesguide.util.TaskManager
import com.battlelancer.seriesguide.util.ThemeUtils

/**
 * Hosts [TraktAddFragment] configured by [DiscoverShowsLink].
 */
class ShowsTraktActivity : BaseMessageActivity(), AddShowDialogFragment.OnAddShowListener {

    lateinit var binding: ActivityTraktShowsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTraktShowsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeUtils.configureForEdgeToEdge(binding.root)

        val link =
            DiscoverShowsLink.fromId(intent.getIntExtra(EXTRA_LINK, DiscoverShowsLink.NO_LINK_ID))!!

        // Change the scrolling view the AppBarLayout should use to determine if it should lift.
        // This is required so the AppBarLayout does not flicker its background when scrolling.
        binding.sgAppBarLayout.sgAppBarLayout.liftOnScrollTargetViewId =
            TraktAddFragment.liftOnScrollTargetViewId

        setupActionBar(link)

        if (savedInstanceState == null) {
            val fragment = TraktAddFragment.newInstance(link)
            supportFragmentManager
                .beginTransaction()
                .add(R.id.containerTraktShowsFragment, fragment)
                .commit()
        }

        SimilarShowsFragment.displaySimilarShowsEventLiveData.observe(this) {
            startActivity(SimilarShowsActivity.intent(this, it.tmdbId, it.title))
        }
    }

    fun setupActionBar(link: DiscoverShowsLink) {
        setupActionBar()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(link.titleRes)
    }

    override fun onAddShow(show: SearchResult) {
        TaskManager.getInstance().performAddTask(this, show)
    }

    companion object {
        const val EXTRA_LINK = "LINK"
        const val TRAKT_BASE_LOADER_ID = 200

        @JvmStatic
        fun intent(context: Context, link: DiscoverShowsLink): Intent {
            return Intent(context, ShowsTraktActivity::class.java).putExtra(EXTRA_LINK, link.id)
        }
    }

}