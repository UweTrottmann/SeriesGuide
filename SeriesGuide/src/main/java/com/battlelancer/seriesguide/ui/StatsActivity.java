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

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.Utils;

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
        actionBar.setTitle(R.string.statistics);
    }

    @Override
    protected void onStart() {
        super.onStart();

        setDrawerSelectedItem(BaseNavDrawerActivity.MENU_ITEM_STATS_POSITION);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // prefs might have changed, update menu
        supportInvalidateOptionsMenu();
    }

    @Override
    protected void fireTrackerEvent(String label) {
        Utils.trackAction(this, TAG, label);
    }
}
