package com.battlelancer.seriesguide.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
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
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.adapters.MoviesAdapter;
import com.battlelancer.seriesguide.loaders.TmdbMoviesLoader;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.SearchSettings;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.SearchHistory;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.widgets.EmptyView;
import com.uwetrottmann.tmdb2.entities.Movie;

/**
 * Allows searching for movies on themoviedb.org, displays results in a nice grid.
 */
public class MoviesSearchFragment extends Fragment implements OnItemClickListener,
        MoviesAdapter.PopupMenuClickListener {

    private static final String SEARCH_QUERY_KEY = "search_query";

    @BindView(R.id.containerMoviesSearchContent) View resultsContainer;
    @BindView(R.id.progressBarMoviesSearch) View progressBar;
    @BindView(R.id.gridViewMoviesSearch) GridView resultsGridView;
    @BindView(R.id.emptyViewMoviesSearch) EmptyView emptyView;
    @BindView(R.id.editTextMoviesSearch) AutoCompleteTextView searchBox;
    @BindView(R.id.buttonMoviesSearchClear) View clearButton;

    private MoviesAdapter resultsAdapter;
    private SearchHistory searchHistory;
    private ArrayAdapter<String> searchHistoryAdapter;
    private Unbinder unbinder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_movies_search, container, false);
        unbinder = ButterKnife.bind(this, v);

        // setup grid view
        // enable app bar scrolling out of view only on L or higher
        ViewCompat.setNestedScrollingEnabled(resultsGridView,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
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

        // setup empty view button
        emptyView.setButtonClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                search();
            }
        });

        setProgressVisible(false, false);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // setup adapter
        resultsAdapter = new MoviesAdapter(getContext(), this);
        resultsGridView.setAdapter(resultsAdapter);

        // setup search history
        if (searchHistory == null || searchHistoryAdapter == null) {
            searchHistory = new SearchHistory(getContext(), SearchSettings.KEY_SUFFIX_TMDB);
            searchHistoryAdapter = new ArrayAdapter<>(getContext(), R.layout.item_dropdown,
                    searchHistory.getSearchHistory());
            searchBox.setAdapter(searchHistoryAdapter);
        }

        getLoaderManager().initLoader(MoviesActivity.SEARCH_LOADER_ID, null, searchLoaderCallbacks);

        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
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
        getLoaderManager().restartLoader(MoviesActivity.SEARCH_LOADER_ID, args,
                searchLoaderCallbacks);

        // update history
        if (searchHistory.saveRecentSearch(query)) {
            searchHistoryAdapter.clear();
            searchHistoryAdapter.addAll(searchHistory.getSearchHistory());
        }
    }

    private void setProgressVisible(boolean visible, boolean animate) {
        if (animate) {
            Animation out = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out);
            Animation in = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);
            resultsContainer.startAnimation(visible ? out : in);
            progressBar.startAnimation(visible ? in : out);
        }
        resultsContainer.setVisibility(visible ? View.GONE : View.VISIBLE);
        progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Movie movie = resultsAdapter.getItem(position);

        // launch details activity
        Intent i = new Intent(getActivity(), MovieDetailsActivity.class);
        i.putExtra(MovieDetailsFragment.InitBundle.TMDB_ID, movie.id);

        MoviesAdapter.ViewHolder viewHolder
                = (MoviesAdapter.ViewHolder) view.getTag();
        Utils.startActivityWithTransition(getActivity(), i, viewHolder.poster,
                R.string.transitionNameMoviePoster);
    }

    @Override
    public void onPopupMenuClick(View v, final int movieTmdbId) {
        PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
        popupMenu.inflate(R.menu.movies_popup_menu);

        // check if movie is already in watchlist or collection
        boolean isInWatchlist = false;
        boolean isInCollection = false;
        Cursor movie = getContext().getContentResolver().query(
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
                        MovieTools.addToWatchlist(SgApp.from(getActivity()), movieTmdbId);
                        return true;
                    }
                    case R.id.menu_action_movies_watchlist_remove: {
                        MovieTools.removeFromWatchlist(SgApp.from(getActivity()), movieTmdbId);
                        return true;
                    }
                    case R.id.menu_action_movies_collection_add: {
                        MovieTools.addToCollection(SgApp.from(getActivity()), movieTmdbId);
                        return true;
                    }
                    case R.id.menu_action_movies_collection_remove: {
                        MovieTools.removeFromCollection(SgApp.from(getActivity()), movieTmdbId);
                        return true;
                    }
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private LoaderCallbacks<TmdbMoviesLoader.Result> searchLoaderCallbacks
            = new LoaderCallbacks<TmdbMoviesLoader.Result>() {
        @Override
        public Loader<TmdbMoviesLoader.Result> onCreateLoader(int id, Bundle args) {
            setProgressVisible(true, false);
            String query = null;
            if (args != null) {
                query = args.getString(SEARCH_QUERY_KEY);
            }
            return new TmdbMoviesLoader(SgApp.from(getActivity()), query);
        }

        @Override
        public void onLoadFinished(Loader<TmdbMoviesLoader.Result> loader,
                TmdbMoviesLoader.Result data) {
            if (!isAdded()) {
                return;
            }
            emptyView.setMessage(data.emptyText);
            resultsAdapter.setData(data.results);
            setProgressVisible(false, true);
        }

        @Override
        public void onLoaderReset(Loader<TmdbMoviesLoader.Result> loader) {
            resultsAdapter.setData(null);
        }
    };
}
