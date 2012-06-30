
package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Window;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.Episode;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.util.Utils;
import com.viewpagerindicator.TitlePageIndicator;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class EpisodeDetailsActivity extends BaseActivity {
    protected static final String TAG = "EpisodeDetailsActivity";

    private EpisodePagerAdapter mAdapter;

    private ViewPager mPager;

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
        setContentView(R.layout.episode_pager);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        List<Episode> episodes = new ArrayList<Episode>();
        int episodeId = getIntent().getIntExtra(InitBundle.EPISODE_TVDBID, 0);
        int startPosition = 0;

        // Lookup show and season of episode
        final Cursor episode = getContentResolver().query(
                Episodes.buildEpisodeWithShowUri(String.valueOf(episodeId)), new String[] {
                        Seasons.REF_SEASON_ID, Shows.POSTER
                }, null, null, null);

        if (episode == null || !episode.moveToFirst()) {
            // nothing to display
            finish();
        }

        // set show poster as background
        final ImageView background = (ImageView) findViewById(R.id.background);
        Utils.setPosterBackground(background, episode.getString(1), this);

        // lookup episodes of season
        final String seasonId = episode.getString(0);
        Constants.EpisodeSorting sorting = Utils.getEpisodeSorting(this);

        Cursor episodeCursor = getContentResolver().query(
                Episodes.buildEpisodesOfSeasonUri(seasonId), new String[] {
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

        mAdapter = new EpisodePagerAdapter(getSupportFragmentManager(), episodes, prefs);

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(mPager, startPosition);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fragment_slide_right_enter,
                R.anim.fragment_slide_right_exit);
    }

    public static class EpisodePagerAdapter extends FragmentStatePagerAdapter {

        private List<Episode> mEpisodes;

        private SharedPreferences mPrefs;

        public EpisodePagerAdapter(FragmentManager fm, List<Episode> episodes,
                SharedPreferences prefs) {
            super(fm);
            mEpisodes = episodes;
            mPrefs = prefs;
        }

        @Override
        public Fragment getItem(int position) {
            return EpisodeDetailsFragment.newInstance(mEpisodes.get(position).episodeId, false);
        }

        @Override
        public int getCount() {
            return mEpisodes.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Episode episode = mEpisodes.get(position);
            return Utils.getEpisodeNumber(mPrefs, episode.seasonNumber, episode.episodeNumber);
        }

    }
}
