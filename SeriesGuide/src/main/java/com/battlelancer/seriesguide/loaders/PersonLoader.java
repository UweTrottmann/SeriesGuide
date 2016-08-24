package com.battlelancer.seriesguide.loaders;

import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.tmdbapi.SgTmdb;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb2.entities.Person;
import com.uwetrottmann.tmdb2.services.PeopleService;
import java.io.IOException;
import javax.inject.Inject;
import retrofit2.Response;

/**
 * Loads details of a crew or cast member from TMDb.
 */
public class PersonLoader extends GenericSimpleLoader<Person> {

    @Inject PeopleService peopleService;
    private final int mTmdbId;

    public PersonLoader(SgApp app, int tmdbId) {
        super(app);
        app.getServicesComponent().inject(this);
        mTmdbId = tmdbId;
    }

    @Override
    public Person loadInBackground() {
        Response<Person> response;
        try {
            response = peopleService.summary(mTmdbId).execute();
            if (response.isSuccessful()) {
                return response.body();
            } else {
                SgTmdb.trackFailedRequest(getContext(), "get person summary", response);
            }
        } catch (IOException e) {
            SgTmdb.trackFailedRequest(getContext(), "get person summary", e);
        }

        return null;
    }
}
