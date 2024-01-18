// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2023 Uwe Trottmann

package com.battlelancer.seriesguide.shows.episodes

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ActivityEpisodesBinding
import com.battlelancer.seriesguide.jobs.episodes.BaseEpisodesJob
import com.battlelancer.seriesguide.notifications.NotificationService
import com.battlelancer.seriesguide.shows.overview.SeasonTools
import com.battlelancer.seriesguide.shows.tools.ShowSync
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.ThemeUtils.setDefaultStyle
import com.google.android.material.shape.MaterialShapeDrawable
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Hosts a fragment which displays episodes of a season in a list and in a view pager.
 * On small screens only one is visible at a time, on larger screens they are shown side-by-side.
 */
class EpisodesActivity : BaseMessageActivity() {

    enum class EpisodesLayoutType(val id: Int) {
        SINGLE_PANE(0),
        MULTI_PANE_VERTICAL(1),
        MULTI_PANE_WIDE(2);

        companion object {
            fun from(id: Int): EpisodesLayoutType = values().first { it.id == id }
        }
    }

    private lateinit var binding: ActivityEpisodesBinding
    private lateinit var layoutType: EpisodesLayoutType

    private var episodesListFragment: EpisodesFragment? = null
    private var episodeDetailsAdapter: EpisodePagerAdapter? = null

    private lateinit var viewModel: EpisodesActivityViewModel
    private var showId: Long = 0
    private var seasonId: Long = 0

    /** Keeps list visibility even in multi-pane view. */
    private var isListVisibleInSinglePaneView: Boolean = false
    /** Remembers if pager was shown due to tap on list item. */
    private var hasTappedItemInSinglePaneView: Boolean = false

    /**
     * If list and pager are displayed side-by-side, or toggleable one or the other.
     */
    private val isSinglePaneView: Boolean
        get() = layoutType == EpisodesLayoutType.SINGLE_PANE

    private val isListGone: Boolean
        get() = binding.containerEpisodesList.visibility == View.GONE

    private val isViewingSeason: Boolean
        get() = intent.hasExtra(EXTRA_LONG_SEASON_ID)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEpisodesBinding.inflate(layoutInflater)
        layoutType = getLayoutType(this)
        setContentView(binding.root)
        ThemeUtils.configureForEdgeToEdge(binding.root as ViewGroup)
        setupActionBar()

        // if coming from a notification, set last cleared time
        NotificationService.handleDeleteIntent(this, intent)

        isListVisibleInSinglePaneView = savedInstanceState?.getBoolean(STATE_IS_LIST_VISIBLE)
            ?: if (isViewingSeason) {
                // When coming to view season, check if list is preferred.
                PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean(PREF_PREFER_LIST_TO_VIEW_SEASON, false)
            } else {
                false
            }
        hasTappedItemInSinglePaneView = savedInstanceState?.
            getBoolean(STATE_HAS_TAPPED_ITEM_SINGLE_PANE) ?: false

        setupViews()

        onBackPressedDispatcher.addCallback {
            // If single pane view and previously switched to pager by tapping on list item,
            // go back to list first instead of finishing activity.
            if (isSinglePaneView && isListGone && hasTappedItemInSinglePaneView) {
                hasTappedItemInSinglePaneView = false
                switchView(makeListVisible = true, updateOptionsMenu = true)
            } else {
                finish()
            }
        }

        val episodeRowId = intent.getLongExtra(EXTRA_LONG_EPISODE_ID, 0)
        @Suppress("DEPRECATION") // For backwards-compat
        val episodeTvdbId = intent.getIntExtra(EXTRA_EPISODE_TVDBID, 0)
        val seasonId = intent.getLongExtra(EXTRA_LONG_SEASON_ID, 0)

        val viewModel by viewModels<EpisodesActivityViewModel> {
            EpisodesActivityViewModelFactory(application, episodeTvdbId, episodeRowId, seasonId)
        }
        this.viewModel = viewModel
        viewModel.seasonAndShowInfoLiveData.observe(this, Observer { info ->
            if (info == null) {
                finish() // Missing required data.
                return@Observer
            }
            this.seasonId = info.seasonAndShowInfo.seasonId
            this.showId = info.seasonAndShowInfo.showId

            updateActionBar(
                info.seasonAndShowInfo.show.title,
                info.seasonAndShowInfo.seasonNumber
            )

            // Set the image background.
            ImageTools.loadShowPosterAlpha(
                this,
                binding.imageViewEpisodesBackground,
                info.seasonAndShowInfo.show.posterSmall
            )

            updateViews(info)

            ShowSync.updateDelayed(
                applicationContext,
                info.seasonAndShowInfo.showId,
                lifecycleScope
            )
        })
    }

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.let {
            it.setDisplayShowTitleEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun updateActionBar(showTitle: String, seasonNumber: Int) {
        val seasonString = SeasonTools.getSeasonString(this, seasonNumber)
        title = "$showTitle $seasonString"
        supportActionBar?.let {
            it.title = showTitle
            it.subtitle = seasonString
        }
    }

    private fun switchView(makeListVisible: Boolean, updateOptionsMenu: Boolean) {
        isListVisibleInSinglePaneView = makeListVisible
        binding.containerEpisodesList.visibility = if (makeListVisible) View.VISIBLE else View.GONE
        val visibilityPagerViews = if (makeListVisible) View.GONE else View.VISIBLE
        binding.containerEpisodesPager!!.visibility = visibilityPagerViews
        binding.tabsEpisodes.visibility = visibilityPagerViews
        if (updateOptionsMenu) {
            invalidateOptionsMenu()
        }
    }

    private fun setupViews() {
        if (isSinglePaneView) {
            // Note: currently app bar does not lift, so the status bar foreground will stay
            // transparent, but still hides content scrolling below it.
            binding.sgAppBarLayout?.statusBarForeground =
                MaterialShapeDrawable.createWithElevationOverlay(this)
            switchView(isListVisibleInSinglePaneView, updateOptionsMenu = false)
        } else {
            // Multi-pane layout
            // Add bottom margin to the card containing episode details.
            ThemeUtils.applyBottomMarginForNavigationBar(binding.cardEpisodes!!)
        }

        // Tabs setup.
        binding.tabsEpisodes.setDefaultStyle()
        binding.tabsEpisodes.setDisplayUnderline(true)

        // Preload next/previous page so swiping is smoother.
        binding.pagerEpisodes.offscreenPageLimit = 1
    }

    private val onPageChangeListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            // do nothing
        }

        override fun onPageSelected(position: Int) {
            // update currently checked episode
            episodesListFragment?.run {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    setItemChecked(position)
                }
            }
        }

        override fun onPageScrollStateChanged(state: Int) {
            // do nothing
        }
    }

    private fun updateViews(info: EpisodesActivityViewModel.EpisodeSeasonAndShowInfo) {
        // Episode list.
        if (episodesListFragment == null) {
            val existingFragment = supportFragmentManager
                .findFragmentByTag("episodes") as EpisodesFragment?
            episodesListFragment = existingFragment ?: EpisodesFragment.newInstance(
                info.seasonAndShowInfo.seasonId,
                info.startPosition
            ).also {
                supportFragmentManager.beginTransaction()
                    .add(R.id.containerEpisodesList, it, "episodes")
                    .commit()
            }
        }

        // Episode pager.
        val pagerEpisodes = binding.pagerEpisodes
        val tabsEpisodes = binding.tabsEpisodes
        val adapter = episodeDetailsAdapter
        if (adapter == null) {
            episodeDetailsAdapter = EpisodePagerAdapter(
                this,
                supportFragmentManager
            ).also {
                it.updateEpisodeList(info.episodes)
            }
            pagerEpisodes.adapter = episodeDetailsAdapter
        } else {
            adapter.updateEpisodeList(info.episodes)
        }
        // Refresh pager tab decoration.
        tabsEpisodes.setViewPager(pagerEpisodes)

        // Remove page change listener to avoid changing checked episode on sort order changes.
        tabsEpisodes.setOnPageChangeListener(null)
        pagerEpisodes.setCurrentItem(info.startPosition, false)
        // Set page listener after current item to avoid null pointer for non-existing content view.
        tabsEpisodes.setOnPageChangeListener(onPageChangeListener)
    }

    private val onSortOrderChangedListener = OnSharedPreferenceChangeListener { _, key ->
        if (EpisodesSettings.KEY_EPISODE_SORT_ORDER == key) {
            reorderAndUpdateTabs()
        }
    }

    override fun onStart() {
        super.onStart()

        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(onSortOrderChangedListener)
    }

    override fun onPause() {
        super.onPause()
        if (isViewingSeason) {
            PreferenceManager.getDefaultSharedPreferences(this).edit {
                putBoolean(PREF_PREFER_LIST_TO_VIEW_SEASON, isListVisibleInSinglePaneView)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_IS_LIST_VISIBLE, isListVisibleInSinglePaneView)
        outState.putBoolean(STATE_HAS_TAPPED_ITEM_SINGLE_PANE, hasTappedItemInSinglePaneView)
    }

    override fun onStop() {
        super.onStop()

        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(onSortOrderChangedListener)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (isSinglePaneView) {
            menuInflater.inflate(R.menu.episodes_menu, menu)
            menu.findItem(R.id.menu_action_episodes_switch_view).setIcon(
                if (isListGone) {
                    R.drawable.ic_view_headline_control_24dp
                } else {
                    R.drawable.ic_view_column_control_24dp
                }
            )
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                val upIntent = OverviewActivity.intentSeasons(this, showId)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(upIntent)
                true
            }
            R.id.menu_action_episodes_switch_view -> {
                hasTappedItemInSinglePaneView = false
                switchView(isListGone, true)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Switch to the episode at the given position.
     */
    fun setCurrentPage(position: Int) {
        if (isSinglePaneView) {
            hasTappedItemInSinglePaneView = true
            switchView(makeListVisible = false, updateOptionsMenu = true)
        }
        // Add setting item position to the event queue as the pager might not have been drawn,
        // yet, e.g. when in single pane view.
        binding.pagerEpisodes.post {
            binding.pagerEpisodes.setCurrentItem(position, true)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ServiceCompletedEvent) {
        if (event.isSuccessful && event.flagJob is BaseEpisodesJob) {
            // order can only change if sorted by unwatched first
            val sortOrder = EpisodesSettings.getEpisodeSortOrder(this)
            if (sortOrder == EpisodesSettings.EpisodeSorting.UNWATCHED_FIRST) {
                // Temporarily remove page change listener to avoid scrolling to checked item.
                binding.tabsEpisodes.setOnPageChangeListener(null)
                // Listener is re-set once view model completes loading.
                reorderAndUpdateTabs()
            }
        }
    }

    private fun reorderAndUpdateTabs() {
        // Get currently selected episode
        val oldPosition = binding.pagerEpisodes.currentItem
        val episodeRowId = episodeDetailsAdapter?.getItemEpisodeId(oldPosition) ?: 0

        // Launch update.
        viewModel.updateEpisodesData(0, episodeRowId, seasonId)
    }

    companion object {
        private const val EXTRA_LONG_SEASON_ID = "season_id"
        const val EXTRA_LONG_EPISODE_ID = "episode_id"
        /** Either this or [EXTRA_EPISODE_TVDBID] is required. */
        @Deprecated("Use intentSeason and season row ID instead.")
        const val EXTRA_SEASON_TVDBID = "season_tvdbid"
        /** Either this or [EXTRA_SEASON_TVDBID] is required. */
        @Deprecated("Use intentEpisode and episode row ID instead.")
        const val EXTRA_EPISODE_TVDBID = "episode_tvdbid"

        private const val PREF_PREFER_LIST_TO_VIEW_SEASON = "com.uwetrottmann.seriesguide.episodes.preferlist"

        private const val STATE_IS_LIST_VISIBLE = "STATE_IS_LIST_VISIBLE"
        private const val STATE_HAS_TAPPED_ITEM_SINGLE_PANE = "STATE_HAS_TAPPED_ITEM_SINGLE_PANE"

        const val EPISODES_LOADER_ID = 100
        const val EPISODE_LOADER_ID = 101
        const val ACTIONS_LOADER_ID = 102

        @JvmStatic
        fun intentSeason(seasonRowId: Long, context: Context): Intent {
            return Intent(context, EpisodesActivity::class.java)
                .putExtra(EXTRA_LONG_SEASON_ID, seasonRowId)
        }

        @JvmStatic
        fun intentEpisode(episodeRowId: Long, context: Context): Intent {
            return Intent(context, EpisodesActivity::class.java)
                .putExtra(EXTRA_LONG_EPISODE_ID, episodeRowId)
        }

        fun getLayoutType(context: Context): EpisodesLayoutType =
            EpisodesLayoutType.from(context.resources.getInteger(R.integer.episodesLayoutType))
    }
}
