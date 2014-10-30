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

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import butterknife.ButterKnife;
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

    private EditText searchBar;
    private View clearButton;
    private ViewPager viewPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        setupActionBar();
        setupNavDrawer();

        setupViews();

        handleIntent(getIntent());
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setCustomView(R.layout.actionbar_search);
        actionBar.setDisplayShowCustomEnabled(true);
    }

    private void setupViews() {
        clearButton = ButterKnife.findById(this, R.id.imageButtonSearchClear);
        clearButton.setVisibility(View.GONE);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchBar.setText(null);
            }
        });

        searchBar = ButterKnife.findById(this, R.id.editTextSearchBar);
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean isEmptyText = TextUtils.isEmpty(s);
                submitSearchQuery(isEmptyText ? "" : s.toString());
                clearButton.setVisibility(isEmptyText ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        viewPager = (ViewPager) findViewById(R.id.pagerSearch);

        TabStripAdapter tabsAdapter = new TabStripAdapter(getSupportFragmentManager(), this,
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

            // searching episodes within a show?
            Bundle appData = extras.getBundle(SearchManager.APP_DATA);
            if (appData != null) {
                String showTitle = appData.getString(EpisodeSearchFragment.InitBundle.SHOW_TITLE);
                if (!TextUtils.isEmpty(showTitle)) {
                    // change title + switch to episodes tab if show restriction was submitted
                    viewPager.setCurrentItem(EPISODES_TAB_INDEX);
                }
            }

            // setting the query automatically triggers a search
            String query = extras.getString(SearchManager.QUERY);
            searchBar.setText(query);
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            String id = data.getLastPathSegment();
            displayEpisode(id);
            Utils.trackCustomEvent(this, TAG, "Search action", "View");
            finish();
        }
    }

    private void submitSearchQuery(String query) {
        Bundle args = new Bundle();
        args.putString(SearchManager.QUERY, query);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Bundle appData = extras.getBundle(SearchManager.APP_DATA);
            if (appData != null) {
                args.putBundle(SearchManager.APP_DATA, appData);
            }
        }

        submitSearchQuery(args);
    }

    private void submitSearchQuery(Bundle args) {
        EventBus.getDefault().postSticky(new SearchQueryEvent(args));

        Utils.trackCustomEvent(this, TAG, "Search action", "Search");
    }

    private void displayEpisode(String episodeTvdbId) {
        Intent i = new Intent(this, EpisodesActivity.class);
        i.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, Integer.valueOf(episodeTvdbId));
        startActivity(i);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // clear any previous search
        EventBus.getDefault().removeStickyEvent(SearchQueryEvent.class);
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

    protected void fireTrackerEvent(String label) {
        Utils.trackAction(this, TAG, label);
    }

}
