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

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.MoviesAdapter;
import com.battlelancer.seriesguide.loaders.TmdbMoviesLoader;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.tmdb.entities.Movie;
import java.util.List;

/**
 * Allows searching for movies on themoviedb.org, displays results in a nice grid.
 */
public class MoviesSearchFragment extends Fragment implements OnEditorActionListener,
        LoaderCallbacks<List<Movie>>, OnItemClickListener,
        MoviesAdapter.PopupMenuClickListener {

    private static final String SEARCH_QUERY_KEY = "search_query";

    protected static final String TAG = "Movies Search";

    private MoviesAdapter mAdapter;

    @InjectView(R.id.emptyViewMovieSearch) TextView mEmptyView;

    @InjectView(R.id.imageButtonClearSearch) ImageButton mClearButton;

    @InjectView(R.id.editTextMoviesSearch) EditText mSearchBox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_movies_search, container, false);
        ButterKnife.inject(this, v);

        // setup search box
        mSearchBox.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        mSearchBox.setInputType(EditorInfo.TYPE_CLASS_TEXT);
        mSearchBox.setOnEditorActionListener(this);

        // setup clear button
        mClearButton.setOnClickListener(new OnClickListener() {
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
        getActivity().setProgressBarIndeterminateVisibility(false);

        mAdapter = new MoviesAdapter(getActivity(), this);

        // setup grid view
        GridView list = (GridView) getView().findViewById(android.R.id.list);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(this);
        list.setEmptyView(mEmptyView);

        getLoaderManager().initLoader(MoviesActivity.SEARCH_LOADER_ID, null, this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        ButterKnife.reset(this);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH
                || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            search();
            return true;
        }
        return false;
    }

    private void search() {
        String query = mSearchBox.getText().toString();
        Bundle args = new Bundle();
        args.putString(SEARCH_QUERY_KEY, query);
        getLoaderManager().restartLoader(MoviesActivity.SEARCH_LOADER_ID, args, this);
    }

    @Override
    public Loader<List<Movie>> onCreateLoader(int loaderId, Bundle args) {
        String query = null;
        if (args != null) {
            query = args.getString(SEARCH_QUERY_KEY);
        }
        getActivity().setProgressBarIndeterminateVisibility(true);
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
        getActivity().setProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onLoaderReset(Loader<List<Movie>> loader) {
        mAdapter.setData(null);
        getActivity().setProgressBarIndeterminateVisibility(false);
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

    @Override
    public void onPopupMenuClick(View v, final int movieTmdbId) {
        PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
        popupMenu.inflate(R.menu.movies_popup_menu);

        // check if movie is already in watchlist or collection
        boolean isInWatchlist = false;
        boolean isInCollection = false;
        Cursor movie = getActivity().getContentResolver().query(
                SeriesGuideContract.Movies.buildMovieUri(movieTmdbId),
                new String[] { SeriesGuideContract.Movies.IN_WATCHLIST,
                        SeriesGuideContract.Movies.IN_COLLECTION }, null, null, null
        );
        if (movie != null) {
            if (movie.moveToFirst()) {
                isInWatchlist = movie.getInt(0) == 1;
                isInCollection = movie.getInt(1) == 1;
            }
            movie.close();
        }

        Menu menu = popupMenu.getMenu();
        menu.findItem(R.id.menu_action_movies_watchlist_add).setVisible(!isInWatchlist);
        menu.findItem(R.id.menu_action_movies_watchlist_remove).setVisible(isInWatchlist);
        menu.findItem(R.id.menu_action_movies_collection_add).setVisible(!isInCollection);
        menu.findItem(R.id.menu_action_movies_collection_remove).setVisible(isInCollection);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_action_movies_watchlist_add: {
                        MovieTools.addToWatchlist(getActivity(), movieTmdbId);
                        fireTrackerEvent("Add to watchlist");
                        return true;
                    }
                    case R.id.menu_action_movies_watchlist_remove: {
                        MovieTools.removeFromWatchlist(getActivity(), movieTmdbId);
                        fireTrackerEvent("Remove from watchlist");
                        return true;
                    }
                    case R.id.menu_action_movies_collection_add: {
                        MovieTools.addToCollection(getActivity(), movieTmdbId);
                        fireTrackerEvent("Add to collection");
                        return true;
                    }
                    case R.id.menu_action_movies_collection_remove: {
                        MovieTools.removeFromCollection(getActivity(), movieTmdbId);
                        fireTrackerEvent("Remove from collection");
                        return true;
                    }
                }
                return false;
            }
        });
        popupMenu.show();
    }
}
