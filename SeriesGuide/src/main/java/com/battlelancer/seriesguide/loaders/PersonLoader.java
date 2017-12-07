package com.battlelancer.seriesguide.loaders;

import android.content.Context;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.tmdbapi.SgTmdb;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb2.entities.Person;
import com.uwetrottmann.tmdb2.services.PeopleService;
import java.io.IOException;
import retrofit2.Response;

/**
 * Loads details of a crew or cast member from TMDb.
 */
public class PersonLoader extends GenericSimpleLoader<Person> {

    private final int tmdbId;

    public PersonLoader(Context context, int tmdbId) {
        super(context);
        this.tmdbId = tmdbId;
    }

    @Override
    public Person loadInBackground() {
        PeopleService peopleService = SgApp.getServicesComponent(getContext()).peopleService();
        Response<Person> response;
        try {
            response = peopleService.summary(tmdbId).execute();
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
