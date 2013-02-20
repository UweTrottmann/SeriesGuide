/*
 * Copyright 2012 Uwe Trottmann
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

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.actionbarsherlock.app.SherlockFragment;
import com.battlelancer.seriesguide.adapters.MoviesAdapter;
import com.battlelancer.seriesguide.loaders.TmdbMoviesLoader;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.seriesguide.R;
import com.uwetrottmann.tmdb.entities.Movie;

import java.util.List;

/**
 * Allows searching for movies on themoviedb.org, displays results in a nice
 * grid.
 */
public class MovieSearchFragment extends SherlockFragment implements OnEditorActionListener,
        LoaderCallbacks<List<Movie>>, OnItemClickListener {

    private static final String SEARCH_QUERY_KEY = "search_query";
    private static final int LOADER_ID = R.layout.movies_fragment;
    protected static final String TAG = "Movies Search";

    private EditText mSearchBox;
    private MoviesAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.movies_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);

        // setup search box
        mSearchBox = (EditText) getView().findViewById(R.id.editTextCheckinSearch);
        mSearchBox.setOnEditorActionListener(this);

        // setup clear button
        getView().findViewById(R.id.imageButtonClearSearch).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSearchBox.setText(null);
                        mSearchBox.requestFocus();
                    }
                });

        mAdapter = new MoviesAdapter(getActivity());

        // setup grid view
        GridView list = (GridView) getView().findViewById(R.id.gridViewMovies);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(this);
        list.setEmptyView(getView().findViewById(R.id.empty));

        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getTracker().sendView(TAG);
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
        
        // launch details fragment
        MovieDetailsFragment f = new MovieDetailsFragment();
        Bundle args = new Bundle();
        args.putInt(MovieDetailsFragment.InitBundle.TMDB_ID, movie.id);
        f.setArguments(args);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.movies_container, f);
        ft.addToBackStack(null);
        ft.commit();
    }
}
