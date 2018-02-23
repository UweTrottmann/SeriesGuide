package com.battlelancer.seriesguide.jobs.movies;

import android.content.Context;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.jobs.episodes.JobAction;
import com.battlelancer.seriesguide.ui.movies.MovieTools;

public class MovieWatchlistJob extends MovieJob {

    private final boolean isInWatchlist;

    public MovieWatchlistJob(int movieTmdbId, boolean isInWatchlist) {
        super(isInWatchlist
                ? JobAction.MOVIE_WATCHLIST_ADD : JobAction.MOVIE_WATCHLIST_REMOVE, movieTmdbId);
        this.isInWatchlist = isInWatchlist;
    }

    @Override
    protected boolean applyDatabaseUpdate(Context context, int movieTmdbId) {
        MovieTools movieTools = SgApp.getServicesComponent(context).movieTools();
        return isInWatchlist
                ? movieTools.addToList(movieTmdbId, MovieTools.Lists.WATCHLIST)
                : MovieTools.removeFromList(context, movieTmdbId, MovieTools.Lists.WATCHLIST);
    }

    @NonNull
    @Override
    public String getConfirmationText(Context context) {
        return context.getString(isInWatchlist
                ? R.string.watchlist_add
                : R.string.watchlist_remove);
    }
}
