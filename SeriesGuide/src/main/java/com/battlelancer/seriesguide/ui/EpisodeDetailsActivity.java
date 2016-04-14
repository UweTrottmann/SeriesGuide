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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.widget.ImageView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.Episode;
import com.battlelancer.seriesguide.loaders.SeasonEpisodesLoader;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.util.SeasonTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.ThemeUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.widgets.SlidingTabLayout;
import java.util.ArrayList;

/**
 * Hosts a {@link ViewPager} displaying an episode per fragment of a complete season. Used on
 * smaller screens which do not allow for multi-pane layouts or if coming from a search result
 * selection.
 */
public class EpisodeDetailsActivity extends BaseNavDrawerActivity {

    protected static final String TAG = "Episode Details";
    private static final int LOADER_EPISODE_ID = 100;
    private static final int LOADER_SEASON_ID = 101;
    private static final String ARGS_SEASON_TVDB_ID = "seasonTvdbId";
    private static final String ARGS_SEASON_NUMBER = "seasonNumber";

    private ImageView imageViewBackground;
    private SlidingTabLayout tabs;
    private ViewPager viewPager;

    private boolean canSafelyCommit;
    private int episodeTvdbId;
    private int seasonTvdbId;
    private int showTvdbId;

    /**
     * Data which has to be passed when creating this activity. All Bundle extras are integer.
     */
    public interface InitBundle {

        String EPISODE_TVDBID = "episode_tvdbid";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        canSafelyCommit = true;

        setContentView(R.layout.activity_episode);
        setupActionBar();
        setupNavDrawer();

        setupViews();
    }

    @Override
    protected void setCustomTheme() {
        // use a special immersive theme
        ThemeUtils.setImmersiveTheme(this);
    }

    private void setupViews() {
        episodeTvdbId = getIntent().getIntExtra(InitBundle.EPISODE_TVDBID, -1);
        if (episodeTvdbId == -1) {
            // have nothing to display, give up.
            finish();
            return;
        }

        imageViewBackground = ButterKnife.findById(this, R.id.imageViewEpisodeDetailsBackground);
        tabs = ButterKnife.findById(this, R.id.tabsEpisodeDetails);
        viewPager = ButterKnife.findById(this, R.id.pagerEpisodeDetails);

        // setup tabs
        tabs.setCustomTabView(R.layout.tabstrip_item_transparent, R.id.textViewTabStripItem);
        tabs.setSelectedIndicatorColors(ContextCompat.getColor(this,
                SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_DarkBlue ? R.color.white
                        : Utils.resolveAttributeToResourceId(getTheme(), R.attr.colorPrimary)));

        // start loading data
        Bundle args = new Bundle();
        args.putInt(InitBundle.EPISODE_TVDBID, episodeTvdbId);
        getSupportLoaderManager().initLoader(LOADER_EPISODE_ID, args, episodeLoaderCallbacks);
    }

    @Override
    protected void onResume() {
        super.onResume();

        canSafelyCommit = true;
    }

    @Override
    protected void onPause() {
        super.onPause();

        canSafelyCommit = false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            Intent upIntent = new Intent(this, EpisodesActivity.class);
            upIntent.putExtra(EpisodesActivity.InitBundle.SEASON_TVDBID, seasonTvdbId);
            if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                // This activity is not part of the application's task, so
                // create a new task with a synthesized back stack.
                TaskStackBuilder
                        .create(this)
                        .addNextIntent(new Intent(this, ShowsActivity.class))
                        .addNextIntent(
                                new Intent(this, OverviewActivity.class).putExtra(
                                        OverviewFragment.InitBundle.SHOW_TVDBID, showTvdbId)
                        )
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
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    interface EpisodeQuery {
        String[] PROJECTION = new String[] {
                Shows.REF_SHOW_ID,
                Shows.TITLE,
                Shows.POSTER,
                Seasons.REF_SEASON_ID,
                Episodes.SEASON
        };
        int SHOW_TVDB_ID = 0;
        int SHOW_TITLE = 1;
        int SHOW_POSTER = 2;
        int SEASON_TVDB_ID = 3;
        int SEASON_NUMBER = 4;
    }

    private LoaderManager.LoaderCallbacks<Cursor> episodeLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            int episodeTvdbId = args.getInt(InitBundle.EPISODE_TVDBID);
            return new CursorLoader(EpisodeDetailsActivity.this,
                    Episodes.buildEpisodeWithShowUri(String.valueOf(episodeTvdbId)),
                    EpisodeQuery.PROJECTION, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            populateBasicInfo(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            // do nothing, keep existing data
        }
    };

    private void populateBasicInfo(@Nullable Cursor episodeQuery) {
        if (episodeQuery == null || !episodeQuery.moveToFirst()) {
            finish();
            return;
        }

        int seasonNumber = episodeQuery.getInt(EpisodeQuery.SEASON_NUMBER);
        setupActionBar(episodeQuery.getString(EpisodeQuery.SHOW_TITLE),
                SeasonTools.getSeasonString(this, seasonNumber));

        // set show poster as background
        Utils.loadPosterBackground(this, imageViewBackground,
                episodeQuery.getString(EpisodeQuery.SHOW_POSTER));

        showTvdbId = episodeQuery.getInt(EpisodeQuery.SHOW_TVDB_ID);
        seasonTvdbId = episodeQuery.getInt(EpisodeQuery.SEASON_TVDB_ID);

        if (showTvdbId != 0) {
            updateShowDelayed(showTvdbId);
        }

        // load episodes of the whole season
        Bundle args = new Bundle();
        args.putInt(ARGS_SEASON_TVDB_ID, seasonTvdbId);
        args.putInt(ARGS_SEASON_NUMBER, seasonNumber);
        args.putInt(InitBundle.EPISODE_TVDBID, episodeTvdbId);
        getSupportLoaderManager().initLoader(LOADER_SEASON_ID, args, seasonLoaderCallbacks);
    }

    private void setupActionBar(String showTitle, String season) {
        setTitle(getString(R.string.episodes) + " " + showTitle + " " + season);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(showTitle);
        }
    }

    private LoaderManager.LoaderCallbacks<SeasonEpisodesLoader.Result> seasonLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<SeasonEpisodesLoader.Result>() {
        @Override
        public Loader<SeasonEpisodesLoader.Result> onCreateLoader(int id, Bundle args) {
            int seasonTvdbId = args.getInt(ARGS_SEASON_TVDB_ID);
            int seasonNumber = args.getInt(ARGS_SEASON_NUMBER);
            int episodeTvdbId = args.getInt(InitBundle.EPISODE_TVDBID);
            return new SeasonEpisodesLoader(EpisodeDetailsActivity.this, seasonTvdbId, seasonNumber,
                    episodeTvdbId);
        }

        @Override
        public void onLoadFinished(Loader<SeasonEpisodesLoader.Result> loader,
                SeasonEpisodesLoader.Result data) {
            populateSeason(data);
        }

        @Override
        public void onLoaderReset(Loader<SeasonEpisodesLoader.Result> loader) {
            // do nothing, keep existing data
        }
    };

    private void populateSeason(SeasonEpisodesLoader.Result data) {
        if (!canSafelyCommit) {
            return; // view pager commits fragment ops, can not do that after onPause, give up.
        }

        // setup adapter
        EpisodePagerAdapter adapter = new EpisodePagerAdapter(this, getSupportFragmentManager(),
                data.episodes, false);
        viewPager.setAdapter(adapter);
        tabs.setViewPager(viewPager);
        // set current item
        viewPager.setCurrentItem(data.requestedEpisodeIndex, false);
    }

    public static class EpisodePagerAdapter extends FragmentStatePagerAdapter {

        private ArrayList<Episode> mEpisodes;

        private Context mContext;

        private final boolean mIsMultiPane;

        public EpisodePagerAdapter(Context context, FragmentManager fm,
                ArrayList<Episode> episodes, boolean isMultiPane) {
            super(fm);
            mEpisodes = episodes;
            mContext = context;
            mIsMultiPane = isMultiPane;
        }

        @Override
        public Fragment getItem(int position) {
            return EpisodeDetailsFragment.newInstance(mEpisodes.get(position).episodeId,
                    mIsMultiPane);
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
            return TextTools.getEpisodeNumber(mContext, episode.seasonNumber,
                    episode.episodeNumber);
        }

        public void updateEpisodeList(ArrayList<Episode> list) {
            if (list != null) {
                mEpisodes = list;
                notifyDataSetChanged();
            }
        }
    }
}
