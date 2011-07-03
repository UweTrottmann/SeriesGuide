
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.beta.R;

import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.ActionBar.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

public class UpcomingRecentActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upcomingrecent);

        // setup Action Bar for tabs
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        // remove the activity title to make space for tabs
        actionBar.setDisplayShowTitleEnabled(false);

        // instantiate fragment for the tab
        Fragment upcomingFragment = new UpcomingFragment();
        // add a new tab and set its title text and tab listener
        actionBar.addTab(actionBar.newTab().setText(R.string.upcoming)
                .setTabListener(new TabListener(upcomingFragment)));

        Fragment recentFragment = new RecentFragment();
        actionBar.addTab(actionBar.newTab().setText(R.string.recent)
                .setTabListener(new TabListener(recentFragment)));
    }

    private class TabListener implements ActionBar.TabListener {
        private Fragment mFragment;

        // Called to create an instance of the listener when adding a new tab
        public TabListener(Fragment fragment) {
            mFragment = fragment;
        }

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.fragment_content, mFragment, null);
            ft.commit();
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            ft = getSupportFragmentManager().beginTransaction();
            ft.remove(mFragment);
            ft.commit();
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            // do nothing
        }

    }
}
