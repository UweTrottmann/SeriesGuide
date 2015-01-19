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

import android.content.ContentValues;
import android.content.Context;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.MovieTools;
import com.uwetrottmann.trakt.v2.entities.MovieIds;
import com.uwetrottmann.trakt.v2.entities.SyncItems;
import com.uwetrottmann.trakt.v2.entities.SyncMovie;
import com.uwetrottmann.trakt.v2.entities.SyncResponse;
import com.uwetrottmann.trakt.v2.enums.Rating;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import com.uwetrottmann.trakt.v2.services.Sync;
import de.greenrobot.event.EventBus;
import retrofit.RetrofitError;
import timber.log.Timber;

public class RateMovieTask extends BaseRateItemTask {

    private final int movieTmdbId;

    public RateMovieTask(Context context, Rating rating, int movieTmdbId) {
        super(context, rating);
        this.movieTmdbId = movieTmdbId;
    }

    @Override
    protected SyncResponse doTraktAction(Sync traktSync) throws OAuthUnauthorizedException {
        SyncItems ratedItems = new SyncItems()
                .movies(new SyncMovie().id(MovieIds.tmdb(movieTmdbId)).rating(getRating()));

        try {
            return traktSync.addRatings(ratedItems);
        } catch (RetrofitError e) {
            Timber.e(e, "doTraktAction: rating movie failed");
            return null;
        }
    }

    @Override
    protected boolean doDatabaseUpdate() {
        ContentValues values = new ContentValues();
        values.put(SeriesGuideContract.Movies.RATING_USER, getRating().value);

        int rowsUpdated = getContext().getContentResolver()
                .update(SeriesGuideContract.Movies.buildMovieUri(movieTmdbId), values, null, null);

        return rowsUpdated > 0;
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);

        // post event so movie UI reloads (it is not listening to database changes)
        EventBus.getDefault().post(new MovieTools.MovieChangedEvent(movieTmdbId));
    }
}
