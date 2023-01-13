package com.battlelancer.seriesguide.util.tasks;

import android.content.Context;
import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
import com.uwetrottmann.trakt5.entities.ShowIds;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncResponse;
import com.uwetrottmann.trakt5.entities.SyncShow;
import com.uwetrottmann.trakt5.services.Sync;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Call;

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
            Sync traktSync = SgApp.getServicesComponent(getContext()).traktSync();

            int result = executeTraktCall(buildTraktCall(traktSync, items), getTraktAction(),
                    body -> {
                        if (isShowNotFound(body)) {
                            return ERROR_TRAKT_API_NOT_FOUND;
                        } else {
                            return SUCCESS;
                        }
                    });
            //noinspection RedundantIfStatement
            if (result != SUCCESS) {
                return result;
            }
        }

        return SUCCESS;
    }

    private static boolean isShowNotFound(SyncResponse response) {
        // if show was not found on trakt
        return response.not_found != null && response.not_found.shows != null
                && response.not_found.shows.size() != 0;
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);

        if (result == SUCCESS) {
            EventBus.getDefault().post(new ShowChangedEvent());
        }
    }

    @NonNull
    protected abstract String getTraktAction();

    @NonNull
    protected abstract Call<SyncResponse> buildTraktCall(Sync traktSync, SyncItems items);
}
