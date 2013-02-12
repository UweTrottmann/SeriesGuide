
package com.battlelancer.seriesguide.dashclock;

import android.content.Intent;
import android.database.Cursor;

import com.battlelancer.seriesguide.ui.UpcomingFragment.UpcomingQuery;
import com.battlelancer.seriesguide.ui.UpcomingRecentActivity;
import com.battlelancer.seriesguide.util.DBUtils;
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

        if (upcomingEpisodes != null && upcomingEpisodes.getCount() > 0
                && upcomingEpisodes.moveToFirst()) {
            final String[] timeAndDay = Utils.formatToTimeAndDay(
                    upcomingEpisodes.getLong(UpcomingQuery.FIRSTAIREDMS), this);

            publishUpdate(new ExtensionData()
                    .visible(true)
                    .icon(R.drawable.ic_notification)
                    .status(timeAndDay[2])
                    .expandedTitle(
                            timeAndDay[2] + " - "
                                    + upcomingEpisodes.getString(UpcomingQuery.SHOW_TITLE))
                    .expandedBody(
                            getString(R.string.upcoming_show_detailed, timeAndDay[0],
                                    upcomingEpisodes.getString(UpcomingQuery.SHOW_NETWORK)))
                    .clickIntent(
                            new Intent(getApplicationContext(), UpcomingRecentActivity.class)
                                    .putExtra(
                                            UpcomingRecentActivity.InitBundle.SELECTED_TAB, 1)));
        } else {
            // nothing to show
            publishUpdate(null);
        }

        if (upcomingEpisodes != null) {
            upcomingEpisodes.close();
        }
    }
}
