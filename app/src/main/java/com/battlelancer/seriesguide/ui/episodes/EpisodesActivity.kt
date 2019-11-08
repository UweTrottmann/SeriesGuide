package com.battlelancer.seriesguide.ui.episodes

import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import butterknife.BindView
import butterknife.ButterKnife
import com.battlelancer.seriesguide.Constants
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.jobs.episodes.BaseEpisodesJob
import com.battlelancer.seriesguide.model.SgShowMinimal
import com.battlelancer.seriesguide.service.NotificationService
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools
import com.battlelancer.seriesguide.ui.BaseNavDrawerActivity
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences
import com.battlelancer.seriesguide.util.SeasonTools
import com.battlelancer.seriesguide.util.Shadows
import com.battlelancer.seriesguide.util.Utils
import com.uwetrottmann.seriesguide.widgets.SlidingTabLayout
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.ArrayList

/**
 * Hosts a fragment which displays episodes of a season in a list and in a [ViewPager].
 * On small screens only one is visible at a time, on larger screens they are shown side-by-side.
 */
class EpisodesActivity : BaseNavDrawerActivity() {

    @BindView(R.id.fragment_episodes)
    lateinit var containerList: ViewGroup
    @BindView(R.id.containerEpisodesPager)
    @JvmField
    var containerPager: ViewGroup? = null
    @BindView(R.id.pagerEpisodes)
    lateinit var episodeDetailsPager: ViewPager
    @BindView(R.id.tabsEpisodes)
    lateinit var episodeDetailsTabs: SlidingTabLayout
    @BindView(R.id.dividerEpisodesTabs)
    @JvmField
    var dividerEpisodesTabs: View? = null
    @BindView(R.id.imageViewEpisodesBackground)
    lateinit var backgroundImageView: ImageView
    @BindView(R.id.viewEpisodesShadowStart)
    @JvmField
    var shadowStart: View? = null
    @BindView(R.id.viewEpisodesShadowEnd)
    @JvmField
    var shadowEnd: View? = null

    private var episodesListFragment: EpisodesFragment? = null
    private var episodeDetailsAdapter: EpisodePagerAdapter? = null

    private lateinit var viewModel: EpisodesActivityViewModel
    private var showTvdbId: Int = 0
    private var seasonTvdbId: Int = 0

    /**
     * If list and pager are displayed side-by-side, or toggleable one or the other.
     */
    private val isSinglePaneView: Boolean
        get() = containerPager != null

    private val isListGone: Boolean
        get() = containerList.visibility == View.GONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_episodes)
        setupNavDrawer()
        setupActionBar()

        // if coming from a notification, set last cleared time
        NotificationService.handleDeleteIntent(this, intent)

        ButterKnife.bind(this)
        setupViews()

        val episodeTvdbId = intent.getIntExtra(EXTRA_EPISODE_TVDBID, 0)
        val seasonTvdbId = intent.getIntExtra(EXTRA_SEASON_TVDBID, 0)

        val viewModelFactory = EpisodesActivityViewModelFactory(
            application,
            episodeTvdbId,
            seasonTvdbId
        )
        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(EpisodesActivityViewModel::class.java)
        viewModel.seasonAndShowInfoLiveData.observe(this, Observer { info ->
            if (info == null) {
                finish() // Missing required data.
                return@Observer
            }
            this.seasonTvdbId = info.seasonAndShowInfo.seasonTvdbId
            this.showTvdbId = info.seasonAndShowInfo.showTvdbId

            updateActionBar(
                info.seasonAndShowInfo.show,
                info.seasonAndShowInfo.seasonNumber
            )

            // Set the image background.
            TvdbImageTools.loadShowPosterAlpha(
                this,
                backgroundImageView,
                info.seasonAndShowInfo.show.posterSmall
            )

            updateViews(
                savedInstanceState,
                info.seasonAndShowInfo.showTvdbId,
                info.seasonAndShowInfo.seasonTvdbId,
                info.seasonAndShowInfo.seasonNumber,
                info.startPosition,
                info.episodes
            )

            updateShowDelayed(info.seasonAndShowInfo.showTvdbId)
        })
    }

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.let {
            it.setDisplayShowTitleEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun updateActionBar(show: SgShowMinimal, seasonNumber: Int) {
        val showTitle = show.title
        val seasonString = SeasonTools.getSeasonString(this, seasonNumber)
        title = "$showTitle $seasonString"
        supportActionBar?.let {
            it.title = showTitle
            it.subtitle = seasonString
        }
    }

    private fun switchView(isListView: Boolean, updateOptionsMenu: Boolean) {
        containerList.visibility = if (isListView) View.VISIBLE else View.GONE
        val visibilityPagerViews = if (isListView) View.GONE else View.VISIBLE
        containerPager!!.visibility = visibilityPagerViews
        episodeDetailsTabs.visibility = visibilityPagerViews
        dividerEpisodesTabs!!.visibility = visibilityPagerViews
        if (updateOptionsMenu) {
            invalidateOptionsMenu()
        }
    }

    private fun setupViews() {
        if (isSinglePaneView) {
            switchView(isListView = false, updateOptionsMenu = false)
        }

        // Tabs setup.
        episodeDetailsTabs.setCustomTabView(
            R.layout.tabstrip_item_transparent,
            R.id.textViewTabStripItem
        )
        episodeDetailsTabs.setSelectedIndicatorColors(
            ContextCompat.getColor(
                this,
                if (SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_DarkBlue) {
                    R.color.white
                } else {
                    Utils.resolveAttributeToResourceId(theme, R.attr.colorPrimary)
                }
            )
        )

        // Set drawables for visible shadows.
        shadowStart?.let {
            Shadows.getInstance().setShadowDrawable(
                this, it,
                GradientDrawable.Orientation.RIGHT_LEFT
            )
        }
        shadowEnd?.let {
            Shadows.getInstance().setShadowDrawable(
                this, it,
                GradientDrawable.Orientation.LEFT_RIGHT
            )
        }
    }

    private val onPageChangeListener = object : OnPageChangeListener {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            // do nothing
        }

        override fun onPageSelected(position: Int) {
            // update currently checked episode
            episodesListFragment?.setItemChecked(position)
        }

        override fun onPageScrollStateChanged(state: Int) {
            // do nothing
        }
    }

    private fun updateViews(
        savedInstanceState: Bundle?,
        showTvdbId: Int,
        seasonTvdbId: Int,
        seasonNumber: Int,
        startPosition: Int,
        episodes: ArrayList<Episode>
    ) {
        // Episode list.
        if (episodesListFragment == null) {
            episodesListFragment = if (savedInstanceState == null) {
                EpisodesFragment.newInstance(
                    showTvdbId,
                    seasonTvdbId,
                    seasonNumber,
                    startPosition
                ).also {
                    supportFragmentManager.beginTransaction()
                        .add(R.id.fragment_episodes, it, "episodes")
                        .commit()
                }
            } else {
                supportFragmentManager
                    .findFragmentByTag("episodes") as EpisodesFragment
            }
        }

        // Episode pager.
        val adapter = episodeDetailsAdapter
        if (adapter == null) {
            episodeDetailsAdapter = EpisodePagerAdapter(
                this,
                supportFragmentManager,
                episodes,
                true
            )
            episodeDetailsPager.adapter = episodeDetailsAdapter
        } else {
            adapter.updateEpisodeList(episodes)
        }
        // Refresh pager tab decoration.
        episodeDetailsTabs.setViewPager(episodeDetailsPager)

        episodeDetailsPager.setCurrentItem(startPosition, false)
        // Set page listener after current item to avoid null pointer for non-existing content view.
        episodeDetailsTabs.setOnPageChangeListener(onPageChangeListener)
    }

    private val onSortOrderChangedListener = OnSharedPreferenceChangeListener { _, key ->
        if (DisplaySettings.KEY_EPISODE_SORT_ORDER == key) {
            reorderAndUpdateTabs()
        }
    }

    override fun onStart() {
        super.onStart()

        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(onSortOrderChangedListener)
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
                    R.drawable.ic_view_headline_white_24dp
                } else {
                    R.drawable.ic_view_column_white_24dp
                }
            )
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                val upIntent = OverviewActivity.intentSeasons(this, showTvdbId)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(upIntent)
                true
            }
            R.id.menu_action_episodes_switch_view -> {
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
        episodeDetailsPager.setCurrentItem(position, true)
        if (isSinglePaneView) {
            switchView(isListView = false, updateOptionsMenu = true)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ServiceCompletedEvent) {
        if (event.isSuccessful && event.flagJob is BaseEpisodesJob) {
            // order can only change if sorted by unwatched first
            val sortOrder = DisplaySettings.getEpisodeSortOrder(this)
            if (sortOrder == Constants.EpisodeSorting.UNWATCHED_FIRST) {
                // Temporarily remove page change listener to avoid scrolling to checked item.
                episodeDetailsTabs.setOnPageChangeListener(null)
                // Listener is re-set once view model completes loading.
                reorderAndUpdateTabs()
            }
        }
    }

    private fun reorderAndUpdateTabs() {
        // Get currently selected episode
        val oldPosition = episodeDetailsPager.currentItem
        val episodeTvdbId = episodeDetailsAdapter?.getItemEpisodeTvdbId(oldPosition) ?: 0

        // Launch update.
        viewModel.updateEpisodesData(episodeTvdbId, seasonTvdbId)
    }

    companion object {
        /** Either this or [EXTRA_EPISODE_TVDBID] is required. */
        const val EXTRA_SEASON_TVDBID = "season_tvdbid"
        /** Either this or [EXTRA_SEASON_TVDBID] is required. */
        const val EXTRA_EPISODE_TVDBID = "episode_tvdbid"

        const val EPISODES_LOADER_ID = 100
        const val EPISODE_LOADER_ID = 101
        const val ACTIONS_LOADER_ID = 102
    }
}
