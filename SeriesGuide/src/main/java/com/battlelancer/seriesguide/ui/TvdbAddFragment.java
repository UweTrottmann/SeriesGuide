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

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
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
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.settings.SearchSettings;
import com.battlelancer.seriesguide.thetvdbapi.TheTVDB;
import com.battlelancer.seriesguide.thetvdbapi.TvdbException;
import com.battlelancer.seriesguide.util.SearchHistory;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import de.greenrobot.event.EventBus;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import timber.log.Timber;

public class TvdbAddFragment extends AddFragment {

    public static TvdbAddFragment newInstance() {
        return new TvdbAddFragment();
    }

    public static class TvdbAddResultsEvent {
        public int emptyTextResId;
        public List<SearchResult> results;

        public TvdbAddResultsEvent(int emptyTextResId, List<SearchResult> results) {
            this.emptyTextResId = emptyTextResId;
            this.results = results;
        }
    }

    @InjectView(R.id.buttonAddTvdbClear) ImageButton clearButton;
    @InjectView(R.id.editTextAddTvdbSearch) AutoCompleteTextView searchBox;

    private SearchTask searchTask;
    private SearchHistory searchHistory;
    private ArrayAdapter<String> searchHistoryAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_addshow_tvdb, container, false);
        ButterKnife.inject(this, v);

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
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
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

        setProgressVisible(false, false);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // create an empty adapter to avoid displaying a progress indicator
        if (adapter == null) {
            adapter = new AddAdapter(getActivity(), R.layout.item_addshow,
                    new ArrayList<SearchResult>());
        }

        if (!AndroidUtils.isNetworkConnected(getActivity())) {
            setEmptyMessage(R.string.offline);
        }

        // setup search history
        if (searchHistory == null || searchHistoryAdapter == null) {
            searchHistory = new SearchHistory(getActivity(), SearchSettings.KEY_SUFFIX_THETVDB);
            searchHistoryAdapter = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_dropdown_item_1line, searchHistory.getSearchHistory());
            searchBox.setAdapter(searchHistoryAdapter);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
        Utils.trackView(getActivity(), "TVDb Search");
    }

    @Override
    public void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
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

        // clear current search results
        setSearchResults(new LinkedList<SearchResult>());

        // nag about no connectivity
        if (!AndroidUtils.isNetworkConnected(getActivity())) {
            setEmptyMessage(R.string.offline);
            return;
        }

        String query = searchBox.getText().toString().trim();
        if (query.length() == 0) {
            return;
        }
        // query for results
        if (searchTask == null || searchTask.getStatus() == AsyncTask.Status.FINISHED) {
            setProgressVisible(true, false);
            searchTask = new SearchTask(getActivity());
            AndroidUtils.executeOnPool(searchTask, query);
        }
        // update history
        if (searchHistory.saveRecentSearch(query)) {
            searchHistoryAdapter.clear();
            searchHistoryAdapter.addAll(searchHistory.getSearchHistory());
        }
    }

    public void onEventMainThread(TvdbAddResultsEvent event) {
        setEmptyMessage(event.emptyTextResId);
        setSearchResults(event.results);
        setProgressVisible(false, true);
    }

    public static class SearchTask extends AsyncTask<String, Void, List<SearchResult>> {

        private Context mContext;

        public SearchTask(Context context) {
            mContext = context;
        }

        @Override
        protected List<SearchResult> doInBackground(String... params) {
            List<SearchResult> results;

            String query = params[0];

            try {
                results = TheTVDB.searchShow(mContext, query, false);
                if (results.size() == 0) {
                    // query again, but allow all languages
                    results = TheTVDB.searchShow(mContext, query, true);
                }
            } catch (TvdbException e) {
                Timber.e(e, "Searching show failed");
                return null;
            }

            return results;
        }

        @Override
        protected void onPostExecute(List<SearchResult> result) {
            TvdbAddResultsEvent event;
            if (result == null) {
                // display error in empty view
                event = new TvdbAddResultsEvent(R.string.search_error,
                        new LinkedList<SearchResult>());
            } else {
                // empty or there are shows
                event = new TvdbAddResultsEvent(R.string.no_results, result);
            }
            EventBus.getDefault().post(event);
        }
    }
}
