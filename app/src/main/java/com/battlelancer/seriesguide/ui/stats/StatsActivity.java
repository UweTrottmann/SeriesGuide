package com.battlelancer.seriesguide.ui.stats;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentTransaction;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.BaseTopActivity;

/**
 * Hosts fragments displaying statistics.
 */
public class StatsActivity extends BaseTopActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        setupActionBar();
        setupBottomNavigation(R.id.navigation_item_stats);

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
    protected View getSnackbarParentView() {
        return findViewById(R.id.coordinatorLayoutStats);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // prefs might have changed, update menu
        supportInvalidateOptionsMenu();
    }
}
