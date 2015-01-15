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
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.SyncErrors;
import com.uwetrottmann.trakt.v2.entities.SyncResponse;
import com.uwetrottmann.trakt.v2.enums.Rating;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import com.uwetrottmann.trakt.v2.services.Sync;

public abstract class BaseRateItemTask extends BaseActionTask {

    private final Rating rating;

    public BaseRateItemTask(Context context, Rating rating) {
        super(context);
        this.rating = rating;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        if (isCancelled()) {
            return null;
        }

        if (isSendingToTrakt()) {
            if (!AndroidUtils.isNetworkConnected(getContext())) {
                return ERROR_NETWORK;
            }

            TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(getContext());
            if (trakt == null) {
                return ERROR_TRAKT_AUTH;
            }

            Sync traktSync = trakt.sync();

            SyncResponse response;
            try {
                response = doTraktAction(traktSync);
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(getContext()).setCredentialsInvalid();
                return ERROR_TRAKT_AUTH;
            }

            if (response == null) {
                // invalid response
                return ERROR_TRAKT_API;
            }

            SyncErrors notFound = response.not_found;
            if (notFound != null) {
                if ((notFound.movies != null && notFound.movies.size() != 0)
                        || (notFound.shows != null && notFound.shows.size() != 0)
                        || (notFound.episodes != null && notFound.episodes.size() != 0)) {
                    // movie, show or episode not found on trakt
                    return ERROR_TRAKT_API;
                }
            }
        }

        if (!doDatabaseUpdate()) {
            return ERROR_DATABASE;
        }

        return SUCCESS;
    }

    protected Rating getRating() {
        return rating;
    }

    @Override
    protected int getSuccessTextResId() {
        return R.string.trakt_success;
    }

    protected abstract SyncResponse doTraktAction(Sync traktSync) throws OAuthUnauthorizedException;

    protected abstract boolean doDatabaseUpdate();
}
