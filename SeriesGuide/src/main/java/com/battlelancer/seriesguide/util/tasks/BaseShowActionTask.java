package com.battlelancer.seriesguide.util.tasks;

import android.content.Context;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShowTools;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.ShowIds;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncResponse;
import com.uwetrottmann.trakt5.entities.SyncShow;
import com.uwetrottmann.trakt5.services.Sync;
import de.greenrobot.event.EventBus;
import java.io.IOException;
import retrofit2.Call;
import retrofit2.Response;

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

            TraktV2 trakt = ServiceUtils.getTrakt(getContext());
            if (!TraktCredentials.get(getContext()).hasCredentials()) {
                return ERROR_TRAKT_AUTH;
            }

            Sync traktSync = trakt.sync();
            SyncItems items = new SyncItems().shows(new SyncShow().id(ShowIds.tvdb(showTvdbId)));

            try {
                Response<SyncResponse> response = doTraktAction(traktSync, items).execute();
                if (response.isSuccessful()) {
                    if (isShowNotFound(response.body())) {
                        return ERROR_TRAKT_API_NOT_FOUND;
                    }
                } else {
                    if (SgTrakt.isUnauthorized(getContext(), response)) {
                        return ERROR_TRAKT_AUTH;
                    }
                    SgTrakt.trackFailedRequest(getContext(), getTraktAction(), response);
                    return ERROR_TRAKT_API;
                }
            } catch (IOException e) {
                SgTrakt.trackFailedRequest(getContext(), getTraktAction(), e);
                return ERROR_TRAKT_API;
            }
        }

        return SUCCESS;
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);

        if (result == SUCCESS) {
            EventBus.getDefault().post(new ShowTools.ShowChangedEvent(showTvdbId));
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
