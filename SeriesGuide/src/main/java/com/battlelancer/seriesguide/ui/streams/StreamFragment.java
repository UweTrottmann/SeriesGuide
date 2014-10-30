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

package com.battlelancer.seriesguide.ui.streams;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.BaseNavDrawerActivity;
import com.battlelancer.seriesguide.ui.EpisodesActivity;
import com.battlelancer.seriesguide.ui.dialogs.AddShowDialogFragment;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.trakt.entities.ActivityItem;
import com.jakewharton.trakt.entities.TvShowEpisode;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersGridView;
import com.uwetrottmann.androidutils.AndroidUtils;

/**
 * Displays a stream of activities that can be refreshed by the user via a swipe gesture (or an
 * action item).
 */
public abstract class StreamFragment extends Fragment implements
        AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {

    @InjectView(R.id.swipeRefreshLayoutStream) SwipeRefreshLayout mContentContainer;

    @InjectView(R.id.gridViewStream) StickyGridHeadersGridView mGridView;
    @InjectView(R.id.emptyViewStream) TextView mEmptyView;

    private ListAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_stream, container, false);
        ButterKnife.inject(this, v);

        mContentContainer.setOnRefreshListener(this);
        mContentContainer.setProgressViewOffset(false, getResources().getDimensionPixelSize(
                R.dimen.swipe_refresh_progress_bar_start_margin),
                getResources().getDimensionPixelSize(
                        R.dimen.swipe_refresh_progress_bar_end_margin));

        mGridView.setOnItemClickListener(this);
        mGridView.setEmptyView(mEmptyView);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        int accentColorResId = Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                R.attr.colorAccent);
        mContentContainer.setColorSchemeResources(accentColorResId, R.color.teal_dark);

        // change empty message if we are offline
        if (!AndroidUtils.isNetworkConnected(getActivity())) {
            mEmptyView.setText(R.string.offline);
        } else {
            mEmptyView.setText(getEmptyMessageResId());
            showProgressBar(true);
        }

        if (mAdapter == null) {
            mAdapter = getListAdapter();
        }
        mGridView.setAdapter(mAdapter);

        initializeStream();

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
            refreshStreamWithNetworkCheck();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        refreshStreamWithNetworkCheck();
    }

    private void refreshStreamWithNetworkCheck() {
        if (!TraktCredentials.ensureCredentials(getActivity())) {
            showProgressBar(false);
            return;
        }

        if (!AndroidUtils.isNetworkConnected(getActivity())) {
            // keep existing data, but update empty view anyhow
            showProgressBar(false);
            mEmptyView.setText(R.string.offline);
            Toast.makeText(getActivity(), R.string.offline, Toast.LENGTH_SHORT).show();
            return;
        }
        showProgressBar(true);
        mEmptyView.setText(getEmptyMessageResId());
        refreshStream();
    }

    /**
     * Return the string resource that should be used if there is no item in the stream.
     */
    protected abstract int getEmptyMessageResId();

    /**
     * Implementers should create their grid view adapter here.
     */
    protected abstract ListAdapter getListAdapter();

    /**
     * Implementers should initialize the activity stream and supply the results to the grid
     * adapter.
     */
    protected abstract void initializeStream();

    /**
     * Implementers should refresh the activity stream and replace the data of the grid adapter.
     * Once finished you should hide the progress bar with {@link #showProgressBar(boolean)}.
     */
    protected abstract void refreshStream();

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // do not respond if we get a header position (e.g. shortly after data was refreshed)
        if (position < 0) {
            return;
        }

        ActivityItem activity = (ActivityItem) mAdapter.getItem(position);
        if (activity == null) {
            return;
        }

        TvShowEpisode episode = activity.episode;
        if (episode == null && activity.episodes != null && activity.episodes.size() > 0) {
            // looks like we have multiple episodes, get first one
            episode = activity.episodes.get(0);
        }
        if (episode == null) {
            // still no episode? give up
            return;
        }

        Cursor episodeQuery = getActivity().getContentResolver().query(
                SeriesGuideContract.Episodes.buildEpisodesOfShowUri(activity.show.tvdb_id),
                new String[] {
                        SeriesGuideContract.Episodes._ID
                }, SeriesGuideContract.Episodes.NUMBER + "=" + episode.number + " AND "
                        + SeriesGuideContract.Episodes.SEASON + "=" + episode.season, null,
                null
        );
        if (episodeQuery == null) {
            return;
        }

        if (episodeQuery.getCount() != 0) {
            // display the episode details if we have a match
            episodeQuery.moveToFirst();
            showDetails(view, episodeQuery.getInt(0));
        } else {
            // offer to add the show if it's not in the show database yet
            SearchResult showToAdd = new SearchResult();
            showToAdd.tvdbid = activity.show.tvdb_id;
            showToAdd.title = activity.show.title;
            showToAdd.overview = activity.show.overview;
            AddShowDialogFragment.showAddDialog(showToAdd, getFragmentManager());
        }

        episodeQuery.close();
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
        mContentContainer.setRefreshing(isShowing);
    }
}
