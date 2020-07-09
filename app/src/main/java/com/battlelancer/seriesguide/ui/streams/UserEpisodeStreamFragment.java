package com.battlelancer.seriesguide.ui.streams;

import android.database.Cursor;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.ui.search.AddShowDialogFragment;

/**
 * Displays the latest trakt episode activity of the user.
 */
public class UserEpisodeStreamFragment extends StreamFragment {

    private EpisodeHistoryAdapter adapter;

    @NonNull
    @Override
    protected BaseHistoryAdapter getListAdapter() {
        if (adapter == null) {
            adapter = new EpisodeHistoryAdapter(requireContext(), itemClickListener);
        }
        return adapter;
    }

    @Override
    protected void initializeStream() {
        LoaderManager.getInstance(this)
                .initLoader(HistoryActivity.EPISODES_LOADER_ID, null, activityLoaderCallbacks);
    }

    @Override
    protected void refreshStream() {
        LoaderManager.getInstance(this)
                .restartLoader(HistoryActivity.EPISODES_LOADER_ID, null,
                        activityLoaderCallbacks);
    }

    private EpisodeHistoryAdapter.OnItemClickListener itemClickListener = (view, item) -> {
        if (item.episode == null || item.episode.season == null || item.episode.number == null
                || item.show == null || item.show.ids == null || item.show.ids.tvdb == null) {
            // no episode or show? give up
            return;
        }

        Cursor episodeQuery = requireContext().getContentResolver().query(
                SeriesGuideContract.Episodes.buildEpisodesOfShowUri(item.show.ids.tvdb),
                new String[]{
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
            AddShowDialogFragment.show(requireContext(), getParentFragmentManager(),
                    item.show.ids.tvdb);
        }

        episodeQuery.close();
    };

    private LoaderManager.LoaderCallbacks<TraktEpisodeHistoryLoader.Result> activityLoaderCallbacks
            =
            new LoaderManager.LoaderCallbacks<TraktEpisodeHistoryLoader.Result>() {
                @Override
                public Loader<TraktEpisodeHistoryLoader.Result> onCreateLoader(int id, Bundle args) {
                    showProgressBar(true);
                    return new TraktEpisodeHistoryLoader(requireContext());
                }

                @Override
                public void onLoadFinished(@NonNull Loader<TraktEpisodeHistoryLoader.Result> loader,
                        TraktEpisodeHistoryLoader.Result data) {
                    if (!isAdded()) {
                        return;
                    }
                    setListData(data.getResults(), data.getEmptyText());
                }

                @Override
                public void onLoaderReset(@NonNull Loader<TraktEpisodeHistoryLoader.Result> loader) {
                    // keep current data
                }
            };
}
