package com.battlelancer.seriesguide.util.tasks;

import android.content.Context;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.util.MovieTools;
import com.uwetrottmann.seriesguide.backend.movies.model.Movie;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncResponse;
import com.uwetrottmann.trakt5.services.Sync;
import retrofit2.Call;

public class AddMovieToCollectionTask extends BaseMovieActionTask {

    private final SgApp app;

    public AddMovieToCollectionTask(SgApp app, int movieTmdbId) {
        super(app, movieTmdbId);
        this.app = app;
    }

    @Override
    protected boolean doDatabaseUpdate(Context context, int movieTmdbId) {
        return app.getMovieTools().addToList(movieTmdbId, MovieTools.Lists.COLLECTION);
    }

    @Override
    protected void setHexagonMovieProperties(Movie movie) {
        movie.setIsInCollection(true);
    }

    @NonNull
    @Override
    protected String getTraktAction() {
        return "add movie to collection";
    }

    @NonNull
    @Override
    protected Call<SyncResponse> doTraktAction(Sync traktSync, SyncItems items) {
        return traktSync.addItemsToCollection(items);
    }

    @Override
    protected int getSuccessTextResId() {
        return R.string.action_collection_added;
    }
}
