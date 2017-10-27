package com.battlelancer.seriesguide.jobs.movies;

import android.content.Context;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.jobs.episodes.JobAction;
import com.battlelancer.seriesguide.util.MovieTools;

public class MovieCollectionJob extends MovieJob {

    private final boolean isInCollection;

    public MovieCollectionJob(int movieTmdbId, boolean isInCollection) {
        super(isInCollection
                ? JobAction.MOVIE_COLLECTION_ADD : JobAction.MOVIE_COLLECTION_REMOVE, movieTmdbId);
        this.isInCollection = isInCollection;
    }

    @Override
    protected boolean applyDatabaseUpdate(Context context, int movieTmdbId) {
        MovieTools movieTools = SgApp.getServicesComponent(context).movieTools();
        return isInCollection
                ? movieTools.addToList(movieTmdbId, MovieTools.Lists.COLLECTION)
                : MovieTools.removeFromList(context, movieTmdbId, MovieTools.Lists.COLLECTION);
    }

    @NonNull
    @Override
    public String getConfirmationText(Context context) {
        return context.getString(isInCollection
                ? R.string.action_collection_add
                : R.string.action_collection_remove);
    }
}
