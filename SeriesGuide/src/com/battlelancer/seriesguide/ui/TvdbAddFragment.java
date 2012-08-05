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

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.thetvdbapi.TheTVDB;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.androidutils.AndroidUtils;

import org.xml.sax.SAXException;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TvdbAddFragment extends AddFragment {

    public static TvdbAddFragment newInstance() {
        TvdbAddFragment f = new TvdbAddFragment();
        return f;
    }

    private EditText mSearchBox;

    private SearchTask mSearchTask;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /*
         * never use this here (on config change the view needed before removing
         * the fragment)
         */
        // if (container == null) {
        // return null;
        // }
        return inflater.inflate(R.layout.tvdbaddfragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // create an empty adapter to avoid displaying a progress indicator
        if (mAdapter == null) {
            mAdapter = new AddAdapter(getActivity(), R.layout.add_searchresult,
                    new ArrayList<SearchResult>(), mAddButtonListener, mDetailsButtonListener);
        }

        ImageButton searchButton = (ImageButton) getView().findViewById(R.id.searchbutton);
        searchButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                search();
            }
        });

        mSearchBox = (EditText) getView().findViewById(R.id.searchbox);
        mSearchBox.setOnKeyListener(new OnKeyListener() {

            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // we only want to react to down events
                if (event.getAction() != KeyEvent.ACTION_DOWN)
                    return false;

                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    search();
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getTracker().trackView("Add TVDb Shows");
    }

    protected void search() {
        // nag about no connectivity
        if (!AndroidUtils.isNetworkConnected(getSherlockActivity())) {
            Toast.makeText(getSherlockActivity(), R.string.offline, Toast.LENGTH_LONG).show();
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
            List<SearchResult> results = new ArrayList<SearchResult>();

            String query = params[0];

            try {
                results = TheTVDB.searchShow(query, mContext);
            } catch (IOException e) {
                return null;
            } catch (SAXException e) {
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
                Toast.makeText(mContext.getApplicationContext(), R.string.search_error,
                        Toast.LENGTH_LONG).show();
            } else if (result.isEmpty()) {
                Toast.makeText(mContext.getApplicationContext(), R.string.no_results,
                        Toast.LENGTH_LONG).show();
            } else {
                setSearchResults(result);
            }
        }

    }
}
