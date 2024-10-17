// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ActivityMoviesSearchBinding
import com.battlelancer.seriesguide.movies.search.MoviesSearchActivity.Companion.intentLink
import com.battlelancer.seriesguide.movies.search.MoviesSearchActivity.Companion.intentSearch
import com.battlelancer.seriesguide.shows.search.discover.ShowsDiscoverPagingActivity.Companion.intentLink
import com.battlelancer.seriesguide.shows.search.discover.ShowsDiscoverPagingActivity.Companion.intentSearch
import com.battlelancer.seriesguide.shows.search.similar.SimilarShowsActivity
import com.battlelancer.seriesguide.shows.search.similar.SimilarShowsFragment
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import com.battlelancer.seriesguide.util.TaskManager
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.commitReorderingAllowed

/**
 * Hosts [ShowsDiscoverPagingFragment] determined by [DiscoverShowsLink].
 *
 * If launched without extras or [intentSearch] the search bar is always shown and initially focused.
 * If launched with [intentLink] the search bar can be shown with a menu item and hidden by
 * going up or back.
 */
class ShowsDiscoverPagingActivity : BaseMessageActivity(), AddShowDialogFragment.OnAddShowListener {

    // Re-using layout of movies as filter chips are currently identical
    lateinit var binding: ActivityMoviesSearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoviesSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeUtils.configureForEdgeToEdge(binding.root)
        ThemeUtils.configureAppBarForContentBelow(this)
        setupActionBar()

        // Change the scrolling view the AppBarLayout should use to determine if it should lift.
        // This is required so the AppBarLayout does not flicker its background when scrolling.
        binding.sgAppBarLayout.liftOnScrollTargetViewId =
            ShowsDiscoverPagingFragment.liftOnScrollTargetViewId

        if (savedInstanceState == null) {
            val link = DiscoverShowsLink.fromId(
                intent.getIntExtra(EXTRA_LINK, DiscoverShowsLink.NO_LINK_ID)
            )
            val query = getQueryFromIntent()
            supportFragmentManager.commitReorderingAllowed {
                add(
                    R.id.containerMoviesSearchFragment,
                    ShowsDiscoverPagingFragment.newInstance(link, query)
                )
            }
        }

        SimilarShowsFragment.displaySimilarShowsEventLiveData.observe(this) {
            startActivity(SimilarShowsActivity.intent(this, it.tmdbId, it.title))
        }
    }

    private fun getQueryFromIntent(): String? {
        val intent = intent ?: return null
        if (intent.action == Intent.ACTION_SEND) {
            // text shared from other apps
            if ("text/plain" == intent.type) {
                return intent.getStringExtra(Intent.EXTRA_TEXT)
            }
        }
        return intent.getStringExtra(EXTRA_QUERY)
    }

    override fun onAddShow(show: SearchResult) {
        TaskManager.performAddTask(this, show)
    }

    companion object {
        private const val EXTRA_LINK = "link"
        private const val EXTRA_QUERY = "query"

        fun intentSearch(context: Context): Intent =
            Intent(context, ShowsDiscoverPagingActivity::class.java)

        fun intentSearch(context: Context, query: String?): Intent {
            val intent = intentSearch(context)
            if (query != null) {
                intent.putExtra(EXTRA_QUERY, query)
            }
            return intent
        }

        fun intentLink(context: Context, link: DiscoverShowsLink): Intent =
            Intent(context, ShowsDiscoverPagingActivity::class.java)
                .putExtra(EXTRA_LINK, link.id)

    }

}