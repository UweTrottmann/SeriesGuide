package com.battlelancer.seriesguide.util.tasks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt5.entities.SyncErrors;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncResponse;
import com.uwetrottmann.trakt5.enums.Rating;
import com.uwetrottmann.trakt5.services.Sync;
import dagger.Lazy;
import java.io.IOException;
import javax.inject.Inject;
import retrofit2.Response;

public abstract class BaseRateItemTask extends BaseActionTask {

    @Inject Lazy<Sync> traktSync;
    private final Rating rating;

    public BaseRateItemTask(SgApp app, Rating rating) {
        super(app);
        app.getServicesComponent().inject(this);
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
                Response<SyncResponse> response = traktSync.get()
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
