package com.battlelancer.seriesguide.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.items.Episode;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.service.NotificationService;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.battlelancer.seriesguide.ui.EpisodeDetailsActivity.EpisodePagerAdapter;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.SeasonTools;
import com.battlelancer.seriesguide.util.Shadows;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.widgets.SlidingTabLayout;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

/**
 * Hosts a fragment which displays episodes of a season. On larger screen hosts a {@link ViewPager}
 * displaying the episodes.
 */
public class EpisodesActivity extends BaseNavDrawerActivity {

    public static final int EPISODES_LOADER_ID = 100;
    public static final int EPISODE_LOADER_ID = 101;
    public static final int ACTIONS_LOADER_ID = 102;

    @Nullable @BindView(R.id.pagerEpisodes) ViewPager episodeDetailsPager;
    @Nullable @BindView(R.id.tabsEpisodes) SlidingTabLayout episodeDetailsTabs;
    @Nullable @BindView(R.id.imageViewEpisodesBackground) ImageView backgroundImageView;
    @Nullable @BindView(R.id.viewEpisodesShadowStart) View shadowStart;
    @Nullable @BindView(R.id.viewEpisodesShadowEnd) View shadowEnd;

    private EpisodesFragment episodesListFragment;
    private EpisodePagerAdapter episodeDetailsAdapter;
    private ArrayList<Episode> episodes;

    private int showTvdbId;
    private int seasonTvdbId;
    private int seasonNumber;

    private boolean isDualPane;

    /**
     * All values have to be integer. Only one is required.
     */
    public interface InitBundle {

        String SEASON_TVDBID = "season_tvdbid";

        String EPISODE_TVDBID = "episode_tvdbid";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_episodes);
        setupNavDrawer();

        // if coming from a notification, set last cleared time
        NotificationService.handleDeleteIntent(this, getIntent());

        // check for dual pane layout
        ButterKnife.bind(this);
        isDualPane = episodeDetailsPager != null;

        boolean isFinishing = false;

        // check if we have a certain episode to display
        final int episodeId = getIntent().getIntExtra(InitBundle.EPISODE_TVDBID, 0);
        if (episodeId != 0) {
            if (!isDualPane) {
                // display just the episode pager in its own activity
                Intent intent = new Intent(this, EpisodeDetailsActivity.class);
                intent.putExtra(EpisodeDetailsActivity.InitBundle.EPISODE_TVDBID, episodeId);
                startActivity(intent);
                isFinishing = true;
            } else {
                // get season id
                final Cursor episode = getContentResolver().query(
                        Episodes.buildEpisodeUri(String.valueOf(episodeId)), new String[] {
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
                Seasons.buildSeasonUri(String.valueOf(seasonTvdbId)), new String[] {
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

        final Show show = DBUtils.getShow(this, showTvdbId);
        if (show == null) {
            finish();
            return;
        }

        setupActionBar(show);
        setupViews(savedInstanceState, show, episodeId);

        updateShowDelayed(showTvdbId);
    }

    private void setupActionBar(Show show) {
        setupActionBar();
        // setup ActionBar
        String showTitle = show.title;
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

    private void setupViews(Bundle savedInstanceState, Show show, int episodeId) {
        // check if we should start with a specific episode
        int startPosition = 0;
        if (isDualPane) {
            // also builds episode list for view pager
            startPosition = updateEpisodeList(episodeId);
        }

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

        // if displaying details pane, set up the episode details pager
        if (isDualPane) {
            // set the pager background
            //noinspection ConstantConditions
            TvdbImageTools.loadShowPosterAlpha(this, backgroundImageView, show.poster);

            // pager setup
            episodeDetailsAdapter = new EpisodePagerAdapter(this, getSupportFragmentManager(),
                    episodes, true);
            //noinspection ConstantConditions
            episodeDetailsPager.setAdapter(episodeDetailsAdapter);

            // tabs setup
            //noinspection ConstantConditions
            episodeDetailsTabs.setCustomTabView(R.layout.tabstrip_item_transparent,
                    R.id.textViewTabStripItem);
            //noinspection ResourceType
            episodeDetailsTabs.setSelectedIndicatorColors(ContextCompat.getColor(this,
                    SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_DarkBlue
                            ? R.color.white
                            : Utils.resolveAttributeToResourceId(getTheme(), R.attr.colorPrimary)));
            episodeDetailsTabs.setViewPager(episodeDetailsPager);

            // set page listener afterwards to avoid null pointer for non-existing content view
            episodeDetailsPager.setCurrentItem(startPosition, false);
            episodeDetailsTabs.setOnPageChangeListener(mOnPageChangeListener);

            if (shadowStart != null) {
                Shadows.getInstance().setShadowDrawable(this, shadowStart,
                        GradientDrawable.Orientation.RIGHT_LEFT);
            }
            if (shadowEnd != null) {
                Shadows.getInstance().setShadowDrawable(this, shadowEnd,
                        GradientDrawable.Orientation.LEFT_RIGHT);
            }
        } else {
            // Make sure no fragments are left over from a config change
            for (Fragment fragment : getActiveFragments()) {
                if (fragment.getTag() == null) {
                    Timber.d("Removing a leftover fragment");
                    getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (isDualPane) {
            PreferenceManager.getDefaultSharedPreferences(this)
                    .registerOnSharedPreferenceChangeListener(mSortOrderChangeListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (isDualPane) {
            PreferenceManager.getDefaultSharedPreferences(this)
                    .unregisterOnSharedPreferenceChangeListener(mSortOrderChangeListener);
        }
    }

    List<WeakReference<Fragment>> mFragments = new ArrayList<>();

    @Override
    public void onAttachFragment(Fragment fragment) {
        mFragments.add(new WeakReference<>(fragment));
    }

    public ArrayList<Fragment> getActiveFragments() {
        ArrayList<Fragment> ret = new ArrayList<>();
        for (WeakReference<Fragment> ref : mFragments) {
            Fragment f = ref.get();
            if (f != null) {
                if (f.isAdded()) {
                    ret.add(f);
                }
            }
        }
        return ret;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            Intent upIntent = OverviewActivity.intentShow(this, showTvdbId);
            upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(upIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Switch to the given page, update the highlighted episode.
     *
     * <p> Only call this if the episode list and episode view pager are available.
     */
    public void setCurrentPage(int position) {
        if (episodeDetailsPager != null) {
            episodeDetailsPager.setCurrentItem(position, true);
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
                new String[] {
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

    private OnPageChangeListener mOnPageChangeListener = new OnPageChangeListener() {
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

    private OnSharedPreferenceChangeListener mSortOrderChangeListener
            = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (DisplaySettings.KEY_EPISODE_SORT_ORDER.equals(key)
                    && episodeDetailsPager != null && episodeDetailsTabs != null) {
                // save currently selected episode
                int oldPosition = episodeDetailsPager.getCurrentItem();
                int episodeId = episodes.get(oldPosition).episodeId;

                // reorder and update tabs
                updateEpisodeList();
                episodeDetailsAdapter.updateEpisodeList(episodes);
                episodeDetailsTabs.setViewPager(episodeDetailsPager);

                // scroll to previously selected episode
                setCurrentPage(getPositionForEpisode(episodeId));
            }
        }
    };

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
