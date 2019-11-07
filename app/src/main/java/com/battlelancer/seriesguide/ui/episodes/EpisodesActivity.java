package com.battlelancer.seriesguide.ui.episodes;

import android.content.Intent;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
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
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.jobs.episodes.BaseEpisodesJob;
import com.battlelancer.seriesguide.model.SgShowMinimal;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
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

    private EpisodesFragment episodesListFragment;
    private EpisodePagerAdapter episodeDetailsAdapter;
    private ArrayList<Episode> episodes;

    private int showTvdbId;
    private int seasonTvdbId;
    private int seasonNumber;

    /**
     * All values have to be integer. Only one is required.
     */
    public interface InitBundle {

        String SEASON_TVDBID = "season_tvdbid";

        String EPISODE_TVDBID = "episode_tvdbid";
    }

    /** If list and pager are displayed side-by-side, or toggleable one or the other. */
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

        // if coming from a notification, set last cleared time
        NotificationService.handleDeleteIntent(this, getIntent());

        ButterKnife.bind(this);

        boolean isFinishing = false;

        // TODO switch to ViewModel.
        // check if we have a certain episode to display
        final int episodeId = getIntent().getIntExtra(InitBundle.EPISODE_TVDBID, 0);
        if (episodeId != 0) {
            // get season id
            final Cursor episode = getContentResolver().query(
                    Episodes.buildEpisodeUri(String.valueOf(episodeId)), new String[]{
                            Episodes._ID, Seasons.REF_SEASON_ID
                    }, null, null, null
            );
            if (episode != null && episode.moveToFirst()) {
                seasonTvdbId = episode.getInt(1);
            } else {
                // could not get season id
                isFinishing = true;
            }
            if (episode != null) {
                episode.close();
            }
        }

        if (isFinishing) {
            finish();
            return;
        }

        if (seasonTvdbId == 0) {
            seasonTvdbId = getIntent().getIntExtra(InitBundle.SEASON_TVDBID, 0);
        }

        // get show id and season number
        final Cursor season = getContentResolver().query(
                Seasons.buildSeasonUri(String.valueOf(seasonTvdbId)), new String[]{
                        Seasons._ID, Seasons.COMBINED, Shows.REF_SHOW_ID
                }, null, null, null
        );
        if (season != null && season.moveToFirst()) {
            seasonNumber = season.getInt(1);
            showTvdbId = season.getInt(2);
        } else {
            isFinishing = true;
        }
        if (season != null) {
            season.close();
        }

        if (isFinishing) {
            finish();
            return;
        }

        final SgShowMinimal show = SgRoomDatabase.getInstance(this)
                .showHelper().getShowMinimal(showTvdbId);
        if (show == null) {
            finish();
            return;
        }

        setupActionBar(show);
        setupViews(savedInstanceState, show, episodeId);

        updateShowDelayed(showTvdbId);
    }

    private void setupActionBar(SgShowMinimal show) {
        setupActionBar();
        // setup ActionBar
        String showTitle = show.getTitle();
        String seasonString = SeasonTools.getSeasonString(this, seasonNumber);
        setTitle(showTitle + " " + seasonString);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);

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
        if (updateOptionsMenu) invalidateOptionsMenu();
    }

    private void setupViews(Bundle savedInstanceState, SgShowMinimal show, int episodeId) {
        if (isSinglePaneView()) {
            switchView(false, false);
        }

        // Set the image background.
        TvdbImageTools.loadShowPosterAlpha(this, backgroundImageView, show.getPosterSmall());

        // TODO Switch to ViewModel.
        // Build episode list for view pager, determine start position if episode ID is given.
        int startPosition = updateEpisodeList(episodeId);

        // set up the episode list fragment
        if (savedInstanceState == null) {
            episodesListFragment = EpisodesFragment.newInstance(showTvdbId, seasonTvdbId,
                    seasonNumber, startPosition);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.fragment_episodes, episodesListFragment, "episodes").commit();
        } else {
            episodesListFragment = (EpisodesFragment) getSupportFragmentManager().findFragmentByTag(
                    "episodes");
        }

        // pager setup
        episodeDetailsAdapter = new EpisodePagerAdapter(this, getSupportFragmentManager(),
                episodes, true);
        episodeDetailsPager.setAdapter(episodeDetailsAdapter);

        // tabs setup
        episodeDetailsTabs.setCustomTabView(R.layout.tabstrip_item_transparent,
                R.id.textViewTabStripItem);
        episodeDetailsTabs.setSelectedIndicatorColors(ContextCompat.getColor(this,
                SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_DarkBlue
                        ? R.color.white
                        : Utils.resolveAttributeToResourceId(getTheme(), R.attr.colorPrimary)));
        episodeDetailsTabs.setViewPager(episodeDetailsPager);

        // set page listener afterwards to avoid null pointer for non-existing content view
        episodeDetailsPager.setCurrentItem(startPosition, false);
        episodeDetailsTabs.setOnPageChangeListener(onPageChangeListener);

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

    /**
     * Updates the episode list, using the current sort order.
     */
    private void updateEpisodeList() {
        updateEpisodeList(0);
    }

    /**
     * Updates the episode list, using the current sorting. If a valid initial episode id is given
     * it will return its position in the created list.
     */
    private int updateEpisodeList(int initialEpisodeId) {
        Constants.EpisodeSorting sortOrder = DisplaySettings.getEpisodeSortOrder(this);

        Cursor episodeCursor = getContentResolver().query(
                Episodes.buildEpisodesOfSeasonWithShowUri(String.valueOf(seasonTvdbId)),
                new String[]{
                        Episodes._ID, Episodes.NUMBER
                }, null, null, sortOrder.query()
        );

        ArrayList<Episode> episodeList = new ArrayList<>();
        int startPosition = 0;
        if (episodeCursor != null) {
            while (episodeCursor.moveToNext()) {
                Episode ep = new Episode();
                ep.episodeId = episodeCursor.getInt(0);
                if (ep.episodeId == initialEpisodeId) {
                    startPosition = episodeCursor.getPosition();
                }
                ep.episodeNumber = episodeCursor.getInt(1);
                ep.seasonNumber = seasonNumber;
                episodeList.add(ep);
            }

            episodeCursor.close();
        }

        episodes = episodeList;

        return startPosition;
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
        if (episodeDetailsPager == null || episodeDetailsTabs == null) {
            return;
        }

        // save currently selected episode
        int oldPosition = episodeDetailsPager.getCurrentItem();
        int episodeId = episodes.get(oldPosition).episodeId;

        // reorder and update tabs
        updateEpisodeList();
        episodeDetailsAdapter.updateEpisodeList(episodes);
        episodeDetailsTabs.setViewPager(episodeDetailsPager);

        // scroll to previously selected episode
        episodeDetailsPager.setCurrentItem(getPositionForEpisode(episodeId), false);
    }

    private int getPositionForEpisode(int episodeTvdbId) {
        // find page index for this episode
        for (int position = 0; position < episodes.size(); position++) {
            if (episodes.get(position).episodeId == episodeTvdbId) {
                return position;
            }
        }

        return 0;
    }
}
