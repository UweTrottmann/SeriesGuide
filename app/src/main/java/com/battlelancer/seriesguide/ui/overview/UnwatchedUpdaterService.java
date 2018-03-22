package com.battlelancer.seriesguide.ui.overview;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.JobIntentService;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.DBUtils;
import timber.log.Timber;

/**
 * Updates episode counts for a specific season or all seasons of a show. Is a {@link
 * JobIntentService} so only one runs at a time. Not using an IntentService as it ran into
 * O background restrictions on some devices (even though it was started during onStart).
 */
public class UnwatchedUpdaterService extends JobIntentService {

    public static final String EXTRA_SHOW_TVDB_ID = "showTvdbId";
    public static final String EXTRA_OPTIONAL_SEASON_TVDB_ID = "seasonTvdbId";

    static void enqueue(Context context, int showTvdbId) {
        enqueue(context, showTvdbId, null);
    }

    static void enqueue(Context context, int showTvdbId, @Nullable Integer seasonTvdbId) {
        Intent work = new Intent(context, UnwatchedUpdaterService.class);
        work.putExtra(EXTRA_SHOW_TVDB_ID, showTvdbId);
        if (seasonTvdbId != null) {
            work.putExtra(EXTRA_OPTIONAL_SEASON_TVDB_ID, seasonTvdbId);
        }
        enqueueWork(context.getApplicationContext(), UnwatchedUpdaterService.class,
                SgApp.JOB_ID_UNWATCHED_UPDATER_SERVICE, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        int showTvdbId = intent.getIntExtra(EXTRA_SHOW_TVDB_ID, -1);
        if (showTvdbId < 0) {
            Timber.e("Not running: no showTvdbId.");
        }

        int seasonTvdbId = intent.getIntExtra(EXTRA_OPTIONAL_SEASON_TVDB_ID, -1);
        if (seasonTvdbId != -1) {
            // update one season
            DBUtils.updateUnwatchedCount(this, seasonTvdbId);
        } else {
            // update all seasons of this show, start with the most recent
            // one
            final Cursor seasons = getContentResolver().query(
                    SeriesGuideContract.Seasons.buildSeasonsOfShowUri(showTvdbId), new String[] {
                            SeriesGuideContract.Seasons._ID
                    }, null, null, SeriesGuideContract.Seasons.COMBINED + " DESC"
            );
            if (seasons == null) {
                return;
            }
            while (seasons.moveToNext()) {
                int seasonId = seasons.getInt(0);
                DBUtils.updateUnwatchedCount(this, seasonId);

                notifyContentProvider(showTvdbId);
            }
            seasons.close();
        }

        notifyContentProvider(showTvdbId);
        Timber.i("Updated watched count: show %d, season %d", showTvdbId, seasonTvdbId);
    }

    private void notifyContentProvider(int showTvdbId) {
        getContentResolver().notifyChange(
                SeriesGuideContract.Seasons.buildSeasonsOfShowUri(showTvdbId), null);
    }
}
