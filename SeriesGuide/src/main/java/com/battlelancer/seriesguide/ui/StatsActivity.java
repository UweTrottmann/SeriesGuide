package com.battlelancer.seriesguide.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import com.battlelancer.seriesguide.R;

/**
 * Hosts fragments displaying statistics.
 */
public class StatsActivity extends BaseTopActivity {

    private static final String TAG = "Statistics";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane_drawer);
        setupActionBar();
        setupNavDrawer();

        if (savedInstanceState == null) {
            StatsFragment f = new StatsFragment();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.content_frame, f);
            ft.commit();
        }
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.statistics);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        setDrawerSelectedItem(R.id.navigation_item_stats);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // prefs might have changed, update menu
        supportInvalidateOptionsMenu();
    }
}
