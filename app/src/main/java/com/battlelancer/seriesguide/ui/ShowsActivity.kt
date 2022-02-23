package com.battlelancer.seriesguide.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.api.Intents
import com.battlelancer.seriesguide.billing.amazon.AmazonHelper
import com.battlelancer.seriesguide.provider.SgRoomDatabase.Companion.getInstance
import com.battlelancer.seriesguide.service.NotificationService
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.settings.DisplaySettings.getLastShowsTabPosition
import com.battlelancer.seriesguide.sync.AccountUtils
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.ui.episodes.EpisodesActivity.Companion.intentEpisode
import com.battlelancer.seriesguide.ui.search.AddShowDialogFragment
import com.battlelancer.seriesguide.ui.search.AddShowDialogFragment.OnAddShowListener
import com.battlelancer.seriesguide.ui.search.SearchResult
import com.battlelancer.seriesguide.ui.shows.CalendarFragment2
import com.battlelancer.seriesguide.ui.shows.ShowsActivityViewModel
import com.battlelancer.seriesguide.ui.shows.ShowsFragment
import com.battlelancer.seriesguide.ui.shows.ShowsNowFragment
import com.battlelancer.seriesguide.util.AppUpgrade
import com.battlelancer.seriesguide.util.TaskManager
import com.battlelancer.seriesguide.util.Utils
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.uwetrottmann.seriesguide.billing.BillingViewModel
import com.uwetrottmann.seriesguide.billing.BillingViewModelFactory
import com.uwetrottmann.seriesguide.widgets.SlidingTabLayout

/**
 * Provides the apps main screen, displays tabs for shows, history, recent and upcoming episodes.
 * Runs upgrade code and checks billing state.
 */
class ShowsActivity : BaseTopActivity(), OnAddShowListener {

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
                    Utils.launchWebsite(this@ShowsActivity, getString(R.string.url_release_notes))
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
        setupSyncProgressBar(R.id.progressBarTabs)
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

        val database = getInstance(this)
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
                            viewIntent = intentEpisode(episodeId, this)
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
                Intent(this@ShowsActivity, SearchActivity::class.java)
                    .putExtra(
                        SearchActivity.EXTRA_DEFAULT_TAB,
                        SearchActivity.TAB_POSITION_SEARCH
                    )
            )
        }

        viewPager = findViewById(R.id.viewPagerTabs)
        val tabs = findViewById<SlidingTabLayout>(R.id.tabLayoutTabs)
        tabs.setOnTabClickListener { position: Int ->
            if (viewPager.currentItem == position) {
                scrollSelectedTabToTop()
            }
        }
        tabsAdapter = TabStripAdapter(this, viewPager, tabs)
        tabs.setOnPageChangeListener(ShowsPageChangeListener(buttonAddShow))

        // shows tab
        tabsAdapter.addTab(R.string.shows, ShowsFragment::class.java, null)

        // history tab
        tabsAdapter.addTab(R.string.user_stream, ShowsNowFragment::class.java, null)

        // upcoming tab
        val argsUpcoming = Bundle().apply {
            putInt(
                CalendarFragment2.ARG_CALENDAR_TYPE,
                CalendarFragment2.CalendarType.UPCOMING.id
            )
        }
        tabsAdapter.addTab(R.string.upcoming, CalendarFragment2::class.java, argsUpcoming)

        // recent tab
        val argsRecent = Bundle().apply {
            putInt(
                CalendarFragment2.ARG_CALENDAR_TYPE,
                CalendarFragment2.CalendarType.RECENT.id
            )
        }
        tabsAdapter.addTab(R.string.recent, CalendarFragment2::class.java, argsRecent)

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
            getLastShowsTabPosition(this)
        ) ?: getLastShowsTabPosition(this) // use last saved selection

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
            .observe(this, {
                // TODO Replace notification with less disturbing in-app info.
                // Sometimes sub is not really expired, only billing API not returning purchase.
                // BillingActivity.showExpiredNotification(this)
            })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // handle intents that just want to view a specific show/episode
        if (!handleViewIntents(intent)) {
            // if no special intent, restore the last selected tab
            setInitialTab(intent.extras)
        }
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
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putInt(DisplaySettings.KEY_LAST_ACTIVE_SHOWS_TAB, viewPager.currentItem)
            .apply()
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

    override fun getSnackbarParentView(): View {
        return findViewById(R.id.rootLayoutShows)
    }

    /**
     * Page change listener which hides the floating action button for all but the shows tab.
     */
    class ShowsPageChangeListener(
        private val floatingActionButton: FloatingActionButton
    ) : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrollStateChanged(arg0: Int) {}
        override fun onPageScrolled(arg0: Int, arg1: Float, arg2: Int) {}

        override fun onPageSelected(position: Int) {
            // only display add show button on Shows tab
            if (position == INDEX_TAB_SHOWS) {
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
        const val INDEX_TAB_SHOWS = 0
        const val INDEX_TAB_NOW = 1
        const val INDEX_TAB_UPCOMING = 2
        const val INDEX_TAB_RECENT = 3
    }
}