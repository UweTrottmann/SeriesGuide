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
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.commitReorderingAllowed

/**
 * Hosts [TraktAddFragment] configured by [DiscoverShowsLink].
 */
class ShowsTraktActivity : BaseMessageActivity() {

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
            val traktListType = when (link) {
                DiscoverShowsLink.WATCHED -> TraktAddLoader.Type.WATCHED
                DiscoverShowsLink.COLLECTION -> TraktAddLoader.Type.COLLECTION
                DiscoverShowsLink.WATCHLIST -> TraktAddLoader.Type.WATCHLIST
                else -> throw IllegalArgumentException("Link $link is not supported")
            }
            supportFragmentManager.commitReorderingAllowed {
                add(R.id.containerTraktShowsFragment, TraktAddFragment.newInstance(traktListType))
            }
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

    companion object {
        const val EXTRA_LINK = "LINK"
        const val TRAKT_BASE_LOADER_ID = 200

        @JvmStatic
        fun intent(context: Context, link: DiscoverShowsLink): Intent {
            return Intent(context, ShowsTraktActivity::class.java).putExtra(EXTRA_LINK, link.id)
        }
    }

}