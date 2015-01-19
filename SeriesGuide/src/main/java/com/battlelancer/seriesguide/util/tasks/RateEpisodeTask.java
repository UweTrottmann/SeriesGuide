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
import android.database.Cursor;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.uwetrottmann.trakt.v2.entities.ShowIds;
import com.uwetrottmann.trakt.v2.entities.SyncEpisode;
import com.uwetrottmann.trakt.v2.entities.SyncItems;
import com.uwetrottmann.trakt.v2.entities.SyncResponse;
import com.uwetrottmann.trakt.v2.entities.SyncSeason;
import com.uwetrottmann.trakt.v2.entities.SyncShow;
import com.uwetrottmann.trakt.v2.enums.Rating;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import com.uwetrottmann.trakt.v2.services.Sync;
import retrofit.RetrofitError;
import timber.log.Timber;

public class RateEpisodeTask extends BaseRateItemTask {

    private final int episodeTvdbId;

    public RateEpisodeTask(Context context, Rating rating, int episodeTvdbId) {
        super(context, rating);
        this.episodeTvdbId = episodeTvdbId;
    }

    @Override
    protected SyncResponse doTraktAction(Sync traktSync) throws OAuthUnauthorizedException {
        int season = -1;
        int episode = -1;
        int showTvdbId = -1;
        Cursor query = getContext().getContentResolver()
                .query(SeriesGuideContract.Episodes.buildEpisodeUri(episodeTvdbId),
                        new String[] {
                                SeriesGuideContract.Episodes.SEASON,
                                SeriesGuideContract.Episodes.NUMBER,
                                SeriesGuideContract.Shows.REF_SHOW_ID }, null, null, null);
        if (query != null) {
            if (query.moveToFirst()) {
                season = query.getInt(0);
                episode = query.getInt(1);
                showTvdbId = query.getInt(2);
            }
            query.close();
        }

        if (season == -1 || episode == -1 || showTvdbId == -1) {
            return null;
        }

        SyncItems ratedItems = new SyncItems()
                .shows(new SyncShow().id(ShowIds.tvdb(showTvdbId))
                        .seasons(new SyncSeason().number(season)
                                .episodes(new SyncEpisode().number(episode)
                                        .rating(getRating()))));

        try {
            return traktSync.addRatings(ratedItems);
        } catch (RetrofitError e) {
            Timber.e(e, "doTraktAction: rating episode failed");
            return null;
        }
    }

    @Override
    protected boolean doDatabaseUpdate() {
        ContentValues values = new ContentValues();
        values.put(SeriesGuideContract.Episodes.RATING_USER, getRating().value);

        int rowsUpdated = getContext().getContentResolver()
                .update(SeriesGuideContract.Episodes.buildEpisodeUri(episodeTvdbId), values, null,
                        null);

        // notify withshow uri as well (used by episode details view)
        getContext().getContentResolver()
                .notifyChange(SeriesGuideContract.Episodes.buildEpisodeWithShowUri(episodeTvdbId),
                        null);

        return rowsUpdated > 0;
    }
}
