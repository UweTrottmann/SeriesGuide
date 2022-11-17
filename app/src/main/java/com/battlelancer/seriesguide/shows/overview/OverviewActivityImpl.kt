package com.battlelancer.seriesguide.shows.overview

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.lists.ManageListsDialogFragment
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.RemoveShowDialogFragment
import com.battlelancer.seriesguide.shows.overview.OverviewActivityImpl.OverviewLayoutType.SINGLE_PANE
import com.battlelancer.seriesguide.shows.search.EpisodeSearchFragment
import com.battlelancer.seriesguide.shows.tools.ShowTools2
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import com.battlelancer.seriesguide.ui.SearchActivity
import com.battlelancer.seriesguide.ui.TabStripAdapter
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.ThemeUtils
import com.google.android.material.appbar.AppBarLayout
import com.uwetrottmann.seriesguide.widgets.SlidingTabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.ref.WeakReference

/**
 * Hosts an [OverviewFragment], [ShowFragment] and [SeasonsFragment] in a view pager on
 * small screens or pane layout on larger screens.
 */
open class OverviewActivityImpl : BaseMessageActivity() {

    enum class OverviewLayoutType(val id: Int) {
        SINGLE_PANE(0),
        MULTI_PANE_VERTICAL(1),
        MULTI_PANE_WIDE(2);

        companion object {
            fun from(id: Int): OverviewLayoutType = values().first { it.id == id }
        }
    }

    private var showId: Long = 0
    private lateinit var layoutType: OverviewLayoutType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_overview)
        setupActionBar()

        val helper = SgRoomDatabase.getInstance(this).sgShow2Helper()

        showId = intent.getLongExtra(EXTRA_LONG_SHOW_ROWID, 0)
        if (showId == 0L) {
            // Try to look up by TMDB ID or TVDB ID (used by shortcuts).
            val showTmdbIdOrZero = intent.getIntExtra(EXTRA_INT_SHOW_TMDBID, 0)
            val showTvdbIdOrZero = intent.getIntExtra(EXTRA_INT_SHOW_TVDBID, 0)
            if (showTmdbIdOrZero > 0) {
                showId = helper.getShowIdByTmdbId(showTmdbIdOrZero)
            } else if (showTvdbIdOrZero > 0) {
                showId = helper.getShowIdByTvdbId(showTvdbIdOrZero)
            }
        }

        if (showId <= 0) {
            finish()
            return
        }

        layoutType = getLayoutType(this)

        setupViews(savedInstanceState)

        updateShowDelayed(showId)
    }

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupViews(savedInstanceState: Bundle?) {
        val rootViewId = if (layoutType == SINGLE_PANE) {
            R.id.coordinatorLayoutOverview
        } else {
            R.id.rootLayoutOverview
        }
        ThemeUtils.configureForEdgeToEdge(findViewById(rootViewId))

        // poster background
        val backgroundImageView = findViewById<ImageView>(R.id.imageViewOverviewBackground)
        lifecycleScope.launch {
            val smallPosterUrl = withContext(Dispatchers.IO) {
                SgRoomDatabase.getInstance(applicationContext).sgShow2Helper()
                    .getShowMinimal(showId)?.posterSmall
            }
            if (smallPosterUrl != null) {
                ImageTools.loadShowPosterAlpha(
                    applicationContext,
                    backgroundImageView,
                    smallPosterUrl
                )
            }
        }

        // look if we are on a multi-pane or single-pane layout...
        if (layoutType == SINGLE_PANE) {
            // Single pane layout with view pager
            ThemeUtils.configureAppBarForContentBelow(this)

            // clear up left-over fragments from multi-pane layout
            findAndRemoveFragment(R.id.fragment_overview)
            findAndRemoveFragment(R.id.fragment_seasons)
            setupViewPager(isNotRestoringState = savedInstanceState == null)
        } else {
            // Multi-pane show, overview and seasons fragment
            // Bottom pad the card containing the overview fragment.
            ThemeUtils.applyBottomPaddingForNavigationBar(findViewById(R.id.wrapperOverview))

            // clear up left-over fragments from single-pane layout
            val isSwitchingLayouts = activeFragments.size != 0
            for (fragment in activeFragments) {
                supportFragmentManager.beginTransaction().remove(fragment).commit()
            }

            // attach new fragments if there are none or if layouts just switched
            if (savedInstanceState == null || isSwitchingLayouts) {
                setupPanes()
            }
        }
    }

    private fun setupPanes() {
        val showsFragment: Fragment = ShowFragment(showId)
        val ft1 = supportFragmentManager.beginTransaction()
        ft1.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
        ft1.replace(R.id.fragment_show, showsFragment)
        ft1.commit()

        val overviewFragment: Fragment = OverviewFragment(showId)
        val ft2 = supportFragmentManager.beginTransaction()
        ft2.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
        ft2.replace(R.id.fragment_overview, overviewFragment)
        ft2.commit()

        val seasonsFragment: Fragment = SeasonsFragment(showId)
        val ft3 = supportFragmentManager.beginTransaction()
        ft3.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
        ft3.replace(R.id.fragment_seasons, seasonsFragment)
        ft3.commit()
    }

    private fun setupViewPager(isNotRestoringState: Boolean) {
        val pager = findViewById<ViewPager2>(R.id.pagerOverview)

        // setup tab strip
        val tabLayout = findViewById<SlidingTabLayout>(R.id.sgTabLayout)
        val tabsAdapter = TabStripAdapter(this, pager, tabLayout)
        tabLayout.setOnPageChangeListener(OverviewPageChangeListener(findViewById(R.id.sgAppBarLayout)))
        tabsAdapter.addTab(
            R.string.show_details,
            ShowFragment::class.java,
            ShowFragment.buildArgs(showId)
        )
        tabsAdapter.addTab(
            R.string.description_overview,
            OverviewFragment::class.java,
            OverviewFragment.buildArgs(showId)
        )
        tabsAdapter.addTab(
            R.string.episodes,
            SeasonsFragment::class.java,
            SeasonsFragment.buildArgs(showId)
        )
        tabsAdapter.notifyTabsChanged()

        // select overview to be shown initially
        if (isNotRestoringState) {
            val displaySeasons = intent.getBooleanExtra(EXTRA_BOOLEAN_DISPLAY_SEASONS, false)
            pager.setCurrentItem(if (displaySeasons) 2 /* seasons */ else 1 /* overview */, false)
        }
    }


    /**
     * Page change listener which sets the scroll view of the current visible tab as the lift on
     * scroll target view of the app bar.
     */
    class OverviewPageChangeListener(
        private val appBarLayout: AppBarLayout
    ) : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrollStateChanged(arg0: Int) {}
        override fun onPageScrolled(arg0: Int, arg1: Float, arg2: Int) {}

        override fun onPageSelected(position: Int) {
            // Change the scrolling view the AppBarLayout should use to determine if it should lift.
            // This is required so the AppBarLayout does not flicker its background when scrolling.
            val liftOnScrollTarget = when (position) {
                0 -> ShowFragment.liftOnScrollTargetViewId
                1 -> OverviewFragment.liftOnScrollTargetViewId
                2 -> SeasonsFragment.liftOnScrollTargetViewId
                else -> throw IllegalArgumentException("Unexpected page position")
            }
            appBarLayout.liftOnScrollTargetViewId = liftOnScrollTarget
        }
    }

    private fun findAndRemoveFragment(fragmentId: Int) {
        val overviewFragment = supportFragmentManager.findFragmentById(fragmentId)
        if (overviewFragment != null) {
            supportFragmentManager.beginTransaction().remove(overviewFragment).commit()
        }
    }

    private var fragments: MutableList<WeakReference<Fragment>> = ArrayList()

    override fun onAttachFragment(fragment: Fragment) {
        /*
         * View pager fragments have tags set by the pager, we can use this to
         * only add refs to those then, making them available to get removed if
         * we switch to a non-pager layout.
         */
        if (fragment.tag != null) {
            fragments.add(WeakReference(fragment))
        }
    }

    val activeFragments: ArrayList<Fragment>
        get() {
            val ret = ArrayList<Fragment>()
            for (ref in fragments) {
                val f = ref.get()
                if (f != null) {
                    if (f.isAdded) {
                        ret.add(f)
                    }
                }
            }
            return ret
        }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.overview_activity_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.menu_overview_search) {
            launchSearch()
            return true
        }
        if (itemId == R.id.menu_show_manage_lists) {
            ManageListsDialogFragment.show(supportFragmentManager, showId)
            return true
        }
        if (itemId == R.id.menu_overview_remove_show) {
            RemoveShowDialogFragment.show(showId, supportFragmentManager, this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ShowTools2.OnRemovingShowEvent) {
        if (event.showId == showId) {
            finish() // finish this activity if the show it displays is about to get removed
        }
    }

    private fun launchSearch() {
        // refine search with the show's title
        val titleOrNull = SgRoomDatabase.getInstance(this).sgShow2Helper().getShowTitle(showId)
        if (titleOrNull != null) {
            val appSearchData = Bundle()
            appSearchData.putString(EpisodeSearchFragment.ARG_SHOW_TITLE, titleOrNull)

            val intent = Intent(this, SearchActivity::class.java)
            intent.putExtra(SearchManager.APP_DATA, appSearchData)
            intent.action = Intent.ACTION_SEARCH
            startActivity(intent)
        }
    }

    override val snackbarParentView: View
        get() = if (layoutType == SINGLE_PANE) {
            // The single pane layout uses a CoordinatorLayout as root view.
            findViewById(R.id.coordinatorLayoutOverview)
        } else {
            super.snackbarParentView
        }

    companion object {
        const val OVERVIEW_ACTIONS_LOADER_ID = 104

        /**
         * Used by legacy shortcuts.
         */
        private const val EXTRA_INT_SHOW_TVDBID = "show_tvdbid"

        /**
         * Used by shortcuts.
         */
        const val EXTRA_INT_SHOW_TMDBID = "show_tmdbid"
        const val EXTRA_LONG_SHOW_ROWID = "show_id"
        const val EXTRA_BOOLEAN_DISPLAY_SEASONS = "EXTRA_DISPLAY_SEASONS"

        fun getLayoutType(context: Context): OverviewLayoutType =
            OverviewLayoutType.from(context.resources.getInteger(R.integer.overviewLayoutType))
    }

}