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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.databinding.FragmentMoviesSearchBinding;
import com.battlelancer.seriesguide.ui.MoviesActivity;
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
        adapter = new MoviesAdapter(requireContext(), new MovieClickListener(requireContext()));
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
}
