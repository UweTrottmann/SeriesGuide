
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

import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.Movie;
import com.uwetrottmann.androidutils.GenericSimpleLoader;

import android.content.Context;

import java.util.List;

import retrofit.RetrofitError;

public class TraktMoviesWatchlistLoader extends GenericSimpleLoader<List<Movie>> {

    private static final String TAG = "TraktMoviesWatchlistLoader";

    public TraktMoviesWatchlistLoader(Context context) {
        super(context);
    }

    @Override
    public List<Movie> loadInBackground() {
        Trakt manager = ServiceUtils.getTraktWithAuth(getContext());
        if (manager == null) {
            return null;
        }

        try {
            return manager.userService()
                    .watchlistMovies(TraktCredentials.get(getContext()).getUsername());
        } catch (RetrofitError e) {
            Utils.trackExceptionAndLog(getContext(), TAG, e);
        }

        return null;
    }
}
