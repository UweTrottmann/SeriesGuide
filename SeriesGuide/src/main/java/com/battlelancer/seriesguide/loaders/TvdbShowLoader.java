/*
 * Copyright 2014 Uwe Trottmann
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
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.thetvdbapi.TheTVDB;
import com.battlelancer.seriesguide.thetvdbapi.TvdbException;
import com.battlelancer.seriesguide.util.DBUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import timber.log.Timber;

/**
 * Loads show details from TVDb.
 */
public class TvdbShowLoader extends GenericSimpleLoader<TvdbShowLoader.Result> {

    public static class Result {
        public Show show;
        public boolean isAdded;
    }

    private final int showTvdbId;
    private String language;

    public TvdbShowLoader(@NonNull Context context, int showTvdbId, @Nullable String language) {
        super(context);
        this.showTvdbId = showTvdbId;
        this.language = language;
    }

    @Override
    public Result loadInBackground() {
        Result result = new Result();

        result.isAdded = DBUtils.isShowExists(getContext(), showTvdbId);
        try {
            if (TextUtils.isEmpty(language)) {
                // fall back to user preferred language
                language = DisplaySettings.getContentLanguage(getContext());
            }
            result.show = TheTVDB.fetchShow(getContext(), showTvdbId, language);
        } catch (TvdbException e) {
            Timber.e(e, "Downloading TVDb show failed");
            result.show = null;
        }

        return result;
    }
}
