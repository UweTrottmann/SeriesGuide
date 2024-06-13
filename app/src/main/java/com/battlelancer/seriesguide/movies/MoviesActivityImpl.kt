// SPDX-License-Identifier: Apache-2.0
// Copyright 2013-2024 Uwe Trottmann
// Copyright 2017 Christophe Beyls

package com.battlelancer.seriesguide.movies

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ActivityMoviesBinding
import com.battlelancer.seriesguide.movies.MoviesSettings.getLastMoviesTabPosition
import com.battlelancer.seriesguide.movies.search.MoviesSearchActivity
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.BaseTopActivity
import com.battlelancer.seriesguide.ui.TabStripAdapter
import com.battlelancer.seriesguide.util.ThemeUtils
import com.google.android.material.appbar.AppBarLayout

/**
 * Movie section of the app, displays tabs to discover movies, history (if connected to Trakt),
 * watchlist, collection and watched movies.
 */
open class MoviesActivityImpl : BaseTopActivity() {

    private lateinit var binding: ActivityMoviesBinding
    private lateinit var tabsAdapter: TabStripAdapter
    private lateinit var pageChangeListener: MoviesPageChangeListener
    private var showHistoryTab = false

    private val viewModel by viewModels<MoviesActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoviesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeUtils.configureForEdgeToEdge(binding.root)
        ThemeUtils.configureAppBarForContentBelow(this)
        setupActionBar()
        setupBottomNavigation(R.id.navigation_item_movies)
        setupViews(savedInstanceState)
        setupSyncProgressBar(R.id.sgProgressBar)

        if (savedInstanceState != null) {
            postponeEnterTransition()
            // Allow the adapters to repopulate during the next layout pass
            // before starting the transition animation
            binding.viewPagerMovies.post { startPostponedEnterTransition() }
        }
    }

    private fun setupViews(savedInstanceState: Bundle?) {
        val tabLayout = binding.sgAppBarLayout.sgTabLayout
        tabLayout.setOnTabClickListener { position: Int ->
            if (binding.viewPagerMovies.currentItem == position) {
                scrollSelectedTabToTop()
            }
        }
        showHistoryTab = TraktCredentials.get(this@MoviesActivityImpl).hasCredentials()
        pageChangeListener =
            MoviesPageChangeListener(binding.sgAppBarLayout.sgAppBarLayout, showHistoryTab)
        tabLayout.setOnPageChangeListener(pageChangeListener)
        tabsAdapter = TabStripAdapter(this, binding.viewPagerMovies, tabLayout)
            .apply {
                // discover
                addTab(R.string.title_discover, MoviesDiscoverFragment::class.java, null)
                // Trakt-only tabs should only be visible if connected
                if (showHistoryTab) {
                    // history tab
                    addTab(R.string.user_stream, MoviesHistoryFragment::class.java, null)
                }
                // watchlist
                addTab(R.string.movies_watchlist, MoviesWatchListFragment::class.java, null)
                // collection
                addTab(R.string.movies_collection, MoviesCollectionFragment::class.java, null)
                // watched
                addTab(R.string.movies_watched, MoviesWatchedFragment::class.java, null)
                notifyTabsChanged()
            }
        if (savedInstanceState == null) {
            binding.viewPagerMovies.setCurrentItem(getLastMoviesTabPosition(this), false)
        }
    }

    private fun scrollSelectedTabToTop() {
        viewModel.scrollTabToTop(binding.viewPagerMovies.currentItem, showHistoryTab)
    }

    override fun onSelectedCurrentNavItem() {
        scrollSelectedTabToTop()
    }

    override fun onStart() {
        super.onStart()
        maybeAddNowTab()
    }

    private fun maybeAddNowTab() {
        val currentTabCount = tabsAdapter.itemCount
        showHistoryTab = TraktCredentials.get(this).hasCredentials()
        pageChangeListener.isShowingNowTab = showHistoryTab
        if (showHistoryTab && currentTabCount != TAB_COUNT_WITH_TRAKT) {
            tabsAdapter.addTab(
                R.string.user_stream, MoviesHistoryFragment::class.java, null, TAB_POSITION_TRAKT_HISTORY
            )
            // update tabs
            tabsAdapter.notifyTabsChanged()
        }
    }

    override fun onPause() {
        super.onPause()
        MoviesSettings.saveLastMoviesTabPosition(this, binding.viewPagerMovies.currentItem)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.movies_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_action_movies_search -> {
                startActivity(MoviesSearchActivity.intentSearch(this))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override val snackbarParentView: View
        get() = binding.coordinatorLayoutMovies

    /**
     * Page change listener which sets the scroll view of the current visible tab as the lift on
     * scroll target view of the app bar.
     */
    class MoviesPageChangeListener(
        private val appBarLayout: AppBarLayout,
        var isShowingNowTab: Boolean
    ) : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrollStateChanged(arg0: Int) {}
        override fun onPageScrolled(arg0: Int, arg1: Float, arg2: Int) {}

        override fun onPageSelected(position: Int) {
            // Change the scrolling view the AppBarLayout should use to determine if it should lift.
            // This is required so the AppBarLayout does not flicker its background when scrolling.
            val liftOnScrollTarget = if (isShowingNowTab) {
                when (position) {
                    TAB_POSITION_DISCOVER -> MoviesDiscoverFragment.liftOnScrollTargetViewId
                    TAB_POSITION_TRAKT_HISTORY -> MoviesHistoryFragment.liftOnScrollTargetViewId
                    TAB_POSITION_WATCHLIST_WITH_HISTORY -> MoviesWatchListFragment.liftOnScrollTargetViewId
                    TAB_POSITION_COLLECTION_WITH_HISTORY -> MoviesCollectionFragment.liftOnScrollTargetViewId
                    TAB_POSITION_WATCHED_WITH_HISTORY -> MoviesWatchedFragment.liftOnScrollTargetViewId
                    else -> return
                }
            } else {
                when (position) {
                    TAB_POSITION_DISCOVER -> MoviesDiscoverFragment.liftOnScrollTargetViewId
                    TAB_POSITION_WATCHLIST_DEFAULT -> MoviesWatchListFragment.liftOnScrollTargetViewId
                    TAB_POSITION_COLLECTION_DEFAULT -> MoviesCollectionFragment.liftOnScrollTargetViewId
                    TAB_POSITION_WATCHED_DEFAULT -> MoviesWatchedFragment.liftOnScrollTargetViewId
                    else -> return
                }
            }
            appBarLayout.liftOnScrollTargetViewId = liftOnScrollTarget
        }
    }

    companion object {
        const val NOW_TRAKT_USER_LOADER_ID = 101
        const val NOW_TRAKT_FRIENDS_LOADER_ID = 102

        const val TAB_POSITION_DISCOVER = 0
        const val TAB_POSITION_WATCHLIST_DEFAULT = 1
        const val TAB_POSITION_COLLECTION_DEFAULT = 2
        const val TAB_POSITION_WATCHED_DEFAULT = 3
        const val TAB_POSITION_TRAKT_HISTORY = 1
        const val TAB_POSITION_WATCHLIST_WITH_HISTORY = 2
        const val TAB_POSITION_COLLECTION_WITH_HISTORY = 3
        const val TAB_POSITION_WATCHED_WITH_HISTORY = 4

        private const val TAB_COUNT_WITH_TRAKT = 5
    }
}