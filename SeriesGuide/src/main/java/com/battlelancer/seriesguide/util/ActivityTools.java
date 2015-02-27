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

import android.content.ContentValues;
import android.content.Context;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import timber.log.Timber;

/**
 * Helper methods for dealing with the {@link com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables#ACTIVITY}
 * table.
 */
public class ActivityTools {

    private static final long HISTORY_THRESHOLD = 30 * DateUtils.DAY_IN_MILLIS;

    /**
     * Adds an activity entry for the given episode with the current time as timestamp. If an entry
     * already exists it is replaced.
     *
     * <p>Also cleans up old entries.
     */
    public static void addActivity(Context context, int episodeTvdbId, int showTvdbId) {
        long timeMonthAgo = System.currentTimeMillis() - HISTORY_THRESHOLD;

        // delete all entries older than 30 days
        int deleted = context.getContentResolver()
                .delete(SeriesGuideContract.Activity.CONTENT_URI,
                        SeriesGuideContract.Activity.TIMESTAMP + "<" + timeMonthAgo, null);
        Timber.d("addActivity: removed " + deleted + " outdated activities");

        // add new entry
        ContentValues values = new ContentValues();
        values.put(SeriesGuideContract.Activity.EPISODE_TVDB_ID, episodeTvdbId);
        values.put(SeriesGuideContract.Activity.SHOW_TVDB_ID, showTvdbId);
        long currentTime = System.currentTimeMillis();
        values.put(SeriesGuideContract.Activity.TIMESTAMP, currentTime);

        context.getContentResolver().insert(SeriesGuideContract.Activity.CONTENT_URI, values);
        Timber.d("addActivity: episode: " + episodeTvdbId + " timestamp: " + currentTime);
    }

    /**
     * Tries to remove any activity with the given episode TheTVDB id.
     */
    public static void removeActivity(Context context, int episodeTvdbId) {
        int deleted = context.getContentResolver().delete(SeriesGuideContract.Activity.CONTENT_URI,
                SeriesGuideContract.Activity.EPISODE_TVDB_ID + "=" + episodeTvdbId, null);
        Timber.d("removeActivity: deleted " + deleted + " activity entries");
    }
}
