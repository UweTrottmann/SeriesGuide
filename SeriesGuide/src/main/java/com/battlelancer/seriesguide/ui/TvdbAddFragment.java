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

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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
import com.battlelancer.seriesguide.thetvdbapi.TheTVDB;
import com.battlelancer.seriesguide.thetvdbapi.TvdbException;
import com.battlelancer.seriesguide.util.SearchHistory;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import timber.log.Timber;

public class TvdbAddFragment extends AddFragment {

    public static TvdbAddFragment newInstance() {
        return new TvdbAddFragment();
    }

    @InjectView(R.id.clearButton) ImageButton mClearButton;
    @InjectView(R.id.searchbox) AutoCompleteTextView mSearchBox;

    private SearchTask mSearchTask;
    private ArrayAdapter<String> searchHistoryAdapter;
    private SearchHistory searchHistory;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_addshow_tvdb, container, false);
        ButterKnife.inject(this, v);

        mClearButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                clearSearchTerm();
            }
        });

        mSearchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
        mSearchBox.setThreshold(1);
        mSearchBox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((AutoCompleteTextView) v).showDropDown();
            }
        });
        mSearchBox.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                search();
            }
        });
        // set in code as XML is overridden
        mSearchBox.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        mSearchBox.setInputType(EditorInfo.TYPE_CLASS_TEXT);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // create an empty adapter to avoid displaying a progress indicator
        if (mAdapter == null) {
            mAdapter = new AddAdapter(getActivity(), R.layout.item_addshow,
                    new ArrayList<SearchResult>(), mDetailsButtonListener);
        }

        if (!AndroidUtils.isNetworkConnected(getActivity())) {
            setEmptyMessage(R.string.offline);
        }

        searchHistory = new SearchHistory(getActivity(), "thetvdb");
        searchHistoryAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_dropdown_item_1line, searchHistory.getSearchHistory());
        mSearchBox.setAdapter(searchHistoryAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();

        Utils.trackView(getActivity(), "TVDb Search");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        ButterKnife.reset(this);
    }

    private void clearSearchTerm() {
        mSearchBox.setText(null);
        mSearchBox.requestFocus();
    }

    private void search() {
        mSearchBox.dismissDropDown();

        // nag about no connectivity
        if (!AndroidUtils.isNetworkConnected(getActivity())) {
            setEmptyMessage(R.string.offline);
            setSearchResults(new LinkedList<SearchResult>());
            return;
        }

        String query = mSearchBox.getText().toString().trim();
        if (query.length() == 0) {
            return;
        }
        // query for results
        if (mSearchTask == null || mSearchTask.getStatus() == AsyncTask.Status.FINISHED) {
            mSearchTask = new SearchTask(getActivity());
            AndroidUtils.executeOnPool(mSearchTask, query);
        }
        // update history
        if (searchHistory.saveRecentSearch(query)) {
            searchHistoryAdapter.clear();
            searchHistoryAdapter.addAll(searchHistory.getSearchHistory());
        }
    }

    public class SearchTask extends AsyncTask<String, Void, List<SearchResult>> {

        private Context mContext;

        public SearchTask(Context context) {
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            final Activity activity = getActivity();
            if (activity != null) {
                activity.setProgressBarIndeterminateVisibility(true);
            }
        }

        @Override
        protected List<SearchResult> doInBackground(String... params) {
            List<SearchResult> results;

            String query = params[0];

            try {
                results = TheTVDB.searchShow(query, mContext);
            } catch (TvdbException e) {
                Timber.e(e, "Searching show failed");
                return null;
            }

            return results;
        }

        @Override
        protected void onPostExecute(List<SearchResult> result) {
            final Activity activity = getActivity();
            if (activity != null) {
                activity.setProgressBarIndeterminateVisibility(false);
            }
            if (result == null) {
                // display error in empty view
                setEmptyMessage(R.string.search_error);
                setSearchResults(new LinkedList<SearchResult>());
            } else {
                // empty or there are shows
                setEmptyMessage(R.string.no_results);
                setSearchResults(result);
            }
        }
    }
}
