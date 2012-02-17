
package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.ActionBar;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.items.Episode;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.thetvdbapi.ImageCache;
import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitleProvider;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
     * extras are strings.
     */
    public interface InitBundle {
        String EPISODE_ID = "episode_id";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.episode_pager);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);

        List<Episode> episodes = new ArrayList<Episode>();
        String episodeId = getIntent().getExtras().getString(InitBundle.EPISODE_ID);
        int startPosition = 0;

        // Lookup show and season of episode
        Cursor episode = getContentResolver().query(Episodes.buildEpisodeWithShowUri(episodeId),
                new String[] {
                        Seasons.REF_SEASON_ID, Shows.POSTER, Shows.TITLE, Episodes.SEASON
                }, null, null, null);

        if (episode != null && episode.moveToFirst()) {
            // display show name as title, season as subtitle
            setTitle(episode.getString(2));
            actionBar.setTitle(episode.getString(2));
            actionBar.setSubtitle(Utils.getSeasonString(this, episode.getString(3)));

            // set show poster as background
            String posterPath = episode.getString(1);
            if (Utils.isFroyoOrHigher()) {
                // using alpha seems not to work on eclair, so only set
                // a background on froyo+ then
                final ImageView background = (ImageView) findViewById(R.id.background);
                Bitmap bg = ImageCache.getInstance(this).get(posterPath);
                if (bg != null) {
                    BitmapDrawable drawable = new BitmapDrawable(getResources(), bg);
                    drawable.setAlpha(50);
                    background.setImageDrawable(drawable);
                }
            }

            // get episode sorting
            Constants.EpisodeSorting sorting = Utils.getEpisodeSorting(this);

            // lookup episodes of season
            String seasonId = episode.getString(0);
            Cursor episodeCursor = getContentResolver().query(
                    Episodes.buildEpisodesOfSeasonUri(seasonId), new String[] {
                            Episodes._ID, Episodes.NUMBER, Episodes.SEASON
                    }, null, null, sorting.query());

            if (episodeCursor != null) {
                int i = 0;
                while (episodeCursor.moveToNext()) {
                    Episode ep = new Episode();
                    String curEpisodeId = episodeCursor.getString(0);
                    // look for episode to show initially
                    if (curEpisodeId.equalsIgnoreCase(episodeId)) {
                        startPosition = i;
                    }
                    ep.setId(curEpisodeId);
                    ep.setNumber(episodeCursor.getString(1));
                    ep.setSeason(episodeCursor.getString(2));
                    episodes.add(ep);
                    i++;
                }
            }

            episodeCursor.close();
            episode.close();
        }

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        mAdapter = new EpisodePagerAdapter(getSupportFragmentManager(), episodes, prefs);

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(mPager, startPosition);
    }

    public static class EpisodePagerAdapter extends FragmentStatePagerAdapter implements
            TitleProvider {

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
            return EpisodeDetailsFragment.newInstance(mEpisodes.get(position).getId(), false);
        }

        @Override
        public int getCount() {
            return mEpisodes.size();
        }

        @Override
        public String getTitle(int position) {
            Episode episode = mEpisodes.get(position);
            return Utils.getEpisodeNumber(mPrefs, episode.getSeason(), episode.getNumber());
        }

    }
}
