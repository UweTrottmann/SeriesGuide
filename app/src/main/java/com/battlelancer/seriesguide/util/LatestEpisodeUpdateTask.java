package com.battlelancer.seriesguide.util;

import android.annotation.SuppressLint;
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

    @SuppressLint("StaticFieldLeak") private final Context context;

    public LatestEpisodeUpdateTask(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    protected Void doInBackground(Integer... params) {
        updateLatestEpisodeFor(context, null);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        TaskManager.getInstance().releaseNextEpisodeUpdateTaskRef();
    }

    public static void updateLatestEpisodeFor(Context context, Long showId) {
        if (showId != null) {
            // update single show
            Timber.d("Updating next episode for show %s", showId);
            new NextEpisodeUpdater().updateForShows(context, showId);
        } else {
            // update all shows
            Timber.d("Updating next episodes for all shows");
            new NextEpisodeUpdater().updateForShows(context, null);
        }

        // Show cursors already notified
        // List item cursors need to be notified manually as uri differs
        context.getContentResolver()
                .notifyChange(SeriesGuideContract.ListItems.CONTENT_WITH_DETAILS_URI, null);
    }
}
