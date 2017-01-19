package com.battlelancer.seriesguide.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.adapters.MoviesDiscoverAdapter;
import com.battlelancer.seriesguide.loaders.TmdbMoviesLoader;
import com.battlelancer.seriesguide.util.AutoGridLayoutManager;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_movies_discover, container, false);
        unbinder = ButterKnife.bind(this, view);

        swipeRefreshLayout.setOnRefreshListener(onRefreshListener);
        swipeRefreshLayout.setRefreshing(false);

        adapter = new MoviesDiscoverAdapter(getContext());

        layoutManager = new AutoGridLayoutManager(getContext(), R.dimen.movie_grid_columnWidth, 2, 6);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int viewType = adapter.getItemViewType(position);
                if (viewType == R.layout.item_discover_link) {
                    return 3;
                }
                if (viewType == R.layout.item_grid_header) {
                    return layoutManager.getSpanCount();
                }
                if (viewType == R.layout.item_movie) {
                    return 2;
                }
                return 0;
            }
        });

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, nowPlayingLoaderCallbacks);
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventTabClick(MoviesActivity.MoviesTabClickEvent event) {
        if (event.position == MoviesActivity.TAB_POSITION_DISCOVER) {
            recyclerView.smoothScrollToPosition(0);
        }
    }

    private LoaderManager.LoaderCallbacks<TmdbMoviesLoader.Result> nowPlayingLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<TmdbMoviesLoader.Result>() {
        @Override
        public Loader<TmdbMoviesLoader.Result> onCreateLoader(int id, Bundle args) {
            return new TmdbMoviesLoader(SgApp.from(getActivity()), null);
        }

        @Override
        public void onLoadFinished(Loader<TmdbMoviesLoader.Result> loader,
                TmdbMoviesLoader.Result data) {
            if (!isAdded()) {
                return;
            }
            swipeRefreshLayout.setRefreshing(false);
            adapter.updateMovies(data.results);
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
            getLoaderManager().restartLoader(0, null, nowPlayingLoaderCallbacks);
        }
    };
}
