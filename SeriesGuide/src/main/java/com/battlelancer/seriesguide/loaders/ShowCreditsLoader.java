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
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb.Tmdb;
import com.uwetrottmann.tmdb.entities.Credits;
import com.uwetrottmann.tmdb.entities.FindResults;
import com.uwetrottmann.tmdb.enumerations.ExternalSource;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Loads show credits from TMDb.
 */
public class ShowCreditsLoader extends GenericSimpleLoader<Credits> {

    private int mShowTvdbId;

    public ShowCreditsLoader(Context context, int showTvdbId) {
        super(context);
        mShowTvdbId = showTvdbId;
    }

    @Override
    public Credits loadInBackground() {
        try {
            Tmdb tmdb = ServiceUtils.getTmdb(getContext());

            // find the show on TMDb
            FindResults findResults = tmdb.findService()
                    .find(String.valueOf(mShowTvdbId), ExternalSource.TVDB_ID, null);
            if (findResults.tv_results.isEmpty()) {
                Timber.d("Downloading show credits failed: show not on TMDb");
                return null;
            }

            // get credits for that show
            return tmdb.tvService().credits(findResults.tv_results.get(0).id, null);
        } catch (RetrofitError e) {
            Timber.e(e, "Downloading show credits failed");
        }

        return null;
    }
}
