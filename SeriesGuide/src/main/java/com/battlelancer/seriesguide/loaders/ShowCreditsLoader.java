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
import com.battlelancer.seriesguide.tmdbapi.SgTmdb;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb2.Tmdb;
import com.uwetrottmann.tmdb2.entities.Credits;
import com.uwetrottmann.tmdb2.entities.FindResults;
import com.uwetrottmann.tmdb2.entities.TvShow;
import com.uwetrottmann.tmdb2.enumerations.ExternalSource;
import java.io.IOException;
import java.util.List;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Loads show credits from TMDb.
 */
public class ShowCreditsLoader extends GenericSimpleLoader<Credits> {

    private final boolean findTmdbId;
    private int showId;

    /**
     * Create a show credit {@link android.support.v4.content.Loader}. Supports show ids from TVDb
     * or TMDb.
     *
     * @param findTmdbId If true, the loader assumes the passed id is from TVDb id and will try to
     * look up the associated TMDb id.
     */
    public ShowCreditsLoader(Context context, int id, boolean findTmdbId) {
        super(context);
        showId = id;
        this.findTmdbId = findTmdbId;
    }

    @Override
    public Credits loadInBackground() {
        Tmdb tmdb = ServiceUtils.getTmdb(getContext());

        if (findTmdbId && !findShowTmdbId(tmdb)) {
            return null; // failed to find the show on TMDb
        }

        if (showId < 0) {
            return null; // do not have a valid id, abort
        }

        // get credits for that show
        try {
            Response<Credits> response = tmdb.tvService().credits(showId, null).execute();
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

    private boolean findShowTmdbId(Tmdb tmdb) {
        try {
            Response<FindResults> response = tmdb.findService()
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
