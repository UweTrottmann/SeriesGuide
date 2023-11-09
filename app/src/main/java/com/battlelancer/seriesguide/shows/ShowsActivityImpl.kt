// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.api.Intents
import com.battlelancer.seriesguide.billing.BillingActivity
import com.battlelancer.seriesguide.billing.amazon.AmazonHelper
import com.battlelancer.seriesguide.notifications.NotificationService
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.calendar.RecentFragment
import com.battlelancer.seriesguide.shows.calendar.UpcomingFragment
import com.battlelancer.seriesguide.shows.episodes.EpisodesActivity
import com.battlelancer.seriesguide.shows.history.ShowsNowFragment
import com.battlelancer.seriesguide.shows.search.discover.AddShowDialogFragment
import com.battlelancer.seriesguide.shows.search.discover.SearchResult
import com.battlelancer.seriesguide.sync.AccountUtils
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.ui.BaseTopActivity
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.ui.SearchActivity
import com.battlelancer.seriesguide.ui.TabStripAdapter
import com.battlelancer.seriesguide.util.AppUpgrade
import com.battlelancer.seriesguide.util.TaskManager
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.WebTools
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.uwetrottmann.seriesguide.billing.BillingViewModel
import com.uwetrottmann.seriesguide.billing.BillingViewModelFactory
import com.uwetrottmann.seriesguide.widgets.SlidingTabLayout

/**
 * Provides the apps main screen, displays tabs for shows, history, recent and upcoming episodes.
 * Runs upgrade code and checks billing state.
 */
open class ShowsActivityImpl : BaseTopActivity(), AddShowDialogFragment.OnAddShowListener {

    private lateinit var tabsAdapter: TabStripAdapter
    private lateinit var viewPager: ViewPager2

    private val viewModel: ShowsActivityViewModel by viewModels()
    private lateinit var billingViewModel: BillingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Due to an Android bug, launching via the app icon may add another launcher activity
        // on top of the task stack instead of resuming the last activity. Work around this
        // by finishing the launcher activity if it isn't the task root.
        // https://issuetracker.google.com/issues/64108432
        if (!isTaskRoot
            && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
            && Intent.ACTION_MAIN == intent.action) {
            finish()
            return
        }
        setContentView(R.layout.activity_shows)
        ThemeUtils.configureForEdgeToEdge(findViewById(R.id.rootLayoutShows))
        ThemeUtils.configureAppBarForContentBelow(this)
        setupActionBar()
        setupBottomNavigation(R.id.navigation_item_shows)

        // Set up a sync account if needed
        if (!AccountUtils.isAccountExists(this)) {
            AccountUtils.createAccount(this)
        }

        if (AppUpgrade(applicationContext).upgradeIfNewVersion()) {
            // Let the user know the app has updated.
            Snackbar.make(snackbarParentView, R.string.updated, Snackbar.LENGTH_LONG)
                .setAction(R.string.updated_details) {
                    WebTools.openInApp(
                        this@ShowsActivityImpl,
                        getString(R.string.url_release_notes)
                    )
                }
                .show()
        }

        // may launch from a notification, then set last cleared time
        NotificationService.handleDeleteIntent(this, intent)

        // handle implicit intents from other apps
        if (handleViewIntents(intent)) {
            finish()
            return
        }

        // setup all the views!
        setupViews()
        setupSyncProgressBar(R.id.sgProgressBar)
        setInitialTab(intent.extras)

        // query for in-app purchases
        if (Utils.isAmazonVersion()) {
            // setup Amazon IAP
            AmazonHelper.create(this)
            AmazonHelper.iapManager.register()
        } else {
            // setup Google IAP
            checkGooglePlayPurchase()
        }
    }

    /**
     * Handles further behavior, if this activity was launched through one of the [Intents]
     * action filters defined in the manifest.
     *
     * @return true if a show or episode is viewed directly and this activity should finish.
     */
    private fun handleViewIntents(intent: Intent): Boolean {
        val action = intent.action
        if (action.isNullOrEmpty()) {
            return false
        }

        val database = SgRoomDatabase.getInstance(this)
        var viewIntent: Intent? = null

        if (Intents.ACTION_VIEW_EPISODE == action) {
            // view an episode

            val showTmdbId = intent.getIntExtra(Intents.EXTRA_SHOW_TMDBID, 0)
            // Note: season may be 0 for specials.
            val season = intent.getIntExtra(Intents.EXTRA_EPISODE_SEASON, -1)
            val number = intent.getIntExtra(Intents.EXTRA_EPISODE_NUMBER, 0)
            if (showTmdbId > 0) {
                val showId = database.sgShow2Helper().getShowIdByTmdbId(showTmdbId)
                if (showId != 0L) {
                    if (season >= 0 && number >= 1) {
                        val episodeId = database.sgEpisode2Helper()
                            .getEpisodeIdByNumber(showId, season, number)
                        if (episodeId != 0L) {
                            // episode exists, display it
                            viewIntent = EpisodesActivity.intentEpisode(episodeId, this)
                        }
                    }
                    if (viewIntent == null) {
                        // No valid episode given or found, display show instead.
                        viewIntent = OverviewActivity.intentShow(this, showId)
                    }
                } else {
                    // Show not added, offer to.
                    AddShowDialogFragment.show(supportFragmentManager, showTmdbId)
                }
            }
        } else if (Intents.ACTION_VIEW_SHOW == action) {
            // view a show

            val showTmdbId = intent.getIntExtra(Intents.EXTRA_SHOW_TMDBID, 0)
            if (showTmdbId <= 0) {
                return false
            }
            val showId = database.sgShow2Helper().getShowIdByTmdbId(showTmdbId)
            if (showId != 0L) {
                // show exists, display it
                viewIntent = OverviewActivity.intentShow(this, showId)
            } else {
                // no such show, offer to add it
                AddShowDialogFragment.show(supportFragmentManager, showTmdbId)
            }
        }

        if (viewIntent != null) {
            startActivity(viewIntent)
            return true
        }

        return false
    }

    override fun setupActionBar() {
        super.setupActionBar()
        setTitle(R.string.shows)
        supportActionBar?.setTitle(R.string.shows)
    }

    private fun setupViews() {
        // setup floating action button for adding shows
        val buttonAddShow = findViewById<FloatingActionButton>(R.id.buttonShowsAdd)
        buttonAddShow.setOnClickListener {
            startActivity(
                SearchActivity.newIntent(this@ShowsActivityImpl)
            )
        }

        viewPager = findViewById(R.id.viewPagerTabs)
        val tabs = findViewById<SlidingTabLayout>(R.id.sgTabLayout)
        tabs.setOnTabClickListener { position: Int ->
            if (viewPager.currentItem == position) {
                scrollSelectedTabToTop()
            }
        }
        tabsAdapter = TabStripAdapter(this, viewPager, tabs)
        tabs.setOnPageChangeListener(ShowsPageChangeListener(findViewById(R.id.sgAppBarLayout), buttonAddShow))

        // shows tab
        tabsAdapter.addTab(R.string.shows, ShowsFragment::class.java, null)

        // history tab
        tabsAdapter.addTab(R.string.user_stream, ShowsNowFragment::class.java, null)

        // upcoming tab
        tabsAdapter.addTab(R.string.upcoming, UpcomingFragment::class.java, null)

        // recent tab
        tabsAdapter.addTab(R.string.recent, RecentFragment::class.java, null)

        // display new tabs
        tabsAdapter.notifyTabsChanged()
    }

    private fun scrollSelectedTabToTop() {
        viewModel.scrollTabToTop(viewPager.currentItem)
    }

    override fun onSelectedCurrentNavItem() {
        scrollSelectedTabToTop()
    }

    /**
     * Tries to restore the current tab from given intent extras. If that fails, uses the last known
     * selected tab. If that fails also, defaults to the first tab.
     */
    private fun setInitialTab(intentExtras: Bundle?) {
        var selection = intentExtras?.getInt(
            EXTRA_SELECTED_TAB,
            ShowsSettings.getLastShowsTabPosition(this)
        ) ?: ShowsSettings.getLastShowsTabPosition(this) // use last saved selection

        // never select a non-existent tab
        if (selection > tabsAdapter.itemCount - 1) {
            selection = 0
        }

        viewPager.setCurrentItem(selection, false)
    }

    private fun checkGooglePlayPurchase() {
        if (Utils.hasXpass(this)) {
            return
        }
        // Automatically starts checking all access status.
        // Ends connection if activity is finished (and was not ended elsewhere already).
        billingViewModel =
            ViewModelProvider(this, BillingViewModelFactory(application, SgApp.coroutineScope))
                .get(BillingViewModel::class.java)
        billingViewModel.entitlementRevokedEvent
            .observe(this) {
                // Note: sometimes sub is not really expired, only billing API not returning purchase.
                BillingActivity.showExpiredNotification(this, snackbarParentView)
            }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Unused outside of onCreate, but set for future use.
        setIntent(intent)
        // handle intents that just want to view a specific show/episode
        handleViewIntents(intent)

        // Note: do not modify UI here. On some devices (Android 8-10) this crashed
        // due to Kotlin complaining about lateinit tabsAdapter not being initialized.
        // Unsure in which case this may happen (onCreate should always be called and set up UI),
        // but do not modify UI here regardless.
    }

    override fun onResume() {
        super.onResume()

        // prefs might have changed, update menus
        invalidateOptionsMenu()

        if (Utils.isAmazonVersion()) {
            // update Amazon IAP
            AmazonHelper.iapManager.activate()
            AmazonHelper.iapManager.requestUserDataAndPurchaseUpdates()
            AmazonHelper.iapManager.validateSupporterState(this)
        }

        // update next episodes
        TaskManager.getInstance().tryNextEpisodeUpdateTask(this)
    }

    override fun onPause() {
        super.onPause()

        if (Utils.isAmazonVersion()) {
            // pause Amazon IAP
            AmazonHelper.iapManager.deactivate()
        }

        // save selected tab index
        ShowsSettings.saveLastShowsTabPosition(this, viewPager.currentItem)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.seriesguide_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search -> {
                startActivity(Intent(this, SearchActivity::class.java))
                true
            }
            R.id.menu_update -> {
                SgSyncAdapter.requestSyncDeltaImmediate(this, true)
                true
            }
            R.id.menu_fullupdate -> {
                SgSyncAdapter.requestSyncFullImmediate(this, true)
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        // prevent navigating to top activity as this is the top activity
        return keyCode == KeyEvent.KEYCODE_BACK
    }

    /**
     * Called if the user adds a show from a trakt stream fragment.
     */
    override fun onAddShow(show: SearchResult) {
        TaskManager.getInstance().performAddTask(this, show)
    }

    override val snackbarParentView: View
        get() = findViewById(R.id.coordinatorLayoutShows)

    /**
     * Page change listener which
     * - sets the scroll view of the current visible tab as the lift on scroll target view of the
     *   app bar and
     * - hides the floating action button for all but the shows tab.
     */
    class ShowsPageChangeListener(
        private val appBarLayout: AppBarLayout,
        private val floatingActionButton: FloatingActionButton
    ) : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrollStateChanged(arg0: Int) {}
        override fun onPageScrolled(arg0: Int, arg1: Float, arg2: Int) {}

        override fun onPageSelected(position: Int) {
            // Change the scrolling view the AppBarLayout should use to determine if it should lift.
            // This is required so the AppBarLayout does not flicker its background when scrolling.
            val liftOnScrollTarget = when (position) {
                Tab.SHOWS.index -> ShowsFragment.liftOnScrollTargetViewId
                Tab.NOW.index -> ShowsNowFragment.liftOnScrollTargetViewId
                Tab.UPCOMING.index -> UpcomingFragment.liftOnScrollTargetViewId
                Tab.RECENT.index -> RecentFragment.liftOnScrollTargetViewId
                else -> throw IllegalArgumentException("Unexpected page position")
            }
            appBarLayout.liftOnScrollTargetViewId = liftOnScrollTarget

            // only display add show button on Shows tab
            if (position == Tab.SHOWS.index) {
                floatingActionButton.show()
            } else {
                floatingActionButton.hide()
            }
        }
    }

    companion object {
        const val NOW_RECENTLY_LOADER_ID = 104
        const val NOW_TRAKT_USER_LOADER_ID = 106
        const val NOW_TRAKT_FRIENDS_LOADER_ID = 107

        const val EXTRA_SELECTED_TAB = "selectedtab"
    }

    enum class Tab(val index: Int) {
        SHOWS(0),
        NOW(1),
        UPCOMING(2),
        RECENT(3)
    }
}