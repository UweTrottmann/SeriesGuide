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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt5.entities.SyncErrors;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncResponse;
import com.uwetrottmann.trakt5.enums.Rating;
import java.io.IOException;
import retrofit2.Response;

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

            if (!TraktCredentials.get(getContext()).hasCredentials()) {
                return ERROR_TRAKT_AUTH;
            }

            SyncItems ratedItems = buildTraktSyncItems();
            if (ratedItems == null) {
                return ERROR_TRAKT_API;
            }
            SyncErrors notFound;
            try {
                Response<SyncResponse> response = ServiceUtils.getTrakt(getContext()).sync()
                        .addRatings(ratedItems)
                        .execute();
                if (response.isSuccessful()) {
                    notFound = response.body().not_found;
                } else {
                    if (SgTrakt.isUnauthorized(getContext(), response)) {
                        return ERROR_TRAKT_AUTH;
                    }
                    SgTrakt.trackFailedRequest(getContext(), getTraktAction(), response);
                    return ERROR_TRAKT_API;
                }
            } catch (IOException e) {
                SgTrakt.trackFailedRequest(getContext(), "rate movie", e);
                return ERROR_TRAKT_API;
            }

            if (notFound != null) {
                if ((notFound.movies != null && notFound.movies.size() != 0)
                        || (notFound.shows != null && notFound.shows.size() != 0)
                        || (notFound.episodes != null && notFound.episodes.size() != 0)) {
                    // movie, show or episode not found on trakt
                    return ERROR_TRAKT_API_NOT_FOUND;
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

    @NonNull
    protected abstract String getTraktAction();

    @Nullable
    protected abstract SyncItems buildTraktSyncItems();

    protected abstract boolean doDatabaseUpdate();
}
