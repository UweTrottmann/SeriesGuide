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

import com.google.analytics.tracking.android.EasyTracker;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.util.ShareUtils.ShareItems;
import com.uwetrottmann.seriesguide.R;

import android.os.Bundle;

public class TraktShoutsActivity extends BaseActivity {

    public static Bundle createInitBundleEpisode(int showTvdbid, int seasonNumber,
            int episodeNumber, String title) {
        Bundle extras = new Bundle();
        extras.putInt(ShareItems.TVDBID, showTvdbid);
        extras.putInt(ShareItems.SEASON, seasonNumber);
        extras.putInt(ShareItems.EPISODE, episodeNumber);
        extras.putString(ShareItems.SHARESTRING, title);
        return extras;
    }

    public static Bundle createInitBundleShow(String title, int tvdbId) {
        Bundle extras = new Bundle();
        extras.putInt(ShareItems.TVDBID, tvdbId);
        extras.putString(ShareItems.SHARESTRING, title);
        return extras;
    }

    public static Bundle createInitBundleMovie(String title, int tmdbId) {
        Bundle extras = new Bundle();
        extras.putInt(ShareItems.TMDBID, tmdbId);
        extras.putString(ShareItems.SHARESTRING, title);
        return extras;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getIntent().getExtras();
        String title = args.getString(ShareItems.SHARESTRING);

        setupActionBar();

        if (savedInstanceState == null) {
            // embed the shouts fragment dialog
            SherlockFragment f;
            int tvdbId = args.getInt(ShareItems.TVDBID);
            int episode = args.getInt(ShareItems.EPISODE);
            if (tvdbId == 0) {
                int tmdbId = args.getInt(ShareItems.TMDBID);
                f = TraktShoutsFragment.newInstanceMovie(title, tmdbId);
            } else if (episode == 0) {
                f = TraktShoutsFragment.newInstanceShow(title, tvdbId);
            } else {
                int season = args.getInt(ShareItems.SEASON);
                f = TraktShoutsFragment
                        .newInstanceEpisode(title, tvdbId, season, episode);
            }
            getSupportFragmentManager().beginTransaction().add(android.R.id.content, f)
                    .commit();
        }
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.shouts);
        actionBar.setSubtitle(getIntent().getExtras().getString(ShareItems.SHARESTRING));
    }

    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
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
