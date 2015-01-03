
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
import android.view.MenuItem;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.TraktCredentials;

/**
 * Shows a {@link ConnectTraktFragment} or if already connected to trakt a
 * {@link ConnectTraktCredentialsFragment}.
 */
public class ConnectTraktActivity extends BaseActivity {

    static final int OAUTH_CODE_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane);
        setupActionBar();

        if (savedInstanceState == null) {
            if (TraktCredentials.get(this).hasCredentials()) {
                // immediately show credentials to allow disconnecting
                ConnectTraktCredentialsFragment f = ConnectTraktCredentialsFragment.newInstance();
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.add(R.id.content_frame, f);
                ft.commit();
            } else {
                // display trakt introduction
                ConnectTraktFragment f = new ConnectTraktFragment();
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
        actionBar.setTitle(R.string.connect_trakt);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return false;
    }
}
