package com.battlelancer.seriesguide.jobs.episodes;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.jobs.BaseJob;
import com.battlelancer.seriesguide.jobs.EpisodeInfo;
import com.battlelancer.seriesguide.jobs.FlagJob;
import com.battlelancer.seriesguide.jobs.SgJobInfo;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;
import com.battlelancer.seriesguide.util.LatestEpisodeUpdateTask;
import com.google.flatbuffers.FlatBufferBuilder;

public abstract class BaseEpisodesJob extends BaseJob implements FlagJob {

    public static final String[] PROJECTION_EPISODE = new String[] {
            Episodes._ID
    };
    public static final String[] PROJECTION_SEASON_NUMBER = new String[] {
            Episodes.SEASON,
            Episodes.NUMBER
    };
    public static final String ORDER_SEASON_ASC_NUMBER_ASC =
            Episodes.SORT_SEASON_ASC + ", " + Episodes.SORT_NUMBER_ASC;

    private final int showTvdbId;
    private final int flagValue;

    public BaseEpisodesJob(int showTvdbId, int flagValue, JobAction action) {
        super(action);
        this.showTvdbId = showTvdbId;
        this.flagValue = flagValue;
    }

    public int getShowTvdbId() {
        return showTvdbId;
    }

    @Override
    public boolean supportsHexagon() {
        return true;
    }

    @Override
    public boolean supportsTrakt() {
        /* No need to create network job for skipped episodes, not supported by trakt.
        Note that a network job might still be created if hexagon is connected. */
        return !EpisodeTools.isSkipped(flagValue);
    }

    protected int getFlagValue() {
        return flagValue;
    }

    protected abstract Uri getDatabaseUri();

    protected abstract String getDatabaseSelection();

    /**
     * Return the column which should get updated, either {@link Episodes}
     * .WATCHED or {@link Episodes}.COLLECTED.
     */
    protected abstract String getDatabaseColumnToUpdate();

    /**
     * Builds and executes the database op required to flag episodes in the local database,
     * notifies affected URIs, may update the list widget.
     */
    @Override
    @CallSuper
    public boolean applyLocalChanges(Context context, boolean requiresNetworkJob) {
        // determine query uri
        Uri uri = getDatabaseUri();
        if (uri == null) {
            return false;
        }

        // prepare network job
        byte[] networkJobInfo = null;
        if (requiresNetworkJob) {
            networkJobInfo = prepareNetworkJob(context, uri);
            if (networkJobInfo == null) {
                return false;
            }
        }

        // apply local updates
        boolean updated = applyDatabaseChanges(context, uri);
        if (!updated) {
            return false;
        }

        // persist network job after successful local updates
        if (requiresNetworkJob) {
            if (!persistNetworkJob(context, networkJobInfo)) {
                return false;
            }
        }

        // notify some other URIs about updates
        context.getContentResolver()
                .notifyChange(Episodes.CONTENT_URI, null);
        context.getContentResolver()
                .notifyChange(SeriesGuideContract.ListItems.CONTENT_WITH_DETAILS_URI, null);

        return true;
    }

    protected boolean applyDatabaseChanges(Context context, Uri uri) {
        ContentValues values = new ContentValues();
        values.put(getDatabaseColumnToUpdate(), getFlagValue());
        int updated = context.getContentResolver()
                .update(uri, values, getDatabaseSelection(), null);
        return updated >= 0; // -1 means error
    }

    @Nullable
    private byte[] prepareNetworkJob(Context context, @NonNull Uri uri) {
        // store affected episodes for network part
        Cursor query = context.getContentResolver()
                .query(uri, PROJECTION_SEASON_NUMBER, getDatabaseSelection(), null,
                        ORDER_SEASON_ASC_NUMBER_ASC);
        if (query == null) {
            return null;
        }
        if (!query.moveToFirst()) {
            query.close();
            return null;
        }

        FlatBufferBuilder builder = new FlatBufferBuilder(0);

        int[] episodeInfos = new int[query.getCount()];
        int i = 0;
        do {
            int season = query.getInt(0);
            int number = query.getInt(1);
            episodeInfos[i] = EpisodeInfo.createEpisodeInfo(builder, season, number);
            i++;
        } while (query.moveToNext());
        query.close();

        int episodes = SgJobInfo.createEpisodesVector(builder, episodeInfos);
        int jobInfo = SgJobInfo.createSgJobInfo(builder, showTvdbId, flagValue, episodes, 0);

        builder.finish(jobInfo);
        return builder.sizedByteArray();
    }

    /**
     * Set last watched episode and/or last watched time of a show, then update the episode shown as
     * next.
     *
     * @param lastWatchedEpisodeId The last watched episode for a show to save to the database. -1
     * for no-op.
     * @param setLastWatchedToNow Whether to set the last watched time of a show to now.
     */
    protected final void updateLastWatched(Context context,
            int lastWatchedEpisodeId, boolean setLastWatchedToNow) {
        if (lastWatchedEpisodeId != -1 || setLastWatchedToNow) {
            ContentValues values = new ContentValues();
            if (lastWatchedEpisodeId != -1) {
                values.put(SeriesGuideContract.Shows.LASTWATCHEDID, lastWatchedEpisodeId);
            }
            if (setLastWatchedToNow) {
                values.put(SeriesGuideContract.Shows.LASTWATCHED_MS,
                        System.currentTimeMillis());
            }
            context.getContentResolver().update(
                    SeriesGuideContract.Shows.buildShowUri(String.valueOf(showTvdbId)),
                    values, null, null);
        }
        LatestEpisodeUpdateTask.updateLatestEpisodeFor(context, getShowTvdbId());
    }
}
