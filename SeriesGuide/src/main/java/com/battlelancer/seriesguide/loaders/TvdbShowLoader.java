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
import com.battlelancer.seriesguide.dataliberation.model.Show;
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

    private int showTvdbId;

    public TvdbShowLoader(Context context, int showTvdbId) {
        super(context);
        this.showTvdbId = showTvdbId;
    }

    @Override
    public Result loadInBackground() {
        Result result = new Result();

        result.isAdded = DBUtils.isShowExists(getContext(), showTvdbId);
        try {
            result.show = TheTVDB.getShow(getContext(), showTvdbId);
        } catch (TvdbException e) {
            Timber.e(e, "Downloading TVDb show failed");
            result.show = null;
        }

        return result;
    }
}
