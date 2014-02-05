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

import com.actionbarsherlock.app.SherlockFragment;
import com.battlelancer.seriesguide.adapters.MoviesAdapter;
import com.battlelancer.seriesguide.loaders.TmdbMoviesLoader;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;
import com.uwetrottmann.tmdb.entities.Movie;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import java.util.List;

/**
 * Allows searching for movies on themoviedb.org, displays results in a nice grid.
 */
public class MoviesSearchFragment extends SherlockFragment implements OnEditorActionListener,
        LoaderCallbacks<List<Movie>>, OnItemClickListener, OnClickListener {

    private static final String SEARCH_QUERY_KEY = "search_query";

    private static final int LOADER_ID = R.layout.fragment_movies_search;

    protected static final String TAG = "Movies Search";

    private static final int CONTEXT_COLLECTION_ADD_ID = 0;

    private static final int CONTEXT_WATCHLIST_ADD_ID = 1;

    private MoviesAdapter mAdapter;

    private EditText mSearchBox;

    private TextView mEmptyView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_movies_search, container, false);

        mEmptyView = (TextView) v.findViewById(R.id.emptyViewMovieSearch);

        // setup search box
        mSearchBox = (EditText) v.findViewById(R.id.editTextMoviesSearch);
        mSearchBox.setOnEditorActionListener(this);

        // setup clear button
        v.findViewById(R.id.imageButtonClearSearch).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSearchBox.setText(null);
                        mSearchBox.requestFocus();
                    }
                });

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);

        mAdapter = new MoviesAdapter(getActivity(), this);

        // setup grid view
        GridView list = (GridView) getView().findViewById(android.R.id.list);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(this);
        list.setEmptyView(mEmptyView);

        registerForContextMenu(list);

        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public void onStart() {
        super.onStart();
        Utils.trackView(getActivity(), TAG);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(0, CONTEXT_COLLECTION_ADD_ID, 0, R.string.action_collection_add);
        menu.add(0, CONTEXT_WATCHLIST_ADD_ID, 1, R.string.watchlist_add);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        /*
         * This fixes all fragments receiving the context menu dispatch, see
         * http://stackoverflow.com/questions/5297842/how-to-handle-
         * oncontextitemselected-in-a-multi-fragment-activity and others.
         */
        if (!getUserVisibleHint()) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case CONTEXT_COLLECTION_ADD_ID: {
                AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
                Movie movie = mAdapter.getItem(info.position);
                MovieTools.addToCollection(getActivity(), movie.id);
                fireTrackerEvent("Add to collection");
                return true;
            }
            case CONTEXT_WATCHLIST_ADD_ID: {
                AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
                Movie movie = mAdapter.getItem(info.position);
                MovieTools.addToWatchlist(getActivity(), movie.id);
                fireTrackerEvent("Add to watchlist");
                return true;
            }
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        getActivity().openContextMenu(v);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH
                || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            onSearch();
            return true;
        }
        return false;
    }

    private void onSearch() {
        String query = mSearchBox.getText().toString();
        Bundle args = new Bundle();
        args.putString(SEARCH_QUERY_KEY, query);
        getLoaderManager().restartLoader(LOADER_ID, args, this);
    }

    @Override
    public Loader<List<Movie>> onCreateLoader(int loaderId, Bundle args) {
        String query = null;
        if (args != null) {
            query = args.getString(SEARCH_QUERY_KEY);
        }
        getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);
        return new TmdbMoviesLoader(getActivity(), query);
    }

    @Override
    public void onLoadFinished(Loader<List<Movie>> loader, List<Movie> data) {
        if (AndroidUtils.isNetworkConnected(getActivity())) {
            mEmptyView.setText(R.string.movies_empty);
        } else {
            mEmptyView.setText(R.string.offline);
        }
        mAdapter.setData(data);
        getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onLoaderReset(Loader<List<Movie>> loader) {
        mAdapter.setData(null);
        getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Movie movie = mAdapter.getItem(position);

        // launch details activity
        Intent i = new Intent(getActivity(), MovieDetailsActivity.class);
        i.putExtra(MovieDetailsFragment.InitBundle.TMDB_ID, movie.id);
        startActivity(i);
    }

    private void fireTrackerEvent(String label) {
        Utils.trackAction(getActivity(), TAG, label);
    }

}
