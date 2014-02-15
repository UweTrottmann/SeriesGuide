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

import com.google.analytics.tracking.android.EasyTracker;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.R;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

/**
 * Handles search intents and displays a {@link SearchFragment} when needed or
 * redirects directly to an {@link EpisodeDetailsActivity}.
 */
public class SearchActivity extends BaseNavDrawerActivity {

    private static final String TAG = "Search";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);
        setupNavDrawer();

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.search_hint);

        handleIntent(getIntent());
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
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            Bundle extras = getIntent().getExtras();

            // set query as subtitle
            String query = intent.getStringExtra(SearchManager.QUERY);
            final ActionBar actionBar = getSupportActionBar();
            actionBar.setSubtitle("\"" + query + "\"");

            // searching within a show?
            Bundle appData = extras.getBundle(SearchManager.APP_DATA);
            if (appData != null) {
                String showTitle = appData.getString(SearchFragment.InitBundle.SHOW_TITLE);
                if (!TextUtils.isEmpty(showTitle)) {
                    actionBar.setTitle(getString(R.string.search_within_show, showTitle));
                }
            }

            // display results in a search fragment
            SearchFragment searchFragment = (SearchFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.search_fragment);
            if (searchFragment == null) {
                SearchFragment newFragment = new SearchFragment();
                newFragment.setArguments(extras);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, newFragment).commit();
            } else {
                searchFragment.onPerformSearch(extras);
            }
            Utils.trackCustomEvent(this, TAG, "Search action", "Search");
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            String id = data.getLastPathSegment();
            onShowEpisodeDetails(id);
            Utils.trackCustomEvent(this, TAG, "Search action", "View");
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.search_menu, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);

        // set incoming query
        String query = getIntent().getStringExtra(SearchManager.QUERY);
        searchView.setQuery(query, false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (itemId == R.id.menu_search) {
            fireTrackerEvent("Search");
            onSearchRequested();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void fireTrackerEvent(String label) {
        Utils.trackAction(this, TAG, label);
    }

    private void onShowEpisodeDetails(String id) {
        Intent i = new Intent(this, EpisodesActivity.class);
        i.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, Integer.valueOf(id));
        startActivity(i);
    }

}
