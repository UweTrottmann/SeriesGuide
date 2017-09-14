package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.uwetrottmann.seriesguide.backend.episodes.model.Episode;
import com.uwetrottmann.trakt5.entities.SyncSeason;
import java.util.List;

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
    protected void setHexagonFlag(Episode episode) {
        episode.setIsInCollection(EpisodeTools.isCollected(getFlagValue()));
    }

    @Override
    protected String getDatabaseColumnToUpdate() {
        return SeriesGuideContract.Episodes.COLLECTED;
    }

    @Override
    public List<SyncSeason> getEpisodesForTrakt(Context context) {
        // send whole show
        return null;
    }
}
