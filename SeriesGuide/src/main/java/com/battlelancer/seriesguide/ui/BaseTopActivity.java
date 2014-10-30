
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

import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import com.battlelancer.seriesguide.R;

/**
 * Activities at the top of the navigation hierarchy, show menu on going up.
 */
public abstract class BaseTopActivity extends BaseNavDrawerActivity {

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void setupNavDrawer() {
        super.setupNavDrawer();

        // show a drawer indicator
        setDrawerIndicatorEnabled();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.activity_fade_enter_sg, R.anim.activity_fade_exit_sg);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // check if we should toggle the navigation drawer (app icon was touched)
        if (toggleDrawer(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Google Analytics helper method for easy sending of click events.
     */
    protected abstract void fireTrackerEvent(String label);
}
