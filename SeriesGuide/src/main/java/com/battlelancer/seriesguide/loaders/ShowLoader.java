
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

import com.battlelancer.seriesguide.items.Series;
import com.battlelancer.seriesguide.util.DBUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;

/**
 * Get a {@link Series} object from the database.
 */
public class ShowLoader extends GenericSimpleLoader<Series> {

    private int mShowTvdbId;

    public ShowLoader(Context context, int showTvdbId) {
        super(context);
        mShowTvdbId = showTvdbId;
    }

    @Override
    public Series loadInBackground() {
        return DBUtils.getShow(getContext(), mShowTvdbId);
    }

}
