package com.battlelancer.seriesguide.util.tasks;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
import com.battlelancer.seriesguide.util.Errors;
import com.uwetrottmann.trakt5.entities.SyncErrors;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncResponse;
import com.uwetrottmann.trakt5.enums.Rating;
import com.uwetrottmann.trakt5.services.Sync;
import retrofit2.Response;

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
                return ERROR_TRAKT_API;
            }
            SyncErrors notFound;
            try {
                Sync traktSync = SgApp.getServicesComponent(getContext()).traktSync();
                Response<SyncResponse> response = traktSync
                        .addRatings(ratedItems)
                        .execute();
                if (response.isSuccessful()) {
                    notFound = response.body().not_found;
                } else {
                    if (SgTrakt.isUnauthorized(getContext(), response)) {
                        return ERROR_TRAKT_AUTH;
                    }
                    Errors.logAndReport(getTraktAction(), response);
                    return ERROR_TRAKT_API;
                }
            } catch (Exception e) {
                Errors.logAndReport(getTraktAction(), e);
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
