package com.battlelancer.seriesguide.loaders;

import android.app.Application;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.tmdbapi.SgTmdb;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb2.entities.Credits;
import com.uwetrottmann.tmdb2.services.MoviesService;
import java.io.IOException;
import javax.inject.Inject;
import retrofit2.Response;

/**
 * Loads movie credits from TMDb.
 */
public class MovieCreditsLoader extends GenericSimpleLoader<Credits> {

    @Inject MoviesService moviesService;
    private final int tmdbId;

    public MovieCreditsLoader(Application app, int tmdbId) {
        super(app);
        SgApp.getServicesComponent(app).inject(this);
        this.tmdbId = tmdbId;
    }

    @Override
    public Credits loadInBackground() {
        try {
            Response<Credits> response = moviesService.credits(tmdbId).execute();
            if (response.isSuccessful()) {
                return response.body();
            } else {
                SgTmdb.trackFailedRequest(getContext(), "get movie credits", response);
            }
        } catch (IOException e) {
            SgTmdb.trackFailedRequest(getContext(), "get movie credits", e);
        }

        return null;
    }
}
