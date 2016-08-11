package com.battlelancer.seriesguide.loaders;

import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.tmdbapi.SgTmdb;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb2.Tmdb;
import com.uwetrottmann.tmdb2.entities.Credits;
import java.io.IOException;
import javax.inject.Inject;
import retrofit2.Response;

/**
 * Loads movie credits from TMDb.
 */
public class MovieCreditsLoader extends GenericSimpleLoader<Credits> {

    @Inject Tmdb tmdb;
    private final int mTmdbId;

    public MovieCreditsLoader(SgApp app, int tmdbId) {
        super(app);
        app.getServicesComponent().inject(this);
        mTmdbId = tmdbId;
    }

    @Override
    public Credits loadInBackground() {
        try {
            Response<Credits> response = tmdb.moviesService()
                    .credits(mTmdbId)
                    .execute();
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
