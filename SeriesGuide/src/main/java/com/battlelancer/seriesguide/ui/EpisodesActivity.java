/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.Episode;
import com.battlelancer.seriesguide.items.Series;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.service.NotificationService;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.EpisodeDetailsActivity.EpisodePagerAdapter;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.SeasonTools;
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

    private EpisodesFragment mEpisodesFragment;

    private EpisodePagerAdapter mAdapter;

    private ViewPager mPager;

    private SlidingTabLayout mTabs;

    private boolean mDualPane;

    private ArrayList<Episode> mEpisodes;

    private int mSeasonId;

    private int mSeasonNumber;

    private int mShowId;

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
        setupActionBar();
        setupNavDrawer();

        // if coming from a notification, set last cleared time
        NotificationService.handleDeleteIntent(this, getIntent());

        // check for dual pane layout
        View pager = findViewById(R.id.pagerEpisodes);
        mDualPane = pager != null && pager.getVisibility() == View.VISIBLE;

        boolean isFinishing = false;

        // check if we have a certain episode to display
        final int episodeId = getIntent().getIntExtra(InitBundle.EPISODE_TVDBID, 0);
        if (episodeId != 0) {
            if (!mDualPane) {
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
                    mSeasonId = episode.getInt(1);
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

        if (mSeasonId == 0) {
            mSeasonId = getIntent().getIntExtra(InitBundle.SEASON_TVDBID, 0);
        }

        // get show id and season number
        final Cursor season = getContentResolver().query(
                Seasons.buildSeasonUri(String.valueOf(mSeasonId)), new String[] {
                        Seasons._ID, Seasons.COMBINED, Shows.REF_SHOW_ID
                }, null, null, null
        );
        if (season != null && season.moveToFirst()) {
            mSeasonNumber = season.getInt(1);
            mShowId = season.getInt(2);
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

        final Series show = DBUtils.getShow(this, mShowId);
        if (show == null) {
            finish();
            return;
        }

        setupActionBar(show);

        // check if we should start at a certain position
        int startPosition = 0;
        if (mDualPane) {
            // also build episode list for view pager
            startPosition = updateEpisodeList(episodeId);
        }

        // setup the episode list fragment
        if (savedInstanceState == null) {
            mEpisodesFragment = EpisodesFragment.newInstance(mShowId, mSeasonId, mSeasonNumber,
                    startPosition);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.fragment_episodes, mEpisodesFragment, "episodes").commit();
        } else {
            mEpisodesFragment = (EpisodesFragment) getSupportFragmentManager().findFragmentByTag(
                    "episodes");
        }

        // setup the episode view pager if available
        if (mDualPane) {
            // set the pager background
            Utils.loadPosterBackground(this, (ImageView) findViewById(R.id.background),
                    show.getPoster());

            // setup view pager
            mAdapter = new EpisodePagerAdapter(this, getSupportFragmentManager(), mEpisodes, true);
            mPager = (ViewPager) pager;
            mPager.setAdapter(mAdapter);

            // setup tabs
            mTabs = (SlidingTabLayout) findViewById(R.id.tabsEpisodes);
            mTabs.setCustomTabView(R.layout.tabstrip_item_transparent, R.id.textViewTabStripItem);
            mTabs.setSelectedIndicatorColors(getResources().getColor(
                    SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_DarkBlue
                            ? R.color.white
                            : Utils.resolveAttributeToResourceId(getTheme(), R.attr.colorPrimary)));
            mTabs.setViewPager(mPager);

            // set page listener afterwards to avoid null pointer for
            // non-existing content view
            mPager.setCurrentItem(startPosition, false);
            mTabs.setOnPageChangeListener(mOnPageChangeListener);
        } else {
            // Make sure no fragments are left over from a config
            // change
            for (Fragment fragment : getActiveFragments()) {
                if (fragment.getTag() == null) {
                    Timber.d("Removing a leftover fragment");
                    getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                }
            }
        }

        updateShowDelayed(mShowId);
    }

    private void setupActionBar(Series show) {
        // setup ActionBar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(show.getTitle());
        actionBar.setSubtitle(SeasonTools.getSeasonString(this, mSeasonNumber));
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mDualPane) {
            PreferenceManager.getDefaultSharedPreferences(this)
                    .registerOnSharedPreferenceChangeListener(mSortOrderChangeListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mDualPane) {
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
            Intent upIntent;
            upIntent = new Intent(this, OverviewActivity.class);
            upIntent.putExtra(OverviewFragment.InitBundle.SHOW_TVDBID, mShowId);
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
        mPager.setCurrentItem(position, true);
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
                Episodes.buildEpisodesOfSeasonWithShowUri(String.valueOf(mSeasonId)),
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
                ep.seasonNumber = mSeasonNumber;
                episodeList.add(ep);
            }

            episodeCursor.close();
        }

        mEpisodes = episodeList;

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
            mEpisodesFragment.setItemChecked(position);
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
            if (DisplaySettings.KEY_EPISODE_SORT_ORDER.equals(key)) {
                // save currently selected episode
                int oldPosition = mPager.getCurrentItem();
                int episodeId = mEpisodes.get(oldPosition).episodeId;

                // reorder and update tabs
                updateEpisodeList();
                mAdapter.updateEpisodeList(mEpisodes);
                mTabs.setViewPager(mPager);

                // scroll to previously selected episode
                setCurrentPage(getPositionForEpisode(episodeId));
            }
        }
    };

    private int getPositionForEpisode(int episodeTvdbId) {
        // find page index for this episode
        for (int position = 0; position < mEpisodes.size(); position++) {
            if (mEpisodes.get(position).episodeId == episodeTvdbId) {
                return position;
            }
        }

        return 0;
    }
}
