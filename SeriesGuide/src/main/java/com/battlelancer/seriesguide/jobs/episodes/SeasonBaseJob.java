package com.battlelancer.seriesguide.jobs.episodes;

import android.net.Uri;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;

/**
 * Flagging whole seasons watched or collected.
 */
public abstract class SeasonBaseJob extends BaseJob {

    protected int seasonTvdbId;
    protected int season;

    public SeasonBaseJob(int showTvdbId, int seasonTvdbId, int season, int flagValue,
            JobAction action) {
        super(showTvdbId, flagValue, action);
        this.seasonTvdbId = seasonTvdbId;
        this.season = season;
    }

    public int getSeasonTvdbId() {
        return seasonTvdbId;
    }

    @Override
    public Uri getDatabaseUri() {
        return SeriesGuideContract.Episodes.buildEpisodesOfSeasonUri(
                String.valueOf(seasonTvdbId));
    }
}
