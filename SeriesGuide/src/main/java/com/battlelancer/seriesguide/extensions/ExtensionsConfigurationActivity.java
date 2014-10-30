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

package com.battlelancer.seriesguide.extensions;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.api.SeriesGuideExtension;
import com.battlelancer.seriesguide.ui.BaseActivity;

/**
 * Just hosting a {@link com.battlelancer.seriesguide.extensions.ExtensionsConfigurationFragment}.
 */
public class ExtensionsConfigurationActivity extends BaseActivity {

    public static final int LOADER_ACTIONS_ID = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane);
        setupActionBar();

        if (getIntent().hasExtra(SeriesGuideExtension.EXTRA_FROM_SERIESGUIDE_SETTINGS)) {
            // launch Amazon extension settings instead
            if (savedInstanceState == null) {
                AmazonConfigurationFragment f = new AmazonConfigurationFragment();
                android.app.FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.add(R.id.content_frame, f);
                ft.commit();
            }
        } else {
            if (savedInstanceState == null) {
                ExtensionsConfigurationFragment f = new ExtensionsConfigurationFragment();
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.add(R.id.content_frame, f);
                ft.commit();
            }
        }
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.action_extensions_configure);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
