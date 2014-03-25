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

package com.battlelancer.seriesguide.extensions;

import android.content.Intent;
import android.net.Uri;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.api.Episode;
import com.battlelancer.seriesguide.api.SeriesGuideExtension;
import com.battlelancer.seriesguide.settings.AmazonSettings;

/**
 * Provides a search link to the users preferred Amazon website.
 */
public class AmazonExtension extends SeriesGuideExtension {

    public static final String SEARCH_URI_PROTOCOL = "http://";
    public static final String SEARCH_URI_PATH = "/s/field-keywords=";

    public AmazonExtension() {
        super("AmazonExtension");
    }

    @Override
    protected void onRequest(int episodeIdentifier, Episode episode) {
        String domain = AmazonSettings.getAmazonCountryDomain(getApplicationContext());
        String uri = SEARCH_URI_PROTOCOL + domain + SEARCH_URI_PATH
                + episode.getShowTitle() + " " + episode.getTitle();

        publishAction(new Action.Builder(getString(R.string.extension_amazon), episodeIdentifier)
                .viewIntent(new Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse(uri)))
                .build());
    }
}
