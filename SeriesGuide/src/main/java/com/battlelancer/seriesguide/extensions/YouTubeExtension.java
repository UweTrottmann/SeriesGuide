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

import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.api.Episode;
import com.battlelancer.seriesguide.api.SeriesGuideExtension;
import com.battlelancer.seriesguide.util.ServiceUtils;

/**
 * Searches YouTube for an episode. Useful for web shows!
 */
public class YouTubeExtension extends SeriesGuideExtension {

    private static final String YOUTUBE_SEARCH = "http://www.youtube.com/results?search_query=%s";

    public YouTubeExtension() {
        super("YouTubeExtension");
    }

    @Override
    protected void onRequest(int episodeIdentifier, Episode episode) {
        // we need at least a show or an episode title
        if (TextUtils.isEmpty(episode.getShowTitle()) || TextUtils.isEmpty(episode.getTitle())) {
            return;
        }

        publishAction(new Action.Builder(getString(R.string.extension_youtube), episodeIdentifier)
                .viewIntent(ServiceUtils.buildYouTubeIntent(getApplicationContext(),
                        episode.getShowTitle() + " " + episode.getTitle()))
                .build());
    }
}
