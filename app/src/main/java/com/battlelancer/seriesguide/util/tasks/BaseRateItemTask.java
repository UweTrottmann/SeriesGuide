// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.util.tasks;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.SyncErrors;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.enums.Rating;
import com.uwetrottmann.trakt5.services.Sync;

/**
 * Stores the rating in the database and sends it to Trakt.
 */
public abstract class BaseRateItemTask extends BaseActionTask {

    private final Rating rating;

    public BaseRateItemTask(Context context, Rating rating) {
        super(context);
        this.rating = rating;
    }

    @Override
    protected boolean isSendingToHexagon() {
        // Hexagon does not support ratings.
        return false;
    }

    @Override
    protected Integer doBackgroundAction(Void... params) {
        if (isSendingToTrakt()) {
            if (!TraktCredentials.get(getContext()).hasCredentials()) {
                return ERROR_TRAKT_AUTH;
            }

            SyncItems ratedItems = buildTraktSyncItems();
            if (ratedItems == null) {
                return ERROR_DATABASE;
            }

            TraktV2 trakt = SgApp.getServicesComponent(getContext()).trakt();
            Sync traktSync = trakt.sync();
            int result = executeTraktCall(traktSync.addRatings(ratedItems), trakt, getTraktAction(),
                    body -> {
                        SyncErrors notFound = body.not_found;
                        if (notFound != null) {
                            if ((notFound.movies != null && notFound.movies.size() != 0)
                                    || (notFound.shows != null && notFound.shows.size() != 0)
                                    || (notFound.episodes != null
                                    && notFound.episodes.size() != 0)) {
                                // movie, show or episode not found on trakt
                                return ERROR_TRAKT_API_NOT_FOUND;
                            }
                        }
                        return SUCCESS;
                    });
            if (result != SUCCESS) {
                return result;
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
        return R.string.ack_rated;
    }

    @NonNull
    protected abstract String getTraktAction();

    @Nullable
    protected abstract SyncItems buildTraktSyncItems();

    protected abstract boolean doDatabaseUpdate();
}
