// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.TaskManager;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import timber.log.Timber;

/**
 * Updates the latest episode value for a given show or all shows. If supplied a show TVDb id will
 * update only latest episode for that show.
 *
 * <p><b>Do NOT run in parallel as this task is memory intensive.</b>
 */
public class LatestEpisodeUpdateTask{

    @SuppressLint("StaticFieldLeak") private final Context context;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    Future futureResult;

    public LatestEpisodeUpdateTask(Context context) {
        this.context = context.getApplicationContext();
    }

    public void performBackgroundTask() {
         futureResult = executor.submit(new Callable<String>() {
            @Override
            public String call() {

                //background task...
                updateLatestEpisodeFor(context, null);
                return null;
            }
        });

        // do post task activities here
        TaskManager.getInstance().releaseNextEpisodeUpdateTaskRef();
    }

    public Boolean getStatus(){
        return futureResult.isDone();
    }

    public static void updateLatestEpisodeFor(Context context, Long showId) {
        if (showId != null) {
            // update single show
            Timber.d("Updating next episode for show %s", showId);
            new NextEpisodeUpdater(context).updateForShows(showId);
        } else {
            // update all shows
            Timber.d("Updating next episodes for all shows");
            new NextEpisodeUpdater(context).updateForShows(null);
        }

        // Show cursors already notified
        // List item cursors need to be notified manually as uri differs
        context.getContentResolver()
                .notifyChange(SeriesGuideContract.ListItems.CONTENT_WITH_DETAILS_URI, null);
    }
}
