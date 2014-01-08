
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

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import com.battlelancer.seriesguide.settings.DashClockSettings;
import com.battlelancer.seriesguide.ui.ActivityFragment;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;

import android.content.Intent;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

public class UpcomingEpisodeExtension extends DashClockExtension {

    @Override
    protected void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);
        setUpdateWhenScreenOn(true);
    }

    @Override
    protected void onUpdateData(int arg0) {
        final Cursor upcomingEpisodes = DBUtils.getUpcomingEpisodes(true, getApplicationContext());
        final long fakeNow = Utils.getFakeCurrentTime(PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext()));
        int hourThreshold = DashClockSettings.getUpcomingTreshold(getApplicationContext());
        long latestTimeToInclude = fakeNow + hourThreshold * DateUtils.HOUR_IN_MILLIS;

        // Ensure there are episodes to show
        if (upcomingEpisodes != null) {
            if (upcomingEpisodes.moveToFirst()) {

                // Ensure those episodes are within the user set time frame
                long firstairedms = upcomingEpisodes.getLong(ActivityFragment.ActivityQuery.FIRSTAIREDMS);
                if (firstairedms <= latestTimeToInclude) {

                    // build our DashClock panel
                    final String[] timeAndDay = Utils.formatToTimeAndDay(
                            firstairedms, this);

                    // Looks like 'Community, NBC' when expanded
                    String expandedBody = upcomingEpisodes.getString(ActivityFragment.ActivityQuery.SHOW_TITLE)
                            + ", "
                            + upcomingEpisodes.getString(ActivityFragment.ActivityQuery.SHOW_NETWORK);
                    // Show all episodes airing the same time as the first one
                    while (upcomingEpisodes.moveToNext()
                            && firstairedms == upcomingEpisodes
                            .getLong(ActivityFragment.ActivityQuery.FIRSTAIREDMS)) {
                        expandedBody += "\n" + upcomingEpisodes.getString(
                                ActivityFragment.ActivityQuery.SHOW_TITLE)
                                + ", "
                                + upcomingEpisodes.getString(ActivityFragment.ActivityQuery.SHOW_NETWORK);
                    }

                    publishUpdate(new ExtensionData()
                            .visible(true)
                            .icon(R.drawable.ic_notification)
                                    // 'Fri \n 15:00'
                            .status(timeAndDay[1] + "\n" + timeAndDay[0])
                                    // 'in 10 mins, Fri 15:00'
                            .expandedTitle(
                                    timeAndDay[2] + ", " + timeAndDay[1] + " " + timeAndDay[0])
                            .expandedBody(expandedBody)
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
