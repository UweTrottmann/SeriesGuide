/*
 * Copyright 2012 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.ActionBar;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.Episode;
import com.battlelancer.seriesguide.items.Series;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.ui.EpisodeDetailsActivity.EpisodePagerAdapter;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.viewpagerindicator.TitlePageIndicator;

import android.content.SharedPreferences;
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
 * Hosts a fragment which displays episodes of a season. Used on smaller screens
 * which do not allow for multi-pane layouts.
 */
public class EpisodesActivity extends BaseActivity {

    private EpisodesFragment mEpisodesFragment;

    private EpisodePagerAdapter mAdapter;

    private ViewPager mPager;

    private boolean mDualPane;

    private ArrayList<Episode> mEpisodes;

    /**
     * All values have to be integer.
     */
    public interface InitBundle {
        String SHOW_TVDBID = "show_tvdbid";

        String SEASON_TVDBID = "season_tvdbid";

        String SEASON_NUMBER = "season_number";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.episodes_multipane);

        final int showId = getIntent().getIntExtra(InitBundle.SHOW_TVDBID, 0);
        final Series show = DBUtils.getShow(this, String.valueOf(showId));
        final int seasonId = getIntent().getIntExtra(InitBundle.SEASON_TVDBID, 0);
        if (show == null || seasonId == 0) {
            finish();
            return;
        }

        // setup ActionBar
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        String showname = show.getSeriesName();

        final int seasonNumber = getIntent().getIntExtra(InitBundle.SEASON_NUMBER, -1);
        final String seasonTitle = Utils.getSeasonString(this, seasonNumber);
        setTitle(showname + " " + seasonTitle);
        actionBar.setTitle(showname);
        actionBar.setSubtitle(seasonTitle);

        // check for dual pane layout
        View pagerFragment = findViewById(R.id.pager);
        mDualPane = pagerFragment != null && pagerFragment.getVisibility() == View.VISIBLE;

        // setup the episode list fragment
        if (savedInstanceState == null) {
            mEpisodesFragment = EpisodesFragment.newInstance(showId, seasonId, seasonNumber);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            if (mDualPane) {
                // only animate enter in a dual pane layout
                ft.setCustomAnimations(R.anim.fragment_slide_left_enter,
                        R.anim.fragment_slide_left_exit);
            }
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
            Constants.EpisodeSorting sorting = Utils.getEpisodeSorting(this);

            Cursor episodeCursor = getContentResolver().query(
                    Episodes.buildEpisodesOfSeasonWithShowUri(String.valueOf(seasonId)),
                    new String[] {
                            Episodes._ID, Episodes.NUMBER
                    }, null, null, sorting.query());

            mEpisodes = new ArrayList<Episode>();
            if (episodeCursor != null) {
                while (episodeCursor.moveToNext()) {
                    Episode ep = new Episode();
                    ep.episodeId = episodeCursor.getInt(0);
                    ep.episodeNumber = episodeCursor.getInt(1);
                    ep.seasonNumber = seasonNumber;
                    mEpisodes.add(ep);
                }
            }

            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext());

            mAdapter = new EpisodePagerAdapter(getSupportFragmentManager(), mEpisodes, prefs);
            mPager = (ViewPager) pagerFragment;
            mPager.setAdapter(mAdapter);

            TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.indicator);
            indicator.setViewPager(mPager, 0);
            indicator.setOnPageChangeListener(new OnPageChangeListener() {

                @Override
                public void onPageSelected(int position) {
                    mEpisodesFragment.setItemChecked(position);
                }

                @Override
                public void onPageScrolled(int arg0, float arg1, int arg2) {
                }

                @Override
                public void onPageScrollStateChanged(int arg0) {
                }
            });
        } else {
            // FIXME Dirty: make sure no fragments are left over from a config
            // change
            for (Fragment fragment : getActiveFragments()) {
                if (fragment.getTag() == null) {
                    Log.d("EpisodesActivity", "Removing a leftover fragment");
                    getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                }
            }
        }
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

    /**
     * Switch the view pager page to show the given episode.
     * 
     * @param episodeId
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
            mPager.setCurrentItem(i, false);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fragment_slide_right_enter,
                R.anim.fragment_slide_right_exit);
    }
}
