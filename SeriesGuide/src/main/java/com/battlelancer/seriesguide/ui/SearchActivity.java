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

import android.app.ActionBar;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SearchView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.TabStripAdapter;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.widgets.SlidingTabLayout;
import de.greenrobot.event.EventBus;

/**
 * Handles search intents and displays a {@link EpisodeSearchFragment} when needed or
 * redirects directly to an {@link EpisodeDetailsActivity}.
 */
public class SearchActivity extends BaseNavDrawerActivity {

    public static final int SHOWS_LOADER_ID = 100;
    public static final int EPISODES_LOADER_ID = 101;

    public class SearchQueryEvent {

        public final Bundle args;

        public SearchQueryEvent(Bundle args) {
            this.args = args;
        }
    }

    private static final String TAG = "Search";
    private static final int EPISODES_TAB_INDEX = 1;

    private ViewPager viewPager;
    private TabStripAdapter tabsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        setupNavDrawer();

        setupActionBar();

        setupViews();

        handleIntent(getIntent());
    }

    private void setupActionBar() {
        final ActionBar actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.search_hint);
    }

    private void setupViews() {
        viewPager = (ViewPager) findViewById(R.id.pagerSearch);

        tabsAdapter = new TabStripAdapter(getSupportFragmentManager(), this,
                (ViewPager) findViewById(R.id.pagerSearch),
                (SlidingTabLayout) findViewById(R.id.tabsSearch));

        tabsAdapter.addTab(R.string.shows, ShowSearchFragment.class, null);
        tabsAdapter.addTab(R.string.episodes, EpisodeSearchFragment.class, null);

        tabsAdapter.notifyTabsChanged();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
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
            final ActionBar actionBar = getActionBar();
            actionBar.setSubtitle("\"" + query + "\"");

            // searching within a show?
            Bundle appData = extras.getBundle(SearchManager.APP_DATA);
            String showTitle = null;
            if (appData != null) {
                showTitle = appData.getString(EpisodeSearchFragment.InitBundle.SHOW_TITLE);
                if (!TextUtils.isEmpty(showTitle)) {
                    // change title + switch to episodes tab if show restriction was submitted
                    viewPager.setCurrentItem(EPISODES_TAB_INDEX);
                    actionBar.setTitle(getString(R.string.search_within_show, showTitle));
                }
            }

            // post query event to search fragments
            EventBus.getDefault().postSticky(new SearchQueryEvent(extras));
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
    protected void onDestroy() {
        super.onDestroy();

        // clear any previous search
        EventBus.getDefault().removeStickyEvent(SearchQueryEvent.class);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);

        if (SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_Light) {
            // override search view style for light theme (because we use dark actionbar theme)
            // search text
            int searchSrcTextId = getResources().getIdentifier("android:id/search_src_text", null,
                    null);
            if (searchSrcTextId != 0) {
                EditText searchEditText = (EditText) searchView.findViewById(searchSrcTextId);
                searchEditText.setTextAppearance(this, R.style.TextAppearance_Inverse);
                searchEditText.setHintTextColor(getResources().getColor(R.color.text_dim));
            }
            // close button
            int closeButtonId = getResources().getIdentifier("android:id/search_close_btn", null,
                    null);
            if (closeButtonId != 0) {
                ImageView closeButtonImage = (ImageView) searchView.findViewById(closeButtonId);
                closeButtonImage.setImageResource(R.drawable.ic_action_cancel);
            }
            // search button
            int searchIconId = getResources().getIdentifier("android:id/search_mag_icon", null,
                    null);
            if (searchIconId != 0) {
                ImageView searchIcon = (ImageView) searchView.findViewById(searchIconId);
                searchIcon.setImageResource(R.drawable.ic_action_search);
            }
        }

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
