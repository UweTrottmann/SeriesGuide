package com.battlelancer.seriesguide.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.PopupMenu;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.adapters.MoviesAdapter;
import com.battlelancer.seriesguide.adapters.MoviesDiscoverAdapter;
import com.battlelancer.seriesguide.enums.MoviesDiscoverLink;
import com.battlelancer.seriesguide.loaders.TmdbMoviesLoader;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.AutoGridLayoutManager;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.widgets.EmptyView;

/**
 * Integrates with a search interface and displays movies based on query results. Can pre-populate
 * the displayed movies based on a sent link.
 */
public class MoviesSearchFragment extends Fragment {

    public interface OnSearchClickListener {
        void onSearchClick();
    }

    private static final String ARG_SEARCH_QUERY = "search_query";
    private static final String ARG_ID_LINK = "linkId";

    @BindView(R.id.containerMoviesSearchContent) View resultsContainer;
    @BindView(R.id.progressBarMoviesSearch) View progressBar;
    @BindView(R.id.recyclerViewMoviesSearch) RecyclerView recyclerView;
    @BindView(R.id.emptyViewMoviesSearch) EmptyView emptyView;

    @Nullable private MoviesDiscoverLink link;
    private OnSearchClickListener searchClickListener;
    private MoviesAdapter adapter;
    private Unbinder unbinder;

    public static MoviesSearchFragment newInstance(@NonNull MoviesDiscoverLink link) {
        MoviesSearchFragment f = new MoviesSearchFragment();

        Bundle args = new Bundle();
        args.putInt(ARG_ID_LINK, link.id);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onAttach(Context context) {
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
        link = MoviesDiscoverLink.fromId(getArguments().getInt(ARG_ID_LINK));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_movies_search, container, false);
        unbinder = ButterKnife.bind(this, v);

        // setup grid view
        AutoGridLayoutManager layoutManager = new AutoGridLayoutManager(getContext(),
                R.dimen.movie_grid_columnWidth, 1, 1);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);

        // setup empty view button
        emptyView.setButtonClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                searchClickListener.onSearchClick();
            }
        });

        setProgressVisible(false, false);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // setup adapter
        adapter = new MoviesAdapter(getContext(), new MovieItemClickListener(getActivity()));
        recyclerView.setAdapter(adapter);

        getLoaderManager().initLoader(MoviesActivity.SEARCH_LOADER_ID, buildLoaderArgs(null),
                searchLoaderCallbacks);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }

    @NonNull
    private Bundle buildLoaderArgs(@Nullable String query) {
        Bundle args = new Bundle();
        args.putInt(ARG_ID_LINK, link == null ? -1 : link.id);
        args.putString(ARG_SEARCH_QUERY, query);
        return args;
    }

    public void search(String query) {
        getLoaderManager().restartLoader(MoviesActivity.SEARCH_LOADER_ID, buildLoaderArgs(query),
                searchLoaderCallbacks);
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

    private LoaderCallbacks<TmdbMoviesLoader.Result> searchLoaderCallbacks
            = new LoaderCallbacks<TmdbMoviesLoader.Result>() {
        @Override
        public Loader<TmdbMoviesLoader.Result> onCreateLoader(int id, Bundle args) {
            setProgressVisible(true, false);
            MoviesDiscoverLink link = MoviesDiscoverAdapter.DISCOVER_LINK_DEFAULT;
            String query = null;
            if (args != null) {
                link = MoviesDiscoverLink.fromId(args.getInt(ARG_ID_LINK));
                query = args.getString(ARG_SEARCH_QUERY);
            }
            return new TmdbMoviesLoader(SgApp.from(getActivity()), link, query);
        }

        @Override
        public void onLoadFinished(Loader<TmdbMoviesLoader.Result> loader,
                TmdbMoviesLoader.Result data) {
            if (!isAdded()) {
                return;
            }
            emptyView.setMessage(data.emptyText);
            boolean hasNoResults = data.results.size() == 0;
            emptyView.setVisibility(hasNoResults ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(hasNoResults ? View.GONE : View.VISIBLE);
            adapter.updateMovies(data.results);
            setProgressVisible(false, true);
        }

        @Override
        public void onLoaderReset(Loader<TmdbMoviesLoader.Result> loader) {
            adapter.updateMovies(null);
        }
    };

    public static class MovieItemClickListener implements MoviesAdapter.ItemClickListener {

        private Activity activity;

        public MovieItemClickListener(Activity activity) {
            this.activity = activity;
        }

        public Activity getActivity() {
            return activity;
        }

        @Override
        public void onClickMovie(int movieTmdbId, ImageView posterView) {
            // launch details activity
            Intent intent = new Intent(getActivity(), MovieDetailsActivity.class);
            intent.putExtra(MovieDetailsFragment.InitBundle.TMDB_ID, movieTmdbId);
            // transition poster
            Utils.startActivityWithTransition(getActivity(), intent, posterView,
                    R.string.transitionNameMoviePoster);
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
    }
}
