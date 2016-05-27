/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.loaders;

import android.content.Context;
import com.battlelancer.seriesguide.tmdbapi.SgTmdb2;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb2.entities.Person;
import java.io.IOException;
import retrofit2.Response;

/**
 * Loads details of a crew or cast member from TMDb.
 */
public class PersonLoader extends GenericSimpleLoader<Person> {

    private final int mTmdbId;

    public PersonLoader(Context context, int tmdbId) {
        super(context);
        mTmdbId = tmdbId;
    }

    @Override
    public Person loadInBackground() {
        Response<Person> response;
        try {
            response = ServiceUtils.getTmdb2(getContext())
                    .personService()
                    .summary(mTmdbId)
                    .execute();
            if (response.isSuccessful()) {
                return response.body();
            } else {
                SgTmdb2.trackFailedRequest(getContext(), "get person summary", response);
            }
        } catch (IOException e) {
            SgTmdb2.trackFailedRequest(getContext(), "get person summary", e);
        }

        return null;
    }
}
