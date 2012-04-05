
package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.ActionBar;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.Series;
import com.battlelancer.seriesguide.util.DBUtils;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

public class OverviewActivity extends BaseActivity {

    private Fragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.overview);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.description_overview));
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            mFragment = new OverviewFragment();
            mFragment.setArguments(getIntent().getExtras());

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
            ft.replace(R.id.fragment_overview, mFragment).commit();
        }
    }

    @Override
    public boolean onSearchRequested() {
        // refine search with the show's title
        int showId = getIntent().getExtras().getInt(OverviewFragment.InitBundle.SHOW_TVDBID);
        if (showId == 0) {
            return false;
        }

        final Series show = DBUtils.getShow(this, String.valueOf(showId));
        final String showTitle = show.getSeriesName();

        Bundle args = new Bundle();
        args.putString(SearchFragment.InitBundle.SHOW_TITLE, showTitle);
        startSearch(null, false, args, false);
        return true;
    }
}
