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

    public static final String EXTRA_EPISODE_TVDBID = "episode_tvdbid";

    public static final String EXTRA_SHOW_TVDBID = "show_tvdbid";

    /**
     * Builds an implicit {@link android.content.Intent} to view an episode in SeriesGuide. Make
     * sure to check with {@link Intent#resolveActivity(android.content.pm.PackageManager)} if
     * SeriesGuide (or another app capable of handling this intent) is available.
     *
     * @param showTvdbId If valid and the episode does not exist, the user will be asked if the show
     *                   should be added to SeriesGuide.
     */
    public static Intent buildViewEpisodeIntent(int showTvdbId, int episodeTvdbId) {
        return new Intent(ACTION_VIEW_EPISODE)
                .putExtra(EXTRA_SHOW_TVDBID, showTvdbId)
                .putExtra(EXTRA_EPISODE_TVDBID, episodeTvdbId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    }

    /**
     * Builds an implicit {@link android.content.Intent} to view a show in SeriesGuide. Make
     * sure to check with {@link Intent#resolveActivity(android.content.pm.PackageManager)} if
     * SeriesGuide (or another app capable of handling this intent) is available.
     *
     * <p> If the show is not added to SeriesGuide, the user will be asked if it should be.
     */
    public static Intent buildViewShowIntent(int showTvdbId) {
        return new Intent(ACTION_VIEW_SHOW)
                .putExtra(EXTRA_SHOW_TVDBID, showTvdbId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    }
}
