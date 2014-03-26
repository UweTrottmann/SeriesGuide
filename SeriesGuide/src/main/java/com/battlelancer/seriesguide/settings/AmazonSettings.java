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

package com.battlelancer.seriesguide.settings;

import android.content.Context;

/**
 * Settings for {@link com.battlelancer.seriesguide.extensions.AmazonExtension}.
 */
public class AmazonSettings {

    public static final String SETTINGS_FILE = "extension_amazon";

    public static final String KEY_COUNTRY = "extension_amazon";

    public static final String DEFAULT_DOMAIN = "amazon.com";

    /**
     * Return the Amazon country domain selected by the user.
     */
    public static String getAmazonCountryDomain(Context context) {
        return context.getSharedPreferences(SETTINGS_FILE, 0)
                .getString(KEY_COUNTRY, DEFAULT_DOMAIN);
    }
}
