package com.battlelancer.seriesguide.ui

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
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.ui.lists.ManageListsDialogFragment
import com.battlelancer.seriesguide.ui.overview.OverviewFragment
import com.battlelancer.seriesguide.ui.overview.SeasonsFragment
import com.battlelancer.seriesguide.ui.overview.ShowFragment
import com.battlelancer.seriesguide.ui.search.EpisodeSearchFragment
import com.battlelancer.seriesguide.ui.shows.RemoveShowDialogFragment.Companion.show
import com.battlelancer.seriesguide.ui.shows.ShowTools2.OnRemovingShowEvent
import com.battlelancer.seriesguide.util.ImageTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.ref.WeakReference
import java.util.ArrayList

/**
 * Hosts an [OverviewFragment].
 */
class OverviewActivity : BaseMessageActivity() {

    private var showId: Long = 0

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

        setupViews(savedInstanceState)

        updateShowDelayed(showId)
    }

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupViews(savedInstanceState: Bundle?) {
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
        val pagerView = findViewById<View>(R.id.pagerOverview)
        if (pagerView != null && pagerView.visibility == View.VISIBLE) {
            // ...single pane layout with view pager

            // clear up left-over fragments from multi-pane layout
            findAndRemoveFragment(R.id.fragment_overview)
            findAndRemoveFragment(R.id.fragment_seasons)
            setupViewPager(pagerView)
        } else {
            // ...multi-pane overview and seasons fragment

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

        val overviewFragment: Fragment = OverviewFragment.newInstance(showId)
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

    private fun setupViewPager(pagerView: View) {
        val pager = pagerView as ViewPager2

        // setup tab strip
        val tabsAdapter = TabStripAdapter(
            this, pager, findViewById(R.id.tabsOverview)
        )
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
        val displaySeasons = intent.getBooleanExtra(EXTRA_BOOLEAN_DISPLAY_SEASONS, false)
        pager.setCurrentItem(if (displaySeasons) 2 /* seasons */ else 1 /* overview */, false)
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
            show(showId, supportFragmentManager, this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnRemovingShowEvent) {
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

    override fun getSnackbarParentView(): View {
        return if (resources.getBoolean(R.bool.isOverviewSinglePane)) {
            findViewById(R.id.coordinatorLayoutOverview)
        } else {
            super.getSnackbarParentView()
        }
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
        private const val EXTRA_INT_SHOW_TMDBID = "show_tmdbid"
        private const val EXTRA_LONG_SHOW_ROWID = "show_id"
        private const val EXTRA_BOOLEAN_DISPLAY_SEASONS = "EXTRA_DISPLAY_SEASONS"

        /**
         * After opening, switches to overview tab (only if not multi-pane).
         */
        @JvmStatic
        fun intentShowByTmdbId(context: Context, showTmdbId: Int): Intent {
            return Intent(context, OverviewActivity::class.java)
                .putExtra(EXTRA_INT_SHOW_TMDBID, showTmdbId)
        }

        /**
         * After opening, switches to overview tab (only if not multi-pane).
         */
        @JvmStatic
        fun intentShow(context: Context, showRowId: Long): Intent {
            return Intent(context, OverviewActivity::class.java)
                .putExtra(EXTRA_LONG_SHOW_ROWID, showRowId)
        }

        /**
         * After opening, switches to seasons tab (only if not multi-pane).
         */
        fun intentSeasons(context: Context, showRowId: Long): Intent {
            return intentShow(context, showRowId).putExtra(EXTRA_BOOLEAN_DISPLAY_SEASONS, true)
        }
    }
}