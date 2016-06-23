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

package com.battlelancer.seriesguide.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.HistoryEntry;
import java.io.IOException;
import java.util.List;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Loads the last few episodes watched on trakt.
 */
public class TraktEpisodeHistoryLoader
        extends GenericSimpleLoader<TraktEpisodeHistoryLoader.Result> {

    public static class Result {
        public List<HistoryEntry> results;
        public int emptyTextResId;

        public Result(List<HistoryEntry> results, int emptyTextResId) {
            this.results = results;
            this.emptyTextResId = emptyTextResId;
        }
    }

    public TraktEpisodeHistoryLoader(Context context) {
        super(context);
    }

    @Override
    public Result loadInBackground() {
        TraktV2 trakt = ServiceUtils.getTrakt(getContext());
        if (!TraktCredentials.get(getContext()).hasCredentials()) {
            return buildResultFailure(R.string.trakt_error_credentials);
        }

        List<HistoryEntry> history = null;
        try {
            Response<List<HistoryEntry>> response = buildCall(trakt).execute();
            if (response.isSuccessful()) {
                history = response.body();
            } else {
                if (SgTrakt.isUnauthorized(getContext(), response)) {
                    return buildResultFailure(R.string.trakt_error_credentials);
                }
                SgTrakt.trackFailedRequest(getContext(), getAction(), response);
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(getContext(), getAction(), e);
            return buildResultFailure(
                    AndroidUtils.isNetworkConnected(getContext()) ? R.string.trakt_error_general
                            : R.string.offline);
        }

        if (history == null) {
            return buildResultFailure(R.string.trakt_error_general);
        } else {
            return new Result(history, getEmptyText());
        }
    }

    @NonNull
    protected String getAction() {
        return "get user episode history";
    }

    @StringRes
    protected int getEmptyText() {
        return R.string.user_stream_empty;
    }

    protected Call<List<HistoryEntry>> buildCall(TraktV2 trakt) {
        return TraktRecentEpisodeHistoryLoader.buildUserEpisodeHistoryCall(trakt);
    }

    private static Result buildResultFailure(int emptyTextResId) {
        return new Result(null, emptyTextResId);
    }
}
