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
import com.battlelancer.seriesguide.R;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.HistoryEntry;
import java.util.List;
import retrofit2.Call;

/**
 * Loads the last few movies watched on trakt.
 */
public class TraktMovieHistoryLoader extends TraktEpisodeHistoryLoader {

    public TraktMovieHistoryLoader(Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected String getAction() {
        return "get user movie history";
    }

    @Override
    protected int getEmptyText() {
        return R.string.user_movie_stream_empty;
    }

    @Override
    protected Call<List<HistoryEntry>> buildCall(TraktV2 trakt) {
        return TraktRecentMovieHistoryLoader.buildUserMovieHistoryCall(trakt);
    }
}
