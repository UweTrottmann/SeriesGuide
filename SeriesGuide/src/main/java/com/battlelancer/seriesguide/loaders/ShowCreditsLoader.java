package com.battlelancer.seriesguide.loaders;

import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.tmdbapi.SgTmdb;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb2.entities.Credits;
import com.uwetrottmann.tmdb2.entities.FindResults;
import com.uwetrottmann.tmdb2.entities.TvShow;
import com.uwetrottmann.tmdb2.enumerations.ExternalSource;
import com.uwetrottmann.tmdb2.services.FindService;
import com.uwetrottmann.tmdb2.services.TvService;
import dagger.Lazy;
import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Loads show credits from TMDb.
 */
public class ShowCreditsLoader extends GenericSimpleLoader<Credits> {

    @Inject Lazy<FindService> findService;
    @Inject Lazy<TvService> tvService;
    private final boolean findTmdbId;
    private int showId;

    /**
     * Create a show credit {@link android.support.v4.content.Loader}. Supports show ids from TVDb
     * or TMDb.
     *
     * @param findTmdbId If true, the loader assumes the passed id is from TVDb id and will try to
     * look up the associated TMDb id.
     */
    public ShowCreditsLoader(SgApp app, int id, boolean findTmdbId) {
        super(app);
        app.getServicesComponent().inject(this);
        showId = id;
        this.findTmdbId = findTmdbId;
    }

    @Override
    public Credits loadInBackground() {
        if (findTmdbId && !findShowTmdbId()) {
            return null; // failed to find the show on TMDb
        }

        if (showId < 0) {
            return null; // do not have a valid id, abort
        }

        // get credits for that show
        try {
            Response<Credits> response = tvService.get().credits(showId, null).execute();
            if (response.isSuccessful()) {
                return response.body();
            } else {
                SgTmdb.trackFailedRequest(getContext(), "get show credits", response);
            }
        } catch (IOException e) {
            SgTmdb.trackFailedRequest(getContext(), "get show credits", e);
        }

        return null;
    }

    private boolean findShowTmdbId() {
        try {
            Response<FindResults> response = findService.get()
                    .find(String.valueOf(showId), ExternalSource.TVDB_ID, null)
                    .execute();
            if (response.isSuccessful()) {
                List<TvShow> tvResults = response.body().tv_results;
                if (!tvResults.isEmpty()) {
                    showId = tvResults.get(0).id;
                    return true; // found it!
                } else {
                    Timber.d("Downloading show credits failed: show not on TMDb");
                }
            } else {
                SgTmdb.trackFailedRequest(getContext(), "find tvdb show", response);
            }
        } catch (IOException e) {
            SgTmdb.trackFailedRequest(getContext(), "find tvdb show", e);
        }

        return false;
    }
}
