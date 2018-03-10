package com.battlelancer.seriesguide.ui.people;

import android.content.Context;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.tmdbapi.SgTmdb;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb2.entities.Credits;
import com.uwetrottmann.tmdb2.services.MoviesService;
import retrofit2.Response;

/**
 * Loads movie credits from TMDb.
 */
public class MovieCreditsLoader extends GenericSimpleLoader<Credits> {

    private final int tmdbId;

    public MovieCreditsLoader(Context context, int tmdbId) {
        super(context);
        this.tmdbId = tmdbId;
    }

    @Override
    public Credits loadInBackground() {
        MoviesService moviesService = SgApp.getServicesComponent(getContext()).moviesService();
        try {
            Response<Credits> response = moviesService.credits(tmdbId).execute();
            if (response.isSuccessful()) {
                return response.body();
            } else {
                SgTmdb.trackFailedRequest(getContext(), "get movie credits", response);
            }
        } catch (Exception e) {
            SgTmdb.trackFailedRequest(getContext(), "get movie credits", e);
        }

        return null;
    }
}
