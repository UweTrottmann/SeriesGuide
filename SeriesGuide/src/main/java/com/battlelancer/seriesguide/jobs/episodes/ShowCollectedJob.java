package com.battlelancer.seriesguide.jobs.episodes;

import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.uwetrottmann.seriesguide.backend.episodes.model.Episode;

public class ShowCollectedJob extends ShowBaseJob {

    public ShowCollectedJob(int showTvdbId, boolean isCollected) {
        super(showTvdbId, isCollected ? 1 : 0, JobAction.SHOW_COLLECTED);
    }

    @Override
    public String getDatabaseSelection() {
        // only exclude specials (here will only affect database + hexagon)
        return SeriesGuideContract.Episodes.SELECTION_NO_SPECIALS;
    }

    @Override
    protected String getDatabaseColumnToUpdate() {
        return SeriesGuideContract.Episodes.COLLECTED;
    }

}
