package com.battlelancer.seriesguide.jobs.episodes;

import android.content.ContentValues;
import android.content.Context;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.jobs.BaseJob;
import com.battlelancer.seriesguide.jobs.EpisodeInfo;
import com.battlelancer.seriesguide.jobs.FlagJob;
import com.battlelancer.seriesguide.jobs.SgJobInfo;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns;
import com.battlelancer.seriesguide.provider.SgEpisode2Numbers;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;
import com.battlelancer.seriesguide.util.LatestEpisodeUpdateTask;
import com.google.flatbuffers.FlatBufferBuilder;
import java.util.List;

public abstract class BaseEpisodesJob extends BaseJob implements FlagJob {

    private final int flagValue;

    public BaseEpisodesJob(int flagValue, JobAction action) {
        super(action);
        this.flagValue = flagValue;
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

    protected abstract long getShowId();

    /**
     * Builds and executes the database op required to flag episodes in the local database,
     * notifies affected URIs, may update the list widget.
     */
    @Override
    @CallSuper
    public boolean applyLocalChanges(Context context, boolean requiresNetworkJob) {
        // prepare network job
        byte[] networkJobInfo = null;
        if (requiresNetworkJob) {
            networkJobInfo = prepareNetworkJob(context);
            if (networkJobInfo == null) {
                return false;
            }
        }

        // apply local updates
        boolean updated = applyDatabaseChanges(context);
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

    protected abstract boolean applyDatabaseChanges(@NonNull Context context);

    /**
     * Note: Ensure episodes are ordered by season number (lowest first),
     * then episode number (lowest first).
     */
    @NonNull
    protected abstract List<SgEpisode2Numbers> getEpisodesForNetworkJob(@NonNull Context context);

    /**
     * Returns the number of plays to upload to Cloud (Trakt currently not supported)
     * based on the current number of plays (before {@link #applyLocalChanges(Context, boolean)}.
     */
    protected abstract int getPlaysForNetworkJob(int plays);

    @Nullable
    private byte[] prepareNetworkJob(Context context) {
        // store affected episodes for network part
        List<SgEpisode2Numbers> episodes = getEpisodesForNetworkJob(context);

        FlatBufferBuilder builder = new FlatBufferBuilder(0);

        int[] episodeInfos = new int[episodes.size()];
        for (int i = 0; i < episodes.size(); i++) {
            SgEpisode2Numbers episode = episodes.get(i);
            int plays = getPlaysForNetworkJob(episode.getPlays());
            episodeInfos[i] = EpisodeInfo
                    .createEpisodeInfo(builder, episode.getSeason(), episode.getEpisodenumber(),
                            plays);
        }

        int episodesVector = SgJobInfo.createEpisodesVector(builder, episodeInfos);
        int jobInfo = SgJobInfo.createSgJobInfo(builder, flagValue, episodesVector, 0, 0, getShowId());

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
            long lastWatchedEpisodeId, boolean setLastWatchedToNow) {
        if (lastWatchedEpisodeId != -1 || setLastWatchedToNow) {
            ContentValues values = new ContentValues();
            if (lastWatchedEpisodeId != -1) {
                values.put(SgShow2Columns.LASTWATCHEDID, lastWatchedEpisodeId);
            }
            if (setLastWatchedToNow) {
                values.put(SgShow2Columns.LASTWATCHED_MS, System.currentTimeMillis());
            }
            context.getContentResolver().update(
                    SgShow2Columns.buildIdUri(getShowId()),
                    values, null, null);
        }
        LatestEpisodeUpdateTask.updateLatestEpisodeFor(context, getShowId());
    }
}
