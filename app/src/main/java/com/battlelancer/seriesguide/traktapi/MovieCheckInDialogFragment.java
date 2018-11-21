package com.battlelancer.seriesguide.traktapi;

import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import com.battlelancer.seriesguide.util.DialogTools;

/**
 * Allows to check into a movie. Launching activities should subscribe to {@link
 * TraktTask.TraktActionCompleteEvent} to display status toasts.
 */
public class MovieCheckInDialogFragment extends GenericCheckInDialogFragment {

    public static boolean show(FragmentManager fragmentManager, int movieTmdbId,
            String movieTitle) {
        MovieCheckInDialogFragment f = new MovieCheckInDialogFragment();

        Bundle args = new Bundle();
        args.putString(InitBundle.ITEM_TITLE, movieTitle);
        args.putInt(InitBundle.MOVIE_TMDB_ID, movieTmdbId);
        f.setArguments(args);

        return DialogTools.safeShow(f, fragmentManager, "movieCheckInDialog");
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
