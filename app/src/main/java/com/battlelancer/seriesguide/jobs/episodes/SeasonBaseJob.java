package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.provider.SgSeason2Numbers;

/**
 * Flagging whole seasons watched or collected.
 */
public abstract class SeasonBaseJob extends BaseEpisodesJob {

    protected final long seasonId;
    private SgSeason2Numbers season;

    public SeasonBaseJob(long seasonId, int flagValue, JobAction action) {
        super(flagValue, action);
        this.seasonId = seasonId;
    }

    @Override
    public boolean applyLocalChanges(Context context, boolean requiresNetworkJob) {
        SgSeason2Numbers season = SgRoomDatabase.getInstance(context).sgSeason2Helper()
                .getSeasonNumbers(seasonId);
        if (season == null) {
            return false;
        }
        this.season = season;

        return super.applyLocalChanges(context, requiresNetworkJob);
    }

    @NonNull
    public SgSeason2Numbers getSeason() {
        return season;
    }

    @Override
    protected long getShowId() {
        return getSeason().getShowId();
    }

    public long getSeasonId() {
        return seasonId;
    }
}
