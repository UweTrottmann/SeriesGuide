package com.battlelancer.seriesguide.ui.movies;

import android.os.AsyncTask;
import android.os.Bundle;
import com.battlelancer.seriesguide.ui.dialogs.GenericCheckInDialogFragment;
import com.battlelancer.seriesguide.util.TraktTask;

/**
 * Allows to check into movies on trakt or GetGlue. Launching activities should subscribe to {@link
 * com.battlelancer.seriesguide.util.TraktTask.TraktActionCompleteEvent} to display status toasts.
 */
public class MovieCheckInDialogFragment extends GenericCheckInDialogFragment {

    static MovieCheckInDialogFragment newInstance(int movieTmdbId, String movieTitle) {
        MovieCheckInDialogFragment f = new MovieCheckInDialogFragment();

        Bundle args = new Bundle();
        args.putString(InitBundle.ITEM_TITLE, movieTitle);
        args.putInt(InitBundle.MOVIE_TMDB_ID, movieTmdbId);
        f.setArguments(args);

        return f;
    }

    /**
     * Start the trakt check in task.
     */
    protected void checkInTrakt(String message) {
        new TraktTask(getContext()).checkInMovie(
                getArguments().getInt(InitBundle.MOVIE_TMDB_ID),
                getArguments().getString(InitBundle.ITEM_TITLE),
                message).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
