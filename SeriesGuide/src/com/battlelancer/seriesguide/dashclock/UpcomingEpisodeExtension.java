
package com.battlelancer.seriesguide.dashclock;

import android.content.Intent;
import android.net.Uri;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.uwetrottmann.seriesguide.R;

public class UpcomingEpisodeExtension extends DashClockExtension {

    @Override
    protected void onUpdateData(int arg0) {
        publishUpdate(new ExtensionData()
                .visible(true)
                .icon(R.drawable.ic_notification)
                .status("Hello")
                .expandedTitle("Hello, world!")
                .expandedBody("This is an example.")
                .clickIntent(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://www.google.com"))));
    }

}
