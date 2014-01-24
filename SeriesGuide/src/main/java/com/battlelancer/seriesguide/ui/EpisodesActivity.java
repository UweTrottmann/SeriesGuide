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

import com.google.analytics.tracking.android.EasyTracker;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.astuetz.PagerSlidingTabStrip;
import com.battlelancer.seriesguide.Constants;
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
import com.uwetrottmann.seriesguide.R;

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
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Hosts a fragment which displays episodes of a season. On larger screen hosts a {@link ViewPager}
 * displaying the episodes.
 */
public class EpisodesActivity extends BaseNavDrawerActivity implements
        OnSharedPreferenceChangeListener, OnPageChangeListener {

    private EpisodesFragment mEpisodesFragment;

    private EpisodePagerAdapter mAdapter;

    private ViewPager mPager;

    private PagerSlidingTabStrip mTabs;

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
        setContentView(R.layout.episodes);
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
                        Episodes.buildEpisodeUri(String.valueOf(episodeId)), new String[]{
                        Episodes._ID, Seasons.REF_SEASON_ID
                }, null, null, null);
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
                Seasons.buildSeasonUri(String.valueOf(mSeasonId)), new String[]{
                Seasons._ID, Seasons.COMBINED, Shows.REF_SHOW_ID
        }, null, null, null);
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

        // setup the episode list fragment
        if (savedInstanceState == null) {
            mEpisodesFragment = EpisodesFragment.newInstance(mShowId, mSeasonId, mSeasonNumber);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.fragment_episodes, mEpisodesFragment, "episodes").commit();
        } else {
            mEpisodesFragment = (EpisodesFragment) getSupportFragmentManager().findFragmentByTag(
                    "episodes");
        }

        // build the episode pager if we are in a dual-pane layout
        if (mDualPane) {

            // set the pager background
            final ImageView background = (ImageView) findViewById(R.id.background);
            Utils.setPosterBackground(background, show.getPoster(), this);

            // set adapters for pager and indicator
            int startPosition = updateEpisodeList(episodeId);
            mAdapter = new EpisodePagerAdapter(this, getSupportFragmentManager(), mEpisodes, false);
            mPager = (ViewPager) pager;
            mPager.setAdapter(mAdapter);

            mTabs = (PagerSlidingTabStrip) findViewById(R.id.tabsEpisodes);
            mTabs.setAllCaps(false);
            mTabs.setViewPager(mPager);

            // set page listener afterwards to avoid null pointer for
            // non-existing content view
            mPager.setCurrentItem(startPosition, false);
            mTabs.setOnPageChangeListener(this);

        } else {
            // Make sure no fragments are left over from a config
            // change
            for (Fragment fragment : getActiveFragments()) {
                if (fragment.getTag() == null) {
                    Log.d("EpisodesActivity", "Removing a leftover fragment");
                    getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                }
            }
        }
    }

    private void setupActionBar(Series show) {
        // setup ActionBar
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(show.getTitle());
        actionBar.setSubtitle(SeasonTools.getSeasonString(this, mSeasonNumber));
    }

    @Override
    protected void onStart() {
        super.onStart();

        // listen to changes to the sorting preference
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // stop listening to changes to the sorting preference
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        EasyTracker.getInstance(this).activityStop(this);
    }

    List<WeakReference<Fragment>> mFragments = new ArrayList<WeakReference<Fragment>>();

    @Override
    public void onAttachFragment(Fragment fragment) {
        mFragments.add(new WeakReference<Fragment>(fragment));
    }

    public ArrayList<Fragment> getActiveFragments() {
        ArrayList<Fragment> ret = new ArrayList<Fragment>();
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
            overridePendingTransition(R.anim.shrink_enter, R.anim.shrink_exit);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Switch the view pager page to show the given episode.
     */
    public void onChangePage(int episodeId) {
        if (mDualPane) {
            // get the index of the given episode in the pager
            int i = 0;
            for (; i < mEpisodes.size(); i++) {
                if (mEpisodes.get(i).episodeId == episodeId) {
                    break;
                }
            }

            // switch to the page immediately
            mPager.setCurrentItem(i, true);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.shrink_enter, R.anim.shrink_exit);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // update the viewpager with new sorting, if shown
        if (DisplaySettings.KEY_EPISODE_SORT_ORDER.equals(key) && mDualPane) {
            // Workaround in combination with
            // EpisodePagerAdapter.getItemPosition()
            // save visible episode
            int oldPosition = mPager.getCurrentItem();
            int episodeId = mEpisodes.get(oldPosition).episodeId;

            // reorder
            updateEpisodeList();
            mAdapter.updateEpisodeList(mEpisodes);
            mTabs.notifyDataSetChanged();

            // restore visible episode
            onChangePage(episodeId);
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
                Episodes.buildEpisodesOfSeasonWithShowUri(String.valueOf(mSeasonId)),
                new String[]{
                        Episodes._ID, Episodes.NUMBER
                }, null, null, sortOrder.query());

        ArrayList<Episode> episodeList = new ArrayList<Episode>();
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

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @Override
    public void onPageSelected(int position) {
        mEpisodesFragment.setItemChecked(position);
    }
}
