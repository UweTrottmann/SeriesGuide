package com.battlelancer.seriesguide.ui.episodes;

import android.content.Intent;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.jobs.episodes.BaseEpisodesJob;
import com.battlelancer.seriesguide.model.SgShowMinimal;
import com.battlelancer.seriesguide.service.NotificationService;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.battlelancer.seriesguide.ui.BaseNavDrawerActivity;
import com.battlelancer.seriesguide.ui.OverviewActivity;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.SeasonTools;
import com.battlelancer.seriesguide.util.Shadows;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.widgets.SlidingTabLayout;
import java.util.ArrayList;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Hosts a fragment which displays episodes of a season in a list and in a {@link ViewPager}.
 * On small screens only one is visible at a time, on larger screens they are shown side-by-side.
 */
public class EpisodesActivity extends BaseNavDrawerActivity {

    public static final int EPISODES_LOADER_ID = 100;
    public static final int EPISODE_LOADER_ID = 101;
    public static final int ACTIONS_LOADER_ID = 102;

    @BindView(R.id.fragment_episodes)
    ViewGroup containerList;
    @Nullable
    @BindView(R.id.containerEpisodesPager)
    ViewGroup containerPager;
    @BindView(R.id.pagerEpisodes)
    ViewPager episodeDetailsPager;
    @BindView(R.id.tabsEpisodes)
    SlidingTabLayout episodeDetailsTabs;
    @Nullable
    @BindView(R.id.dividerEpisodesTabs)
    View dividerEpisodesTabs;
    @BindView(R.id.imageViewEpisodesBackground)
    ImageView backgroundImageView;
    @Nullable
    @BindView(R.id.viewEpisodesShadowStart)
    View shadowStart;
    @Nullable
    @BindView(R.id.viewEpisodesShadowEnd)
    View shadowEnd;

    @Nullable
    private EpisodesFragment episodesListFragment;
    @Nullable
    private EpisodePagerAdapter episodeDetailsAdapter;

    private EpisodesActivityViewModel viewModel;
    private int showTvdbId;
    private int seasonTvdbId;

    /**
     * All values have to be integer. Only one is required.
     */
    public interface InitBundle {

        String SEASON_TVDBID = "season_tvdbid";

        String EPISODE_TVDBID = "episode_tvdbid";
    }

    /**
     * If list and pager are displayed side-by-side, or toggleable one or the other.
     */
    private boolean isSinglePaneView() {
        return containerPager != null;
    }

    private boolean isListGone() {
        return containerList.getVisibility() == View.GONE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_episodes);
        setupNavDrawer();
        setupActionBar();

        // if coming from a notification, set last cleared time
        NotificationService.handleDeleteIntent(this, getIntent());

        ButterKnife.bind(this);
        setupViews();

        final int episodeTvdbId = getIntent().getIntExtra(InitBundle.EPISODE_TVDBID, 0);
        final int seasonTvdbId = getIntent().getIntExtra(InitBundle.SEASON_TVDBID, 0);

        EpisodesActivityViewModelFactory viewModelFactory = new EpisodesActivityViewModelFactory(
                getApplication(),
                episodeTvdbId,
                seasonTvdbId
        );
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(EpisodesActivityViewModel.class);
        viewModel.getSeasonAndShowInfoLiveData().observe(this, info -> {
            if (info == null) {
                finish(); // Missing required data.
                return;
            }
            this.seasonTvdbId = info.getSeasonAndShowInfo().getSeasonTvdbId();
            this.showTvdbId = info.getSeasonAndShowInfo().getShowTvdbId();

            updateActionBar(
                    info.getSeasonAndShowInfo().getShow(),
                    info.getSeasonAndShowInfo().getSeasonNumber()
            );

            // Set the image background.
            TvdbImageTools.loadShowPosterAlpha(this, backgroundImageView,
                    info.getSeasonAndShowInfo().getShow().getPosterSmall());

            updateViews(
                    savedInstanceState,
                    info.getSeasonAndShowInfo().getShowTvdbId(),
                    info.getSeasonAndShowInfo().getSeasonTvdbId(),
                    info.getSeasonAndShowInfo().getSeasonNumber(),
                    info.getStartPosition(),
                    info.getEpisodes()
            );

            updateShowDelayed(info.getSeasonAndShowInfo().getShowTvdbId());
        });
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void updateActionBar(SgShowMinimal show, int seasonNumber) {
        String showTitle = show.getTitle();
        String seasonString = SeasonTools.getSeasonString(this, seasonNumber);
        setTitle(showTitle + " " + seasonString);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(showTitle);
            actionBar.setSubtitle(seasonString);
        }
    }

    private void switchView(boolean isListView, boolean updateOptionsMenu) {
        containerList.setVisibility(isListView ? View.VISIBLE : View.GONE);
        int visibilityPagerViews = isListView ? View.GONE : View.VISIBLE;
        //noinspection ConstantConditions
        containerPager.setVisibility(visibilityPagerViews);
        episodeDetailsTabs.setVisibility(visibilityPagerViews);
        //noinspection ConstantConditions
        dividerEpisodesTabs.setVisibility(visibilityPagerViews);
        if (updateOptionsMenu) {
            invalidateOptionsMenu();
        }
    }

    private void setupViews() {
        if (isSinglePaneView()) {
            switchView(false, false);
        }

        // Tabs setup.
        episodeDetailsTabs.setCustomTabView(R.layout.tabstrip_item_transparent,
                R.id.textViewTabStripItem);
        episodeDetailsTabs.setSelectedIndicatorColors(ContextCompat.getColor(this,
                SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_DarkBlue
                        ? R.color.white
                        : Utils.resolveAttributeToResourceId(getTheme(), R.attr.colorPrimary)));

        // Set drawables for visible shadows.
        if (shadowStart != null) {
            Shadows.getInstance().setShadowDrawable(this, shadowStart,
                    GradientDrawable.Orientation.RIGHT_LEFT);
        }
        if (shadowEnd != null) {
            Shadows.getInstance().setShadowDrawable(this, shadowEnd,
                    GradientDrawable.Orientation.LEFT_RIGHT);
        }
    }

    private void updateViews(
            Bundle savedInstanceState,
            int showTvdbId,
            int seasonTvdbId,
            int seasonNumber,
            int startPosition,
            ArrayList<Episode> episodes
    ) {
        // Episode list.
        if (episodesListFragment == null) {
            if (savedInstanceState == null) {
                episodesListFragment = EpisodesFragment.newInstance(showTvdbId, seasonTvdbId,
                        seasonNumber, startPosition);
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.add(R.id.fragment_episodes, episodesListFragment, "episodes").commit();
            } else {
                episodesListFragment = (EpisodesFragment) getSupportFragmentManager()
                        .findFragmentByTag("episodes");
            }
        }

        // Episode pager.
        if (episodeDetailsAdapter == null) {
            episodeDetailsAdapter = new EpisodePagerAdapter(this, getSupportFragmentManager(),
                    episodes, true);
            episodeDetailsPager.setAdapter(episodeDetailsAdapter);
        } else {
            episodeDetailsAdapter.updateEpisodeList(episodes);
        }
        // Refresh pager tab decoration.
        episodeDetailsTabs.setViewPager(episodeDetailsPager);

        episodeDetailsPager.setCurrentItem(startPosition, false);
        // Set page listener after current item to avoid null pointer for non-existing content view.
        episodeDetailsTabs.setOnPageChangeListener(onPageChangeListener);
    }

    @Override
    protected void onStart() {
        super.onStart();

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(onSortOrderChangedListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(onSortOrderChangedListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isSinglePaneView()) {
            getMenuInflater().inflate(R.menu.episodes_menu, menu);
            menu.findItem(R.id.menu_action_episodes_switch_view).setIcon(
                    isListGone()
                            ? R.drawable.ic_view_headline_white_24dp
                            : R.drawable.ic_view_column_white_24dp
            );
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            Intent upIntent = OverviewActivity.intentSeasons(this, showTvdbId);
            upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(upIntent);
            return true;
        } else if (itemId == R.id.menu_action_episodes_switch_view) {
            switchView(isListGone(), true);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Switch to the episode at the given position.
     */
    public void setCurrentPage(int position) {
        episodeDetailsPager.setCurrentItem(position, true);
        if (isSinglePaneView()) {
            switchView(false, true);
        }
    }

    private OnPageChangeListener onPageChangeListener = new OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // do nothing
        }

        @Override
        public void onPageSelected(int position) {
            // update currently checked episode
            episodesListFragment.setItemChecked(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // do nothing
        }
    };

    private OnSharedPreferenceChangeListener onSortOrderChangedListener
            = (sharedPreferences, key) -> {
        if (DisplaySettings.KEY_EPISODE_SORT_ORDER.equals(key)) {
            reorderAndUpdateTabs();
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(BaseNavDrawerActivity.ServiceCompletedEvent event) {
        if (event.isSuccessful && event.flagJob instanceof BaseEpisodesJob) {
            // order can only change if sorted by unwatched first
            Constants.EpisodeSorting sortOrder = DisplaySettings.getEpisodeSortOrder(this);
            if (sortOrder == Constants.EpisodeSorting.UNWATCHED_FIRST
                    && episodeDetailsTabs != null) {
                // temporarily remove page change listener to avoid scrolling to checked item
                episodeDetailsTabs.setOnPageChangeListener(null);
                reorderAndUpdateTabs();
                episodeDetailsTabs.setOnPageChangeListener(onPageChangeListener);
            }
        }
    }

    private void reorderAndUpdateTabs() {
        // Get currently selected episode
        int oldPosition = episodeDetailsPager.getCurrentItem();
        EpisodePagerAdapter adapter = episodeDetailsAdapter;
        Integer episodeTvdbIdNullable;
        if (adapter != null) {
            episodeTvdbIdNullable = adapter.getItemEpisodeTvdbId(oldPosition);
        } else {
            episodeTvdbIdNullable = 0;
        }
        int episodeTvdbId = episodeTvdbIdNullable != null ? episodeTvdbIdNullable : 0;

        // Launch update.
        viewModel.updateEpisodesData(episodeTvdbId, seasonTvdbId);
    }
}
