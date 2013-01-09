/*
 * Copyright 2013 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.util;

import android.content.Context;

import com.uwetrottmann.seriesguide.R;

/**
 * Helper methods to interact with third-party services used within SeriesGuide.
 */
public class ServiceUtils {

    private static com.uwetrottmann.tmdb.ServiceManager sTmdbServiceManagerInstance;

    /**
     * Get a tmdb-java ServiceManager with our API key set.
     */
    public static synchronized com.uwetrottmann.tmdb.ServiceManager getTmdbServiceManager(
            Context context) {
        if (sTmdbServiceManagerInstance == null) {
            sTmdbServiceManagerInstance = new com.uwetrottmann.tmdb.ServiceManager();
            sTmdbServiceManagerInstance.setReadTimeout(10000);
            sTmdbServiceManagerInstance.setConnectionTimeout(15000);
            sTmdbServiceManagerInstance.setApiKey(context.getResources().getString(
                    R.string.tmdb_apikey));
        }

        return sTmdbServiceManagerInstance;
    }
}
