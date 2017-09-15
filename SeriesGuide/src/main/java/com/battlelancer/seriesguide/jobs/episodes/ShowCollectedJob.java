package com.battlelancer.seriesguide.jobs.episodes;

import com.battlelancer.seriesguide.provider.SeriesGuideContract;

public class ShowCollectedJob extends ShowBaseJob {

    public ShowCollectedJob(int showTvdbId, boolean isCollected) {
        super(showTvdbId, isCollected ? 1 : 0, JobAction.EPISODE_COLLECTION);
    }

    @Override
    public String getDatabaseSelection() {
        // only exclude specials
        return SeriesGuideContract.Episodes.SELECTION_NO_SPECIALS;
    }

    @Override
    protected String getDatabaseColumnToUpdate() {
        return SeriesGuideContract.Episodes.COLLECTED;
    }

}
