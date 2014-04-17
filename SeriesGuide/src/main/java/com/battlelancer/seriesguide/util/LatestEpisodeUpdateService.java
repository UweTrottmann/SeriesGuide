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

package com.battlelancer.seriesguide.util;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import java.util.HashSet;

/**
 * Updates the latest episode value for a given show or all shows.
 */
public class LatestEpisodeUpdateService extends IntentService {

    public interface InitBundle {
        /** TVDb id of the show to update or 0 to update all shows. */
        String SHOW_TVDB_ID = "show_tvdbid";
    }

    public LatestEpisodeUpdateService() {
        super("LatestEpisodeUpdateService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int showTvdbId = intent.getIntExtra(InitBundle.SHOW_TVDB_ID, 0);

        boolean isNoReleasedEpisodes = DisplaySettings.isNoReleasedEpisodes(
                getApplicationContext());
        boolean isNoSpecials = DisplaySettings.isHidingSpecials(getApplicationContext());

        if (showTvdbId > 0) {
            // update single show
            DBUtils.updateLatestEpisode(getApplicationContext(), showTvdbId, isNoReleasedEpisodes,
                    isNoSpecials);
        } else {
            // update all shows
            HashSet<Integer> shows = ShowTools.getShowTvdbIdsAsSet(getApplicationContext());
            for (int tvdbId : shows) {
                DBUtils.updateLatestEpisode(getApplicationContext(), tvdbId,
                        isNoReleasedEpisodes, isNoSpecials);
            }
        }

        // Show adapter gets notified by ContentProvider
        // Lists adapter needs to be notified manually
        getContentResolver().notifyChange(SeriesGuideContract.ListItems.CONTENT_WITH_DETAILS_URI,
                null);
    }
}
