package com.battlelancer.seriesguide.jobs.movies;

import android.content.Context;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.jobs.episodes.JobAction;
import com.battlelancer.seriesguide.util.MovieTools;

public class MovieWatchedJob extends MovieJob {

    private final boolean isWatched;

    public MovieWatchedJob(int movieTmdbId, boolean isWatched) {
        super(isWatched
                ? JobAction.MOVIE_WATCHED_SET : JobAction.MOVIE_WATCHED_REMOVE, movieTmdbId);
        this.isWatched = isWatched;
    }

    @Override
    protected boolean applyDatabaseUpdate(Context context, int movieTmdbId) {
        return MovieTools.setWatchedFlag(context, movieTmdbId, isWatched);
    }

    @Override
    public boolean supportsHexagon() {
        return false; // hexagon does only have watchlist and collection flags
    }

    @NonNull
    @Override
    public String getConfirmationText(Context context) {
        return context.getString(isWatched
                ? R.string.action_watched
                : R.string.action_unwatched);
    }
}
