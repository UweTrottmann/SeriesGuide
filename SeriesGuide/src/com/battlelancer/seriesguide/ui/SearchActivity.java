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

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.seriesguide.R;

/**
 * Handles search intents and displays a {@link SearchFragment} when needed or
 * redirects directly to an {@link EpisodeDetailsActivity}.
 */
public class SearchActivity extends BaseActivity {

    private static final String TAG = "SearchActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);
        handleIntent(getIntent());

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        setTitle(R.string.search_title);
        actionBar.setTitle(R.string.search_title);
        actionBar.setDisplayShowTitleEnabled(true);
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
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            EasyTracker.getTracker().trackEvent(TAG, "Search action", "Search", (long) 0);
            String query = intent.getStringExtra(SearchManager.QUERY);
            getSupportActionBar().setSubtitle("\"" + query + "\"");

            // display results in a search fragment
            SearchFragment searchFragment = (SearchFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.search_fragment);
            if (searchFragment == null) {
                SearchFragment newFragment = new SearchFragment();
                newFragment.setArguments(getIntent().getExtras());
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.search_fragment, newFragment).commit();
            } else {
                searchFragment.onPerformSearch(getIntent().getExtras());
            }
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            EasyTracker.getTracker().trackEvent(TAG, "Search action", "View", (long) 0);
            Uri data = intent.getData();
            String id = data.getLastPathSegment();
            onShowEpisodeDetails(id);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.search_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_search) {
            onSearchRequested();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onShowEpisodeDetails(String id) {
        Intent i = new Intent(this, EpisodeDetailsActivity.class);
        i.putExtra(EpisodeDetailsActivity.InitBundle.EPISODE_TVDBID, Integer.valueOf(id));
        startActivity(i);
    }

}
