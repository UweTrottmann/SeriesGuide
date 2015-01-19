/*
 * Copyright 2015 Uwe Trottmann
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

package com.battlelancer.seriesguide.util.tasks;

import android.content.Context;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.MovieTools;
import com.uwetrottmann.seriesguide.backend.movies.model.Movie;
import com.uwetrottmann.trakt.v2.entities.SyncItems;
import com.uwetrottmann.trakt.v2.entities.SyncResponse;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import com.uwetrottmann.trakt.v2.services.Sync;
import retrofit.RetrofitError;
import timber.log.Timber;

public class SetMovieUnwatchedTask extends BaseMovieActionTask {

    public SetMovieUnwatchedTask(Context context, int movieTmdbId) {
        super(context, movieTmdbId);
    }

    @Override
    protected int getSuccessTextResId() {
        return R.string.action_unwatched;
    }

    @Override
    protected boolean isSendingToHexagon() {
        return false;
    }

    @Override
    protected boolean doDatabaseUpdate(Context context, int movieTmdbId) {
        return MovieTools.setWatchedFlag(context, movieTmdbId, false);
    }

    @Override
    protected void setHexagonMovieProperties(Movie movie) {
        // do nothing
    }

    @Override
    protected SyncResponse doTraktAction(Sync traktSync, SyncItems items)
            throws OAuthUnauthorizedException {
        try {
            return traktSync.deleteItemsFromWatchedHistory(items);
        } catch (RetrofitError e) {
            Timber.e(e, "doTraktAction: setting movie unwatched failed");
            return null;
        }
    }
}
