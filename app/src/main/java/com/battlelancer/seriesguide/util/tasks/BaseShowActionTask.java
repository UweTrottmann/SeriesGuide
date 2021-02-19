package com.battlelancer.seriesguide.util.tasks;

import android.content.Context;
import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
import com.battlelancer.seriesguide.util.Errors;
import com.uwetrottmann.trakt5.entities.ShowIds;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncResponse;
import com.uwetrottmann.trakt5.entities.SyncShow;
import com.uwetrottmann.trakt5.services.Sync;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Call;
import retrofit2.Response;

public abstract class BaseShowActionTask extends BaseActionTask {

    public static class ShowChangedEvent {
    }

    private final int showTmdbId;

    public BaseShowActionTask(Context context, int showTmdbId) {
        super(context);
        this.showTmdbId = showTmdbId;
    }

    @Override
    protected boolean isSendingToHexagon() {
        return false;
    }

    @Override
    protected Integer doBackgroundAction(Void... params) {
        if (isSendingToTrakt()) {
            if (!TraktCredentials.get(getContext()).hasCredentials()) {
                return ERROR_TRAKT_AUTH;
            }

            SyncItems items = new SyncItems().shows(new SyncShow().id(ShowIds.tmdb(showTmdbId)));

            try {
                Sync traktSync = SgApp.getServicesComponent(getContext()).traktSync();
                Response<SyncResponse> response = doTraktAction(traktSync, items).execute();
                if (response.isSuccessful()) {
                    if (isShowNotFound(response.body())) {
                        return ERROR_TRAKT_API_NOT_FOUND;
                    }
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
        }

        return SUCCESS;
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);

        if (result == SUCCESS) {
            EventBus.getDefault().post(new ShowChangedEvent());
        }
    }

    private static boolean isShowNotFound(SyncResponse response) {
        // if show was not found on trakt
        return response.not_found != null && response.not_found.shows != null
                && response.not_found.shows.size() != 0;
    }

    @NonNull
    protected abstract String getTraktAction();

    @NonNull
    protected abstract Call<SyncResponse> doTraktAction(Sync traktSync, SyncItems items);
}
