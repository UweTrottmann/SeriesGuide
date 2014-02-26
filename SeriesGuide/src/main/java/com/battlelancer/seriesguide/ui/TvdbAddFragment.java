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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.thetvdbapi.TheTVDB;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TvdbAddFragment extends AddFragment {

    public static TvdbAddFragment newInstance() {
        return new TvdbAddFragment();
    }

    @InjectView(R.id.clearButton) ImageButton mClearButton;

    @InjectView(R.id.searchbox) EditText mSearchBox;

    private SearchTask mSearchTask;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tvdbaddfragment, container, false);
        ButterKnife.inject(this, v);

        mClearButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                clearSearchTerm();
            }
        });

        mSearchBox.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        mSearchBox.setInputType(EditorInfo.TYPE_CLASS_TEXT);
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

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // create an empty adapter to avoid displaying a progress indicator
        if (mAdapter == null) {
            mAdapter = new AddAdapter(getActivity(), R.layout.add_searchresult,
                    new ArrayList<SearchResult>(), mDetailsButtonListener);
        }

        if (!AndroidUtils.isNetworkConnected(getActivity())) {
            setEmptyMessage(R.string.offline);
        }
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
        // nag about no connectivity
        if (!AndroidUtils.isNetworkConnected(getSherlockActivity())) {
            setEmptyMessage(R.string.offline);
            setSearchResults(new LinkedList<SearchResult>());
            return;
        }

        String query = mSearchBox.getText().toString().trim();
        if (query.length() == 0) {
            return;
        }
        if (mSearchTask == null || mSearchTask.getStatus() == AsyncTask.Status.FINISHED) {
            mSearchTask = new SearchTask(getActivity());
            AndroidUtils.executeAsyncTask(mSearchTask, query);
        }
    }

    public class SearchTask extends AsyncTask<String, Void, List<SearchResult>> {

        private Context mContext;

        public SearchTask(Context context) {
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            final SherlockFragmentActivity activity = getSherlockActivity();
            if (activity != null) {
                activity.setSupportProgressBarIndeterminateVisibility(true);
            }
        }

        @Override
        protected List<SearchResult> doInBackground(String... params) {
            List<SearchResult> results;

            String query = params[0];

            try {
                results = TheTVDB.searchShow(query, mContext);
            } catch (IOException e) {
                return null;
            }

            return results;
        }

        @Override
        protected void onPostExecute(List<SearchResult> result) {
            final SherlockFragmentActivity activity = getSherlockActivity();
            if (activity != null) {
                activity.setSupportProgressBarIndeterminateVisibility(false);
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
