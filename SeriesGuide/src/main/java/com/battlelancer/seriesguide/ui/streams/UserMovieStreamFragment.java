package com.battlelancer.seriesguide.ui.streams;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListAdapter;
import com.battlelancer.seriesguide.adapters.MovieHistoryAdapter;
import com.battlelancer.seriesguide.loaders.TraktMovieHistoryLoader;
import com.battlelancer.seriesguide.ui.HistoryActivity;
import com.battlelancer.seriesguide.ui.MovieDetailsActivity;
import com.uwetrottmann.trakt5.entities.HistoryEntry;

/**
 * Displays a stream of movies the user has recently watched on trakt.
 */
public class UserMovieStreamFragment extends StreamFragment {

    private MovieHistoryAdapter adapter;

    @Override
    protected ListAdapter getListAdapter() {
        if (adapter == null) {
            adapter = new MovieHistoryAdapter(getActivity(), itemClickListener);
        }
        return adapter;
    }

    @Override
    protected void initializeStream() {
        getLoaderManager().initLoader(HistoryActivity.MOVIES_LOADER_ID, null,
                activityLoaderCallbacks);
    }

    @Override
    protected void refreshStream() {
        getLoaderManager().restartLoader(HistoryActivity.MOVIES_LOADER_ID, null,
                activityLoaderCallbacks);
    }

    private MovieHistoryAdapter.OnItemClickListener itemClickListener
            = new MovieHistoryAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(View view, HistoryEntry item) {
            if (item == null) {
                return;
            }

            // display movie details
            if (item.movie == null || item.movie.ids == null) {
                return;
            }
            Intent i = MovieDetailsActivity.intentMovie(getActivity(), item.movie.ids.tmdb);

            ActivityCompat.startActivity(getActivity(), i,
                    ActivityOptionsCompat
                            .makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight())
                            .toBundle()
            );
        }
    };

    private LoaderManager.LoaderCallbacks<TraktMovieHistoryLoader.Result> activityLoaderCallbacks =
            new LoaderManager.LoaderCallbacks<TraktMovieHistoryLoader.Result>() {
                @Override
                public Loader<TraktMovieHistoryLoader.Result> onCreateLoader(int id, Bundle args) {
                    showProgressBar(true);
                    return new TraktMovieHistoryLoader(getActivity());
                }

                @Override
                public void onLoadFinished(Loader<TraktMovieHistoryLoader.Result> loader,
                        TraktMovieHistoryLoader.Result data) {
                    if (!isAdded()) {
                        return;
                    }
                    adapter.setData(data.results);
                    setEmptyMessage(data.emptyText);
                    showProgressBar(false);
                }

                @Override
                public void onLoaderReset(Loader<TraktMovieHistoryLoader.Result> loader) {
                    // keep current data
                }
            };
}
