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
import com.battlelancer.seriesguide.R;
import com.uwetrottmann.trakt.v2.entities.SyncItems;
import com.uwetrottmann.trakt.v2.entities.SyncResponse;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import com.uwetrottmann.trakt.v2.services.Sync;
import retrofit.RetrofitError;
import timber.log.Timber;

public class AddShowToWatchlistTask extends BaseShowActionTask {

    public AddShowToWatchlistTask(Context context, int showTvdbId) {
        super(context, showTvdbId);
    }

    @Override
    protected SyncResponse doTraktAction(Sync traktSync, SyncItems items)
            throws OAuthUnauthorizedException {
        try {
            return traktSync.addItemsToWatchlist(items);
        } catch (RetrofitError e) {
            Timber.e(e, "doTraktAction: adding show to watchlist failed.");
            return null;
        }
    }

    @Override
    protected int getSuccessTextResId() {
        return R.string.watchlist_added;
    }
}
