
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

package com.battlelancer.seriesguide.dashclock;

import android.content.Intent;
import android.database.Cursor;
import android.text.TextUtils;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DashClockSettings;
import com.battlelancer.seriesguide.ui.ActivityFragment;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.TimeTools;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import java.util.Date;

public class UpcomingEpisodeExtension extends DashClockExtension {

    @Override
    protected void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);
        setUpdateWhenScreenOn(true);
    }

    @Override
    protected void onUpdateData(int arg0) {
        final Cursor upcomingEpisodes = DBUtils.getUpcomingEpisodes(true, getApplicationContext());
        final long customCurrentTime = TimeTools.getCurrentTime(getApplicationContext());
        int hourThreshold = DashClockSettings.getUpcomingTreshold(getApplicationContext());
        long latestTimeToInclude = customCurrentTime + hourThreshold * DateUtils.HOUR_IN_MILLIS;

        // Ensure there are episodes to show
        if (upcomingEpisodes != null) {
            if (upcomingEpisodes.moveToFirst()) {

                // Ensure those episodes are within the user set time frame
                long releaseTime = upcomingEpisodes
                        .getLong(ActivityFragment.ActivityQuery.EPISODE_FIRST_RELEASE_MS);
                if (releaseTime <= latestTimeToInclude) {
                    // build our DashClock panel

                    // title of first show
                    String expandedTitle = upcomingEpisodes.getString(
                            ActivityFragment.ActivityQuery.SHOW_TITLE);

                    // get the actual release time
                    Date actualRelease = TimeTools.getEpisodeReleaseTime(this, releaseTime);
                    String releaseDay = TimeTools.formatToLocalReleaseDay(actualRelease);
                    String absoluteTime = TimeTools.formatToLocalReleaseTime(this, actualRelease);

                    // time and network, e.g. 'Mon 10:00, Network'
                    StringBuilder expandedBody = new StringBuilder();
                    expandedBody.append(releaseDay).append(" ").append(absoluteTime);
                    String network = upcomingEpisodes
                            .getString(ActivityFragment.ActivityQuery.SHOW_NETWORK);
                    if (!TextUtils.isEmpty(network)) {
                        expandedBody.append(" — ").append(network);
                    }

                    // more than one episode at this time? Append e.g. '3 more'
                    int additionalEpisodes = 0;
                    while (upcomingEpisodes.moveToNext()
                            && releaseTime == upcomingEpisodes
                            .getLong(ActivityFragment.ActivityQuery.EPISODE_FIRST_RELEASE_MS)) {
                        additionalEpisodes++;
                    }
                    if (additionalEpisodes > 0) {
                        expandedBody.append("\n");
                        expandedBody.append(getString(R.string.more, additionalEpisodes));
                    }

                    publishUpdate(new ExtensionData()
                            .visible(true)
                            .icon(R.drawable.ic_notification)
                                    // 'Fri\n15:00'
                            .status(releaseDay + "\n" + absoluteTime)
                            .expandedTitle(expandedTitle)
                            .expandedBody(expandedBody.toString())
                            .clickIntent(
                                    new Intent(getApplicationContext(),
                                            ShowsActivity.class)
                                            .putExtra(
                                                    ShowsActivity.InitBundle.SELECTED_TAB,
                                                    ShowsActivity.InitBundle.INDEX_TAB_UPCOMING)));
                    upcomingEpisodes.close();
                    return;
                }
            }
            upcomingEpisodes.close();
        }

        // nothing to show
        publishUpdate(null);
    }
}
