
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.SeriesDatabase;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.thetvdbapi.Series;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.Fragment;

public class EpisodesActivity extends BaseActivity {

    private Fragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.episodes_multipane);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);

        String customTitle = getIntent().getStringExtra(Intent.EXTRA_TITLE);
        if (customTitle == null) {
            customTitle = "";
        }

        final String seriesid = getIntent().getStringExtra(Shows.REF_SHOW_ID);
        final Series show = SeriesDatabase.getShow(this, seriesid);
        if (show != null) {
            String showname = show.getSeriesName();
            actionBar.setTitle(showname + " " + customTitle);
            setTitle(showname + " " + customTitle);
        } else {
            actionBar.setTitle(getString(R.string.seasons));
            setTitle(getString(R.string.seasons));
        }
        
        if (savedInstanceState == null) {
            mFragment = onCreatePane();
            mFragment.setArguments(intentToFragmentArguments(getIntent()));

            getSupportFragmentManager().beginTransaction().add(R.id.fragment_episodes, mFragment)
                    .commit();
        }
    }

    protected Fragment onCreatePane() {
        return new EpisodesFragment();
    }

}
