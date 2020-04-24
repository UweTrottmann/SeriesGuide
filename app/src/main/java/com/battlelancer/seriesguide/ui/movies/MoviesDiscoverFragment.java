package com.battlelancer.seriesguide.ui.movies;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.MoviesActivity;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.util.ViewTools;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MoviesDiscoverFragment extends Fragment {

    @BindView(R.id.swipeRefreshLayoutMoviesDiscover) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.recyclerViewMoviesDiscover) RecyclerView recyclerView;

    private MoviesDiscoverAdapter adapter;
    private GridLayoutManager layoutManager;
    private Unbinder unbinder;

    public MoviesDiscoverFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_movies_discover, container, false);
        unbinder = ButterKnife.bind(this, view);

        swipeRefreshLayout.setOnRefreshListener(onRefreshListener);
        swipeRefreshLayout.setRefreshing(false);
        ViewTools.setSwipeRefreshLayoutColors(requireActivity().getTheme(), swipeRefreshLayout);

        adapter = new MoviesDiscoverAdapter(requireContext(),
                new MovieItemClickListener(requireContext()));

        layoutManager = new AutoGridLayoutManager(getContext(),
                R.dimen.movie_grid_columnWidth, 2, 6);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int viewType = adapter.getItemViewType(position);
                if (viewType == MoviesDiscoverAdapter.VIEW_TYPE_LINK) {
                    return 3;
                }
                if (viewType == MoviesDiscoverAdapter.VIEW_TYPE_HEADER) {
                    return layoutManager.getSpanCount();
                }
                if (viewType == MoviesDiscoverAdapter.VIEW_TYPE_MOVIE) {
                    return 2;
                }
                return 0;
            }
        });

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        new ViewModelProvider(requireActivity()).get(MoviesActivityViewModel.class)
                .getScrollTabToTopLiveData()
                .observe(getViewLifecycleOwner(), event -> {
                    if (event != null
                            && event.getTabPosition() == MoviesActivity.TAB_POSITION_DISCOVER) {
                        recyclerView.smoothScrollToPosition(0);
                    }
                });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LoaderManager.getInstance(this).initLoader(0, null, nowPlayingLoaderCallbacks);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.movies_discover_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_movies_search_change_language) {
            MovieLocalizationDialogFragment.show(getParentFragmentManager());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventLanguageChanged(
            MovieLocalizationDialogFragment.LocalizationChangedEvent event) {
        LoaderManager.getInstance(this).restartLoader(0, null, nowPlayingLoaderCallbacks);
    }

    private static class MovieItemClickListener extends MovieClickListener
            implements MoviesDiscoverAdapter.ItemClickListener {

        MovieItemClickListener(Context context) {
            super(context);
        }

        @Override
        public void onClickLink(MoviesDiscoverLink link, View anchor) {
            Intent intent = new Intent(getContext(), MoviesSearchActivity.class);
            intent.putExtra(MoviesSearchActivity.EXTRA_ID_LINK, link.id);
            Utils.startActivityWithAnimation(getContext(), intent, anchor);
        }
    }

    private LoaderManager.LoaderCallbacks<TmdbMoviesLoader.Result> nowPlayingLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<TmdbMoviesLoader.Result>() {
        @Override
        public Loader<TmdbMoviesLoader.Result> onCreateLoader(int id, Bundle args) {
            return new TmdbMoviesLoader(requireContext(),
                    MoviesDiscoverAdapter.DISCOVER_LINK_DEFAULT, null);
        }

        @Override
        public void onLoadFinished(@NonNull Loader<TmdbMoviesLoader.Result> loader,
                TmdbMoviesLoader.Result data) {
            if (!isAdded()) {
                return;
            }
            swipeRefreshLayout.setRefreshing(false);
            adapter.updateMovies(data.getResults());
        }

        @Override
        public void onLoaderReset(@NonNull Loader<TmdbMoviesLoader.Result> loader) {
            adapter.updateMovies(null);
        }
    };

    private SwipeRefreshLayout.OnRefreshListener onRefreshListener
            = () -> LoaderManager.getInstance(MoviesDiscoverFragment.this)
                    .restartLoader(0, null, nowPlayingLoaderCallbacks);
}
