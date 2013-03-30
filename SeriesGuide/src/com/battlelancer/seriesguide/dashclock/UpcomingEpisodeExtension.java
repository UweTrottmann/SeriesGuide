
package com.battlelancer.seriesguide.dashclock;

import android.content.Intent;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

import com.battlelancer.seriesguide.ui.UpcomingFragment.UpcomingQuery;
import com.battlelancer.seriesguide.ui.UpcomingRecentActivity;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.DashClockSettings;
import com.battlelancer.seriesguide.util.Utils;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.uwetrottmann.seriesguide.R;

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
                long firstairedms = upcomingEpisodes.getLong(UpcomingQuery.FIRSTAIREDMS);
                if (firstairedms <= latestTimeToInclude) {

                    // build our DashClock panel
                    final String[] timeAndDay = Utils.formatToTimeAndDay(
                            firstairedms, this);

                    // Looks like 'Community, NBC' when expanded
                    String expandedBody = upcomingEpisodes.getString(UpcomingQuery.SHOW_TITLE)
                            + ", "
                            + upcomingEpisodes.getString(UpcomingQuery.SHOW_NETWORK);
                    // Show all episodes airing the same time as the first one
                    while (upcomingEpisodes.moveToNext()
                            && firstairedms == upcomingEpisodes.getLong(UpcomingQuery.FIRSTAIREDMS)) {
                        expandedBody += "\n" + upcomingEpisodes.getString(UpcomingQuery.SHOW_TITLE)
                                + ", "
                                + upcomingEpisodes.getString(UpcomingQuery.SHOW_NETWORK);
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
                                            UpcomingRecentActivity.class)
                                            .putExtra(
                                                    UpcomingRecentActivity.InitBundle.SELECTED_TAB,
                                                    0)));
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
