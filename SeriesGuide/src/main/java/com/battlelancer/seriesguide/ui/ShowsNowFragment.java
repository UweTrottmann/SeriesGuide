package com.battlelancer.seriesguide.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
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
import com.battlelancer.seriesguide.adapters.NowAdapter;
import com.battlelancer.seriesguide.jobs.episodes.EpisodeJobAsyncTask;
import com.battlelancer.seriesguide.jobs.episodes.EpisodeWatchedJob;
import com.battlelancer.seriesguide.loaders.RecentlyWatchedLoader;
import com.battlelancer.seriesguide.loaders.TraktFriendsEpisodeHistoryLoader;
import com.battlelancer.seriesguide.loaders.TraktRecentEpisodeHistoryLoader;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.dialogs.AddShowDialogFragment;
import com.battlelancer.seriesguide.util.TabClickEvent;
import com.battlelancer.seriesguide.util.ViewTools;
import com.battlelancer.seriesguide.widgets.EmptyViewSwipeRefreshLayout;
import java.util.List;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Displays recently watched episodes and recent episodes from friends (if connected to trakt).
 */
public class ShowsNowFragment extends Fragment {

    @BindView(R.id.swipeRefreshLayoutNow) EmptyViewSwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.recyclerViewNow) RecyclerView recyclerView;
    @BindView(R.id.emptyViewNow) TextView emptyView;
    @BindView(R.id.containerSnackbar) View snackbar;
    @BindView(R.id.textViewSnackbar) TextView snackbarText;
    @BindView(R.id.buttonSnackbar) Button snackbarButton;

    private Unbinder unbinder;
    private NowAdapter adapter;
    private boolean isLoadingRecentlyWatched;
    private boolean isLoadingFriends;

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

        emptyView.setText(R.string.now_empty);

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
        recyclerView.setHasFixedSize(true);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ViewTools.setSwipeRefreshLayoutColors(getActivity().getTheme(), swipeRefreshLayout);

        // define dataset
        adapter = new NowAdapter(getActivity(), itemClickListener);
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
            getLoaderManager().initLoader(ShowsActivity.NOW_TRAKT_USER_LOADER_ID, null,
                    recentlyTraktCallbacks);
            getLoaderManager().initLoader(ShowsActivity.NOW_TRAKT_FRIENDS_LOADER_ID, null,
                    traktFriendsHistoryCallbacks);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);

        /*
          Init recently watched loader here the earliest.
          So we can restart them if they already exist to ensure up to date data (the loaders do
          not react to database changes themselves) and avoid loading data twice in a row.
         */
        if (!TraktCredentials.get(getActivity()).hasCredentials()) {
            isLoadingRecentlyWatched = true;
            initAndMaybeRestartLoader(ShowsActivity.NOW_RECENTLY_LOADER_ID, recentlyLocalCallbacks);
        }
    }

    /**
     * Init the loader. If the loader already exists, will restart it (the default behavior of init
     * would be to get the last loaded data).
     */
    private <D> void initAndMaybeRestartLoader(int loaderId,
            LoaderManager.LoaderCallbacks<D> callbacks) {
        boolean isLoaderExists = getLoaderManager().getLoader(loaderId) != null;
        getLoaderManager().initLoader(loaderId, null, callbacks);
        if (isLoaderExists) {
            getLoaderManager().restartLoader(loaderId, null, callbacks);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // when switching tabs while still showing refresh animation, old content remains stuck
        // so force clear the drawing cache and animation: http://stackoverflow.com/a/27073879
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
            swipeRefreshLayout.destroyDrawingCache();
            swipeRefreshLayout.clearAnimation();
        }

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

        // if connected to trakt, replace local history with trakt history, show friends history
        // user might get disconnected during our life-time,
        // so properly clean up old loaders so they won't interfere
        isLoadingRecentlyWatched = true;
        if (TraktCredentials.get(getActivity()).hasCredentials()) {
            destroyLoaderIfExists(ShowsActivity.NOW_RECENTLY_LOADER_ID);

            getLoaderManager().restartLoader(ShowsActivity.NOW_TRAKT_USER_LOADER_ID, null,
                    recentlyTraktCallbacks);
            isLoadingFriends = true;
            getLoaderManager().restartLoader(ShowsActivity.NOW_TRAKT_FRIENDS_LOADER_ID, null,
                    traktFriendsHistoryCallbacks);
        } else {
            // destroy trakt loaders and remove any shown error message
            destroyLoaderIfExists(ShowsActivity.NOW_TRAKT_USER_LOADER_ID);
            destroyLoaderIfExists(ShowsActivity.NOW_TRAKT_FRIENDS_LOADER_ID);
            showError(null);

            getLoaderManager().restartLoader(ShowsActivity.NOW_RECENTLY_LOADER_ID, null,
                    recentlyLocalCallbacks);
        }
    }

    private void destroyLoaderIfExists(int loaderId) {
        if (getLoaderManager().getLoader(loaderId) != null) {
            getLoaderManager().destroyLoader(loaderId);
        }
    }

    /**
     * Starts an activity to display the given episode.
     */
    protected void showDetails(View view, int episodeId) {
        Intent intent = new Intent();
        intent.setClass(getActivity(), EpisodesActivity.class);
        intent.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, episodeId);

        ActivityCompat.startActivity(getActivity(), intent,
                ActivityOptionsCompat
                        .makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight())
                        .toBundle()
        );
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventEpisodeTask(EpisodeJobAsyncTask.CompletedEvent event) {
        if (!event.isSuccessful) {
            return; // no changes applied
        }
        if (!isAdded()) {
            return; // no longer added to activity
        }
        // reload recently watched if user set or unset an episode watched
        // however, if connected to trakt do not show local history
        if (event.job instanceof EpisodeWatchedJob
                && !TraktCredentials.get(getActivity()).hasCredentials()) {
            isLoadingRecentlyWatched = true;
            getLoaderManager().restartLoader(ShowsActivity.NOW_RECENTLY_LOADER_ID, null,
                    recentlyLocalCallbacks);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventTabClick(TabClickEvent event) {
        if (event.position == ShowsActivity.InitBundle.INDEX_TAB_NOW) {
            recyclerView.smoothScrollToPosition(0);
        }
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
                        HistoryActivity.DISPLAY_EPISODE_HISTORY));
                return;
            }

            // other actions need at least an episode TVDB id
            if (item.episodeTvdbId == null) {
                return;
            }

            // check if episode is in database
            Cursor query = getActivity().getContentResolver()
                    .query(SeriesGuideContract.Episodes.buildEpisodeUri(item.episodeTvdbId),
                            new String[] { SeriesGuideContract.Episodes._ID }, null, null, null);
            if (query == null) {
                // query failed
                return;
            }
            if (query.getCount() == 1) {
                // episode in database: display details
                showDetails(view, item.episodeTvdbId);
            } else if (item.showTvdbId != null) {
                // episode missing: show likely not in database, suggest adding it
                AddShowDialogFragment.showAddDialog(item.showTvdbId, getFragmentManager());
            }
            query.close();
        }
    };

    private LoaderManager.LoaderCallbacks<List<NowAdapter.NowItem>> recentlyLocalCallbacks
            = new LoaderManager.LoaderCallbacks<List<NowAdapter.NowItem>>() {
        @Override
        public Loader<List<NowAdapter.NowItem>> onCreateLoader(int id, Bundle args) {
            return new RecentlyWatchedLoader(getActivity());
        }

        @Override
        public void onLoadFinished(Loader<List<NowAdapter.NowItem>> loader,
                List<NowAdapter.NowItem> data) {
            if (!isAdded()) {
                return;
            }
            adapter.setRecentlyWatched(data);
            isLoadingRecentlyWatched = false;
            showProgressBar(false);
        }

        @Override
        public void onLoaderReset(Loader<List<NowAdapter.NowItem>> loader) {
            if (!isVisible()) {
                return;
            }
            // clear existing data
            adapter.setRecentlyWatched(null);
        }
    };

    private LoaderManager.LoaderCallbacks<TraktRecentEpisodeHistoryLoader.Result>
            recentlyTraktCallbacks
            = new LoaderManager.LoaderCallbacks<TraktRecentEpisodeHistoryLoader.Result>() {
        @Override
        public Loader<TraktRecentEpisodeHistoryLoader.Result> onCreateLoader(int id, Bundle args) {
            return new TraktRecentEpisodeHistoryLoader(getActivity());
        }

        @Override
        public void onLoadFinished(Loader<TraktRecentEpisodeHistoryLoader.Result> loader,
                TraktRecentEpisodeHistoryLoader.Result data) {
            if (!isAdded()) {
                return;
            }
            adapter.setRecentlyWatched(data.items);
            isLoadingRecentlyWatched = false;
            showProgressBar(false);
            showError(data.errorText);
        }

        @Override
        public void onLoaderReset(Loader<TraktRecentEpisodeHistoryLoader.Result> loader) {
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
            return new TraktFriendsEpisodeHistoryLoader(getActivity());
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
