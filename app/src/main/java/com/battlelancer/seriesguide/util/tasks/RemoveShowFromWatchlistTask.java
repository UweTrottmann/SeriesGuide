// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.util.tasks;

import android.content.Context;
import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.R;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncResponse;
import com.uwetrottmann.trakt5.services.Sync;
import retrofit2.Call;

public class RemoveShowFromWatchlistTask extends BaseShowActionTask {

    public RemoveShowFromWatchlistTask(Context app, int showTmdbId) {
        super(app, showTmdbId);
    }

    @NonNull
    @Override
    protected String getTraktAction() {
        return "remove show from watchlist";
    }

    @NonNull
    @Override
    protected Call<SyncResponse> buildTraktCall(Sync traktSync, SyncItems items) {
        return traktSync.deleteItemsFromWatchlist(items);
    }

    @Override
    protected int getSuccessTextResId() {
        return R.string.watchlist_removed;
    }
}
