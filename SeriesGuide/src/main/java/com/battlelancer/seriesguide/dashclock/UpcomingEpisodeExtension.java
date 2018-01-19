
package com.battlelancer.seriesguide.dashclock;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.text.TextUtils;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DashClockSettings;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.ui.shows.CalendarQuery;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.TextTools;
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
        Context context = getApplicationContext();
        // as details for only 1 episode can be shown, always exclude watched episodes
        final Cursor upcomingEpisodes = DBUtils.upcomingEpisodesQuery(context, true);
        final long customCurrentTime = TimeTools.getCurrentTime(context);
        int hourThreshold = DashClockSettings.getUpcomingTreshold(context);
        long latestTimeToInclude = customCurrentTime + hourThreshold * DateUtils.HOUR_IN_MILLIS;

        // Ensure there are episodes to show
        if (upcomingEpisodes != null) {
            if (upcomingEpisodes.moveToFirst()) {

                // Ensure those episodes are within the user set time frame
                long releaseTime = upcomingEpisodes
                        .getLong(CalendarQuery.RELEASE_TIME_MS);
                if (releaseTime <= latestTimeToInclude) {
                    // build our DashClock panel

                    // title and episode of first show, like 'Title 1x01'
                    String expandedTitle = TextTools.getShowWithEpisodeNumber(context,
                            upcomingEpisodes.getString(CalendarQuery.SHOW_TITLE),
                            upcomingEpisodes.getInt(CalendarQuery.SEASON),
                            upcomingEpisodes.getInt(CalendarQuery.NUMBER)
                    );

                    // get the actual release time
                    Date actualRelease = TimeTools.applyUserOffset(this, releaseTime);
                    String absoluteTime = TimeTools.formatToLocalTime(this, actualRelease);
                    String releaseDay = TimeTools.formatToLocalDay(actualRelease);

                    // time and network, e.g. 'Mon 10:00, Network'
                    StringBuilder expandedBody = new StringBuilder();
                    if (!DateUtils.isToday(actualRelease.getTime())) {
                        expandedBody.append(releaseDay).append(" ");
                    }
                    expandedBody.append(absoluteTime);
                    String network = upcomingEpisodes
                            .getString(CalendarQuery.SHOW_NETWORK);
                    if (!TextUtils.isEmpty(network)) {
                        expandedBody.append(" — ").append(network);
                    }

                    // more than one episode at this time? Append e.g. '3 more'
                    int additionalEpisodes = 0;
                    while (upcomingEpisodes.moveToNext()
                            && releaseTime == upcomingEpisodes
                            .getLong(CalendarQuery.RELEASE_TIME_MS)) {
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
                                    new Intent(context, ShowsActivity.class)
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
