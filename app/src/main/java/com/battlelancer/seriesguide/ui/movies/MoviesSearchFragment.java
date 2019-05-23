package com.battlelancer.seriesguide.ui.movies;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.MoviesActivity;
import com.battlelancer.seriesguide.util.ViewTools;
import com.battlelancer.seriesguide.widgets.EmptyView;
import com.uwetrottmann.seriesguide.widgets.EmptyViewSwipeRefreshLayout;

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

    @BindView(R.id.swipeRefreshLayoutMoviesSearch) EmptyViewSwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.recyclerViewMoviesSearch) RecyclerView recyclerView;
    @BindView(R.id.emptyViewMoviesSearch) EmptyView emptyView;

    @Nullable private MoviesDiscoverLink link;
    private OnSearchClickListener searchClickListener;
    private MoviesAdapter adapter;
    private Unbinder unbinder;

    static MoviesSearchFragment newInstance(@NonNull MoviesDiscoverLink link) {
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
        View view = inflater.inflate(R.layout.fragment_movies_search, container, false);
        unbinder = ButterKnife.bind(this, view);

        swipeRefreshLayout.setSwipeableChildren(R.id.scrollViewMoviesSearch,
                R.id.recyclerViewMoviesSearch);
        swipeRefreshLayout.setOnRefreshListener(onRefreshListener);
        ViewTools.setSwipeRefreshLayoutColors(getActivity().getTheme(), swipeRefreshLayout);

        // setup grid view
        AutoGridLayoutManager layoutManager = new AutoGridLayoutManager(getContext(),
                R.dimen.movie_grid_columnWidth, 1, 1);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);

        // setup empty view button
        emptyView.setButtonClickListener(v -> searchClickListener.onSearchClick());

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // setup adapter
        adapter = new MoviesAdapter(getContext(), new MovieClickListener(getActivity()));
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setRefreshing(true);
        LoaderManager.getInstance(this)
                .initLoader(MoviesActivity.SEARCH_LOADER_ID, buildLoaderArgs(null),
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

    void search(String query) {
        if (!swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(true);
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
            return new TmdbMoviesLoader(getContext(), link, query);
        }

        @Override
        public void onLoadFinished(Loader<TmdbMoviesLoader.Result> loader,
                TmdbMoviesLoader.Result data) {
            if (!isAdded()) {
                return;
            }
            emptyView.setMessage(data.emptyText);
            boolean hasNoResults = data.results == null || data.results.size() == 0;
            emptyView.setVisibility(hasNoResults ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(hasNoResults ? View.GONE : View.VISIBLE);
            adapter.updateMovies(data.results);
            swipeRefreshLayout.setRefreshing(false);
        }

        @Override
        public void onLoaderReset(Loader<TmdbMoviesLoader.Result> loader) {
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
}
