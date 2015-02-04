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
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.NowAdapter;
import com.battlelancer.seriesguide.loaders.FriendsHistoryLoader;
import com.battlelancer.seriesguide.loaders.RecentlyWatchedLoader;
import com.battlelancer.seriesguide.loaders.ReleasedTodayLoader;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.ui.dialogs.AddShowDialogFragment;
import com.battlelancer.seriesguide.util.Utils;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersGridView;
import java.util.List;

/**
 * Shows recently watched episodes, today's releases and recent episodes from friends (if connected
 * to trakt).
 */
public class NowFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener,
        AdapterView.OnItemClickListener {

    @InjectView(R.id.swipeRefreshLayoutNow) SwipeRefreshLayout swipeRefreshLayout;

    @InjectView(R.id.gridViewNow) StickyGridHeadersGridView gridView;
    @InjectView(R.id.emptyViewNow) TextView emptyView;

    private NowAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_now, container, false);
        ButterKnife.inject(this, v);

        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setProgressViewOffset(false,
                getResources().getDimensionPixelSize(
                        R.dimen.swipe_refresh_progress_bar_start_margin),
                getResources().getDimensionPixelSize(
                        R.dimen.swipe_refresh_progress_bar_end_margin));

        gridView.setOnItemClickListener(this);
        gridView.setEmptyView(emptyView);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        int accentColorResId = Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                R.attr.colorAccent);
        swipeRefreshLayout.setColorSchemeResources(accentColorResId, R.color.teal_dark);

        adapter = new NowAdapter(getActivity());

        gridView.setAdapter(adapter);
        gridView.setAreHeadersSticky(false);

        getLoaderManager().initLoader(ShowsActivity.NOW_RECENTLY_LOADER_ID, null,
                recentlyCallbacks);
        getLoaderManager().initLoader(ShowsActivity.NOW_TODAY_LOADER_ID, null,
                releasedTodayCallbacks);
        getLoaderManager().initLoader(ShowsActivity.NOW_FRIENDS_LOADER_ID, null,
                friendsHistoryCallbacks);

        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        ButterKnife.reset(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.stream_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_stream_refresh) {
            refreshStream();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        refreshStream();
    }

    private void refreshStream() {
        showProgressBar(true);
        getLoaderManager().restartLoader(ShowsActivity.NOW_RECENTLY_LOADER_ID, null,
                recentlyCallbacks);
        getLoaderManager().restartLoader(ShowsActivity.NOW_TODAY_LOADER_ID, null,
                releasedTodayCallbacks);
        getLoaderManager().restartLoader(ShowsActivity.NOW_FRIENDS_LOADER_ID, null,
                friendsHistoryCallbacks);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // do not respond if we get a header position (e.g. shortly after data was refreshed)
        if (position < 0) {
            return;
        }

        NowAdapter.NowItem item = adapter.getItem(position);
        if (item == null || item.episodeTvdbId == null) {
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

    /**
     * Show or hide the progress bar of the {@link android.support.v4.widget.SwipeRefreshLayout}
     * wrapping the stream view.
     */
    protected void showProgressBar(boolean isShowing) {
        swipeRefreshLayout.setRefreshing(isShowing);
    }

    private LoaderManager.LoaderCallbacks<List<NowAdapter.NowItem>> recentlyCallbacks
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
            showProgressBar(false);
        }

        @Override
        public void onLoaderReset(Loader<List<NowAdapter.NowItem>> loader) {
            // do nothing
        }
    };

    private LoaderManager.LoaderCallbacks<List<NowAdapter.NowItem>> releasedTodayCallbacks
            = new LoaderManager.LoaderCallbacks<List<NowAdapter.NowItem>>() {
        @Override
        public Loader<List<NowAdapter.NowItem>> onCreateLoader(int id, Bundle args) {
            return new ReleasedTodayLoader(getActivity());
        }

        @Override
        public void onLoadFinished(Loader<List<NowAdapter.NowItem>> loader,
                List<NowAdapter.NowItem> data) {
            if (!isAdded()) {
                return;
            }
            adapter.setReleasedTodayData(data);
            showProgressBar(false);
        }

        @Override
        public void onLoaderReset(Loader<List<NowAdapter.NowItem>> loader) {
            // do nothing
        }
    };

    private LoaderManager.LoaderCallbacks<List<NowAdapter.NowItem>> friendsHistoryCallbacks
            = new LoaderManager.LoaderCallbacks<List<NowAdapter.NowItem>>() {
        @Override
        public Loader<List<NowAdapter.NowItem>> onCreateLoader(int id, Bundle args) {
            return new FriendsHistoryLoader(getActivity());
        }

        @Override
        public void onLoadFinished(Loader<List<NowAdapter.NowItem>> loader,
                List<NowAdapter.NowItem> data) {
            if (!isAdded()) {
                return;
            }
            adapter.setFriendsRecentlyWatched(data);
            showProgressBar(false);
        }

        @Override
        public void onLoaderReset(Loader<List<NowAdapter.NowItem>> loader) {
            // do nothing
        }
    };
}
