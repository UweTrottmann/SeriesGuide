package com.battlelancer.seriesguide.ui.movies;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.databinding.FragmentMoviesSearchBinding;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.ui.MoviesActivity;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.util.ViewTools;

/**
 * Integrates with a search interface and displays movies based on query results. Can pre-populate
 * the displayed movies based on a sent link.
 */
public class MoviesSearchFragment extends Fragment {

    interface OnSearchClickListener {
        void onSearchClick();
    }

    private static final String ARG_SEARCH_QUERY = "search_query";
    private static final String ARG_ID_LINK = "linkId";

    private FragmentMoviesSearchBinding binding;

    @Nullable private MoviesDiscoverLink link;
    private OnSearchClickListener searchClickListener;
    private MoviesAdapter adapter;

    static MoviesSearchFragment newInstance(@NonNull MoviesDiscoverLink link) {
        MoviesSearchFragment f = new MoviesSearchFragment();

        Bundle args = new Bundle();
        args.putInt(ARG_ID_LINK, link.id);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            searchClickListener = (OnSearchClickListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnSearchClickListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        link = MoviesDiscoverLink.fromId(requireArguments().getInt(ARG_ID_LINK));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        binding = FragmentMoviesSearchBinding.inflate(inflater, container, false);

        binding.swipeRefreshLayoutMoviesSearch
                .setSwipeableChildren(R.id.scrollViewMoviesSearch, R.id.recyclerViewMoviesSearch);
        binding.swipeRefreshLayoutMoviesSearch
                .setOnRefreshListener(onRefreshListener);
        ViewTools.setSwipeRefreshLayoutColors(requireActivity().getTheme(),
                binding.swipeRefreshLayoutMoviesSearch);

        // setup grid view
        AutoGridLayoutManager layoutManager = new AutoGridLayoutManager(getContext(),
                R.dimen.movie_grid_columnWidth, 1, 1);
        binding.recyclerViewMoviesSearch.setHasFixedSize(true);
        binding.recyclerViewMoviesSearch.setLayoutManager(layoutManager);

        // setup empty view button
        binding.emptyViewMoviesSearch
                .setButtonClickListener(v -> searchClickListener.onSearchClick());

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // setup adapter
        adapter = new MoviesAdapter(requireContext(), new MovieItemClickListener(requireActivity()));
        binding.recyclerViewMoviesSearch.setAdapter(adapter);

        binding.swipeRefreshLayoutMoviesSearch.setRefreshing(true);
        LoaderManager.getInstance(this)
                .initLoader(MoviesActivity.SEARCH_LOADER_ID, buildLoaderArgs(null),
                        searchLoaderCallbacks);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @NonNull
    private Bundle buildLoaderArgs(@Nullable String query) {
        Bundle args = new Bundle();
        args.putInt(ARG_ID_LINK, link == null ? -1 : link.id);
        args.putString(ARG_SEARCH_QUERY, query);
        return args;
    }

    void search(String query) {
        if (!binding.swipeRefreshLayoutMoviesSearch.isRefreshing()) {
            binding.swipeRefreshLayoutMoviesSearch.setRefreshing(true);
        }
        LoaderManager.getInstance(this)
                .restartLoader(MoviesActivity.SEARCH_LOADER_ID, buildLoaderArgs(query),
                        searchLoaderCallbacks);
    }

    private LoaderCallbacks<TmdbMoviesLoader.Result> searchLoaderCallbacks
            = new LoaderCallbacks<TmdbMoviesLoader.Result>() {
        @Override
        public Loader<TmdbMoviesLoader.Result> onCreateLoader(int id, Bundle args) {
            MoviesDiscoverLink link = MoviesDiscoverAdapter.DISCOVER_LINK_DEFAULT;
            String query = null;
            if (args != null) {
                link = MoviesDiscoverLink.fromId(args.getInt(ARG_ID_LINK));
                query = args.getString(ARG_SEARCH_QUERY);
            }
            return new TmdbMoviesLoader(requireContext(), link, query);
        }

        @Override
        public void onLoadFinished(@NonNull Loader<TmdbMoviesLoader.Result> loader,
                TmdbMoviesLoader.Result data) {
            if (!isAdded()) {
                return;
            }
            binding.emptyViewMoviesSearch.setMessage(data.getEmptyText());
            boolean hasNoResults = data.getResults() == null || data.getResults().size() == 0;
            binding.emptyViewMoviesSearch.setVisibility(hasNoResults ? View.VISIBLE : View.GONE);
            binding.recyclerViewMoviesSearch.setVisibility(hasNoResults ? View.GONE : View.VISIBLE);
            adapter.updateMovies(data.getResults());
            binding.swipeRefreshLayoutMoviesSearch.setRefreshing(false);
        }

        @Override
        public void onLoaderReset(@NonNull Loader<TmdbMoviesLoader.Result> loader) {
            adapter.updateMovies(null);
        }
    };

    private SwipeRefreshLayout.OnRefreshListener onRefreshListener
            = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            searchClickListener.onSearchClick();
        }
    };

    static class MovieItemClickListener implements MoviesAdapter.ItemClickListener {

        private Activity activity;

        MovieItemClickListener(Activity activity) {
            this.activity = activity;
        }

        Activity getActivity() {
            return activity;
        }

        @Override
        public void onClickMovie(int movieTmdbId, ImageView posterView) {
            // launch details activity
            Intent intent = MovieDetailsActivity.intentMovie(getActivity(), movieTmdbId);
            Utils.startActivityWithAnimation(getActivity(), intent, posterView);
        }

        @Override
        public void onClickMovieMoreOptions(final int movieTmdbId, View anchor) {
            PopupMenu popupMenu = new PopupMenu(anchor.getContext(), anchor);
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

            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.menu_action_movies_watchlist_add: {
                        MovieTools.addToWatchlist(getActivity(), movieTmdbId);
                        return true;
                    }
                    case R.id.menu_action_movies_watchlist_remove: {
                        MovieTools.removeFromWatchlist(getActivity(), movieTmdbId);
                        return true;
                    }
                    case R.id.menu_action_movies_collection_add: {
                        MovieTools.addToCollection(getActivity(), movieTmdbId);
                        return true;
                    }
                    case R.id.menu_action_movies_collection_remove: {
                        MovieTools.removeFromCollection(getActivity(), movieTmdbId);
                        return true;
                    }
                }
                return false;
            });
            popupMenu.show();
        }
    }
}
