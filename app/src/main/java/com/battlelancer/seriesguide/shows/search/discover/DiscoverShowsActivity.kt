// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ActivityDiscoverShowsBinding
import com.battlelancer.seriesguide.shows.search.popular.ShowsPopularFragment
import com.battlelancer.seriesguide.shows.search.similar.SimilarShowsActivity
import com.battlelancer.seriesguide.shows.search.similar.SimilarShowsFragment
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import com.battlelancer.seriesguide.util.TaskManager
import com.battlelancer.seriesguide.util.ThemeUtils

class DiscoverShowsActivity : BaseMessageActivity(), AddShowDialogFragment.OnAddShowListener {

    lateinit var binding: ActivityDiscoverShowsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiscoverShowsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeUtils.configureForEdgeToEdge(binding.root)

        val link = DiscoverShowsLink.fromId(intent.getIntExtra(EXTRA_LINK, -1))

        // Change the scrolling view the AppBarLayout should use to determine if it should lift.
        // This is required so the AppBarLayout does not flicker its background when scrolling.
        binding.sgAppBarLayout.liftOnScrollTargetViewId = when (link) {
            DiscoverShowsLink.POPULAR -> ShowsPopularFragment.liftOnScrollTargetViewId
            else -> TraktAddFragment.liftOnScrollTargetViewId
        }
        // Filters currently only supported for the popular screen
        binding.scrollViewTraktShowsChips.isVisible = link == DiscoverShowsLink.POPULAR
        setupActionBar(link)

        if (savedInstanceState == null) {
            val fragment = when (link) {
                DiscoverShowsLink.POPULAR -> ShowsPopularFragment()
                else -> TraktAddFragment.newInstance(link)
            }
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

        @JvmStatic
        fun intent(context: Context, link: DiscoverShowsLink): Intent {
            return Intent(context, DiscoverShowsActivity::class.java).putExtra(EXTRA_LINK, link.id)
        }
    }

}