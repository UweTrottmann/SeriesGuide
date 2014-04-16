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
import com.battlelancer.thetvdbapi.TheTVDB;
import com.battlelancer.thetvdbapi.TvdbException;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import timber.log.Timber;

/**
 * Loads show details from TVDb.
 */
public class TvdbShowLoader extends GenericSimpleLoader<Show> {

    private int mShowTvdbId;

    public TvdbShowLoader(Context context, int showTvdbId) {
        super(context);
        mShowTvdbId = showTvdbId;
    }

    @Override
    public Show loadInBackground() {
        try {
            return TheTVDB.getShow(getContext(), mShowTvdbId);
        } catch (TvdbException e) {
            Timber.e("Downloading TVDb show failed", e);
        }
        return null;
    }
}
