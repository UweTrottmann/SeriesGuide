
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

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.billing.BillingActivity;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.R;

import android.content.Intent;
import android.os.Bundle;

/**
 * Activities at the top of the navigation hierarchy, show menu on going up.
 */
public abstract class BaseTopActivity extends BaseNavDrawerActivity {

    private static final String TAG = "BaseTopActivity";

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        setupActionBar();

    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void setupNavDrawer() {
        super.setupNavDrawer();

        // show a drawer indicator
        setDrawerIndicatorEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.base_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        // show subscribe button if not subscribed, yet
        menu.findItem(R.id.menu_subscribe).setVisible(!Utils.hasAccessToX(this));

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // check if we should toggle the navigation drawer (app icon was touched)
        if (toggleDrawer(item)) {
            return true;
        }

        int itemId = item.getItemId();
        if (itemId == R.id.menu_subscribe) {
            startActivity(new Intent(this, BillingActivity.class));

            fireTrackerEvent("Subscribe");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Google Analytics helper method for easy sending of click events.
     */
    protected abstract void fireTrackerEvent(String label);
}
