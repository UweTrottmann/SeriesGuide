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

package com.battlelancer.seriesguide.api;

import android.content.Intent;

/**
 * Helper methods to view shows or episodes within SeriesGuide.
 */
public class Intents {

    public static final String ACTION_VIEW_EPISODE
            = "com.battlelancer.seriesguide.api.action.VIEW_EPISODE";

    public static final String ACTION_VIEW_SHOW
            = "com.battlelancer.seriesguide.api.action.VIEW_SHOW";

    public static final String EXTRA_TVDBID = "tvdbid";

    /**
     * Builds an implicit {@link android.content.Intent} to view an episode in SeriesGuide. Make
     * sure to check with {@link Intent#resolveActivity(android.content.pm.PackageManager)} if
     * SeriesGuide (or another app capable of handling this intent) is available.
     */
    public static Intent buildViewEpisodeIntent(int episodeTvdbId) {
        return new Intent(ACTION_VIEW_EPISODE)
                .putExtra(EXTRA_TVDBID, episodeTvdbId);
    }

    /**
     * Builds an implicit {@link android.content.Intent} to view an episode in SeriesGuide. Make
     * sure to check with {@link Intent#resolveActivity(android.content.pm.PackageManager)} if
     * SeriesGuide (or another app capable of handling this intent) is available.
     */
    public static Intent buildViewShowIntent(int showTvdbId) {
        return new Intent(ACTION_VIEW_SHOW)
                .putExtra(EXTRA_TVDBID, showTvdbId);
    }
}
