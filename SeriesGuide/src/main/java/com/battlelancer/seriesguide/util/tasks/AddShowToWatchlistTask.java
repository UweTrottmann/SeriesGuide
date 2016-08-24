package com.battlelancer.seriesguide.util.tasks;

import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncResponse;
import com.uwetrottmann.trakt5.services.Sync;
import retrofit2.Call;

public class AddShowToWatchlistTask extends BaseShowActionTask {

    public AddShowToWatchlistTask(SgApp app, int showTvdbId) {
        super(app, showTvdbId);
    }

    @NonNull
    @Override
    protected String getTraktAction() {
        return "add show to watchlist";
    }

    @NonNull
    @Override
    protected Call<SyncResponse> doTraktAction(Sync traktSync, SyncItems items) {
        return traktSync.addItemsToWatchlist(items);
    }

    @Override
    protected int getSuccessTextResId() {
        return R.string.watchlist_added;
    }
}
