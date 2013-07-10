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

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.ViewPager;
import android.widget.ImageView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.items.Episode;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.seriesguide.R;
import com.viewpagerindicator.TitlePageIndicator;

import net.simonvt.menudrawer.MenuDrawer;

import java.util.ArrayList;

/**
 * Hosts a {@link ViewPager} displaying an episode per fragment of a complete
 * season. Used on smaller screens which do not allow for multi-pane layouts or
 * if coming from a search result selection.
 */
public class EpisodeDetailsActivity extends BaseNavDrawerActivity {
    protected static final String TAG = "Episode Details";

    private EpisodePagerAdapter mAdapter;

    private ViewPager mPager;

    private int mSeasonId;

    private int mShowId;

    /**
     * Data which has to be passed when creating this activity. All Bundle
     * extras are integer.
     */
    public interface InitBundle {
        String EPISODE_TVDBID = "episode_tvdbid";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        getMenu().setContentView(R.layout.episode_pager);

        setupActionBar();

        final int episodeId = getIntent().getIntExtra(InitBundle.EPISODE_TVDBID, 0);
        if (episodeId == 0) {
            // nothing to display
            finish();
            return;
        }

        ArrayList<Episode> episodes = new ArrayList<Episode>();
        int startPosition = 0;

        // Lookup show and season of episode
        final Cursor episode = getContentResolver().query(
                Episodes.buildEpisodeWithShowUri(String.valueOf(episodeId)), new String[] {
                        Seasons.REF_SEASON_ID, Shows.POSTER, Shows.REF_SHOW_ID
                }, null, null, null);

        if (episode == null || !episode.moveToFirst()) {
            // nothing to display
            finish();
            return;
        }

        // set show poster as background
        final ImageView background = (ImageView) findViewById(R.id.background);
        Utils.setPosterBackground(background, episode.getString(1), this);

        // lookup episodes of season
        mShowId = episode.getInt(2);
        mSeasonId = episode.getInt(0);
        Constants.EpisodeSorting sorting = Utils.getEpisodeSorting(this);

        Cursor episodeCursor = getContentResolver().query(
                Episodes.buildEpisodesOfSeasonUri(String.valueOf(mSeasonId)), new String[] {
                        Episodes._ID, Episodes.NUMBER, Episodes.SEASON
                }, null, null, sorting.query());

        if (episodeCursor != null) {
            int i = 0;
            while (episodeCursor.moveToNext()) {
                Episode ep = new Episode();
                int curEpisodeId = episodeCursor.getInt(0);
                // look for episode to show initially
                if (curEpisodeId == episodeId) {
                    startPosition = i;
                }
                ep.episodeId = curEpisodeId;
                ep.episodeNumber = episodeCursor.getInt(1);
                ep.seasonNumber = episodeCursor.getInt(2);
                episodes.add(ep);
                i++;
            }
            episodeCursor.close();
        }

        episode.close();

        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        mAdapter = new EpisodePagerAdapter(getSupportFragmentManager(), episodes, prefs, true);

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(mPager, startPosition);
    }
    
    @Override
    protected MenuDrawer getAttachedMenuDrawer() {
        MenuDrawer menuDrawer = MenuDrawer.attach(this, MenuDrawer.Type.OVERLAY);
        menuDrawer.setMenuView(R.layout.menu_frame_with_spacer);
        return menuDrawer;
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setBackgroundDrawable(null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.shrink_enter, R.anim.shrink_exit);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            Intent upIntent = new Intent(this, EpisodesActivity.class);
            upIntent.putExtra(EpisodesActivity.InitBundle.SEASON_TVDBID, mSeasonId);
            if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                // This activity is not part of the application's task, so
                // create a new task with a synthesized back stack.
                TaskStackBuilder
                        .create(this)
                        .addNextIntent(new Intent(this, ShowsActivity.class))
                        .addNextIntent(
                                new Intent(this, OverviewActivity.class).putExtra(
                                        OverviewFragment.InitBundle.SHOW_TVDBID, mShowId))
                        .addNextIntent(upIntent)
                        .startActivities();
                finish();
            } else {
                /*
                 * This activity is part of the application's task, so simply
                 * navigate up to the hierarchical parent activity.
                 * NavUtils.navigateUpTo() does not seem to work here.
                 */
                upIntent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(upIntent);
            }
            overridePendingTransition(R.anim.shrink_enter, R.anim.shrink_exit);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class EpisodePagerAdapter extends FragmentStatePagerAdapter {

        private ArrayList<Episode> mEpisodes;

        private SharedPreferences mPrefs;

        private boolean mIsShowingShowLink;

        public EpisodePagerAdapter(FragmentManager fm, ArrayList<Episode> episodes,
                SharedPreferences prefs, boolean isShowingShowLink) {
            super(fm);
            mEpisodes = episodes;
            mPrefs = prefs;
            mIsShowingShowLink = isShowingShowLink;
        }

        @Override
        public Fragment getItem(int position) {
            return EpisodeDetailsFragment.newInstance(mEpisodes.get(position).episodeId, false,
                    mIsShowingShowLink);
        }

        @Override
        public int getItemPosition(Object object) {
            /*
             * This breaks the FragmentStatePagerAdapter (see
             * http://code.google.com/p/android/issues/detail?id=37990), so we
             * just destroy everything!
             */
            // EpisodeDetailsFragment fragment = (EpisodeDetailsFragment)
            // object;
            // int episodeId = fragment.getEpisodeId();
            // for (int i = 0; i < mEpisodes.size(); i++) {
            // if (episodeId == mEpisodes.get(i).episodeId) {
            // return i;
            // }
            // }
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            if (mEpisodes != null) {
                return mEpisodes.size();
            } else {
                return 0;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Episode episode = mEpisodes.get(position);
            return Utils.getEpisodeNumber(mPrefs, episode.seasonNumber, episode.episodeNumber);
        }

        public void updateEpisodeList(ArrayList<Episode> list) {
            if (list != null) {
                mEpisodes = list;
                notifyDataSetChanged();
            }
        }

    }
}
