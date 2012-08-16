/*
 * Copyright 2011 Uwe Trottmann
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

package com.battlelancer.seriesguide.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.ActionBar;
import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.items.Series;
import com.battlelancer.seriesguide.util.DBUtils;
import com.google.analytics.tracking.android.EasyTracker;

/**
 * Hosts a {@link SeasonsFragment}. Used on smaller screens which do not allow
 * for multi-pane layouts.
 */
public class SeasonsActivity extends BaseActivity {

    private Fragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane_empty);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);

        Bundle extras = getIntent().getExtras();
        int showId = extras.getInt(SeasonsFragment.InitBundle.SHOW_TVDBID);
        final Series show = DBUtils.getShow(this, String.valueOf(showId));
        if (show != null) {
            String showname = show.getSeriesName();
            actionBar.setTitle(showname);
            setTitle(showname);
        } else {
            actionBar.setTitle(getString(R.string.seasons));
            setTitle(getString(R.string.seasons));
        }

        if (savedInstanceState == null) {
            mFragment = SeasonsFragment.newInstance(showId);

            getSupportFragmentManager().beginTransaction().replace(R.id.root_container, mFragment)
                    .commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fragment_slide_right_enter,
                R.anim.fragment_slide_right_exit);
    }
}
