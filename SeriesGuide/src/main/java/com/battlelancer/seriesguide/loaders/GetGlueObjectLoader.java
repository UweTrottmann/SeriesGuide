
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

import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.getglue.GetGlue;
import com.uwetrottmann.getglue.entities.GetGlueObject;
import com.uwetrottmann.getglue.entities.GetGlueObjects;

import java.util.List;

import retrofit.RetrofitError;

/**
 * Loads a list of TV shows using a search term against GetGlue's
 * glue/findObjects end point.
 *
 * @see <a
 * href="http://getglue.com/api#networkwide-methods">http://getglue.com/api#networkwide-methods</a>
 */
public class GetGlueObjectLoader extends GenericSimpleLoader<List<GetGlueObject>> {

    private static final String TAG = "GetGlueObjectLoader";
    private String mQuery;

    public GetGlueObjectLoader(String query, Context context) {
        super(context);
        mQuery = query;
    }

    @Override
    public List<GetGlueObject> loadInBackground() {
        GetGlue getglue = new GetGlue();

        try {
            GetGlueObjects results = getglue.searchService().searchTvShows(mQuery);
            return results.objects;
        } catch (RetrofitError e) {
            Utils.trackExceptionAndLog(getContext(), TAG, e);
            return null;
        }
    }

}
