package com.battlelancer.seriesguide.ui.streams;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import com.battlelancer.seriesguide.ui.movies.MovieDetailsActivity;

/**
 * Displays a stream of movies the user has recently watched on trakt.
 */
public class UserMovieStreamFragment extends StreamFragment {

    private MovieHistoryAdapter adapter;

    @NonNull
    @Override
    protected BaseHistoryAdapter getListAdapter() {
        if (adapter == null) {
            adapter = new MovieHistoryAdapter(requireContext(), itemClickListener);
        }
        return adapter;
    }

    @Override
    protected void initializeStream() {
        LoaderManager.getInstance(this).initLoader(HistoryActivity.MOVIES_LOADER_ID, null,
                activityLoaderCallbacks);
    }

    @Override
    protected void refreshStream() {
        LoaderManager.getInstance(this).restartLoader(HistoryActivity.MOVIES_LOADER_ID, null,
                activityLoaderCallbacks);
    }

    private MovieHistoryAdapter.OnItemClickListener itemClickListener = (view, item) -> {
        // display movie details
        if (item.movie == null || item.movie.ids == null || item.movie.ids.tmdb == null) {
            return;
        }
        Intent i = MovieDetailsActivity.intentMovie(getActivity(), item.movie.ids.tmdb);

        ActivityCompat.startActivity(requireContext(), i, ActivityOptionsCompat
                .makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight())
                .toBundle()
        );
    };

    private LoaderManager.LoaderCallbacks<TraktMovieHistoryLoader.Result> activityLoaderCallbacks =
            new LoaderManager.LoaderCallbacks<TraktMovieHistoryLoader.Result>() {
                @Override
                public Loader<TraktMovieHistoryLoader.Result> onCreateLoader(int id, Bundle args) {
                    showProgressBar(true);
                    return new TraktMovieHistoryLoader(requireContext());
                }

                @Override
                public void onLoadFinished(@NonNull Loader<TraktMovieHistoryLoader.Result> loader,
                        TraktMovieHistoryLoader.Result data) {
                    if (!isAdded()) {
                        return;
                    }
                    setListData(data.getResults(), data.getEmptyText());
                }

                @Override
                public void onLoaderReset(@NonNull Loader<TraktMovieHistoryLoader.Result> loader) {
                    // keep current data
                }
            };
}
