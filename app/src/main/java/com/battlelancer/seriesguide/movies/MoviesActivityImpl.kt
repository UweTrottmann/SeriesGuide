package com.battlelancer.seriesguide.movies

import android.content.Intent
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
 * Movie section of the app, displays various movie tabs.
 */
open class MoviesActivityImpl : BaseTopActivity() {

    private lateinit var binding: ActivityMoviesBinding
    private lateinit var tabsAdapter: TabStripAdapter
    private lateinit var pageChangeListener: MoviesPageChangeListener
    private var showNowTab = false

    private val viewModel by viewModels<MoviesActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoviesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeUtils.configureAppBarForContentBelow(this)
        ThemeUtils.dispatchWindowInsetsToAllChildren(binding.root)
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
        showNowTab = TraktCredentials.get(this@MoviesActivityImpl).hasCredentials()
        pageChangeListener =
            MoviesPageChangeListener(binding.sgAppBarLayout.sgAppBarLayout, showNowTab)
        tabLayout.setOnPageChangeListener(pageChangeListener)
        tabsAdapter = TabStripAdapter(this, binding.viewPagerMovies, tabLayout)
            .apply {
                // discover
                addTab(R.string.title_discover, MoviesDiscoverFragment::class.java, null)
                // Trakt-only tabs should only be visible if connected
                if (showNowTab) {
                    // history tab
                    addTab(R.string.user_stream, MoviesNowFragment::class.java, null)
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
        viewModel.scrollTabToTop(binding.viewPagerMovies.currentItem, showNowTab)
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
        showNowTab = TraktCredentials.get(this).hasCredentials()
        pageChangeListener.isShowingNowTab = showNowTab
        if (showNowTab && currentTabCount != TAB_COUNT_WITH_TRAKT) {
            tabsAdapter.addTab(
                R.string.user_stream, MoviesNowFragment::class.java, null, TAB_POSITION_NOW
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
                startActivity(Intent(this, MoviesSearchActivity::class.java))
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
                    TAB_POSITION_NOW -> MoviesNowFragment.liftOnScrollTargetViewId
                    TAB_POSITION_WATCHLIST_WITH_NOW -> MoviesWatchListFragment.liftOnScrollTargetViewId
                    TAB_POSITION_COLLECTION_WITH_NOW -> MoviesCollectionFragment.liftOnScrollTargetViewId
                    TAB_POSITION_WATCHED_WITH_NOW -> MoviesWatchedFragment.liftOnScrollTargetViewId
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
        //        const val SEARCH_LOADER_ID = 100
        const val NOW_TRAKT_USER_LOADER_ID = 101
        const val NOW_TRAKT_FRIENDS_LOADER_ID = 102
        const val WATCHLIST_LOADER_ID = 103
        const val COLLECTION_LOADER_ID = 104

        const val TAB_POSITION_DISCOVER = 0
        const val TAB_POSITION_WATCHLIST_DEFAULT = 1
        const val TAB_POSITION_COLLECTION_DEFAULT = 2
        const val TAB_POSITION_WATCHED_DEFAULT = 3
        const val TAB_POSITION_NOW = 1
        const val TAB_POSITION_WATCHLIST_WITH_NOW = 2
        const val TAB_POSITION_COLLECTION_WITH_NOW = 3
        const val TAB_POSITION_WATCHED_WITH_NOW = 4

        private const val TAB_COUNT_WITH_TRAKT = 5
    }
}