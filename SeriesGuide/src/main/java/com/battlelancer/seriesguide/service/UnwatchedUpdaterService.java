package com.battlelancer.seriesguide.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.Nullable;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.DBUtils;
import timber.log.Timber;

/**
 * Updates episode counts for a specific season or all seasons of a show. Is an {@link
 * IntentService} so only one runs at a time.
 */
public class UnwatchedUpdaterService extends IntentService {

    public static final String EXTRA_SHOW_TVDB_ID = "showTvdbId";
    public static final String EXTRA_OPTIONAL_SEASON_TVDB_ID = "seasonTvdbId";

    public static Intent buildIntent(Context context, int showTvdbId) {
        return buildIntent(context, showTvdbId, null);
    }

    public static Intent buildIntent(Context context, int showTvdbId,
            @Nullable Integer seasonTvdbId) {
        Intent intent = new Intent(context, UnwatchedUpdaterService.class);
        intent.putExtra(EXTRA_SHOW_TVDB_ID, showTvdbId);
        if (seasonTvdbId != null) {
            intent.putExtra(EXTRA_OPTIONAL_SEASON_TVDB_ID, seasonTvdbId);
        }
        return intent;
    }

    public UnwatchedUpdaterService() {
        super("UnwatchedUpdaterService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) {
            Timber.e("Not running: intent is null.");
            return;
        }
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
