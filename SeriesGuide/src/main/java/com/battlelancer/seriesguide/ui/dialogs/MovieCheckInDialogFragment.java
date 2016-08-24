package com.battlelancer.seriesguide.ui.dialogs;

import android.os.Bundle;
import android.support.v4.os.AsyncTaskCompat;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.Utils;

/**
 * Allows to check into movies on trakt or GetGlue. Launching activities should subscribe to {@link
 * com.battlelancer.seriesguide.util.TraktTask.TraktActionCompleteEvent} to display status toasts.
 */
public class MovieCheckInDialogFragment extends GenericCheckInDialogFragment {

    public static MovieCheckInDialogFragment newInstance(int movieTmdbId, String movieTitle) {
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
        AsyncTaskCompat.executeParallel(
                new TraktTask(getActivity()).checkInMovie(
                        getArguments().getInt(InitBundle.MOVIE_TMDB_ID),
                        getArguments().getString(InitBundle.ITEM_TITLE),
                        message));
    }
}
