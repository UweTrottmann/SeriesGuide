// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2014 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.shows.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import com.battlelancer.seriesguide.util.TaskManager;
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
        TaskManager.releaseNextEpisodeUpdateTaskRef();
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
    }
}
