package com.battlelancer.seriesguide.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.MoviesNowAdapter;
import com.battlelancer.seriesguide.adapters.NowAdapter;
import com.battlelancer.seriesguide.loaders.TraktFriendsMovieHistoryLoader;
import com.battlelancer.seriesguide.loaders.TraktRecentMovieHistoryLoader;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.util.GridInsetDecoration;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.widgets.EmptyViewSwipeRefreshLayout;
import java.util.List;

/**
 * Displays recently watched movies, today's releases and recent watches from trakt friends (if
 * connected to trakt).
 */
public class MoviesNowFragment extends Fragment {

    @BindView(R.id.swipeRefreshLayoutNow) EmptyViewSwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.recyclerViewNow) RecyclerView recyclerView;
    @BindView(R.id.emptyViewNow) TextView emptyView;
    @BindView(R.id.containerSnackbar) View snackbar;
    @BindView(R.id.textViewSnackbar) TextView snackbarText;
    @BindView(R.id.buttonSnackbar) Button snackbarButton;

    private MoviesNowAdapter adapter;
    private boolean isLoadingRecentlyWatched;
    private boolean isLoadingFriends;
    private Unbinder unbinder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_now, container, false);
        unbinder = ButterKnife.bind(this, v);

        swipeRefreshLayout.setSwipeableChildren(R.id.scrollViewNow, R.id.recyclerViewNow);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshStream();
            }
        });
        swipeRefreshLayout.setProgressViewOffset(false,
                getResources().getDimensionPixelSize(
                        R.dimen.swipe_refresh_progress_bar_start_margin),
                getResources().getDimensionPixelSize(
                        R.dimen.swipe_refresh_progress_bar_end_margin));

        emptyView.setText(R.string.now_movies_empty);

        showError(null);
        snackbarButton.setText(R.string.refresh);
        snackbarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshStream();
            }
        });

        // recycler view layout manager
        final int spanCount = getResources().getInteger(R.integer.grid_column_count);
        final GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), spanCount);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (adapter == null) {
                    return 1;
                }
                if (position >= adapter.getItemCount()) {
                    return 1;
                }
                // make headers and more links span all columns
                int type = adapter.getItem(position).type;
                return (type == NowAdapter.ItemType.HEADER || type == NowAdapter.ItemType.MORE_LINK)
                        ? spanCount : 1;
            }
        });
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new GridInsetDecoration(getResources()));
        recyclerView.setHasFixedSize(true);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        int accentColorResId = Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                R.attr.colorAccent);
        swipeRefreshLayout.setColorSchemeResources(accentColorResId, R.color.teal_500);

        // define dataset
        adapter = new MoviesNowAdapter(getActivity(), itemClickListener);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                updateEmptyState();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                updateEmptyState();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                updateEmptyState();
            }
        });
        recyclerView.setAdapter(adapter);

        // if connected to trakt, replace local history with trakt history, show friends history
        if (TraktCredentials.get(getActivity()).hasCredentials()) {
            isLoadingRecentlyWatched = true;
            isLoadingFriends = true;
            showProgressBar(true);
            getLoaderManager().initLoader(MoviesActivity.NOW_TRAKT_USER_LOADER_ID, null,
                    recentlyTraktCallbacks);
            getLoaderManager().initLoader(MoviesActivity.NOW_TRAKT_FRIENDS_LOADER_ID, null,
                    traktFriendsHistoryCallbacks);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // guard against not attached to activity
        if (!isAdded()) {
            return;
        }

        inflater.inflate(R.menu.now_menu, menu);

        // TODO ut: not currently using this
        MenuItem todayFilterItem = menu.findItem(R.id.menu_action_now_filter_released_today);
        todayFilterItem.setEnabled(false);
        todayFilterItem.setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_now_refresh) {
            refreshStream();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshStream() {
        showProgressBar(true);
        showError(null);

        // user might get disconnected during our life-time,
        // so properly clean up old loaders so they won't interfere
        if (TraktCredentials.get(getActivity()).hasCredentials()) {
            isLoadingRecentlyWatched = true;
            getLoaderManager().restartLoader(MoviesActivity.NOW_TRAKT_USER_LOADER_ID, null,
                    recentlyTraktCallbacks);
            isLoadingFriends = true;
            getLoaderManager().restartLoader(ShowsActivity.NOW_TRAKT_FRIENDS_LOADER_ID, null,
                    traktFriendsHistoryCallbacks);
        } else {
            // destroy trakt loaders and remove any shown error message
            destroyLoaderIfExists(MoviesActivity.NOW_TRAKT_USER_LOADER_ID);
            destroyLoaderIfExists(MoviesActivity.NOW_TRAKT_FRIENDS_LOADER_ID);
            showError(null);
        }
    }

    private void destroyLoaderIfExists(int loaderId) {
        if (getLoaderManager().getLoader(loaderId) != null) {
            getLoaderManager().destroyLoader(loaderId);
        }
    }

    private void showError(@Nullable String errorText) {
        boolean show = errorText != null;
        if (show) {
            snackbarText.setText(errorText);
        }
        if (snackbar.getVisibility() == (show ? View.VISIBLE : View.GONE)) {
            // already in desired state, avoid replaying animation
            return;
        }
        snackbar.startAnimation(AnimationUtils.loadAnimation(snackbar.getContext(),
                show ? R.anim.fade_in : R.anim.fade_out));
        snackbar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Show or hide the progress bar of the {@link android.support.v4.widget.SwipeRefreshLayout}
     * wrapping view.
     */
    private void showProgressBar(boolean show) {
        // only hide if everybody has finished loading
        if (!show) {
            if (isLoadingRecentlyWatched || isLoadingFriends) {
                return;
            }
        }
        swipeRefreshLayout.setRefreshing(show);
    }

    private void updateEmptyState() {
        boolean isEmpty = adapter.getItemCount() == 0;
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private NowAdapter.ItemClickListener itemClickListener = new NowAdapter.ItemClickListener() {
        @Override
        public void onItemClick(View view, int position) {
            NowAdapter.NowItem item = adapter.getItem(position);
            if (item == null) {
                return;
            }

            // more history link?
            if (item.type == NowAdapter.ItemType.MORE_LINK) {
                startActivity(new Intent(getActivity(), HistoryActivity.class).putExtra(
                        HistoryActivity.InitBundle.HISTORY_TYPE,
                        HistoryActivity.DISPLAY_MOVIE_HISTORY));
                return;
            }

            if (item.movieTmdbId == null) {
                return;
            }

            // display movie details
            Intent i = new Intent(getActivity(), MovieDetailsActivity.class);
            i.putExtra(MovieDetailsFragment.InitBundle.TMDB_ID, item.movieTmdbId);

            // simple scale up animation as there are no images
            Utils.startActivityWithAnimation(getActivity(), i, view);
        }
    };

    private LoaderManager.LoaderCallbacks<TraktRecentMovieHistoryLoader.Result>
            recentlyTraktCallbacks
            = new LoaderManager.LoaderCallbacks<TraktRecentMovieHistoryLoader.Result>() {
        @Override
        public Loader<TraktRecentMovieHistoryLoader.Result> onCreateLoader(int id, Bundle args) {
            return new TraktRecentMovieHistoryLoader(getActivity());
        }

        @Override
        public void onLoadFinished(Loader<TraktRecentMovieHistoryLoader.Result> loader,
                TraktRecentMovieHistoryLoader.Result data) {
            if (!isAdded()) {
                return;
            }
            adapter.setRecentlyWatched(data.items);
            isLoadingRecentlyWatched = false;
            showProgressBar(false);
            showError(data.errorText);
        }

        @Override
        public void onLoaderReset(Loader<TraktRecentMovieHistoryLoader.Result> loader) {
            if (!isVisible()) {
                return;
            }
            // clear existing data
            adapter.setRecentlyWatched(null);
        }
    };

    private LoaderManager.LoaderCallbacks<List<NowAdapter.NowItem>> traktFriendsHistoryCallbacks
            = new LoaderManager.LoaderCallbacks<List<NowAdapter.NowItem>>() {
        @Override
        public Loader<List<NowAdapter.NowItem>> onCreateLoader(int id, Bundle args) {
            return new TraktFriendsMovieHistoryLoader(getActivity());
        }

        @Override
        public void onLoadFinished(Loader<List<NowAdapter.NowItem>> loader,
                List<NowAdapter.NowItem> data) {
            if (!isAdded()) {
                return;
            }
            adapter.setFriendsRecentlyWatched(data);
            isLoadingFriends = false;
            showProgressBar(false);
        }

        @Override
        public void onLoaderReset(Loader<List<NowAdapter.NowItem>> loader) {
            if (!isVisible()) {
                return;
            }
            // clear existing data
            adapter.setFriendsRecentlyWatched(null);
        }
    };
}
