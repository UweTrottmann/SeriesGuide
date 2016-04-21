/*
 * Copyright 2016 Uwe Trottmann
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
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.ShowIds;
import com.uwetrottmann.trakt.v2.entities.SyncItems;
import com.uwetrottmann.trakt.v2.entities.SyncResponse;
import com.uwetrottmann.trakt.v2.entities.SyncShow;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import com.uwetrottmann.trakt.v2.services.Sync;

public abstract class BaseShowActionTask extends BaseActionTask {

    private final int showTvdbId;

    public BaseShowActionTask(Context context, int showTvdbId) {
        super(context);
        this.showTvdbId = showTvdbId;
    }

    @Override
    protected boolean isSendingToHexagon() {
        return false;
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
            SyncItems items = new SyncItems().shows(new SyncShow().id(ShowIds.tvdb(showTvdbId)));

            SyncResponse response;
            try {
                response = doTraktAction(traktSync, items);
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(getContext()).setCredentialsInvalid();
                return ERROR_TRAKT_AUTH;
            }

            if (response == null) {
                // invalid response
                return ERROR_TRAKT_API;
            }

            if (!isTraktActionSuccessful(response)) {
                return ERROR_TRAKT_API_NOT_FOUND;
            }
        }

        return SUCCESS;
    }

    private static boolean isTraktActionSuccessful(SyncResponse response) {
        // false if show was not found on trakt
        return !(response.not_found != null && response.not_found.shows != null
                && response.not_found.shows.size() != 0);
    }

    /**
     * Ensure to catch {@link retrofit.RetrofitError} and return {@code null} in that case.
     */
    protected abstract SyncResponse doTraktAction(Sync traktSync, SyncItems items)
            throws OAuthUnauthorizedException;
}
