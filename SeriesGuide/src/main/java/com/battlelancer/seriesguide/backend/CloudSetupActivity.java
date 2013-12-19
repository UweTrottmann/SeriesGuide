/*
 * Copyright 2013 Uwe Trottmann
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
 *
 */

package com.battlelancer.seriesguide.backend;

import com.actionbarsherlock.app.ActionBar;
import com.battlelancer.seriesguide.ui.BaseNavDrawerActivity;
import com.battlelancer.seriesguide.ui.BaseTopActivity;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;

import android.os.Bundle;

/**
 * Prepares for Hexagon setup by checking Google Play services availability.
 */
public class CloudSetupActivity extends BaseTopActivity {

    public static final String TAG = "Hexagon";

    protected static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;

    protected static final int REQUEST_ACCOUNT_PICKER = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane_drawer);
        setupNavDrawer();

        setupActionBar();

        if (savedInstanceState == null) {
            CloudSetupFragment f = new CloudSetupFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.content_frame, f).commit();
        }
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.hexagon);
        actionBar.setIcon(R.drawable.ic_action_lab);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setDrawerSelectedItem(BaseNavDrawerActivity.MENU_ITEM_CLOUD_POSITION);
    }

    @Override
    protected void fireTrackerEvent(String label) {
        Utils.trackAction(this, TAG, label);
    }

}
