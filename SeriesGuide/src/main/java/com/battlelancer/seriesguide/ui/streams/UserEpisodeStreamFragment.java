package com.battlelancer.seriesguide.ui.streams;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListAdapter;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.ui.dialogs.AddShowDialogFragment;
import com.uwetrottmann.trakt5.entities.HistoryEntry;

/**
 * Displays the latest trakt episode activity of the user.
 */
public class UserEpisodeStreamFragment extends StreamFragment {

    private EpisodeHistoryAdapter adapter;

    @Override
    protected ListAdapter getListAdapter() {
        if (adapter == null) {
            adapter = new EpisodeHistoryAdapter(getActivity(), itemClickListener);
        }
        return adapter;
    }

    @Override
    protected void initializeStream() {
        getLoaderManager().initLoader(HistoryActivity.EPISODES_LOADER_ID, null,
                activityLoaderCallbacks);
    }

    @Override
    protected void refreshStream() {
        getLoaderManager().restartLoader(HistoryActivity.EPISODES_LOADER_ID, null,
                activityLoaderCallbacks);
    }

    private EpisodeHistoryAdapter.OnItemClickListener itemClickListener
            = new EpisodeHistoryAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(View view, HistoryEntry item) {
            if (item == null) {
                return;
            }

            if (item.episode == null || item.episode.season == null || item.episode.number == null
                    || item.show == null || item.show.ids == null || item.show.ids.tvdb == null) {
                // no episode or show? give up
                return;
            }

            Cursor episodeQuery = getActivity().getContentResolver().query(
                    SeriesGuideContract.Episodes.buildEpisodesOfShowUri(item.show.ids.tvdb),
                    new String[] {
                            SeriesGuideContract.Episodes._ID
                    }, SeriesGuideContract.Episodes.NUMBER + "=" + item.episode.number + " AND "
                            + SeriesGuideContract.Episodes.SEASON + "=" + item.episode.season, null,
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
                AddShowDialogFragment.showAddDialog(item.show.ids.tvdb, getFragmentManager());
            }

            episodeQuery.close();
        }
    };

    private LoaderManager.LoaderCallbacks<TraktEpisodeHistoryLoader.Result> activityLoaderCallbacks
            =
            new LoaderManager.LoaderCallbacks<TraktEpisodeHistoryLoader.Result>() {
                @Override
                public Loader<TraktEpisodeHistoryLoader.Result> onCreateLoader(int id, Bundle args) {
                    showProgressBar(true);
                    return new TraktEpisodeHistoryLoader(getActivity());
                }

                @Override
                public void onLoadFinished(Loader<TraktEpisodeHistoryLoader.Result> loader,
                        TraktEpisodeHistoryLoader.Result data) {
                    if (!isAdded()) {
                        return;
                    }
                    adapter.setData(data.results);
                    setEmptyMessage(data.emptyText);
                    showProgressBar(false);
                }

                @Override
                public void onLoaderReset(Loader<TraktEpisodeHistoryLoader.Result> loader) {
                    // keep current data
                }
            };
}
