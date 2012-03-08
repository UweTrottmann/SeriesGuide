
package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.ActionBar;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.Series;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.util.DBUtils;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class SeasonsActivity extends BaseActivity {

    private Fragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane_empty);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);

        Bundle extras = getIntent().getExtras();
        String seriesid = extras.getString(Shows._ID);
        final Series show = DBUtils.getShow(this, seriesid);
        if (show != null) {
            String showname = show.getSeriesName();
            actionBar.setTitle(showname);
            setTitle(showname);
        } else {
            actionBar.setTitle(getString(R.string.seasons));
            setTitle(getString(R.string.seasons));
        }

        if (savedInstanceState == null) {
            mFragment = new SeasonsFragment();
            mFragment.setArguments(getIntent().getExtras());

            getSupportFragmentManager().beginTransaction().replace(R.id.root_container, mFragment)
                    .commit();
        }
    }
}
