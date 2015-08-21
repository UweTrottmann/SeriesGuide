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
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.GridView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.MoviesAdapter;
import com.battlelancer.seriesguide.loaders.TmdbMoviesLoader;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.SearchSettings;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.SearchHistory;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.tmdb.entities.Movie;
import java.util.List;

/**
 * Allows searching for movies on themoviedb.org, displays results in a nice grid.
 */
public class MoviesSearchFragment extends Fragment implements
        LoaderCallbacks<List<Movie>>, OnItemClickListener,
        MoviesAdapter.PopupMenuClickListener {

    private static final String SEARCH_QUERY_KEY = "search_query";

    protected static final String TAG = "Movies Search";

    private MoviesAdapter resultsAdapter;
    private SearchHistory searchHistory;
    private ArrayAdapter<String> searchHistoryAdapter;

    @Bind(R.id.containerMoviesSearchContent) View resultsContainer;
    @Bind(R.id.progressBarMoviesSearch) View progressBar;
    @Bind(R.id.gridViewMoviesSearch) GridView resultsGridView;
    @Bind(R.id.emptyViewMoviesSearch) TextView emptyView;
    @Bind(R.id.editTextMoviesSearch) AutoCompleteTextView searchBox;
    @Bind(R.id.buttonMoviesSearchClear) View clearButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_movies_search, container, false);
        ButterKnife.bind(this, v);

        // setup grid view
        resultsGridView.setEmptyView(emptyView);
        resultsGridView.setOnItemClickListener(this);

        // setup search box
        searchBox.setThreshold(1);
        searchBox.setOnEditorActionListener(new OnEditorActionListener() {
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

        // setup clear button
        clearButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                searchBox.setText(null);
                searchBox.requestFocus();
            }
        });

        setProgressVisible(false, false);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // setup adapter
        resultsAdapter = new MoviesAdapter(getActivity(), this);
        resultsGridView.setAdapter(resultsAdapter);

        // setup search history
        if (searchHistory == null || searchHistoryAdapter == null) {
            searchHistory = new SearchHistory(getActivity(), SearchSettings.KEY_SUFFIX_TMDB);
            searchHistoryAdapter = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_dropdown_item_1line, searchHistory.getSearchHistory());
            searchBox.setAdapter(searchHistoryAdapter);
        }

        getLoaderManager().initLoader(MoviesActivity.SEARCH_LOADER_ID, null, this);

        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        ButterKnife.unbind(this);
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

    private void search() {
        String query = searchBox.getText().toString();
        Bundle args = new Bundle();
        args.putString(SEARCH_QUERY_KEY, query);
        getLoaderManager().restartLoader(MoviesActivity.SEARCH_LOADER_ID, args, this);

        // update history
        if (searchHistory.saveRecentSearch(query)) {
            searchHistoryAdapter.clear();
            searchHistoryAdapter.addAll(searchHistory.getSearchHistory());
        }
    }

    private void setProgressVisible(boolean visible, boolean animate) {
        if (animate) {
            Animation out = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
            Animation in = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
            resultsContainer.startAnimation(visible ? out : in);
            progressBar.startAnimation(visible ? in : out);
        }
        resultsContainer.setVisibility(visible ? View.GONE : View.VISIBLE);
        progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public Loader<List<Movie>> onCreateLoader(int loaderId, Bundle args) {
        setProgressVisible(true, false);
        String query = null;
        if (args != null) {
            query = args.getString(SEARCH_QUERY_KEY);
        }
        return new TmdbMoviesLoader(getActivity(), query);
    }

    @Override
    public void onLoadFinished(Loader<List<Movie>> loader, List<Movie> data) {
        if (!isAdded()) {
            return;
        }
        if (AndroidUtils.isNetworkConnected(getActivity())) {
            emptyView.setText(R.string.movies_empty);
        } else {
            emptyView.setText(R.string.offline);
        }
        resultsAdapter.setData(data);
        setProgressVisible(false, true);
    }

    @Override
    public void onLoaderReset(Loader<List<Movie>> loader) {
        resultsAdapter.setData(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Movie movie = resultsAdapter.getItem(position);

        // launch details activity
        Intent i = new Intent(getActivity(), MovieDetailsActivity.class);
        i.putExtra(MovieDetailsFragment.InitBundle.TMDB_ID, movie.id);

        ActivityCompat.startActivity(getActivity(), i,
                ActivityOptionsCompat
                        .makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight())
                        .toBundle()
        );
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
