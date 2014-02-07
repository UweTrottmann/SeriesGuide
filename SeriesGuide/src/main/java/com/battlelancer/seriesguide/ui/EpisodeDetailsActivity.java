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
import com.actionbarsherlock.view.Window;
import com.astuetz.PagerSlidingTabStrip;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.items.Episode;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.util.Utils;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.battlelancer.seriesguide.R;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;

/**
 * Hosts a {@link ViewPager} displaying an episode per fragment of a complete season. Used on
 * smaller screens which do not allow for multi-pane layouts or if coming from a search result
 * selection.
 */
public class EpisodeDetailsActivity extends BaseNavDrawerActivity {

    protected static final String TAG = "Episode Details";

    private int mSeasonId;

    private int mShowId;

    private SystemBarTintManager mSystemBarTintManager;

    /**
     * Data which has to be passed when creating this activity. All Bundle extras are integer.
     */
    public interface InitBundle {

        String EPISODE_TVDBID = "episode_tvdbid";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.episode_pager);
        setupNavDrawer();

        setupActionBar();

        setupViews();
    }

    @Override
    protected void setCustomTheme() {
        // use a special immersive theme
        if (SeriesGuidePreferences.THEME == R.style.SeriesGuideThemeLight) {
            setTheme(R.style.ImmersiveTheme_Light);
        } else if (SeriesGuidePreferences.THEME == R.style.SeriesGuideTheme) {
            setTheme(R.style.ImmersiveTheme);
        } else {
            setTheme(R.style.ImmersiveTheme_Stock);
        }
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void setupViews() {
        // get episode id
        final int episodeId = getIntent().getIntExtra(InitBundle.EPISODE_TVDBID, 0);
        if (episodeId == 0) {
            // nothing to display
            finish();
            return;
        }

        // get show and season id, poster path
        final Cursor episode = getContentResolver().query(
                Episodes.buildEpisodeWithShowUri(String.valueOf(episodeId)), new String[]{
                Seasons.REF_SEASON_ID, Shows.POSTER, Shows.REF_SHOW_ID
        }, null, null, null);
        if (episode == null || !episode.moveToFirst()) {
            // nothing to display
            if (episode != null) {
                episode.close();
            }
            finish();
            return;
        }

        // set show poster as background
        ImageView background = (ImageView) findViewById(R.id.background);
        Utils.setPosterBackground(background, episode.getString(1), this);

        mShowId = episode.getInt(2);
        mSeasonId = episode.getInt(0);
        episode.close();

        // get episodes of season
        Constants.EpisodeSorting sortOrder = DisplaySettings.getEpisodeSortOrder(this);
        Cursor episodesOfSeason = getContentResolver().query(
                Episodes.buildEpisodesOfSeasonUri(String.valueOf(mSeasonId)), new String[]{
                Episodes._ID, Episodes.NUMBER, Episodes.SEASON
        }, null, null, sortOrder.query());

        ArrayList<Episode> episodes = new ArrayList<>();
        int startPosition = 0;
        if (episodesOfSeason != null) {
            int i = 0;
            while (episodesOfSeason.moveToNext()) {
                Episode ep = new Episode();
                int curEpisodeId = episodesOfSeason.getInt(0);
                // look for episode to show initially
                if (curEpisodeId == episodeId) {
                    startPosition = i;
                }
                ep.episodeId = curEpisodeId;
                ep.episodeNumber = episodesOfSeason.getInt(1);
                ep.seasonNumber = episodesOfSeason.getInt(2);
                episodes.add(ep);
                i++;
            }
            episodesOfSeason.close();
        }

        // setup adapter
        EpisodePagerAdapter adapter = new EpisodePagerAdapter(this, getSupportFragmentManager(),
                episodes, true);

        // setup view pager
        ViewPager pager = (ViewPager) findViewById(R.id.pagerEpisodeDetails);
        pager.setAdapter(adapter);

        // setup tabs
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabsEpisodeDetails);
        tabs.setAllCaps(false);
        tabs.setViewPager(pager);

        // fix padding for translucent system bars
        if (AndroidUtils.isKitKatOrHigher()) {
            mSystemBarTintManager = new SystemBarTintManager(this);
            SystemBarTintManager.SystemBarConfig config = getSystemBarTintManager().getConfig();
            ViewGroup contentContainer = (ViewGroup) findViewById(
                    R.id.contentContainerEpisodeDetails);
            contentContainer.setClipToPadding(false);
            contentContainer.setPadding(0, config.getPixelInsetTop(true),
                    config.getPixelInsetRight(), 0);
        }

        // set current item
        pager.setCurrentItem(startPosition, false);
    }

    public SystemBarTintManager getSystemBarTintManager() {
        return mSystemBarTintManager;
    }

    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
        EventBus.getDefault().unregister(this);
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

        private Context mContext;

        private boolean mIsShowingShowLink;

        public EpisodePagerAdapter(Context context, FragmentManager fm, ArrayList<Episode> episodes,
                boolean isShowingShowLink) {
            super(fm);
            mEpisodes = episodes;
            mContext = context;
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
            return Utils.getEpisodeNumber(mContext, episode.seasonNumber, episode.episodeNumber);
        }

        public void updateEpisodeList(ArrayList<Episode> list) {
            if (list != null) {
                mEpisodes = list;
                notifyDataSetChanged();
            }
        }

    }
}
