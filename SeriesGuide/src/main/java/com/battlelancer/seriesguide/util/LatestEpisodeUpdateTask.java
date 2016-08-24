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
