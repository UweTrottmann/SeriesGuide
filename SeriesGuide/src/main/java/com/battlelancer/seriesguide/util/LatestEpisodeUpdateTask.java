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

import android.content.Context;
import android.os.AsyncTask;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import timber.log.Timber;

/**
 * Updates the latest episode value for a given show or all shows. If supplied a show TVDb id will
 * update only latest episode for that show.
 *
 * <p><b>Do NOT run in parallel as this task is memory intensive.</b>
 */
public class LatestEpisodeUpdateTask extends AsyncTask<Integer, Void, Void> {

    private final Context mContext;

    public LatestEpisodeUpdateTask(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    protected Void doInBackground(Integer... params) {
        int showTvdbId = (params != null && params.length > 0) ? params[0] : -1;

        if (showTvdbId > 0) {
            // update single show
            Timber.d("Updating next episode for show " + showTvdbId);
            DBUtils.updateLatestEpisode(mContext, showTvdbId);
        } else {
            // update all shows
            Timber.d("Updating next episodes for all shows");
            DBUtils.updateLatestEpisode(mContext, null);
        }

        // Show cursors already notified
        // List item cursors need to be notified manually as uri differs
        mContext.getContentResolver()
                .notifyChange(SeriesGuideContract.ListItems.CONTENT_WITH_DETAILS_URI, null);

        return null;
    }
}
