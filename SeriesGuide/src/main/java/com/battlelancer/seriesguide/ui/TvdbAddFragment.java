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
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.loaders.TvdbAddLoader;
import com.battlelancer.seriesguide.settings.SearchSettings;
import com.battlelancer.seriesguide.util.SearchHistory;
import java.util.ArrayList;

public class TvdbAddFragment extends AddFragment {

    public static TvdbAddFragment newInstance() {
        return new TvdbAddFragment();
    }

    private static final String KEY_QUERY = "search-query";

    @Bind(R.id.buttonAddTvdbClear) ImageButton clearButton;
    @Bind(R.id.editTextAddTvdbSearch) AutoCompleteTextView searchBox;

    private SearchHistory searchHistory;
    private ArrayAdapter<String> searchHistoryAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_addshow_tvdb, container, false);
        ButterKnife.bind(this, v);

        clearButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                clearSearchTerm();
            }
        });

        searchBox.setThreshold(1);
        searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || (event != null && event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    search();
                    return true;
                }
                return false;
            }
        });
        searchBox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((AutoCompleteTextView) v).showDropDown();
            }
        });
        searchBox.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                search();
            }
        });
        // set in code as XML is overridden
        searchBox.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchBox.setInputType(EditorInfo.TYPE_CLASS_TEXT);
        // drop-down is auto-shown on config change, ensure it is hidden when recreating views
        searchBox.dismissDropDown();

        // set initial view states
        setProgressVisible(true, false);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // create adapter
        adapter = new AddAdapter(getActivity(), R.layout.item_addshow,
                new ArrayList<SearchResult>());

        // setup search history
        searchHistory = new SearchHistory(getActivity(), SearchSettings.KEY_SUFFIX_THETVDB);
        searchHistoryAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_dropdown_item_1line, searchHistory.getSearchHistory());
        searchBox.setAdapter(searchHistoryAdapter);

        // load data
        getLoaderManager().initLoader(AddActivity.AddPagerAdapter.SEARCH_TAB_DEFAULT_POSITION, null,
                mTvdbAddCallbacks);

        // enable menu
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.search_history_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_search_clear_history) {
            if (searchHistory != null && searchHistoryAdapter != null) {
                searchHistory.clearHistory();
                searchHistoryAdapter.clear();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearSearchTerm() {
        searchBox.setText(null);
        searchBox.requestFocus();
    }

    private void search() {
        searchBox.dismissDropDown();

        // extract query
        String query = searchBox.getText().toString().trim();

        // do search
        Bundle args = new Bundle();
        args.putString(KEY_QUERY, query);
        getLoaderManager().restartLoader(AddActivity.AddPagerAdapter.SEARCH_TAB_DEFAULT_POSITION,
                args, mTvdbAddCallbacks);

        // update history
        if (query.length() > 0) {
            if (searchHistory.saveRecentSearch(query)) {
                searchHistoryAdapter.clear();
                searchHistoryAdapter.addAll(searchHistory.getSearchHistory());
            }
        }
    }

    private LoaderManager.LoaderCallbacks<TvdbAddLoader.Result> mTvdbAddCallbacks
            = new LoaderManager.LoaderCallbacks<TvdbAddLoader.Result>() {
        @Override
        public Loader<TvdbAddLoader.Result> onCreateLoader(int id, Bundle args) {
            setProgressVisible(true, false);

            String query = null;
            if (args != null) {
                query = args.getString(KEY_QUERY);
            }
            return new TvdbAddLoader(getActivity(), query);
        }

        @Override
        public void onLoadFinished(Loader<TvdbAddLoader.Result> loader, TvdbAddLoader.Result data) {
            if (!isAdded()) {
                return;
            }
            setSearchResults(data.results);
            setEmptyMessage(data.emptyTextResId);
            setProgressVisible(false, true);
        }

        @Override
        public void onLoaderReset(Loader<TvdbAddLoader.Result> loader) {
            // keep existing data
        }
    };
}
