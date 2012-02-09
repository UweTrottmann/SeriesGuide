
package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.ActionBar;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.items.Episode;
import com.battlelancer.seriesguide.items.Series;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.ui.EpisodeDetailsActivity.EpisodePagerAdapter;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.thetvdbapi.ImageCache;
import com.viewpagerindicator.TitlePageIndicator;

import com.battlelancer.seriesguide.R;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;

public class EpisodesActivity extends BaseActivity {

    private Fragment mFragment;

    private EpisodePagerAdapter mAdapter;

    private ViewPager mPager;

    private boolean mDualPane;

    private ArrayList<Episode> mEpisodes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.episodes_multipane);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);

        View pagerFragment = findViewById(R.id.pager);
        mDualPane = pagerFragment != null && pagerFragment.getVisibility() == View.VISIBLE;

        String customTitle = getIntent().getStringExtra(Intent.EXTRA_TITLE);
        if (customTitle == null) {
            customTitle = "";
        }

        final String seriesid = getIntent().getStringExtra(Shows.REF_SHOW_ID);
        final Series show = DBUtils.getShow(this, seriesid);
        String posterPath = "";
        if (show != null) {
            String showname = show.getSeriesName();
            actionBar.setTitle(showname);
            setTitle(showname + " " + customTitle);
            actionBar.setSubtitle(customTitle);

            posterPath = show.getPoster();
        } else {
            // just in case
            finish();
        }

        if (savedInstanceState == null) {
            // build the episode list fragment
            mFragment = onCreatePane();
            mFragment.setArguments(intentToFragmentArguments(getIntent()));

            getSupportFragmentManager().beginTransaction().add(R.id.fragment_episodes, mFragment)
                    .commit();
        }

        // build the episode pager if we are in a multi-pane layout
        if (mDualPane) {
            // set the pager background
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

            // set adapters for pager and indicator
            Constants.EpisodeSorting sorting = Utils.getEpisodeSorting(this);
            String seasonId = getIntent().getStringExtra(Seasons._ID);

            Cursor episodeCursor = getContentResolver().query(
                    Episodes.buildEpisodesOfSeasonWithShowUri(seasonId), new String[] {
                            Episodes._ID, Episodes.NUMBER, Episodes.SEASON
                    }, null, null, sorting.query());

            mEpisodes = new ArrayList<Episode>();
            if (episodeCursor != null) {
                while (episodeCursor.moveToNext()) {
                    Episode ep = new Episode();
                    ep.setId(episodeCursor.getString(0));
                    ep.setNumber(episodeCursor.getString(1));
                    ep.setSeason(episodeCursor.getString(2));
                    mEpisodes.add(ep);
                }
            }

            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext());
            mAdapter = new EpisodePagerAdapter(getSupportFragmentManager(), mEpisodes, prefs);

            mPager = (ViewPager) pagerFragment;
            mPager.setAdapter(mAdapter);

            TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.indicator);
            indicator.setViewPager(mPager, 0);
        }
    }

    protected Fragment onCreatePane() {
        return new EpisodesFragment();
    }

    /**
     * Switch the view pager page to show the given episode.
     * 
     * @param episodeId
     */
    public void onChangePage(String episodeId) {
        if (mDualPane) {
            // get the index of the given episode in the pager
            int i = 0;
            for (; i < mEpisodes.size(); i++) {
                if (mEpisodes.get(i).getId().equalsIgnoreCase(episodeId)) {
                    break;
                }
            }

            // switch to the page immediately
            mPager.setCurrentItem(i, false);
        }
    }
}
