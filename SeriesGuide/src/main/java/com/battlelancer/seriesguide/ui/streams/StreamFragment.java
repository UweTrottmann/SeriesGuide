package com.battlelancer.seriesguide.ui.streams;

import android.content.Intent;
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
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.EpisodesActivity;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.widgets.EmptyViewSwipeRefreshLayout;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersGridView;
import com.uwetrottmann.androidutils.AndroidUtils;

/**
 * Displays a stream of activities that can be refreshed by the user via a swipe gesture (or an
 * action item).
 */
public abstract class StreamFragment extends Fragment implements
        AdapterView.OnItemClickListener {

    @BindView(R.id.swipeRefreshLayoutStream) EmptyViewSwipeRefreshLayout contentContainer;

    @BindView(R.id.gridViewStream) StickyGridHeadersGridView gridView;
    @BindView(R.id.emptyViewStream) TextView emptyView;

    private ListAdapter adapter;
    private Unbinder unbinder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_stream, container, false);
        unbinder = ButterKnife.bind(this, v);

        contentContainer.setSwipeableChildren(R.id.scrollViewStream, R.id.gridViewStream);
        contentContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshStreamWithNetworkCheck();
            }
        });
        contentContainer.setProgressViewOffset(false, getResources().getDimensionPixelSize(
                        R.dimen.swipe_refresh_progress_bar_start_margin),
                getResources().getDimensionPixelSize(
                        R.dimen.swipe_refresh_progress_bar_end_margin));

        gridView.setOnItemClickListener(this);
        gridView.setEmptyView(emptyView);
        gridView.setAreHeadersSticky(false);

        // set initial view states
        showProgressBar(true);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Utils.setSwipeRefreshLayoutColors(getActivity().getTheme(), contentContainer);

        if (adapter == null) {
            adapter = getListAdapter();
        }
        gridView.setAdapter(adapter);

        initializeStream();

        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
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

    private void refreshStreamWithNetworkCheck() {
        // launch trakt connect flow if disconnected
        TraktCredentials.ensureCredentials(getActivity());

        // intercept loader call if offline to avoid replacing data with error message
        // once trakt data has proper cache headers this might become irrelevant
        if (!AndroidUtils.isNetworkConnected(getActivity())) {
            showProgressBar(false);
            setEmptyMessage(getString(R.string.offline));
            Toast.makeText(getActivity(), R.string.offline, Toast.LENGTH_SHORT).show();
            return;
        }

        refreshStream();
    }

    /**
     * Changes the empty message.
     */
    protected void setEmptyMessage(String emptyMessage) {
        emptyView.setText(emptyMessage);
    }

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
        contentContainer.setRefreshing(isShowing);
    }
}
