package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.TextTools;

public class SeasonCollectedJob extends SeasonBaseJob {

    public SeasonCollectedJob(int showTvdbId, int seasonTvdbId, int season, boolean isCollected) {
        super(showTvdbId, seasonTvdbId, season, isCollected ? 1 : 0,
                JobAction.EPISODE_COLLECTION);
    }

    @Override
    public String getDatabaseSelection() {
        // include all episodes of season
        return null;
    }

    @Override
    protected String getDatabaseColumnToUpdate() {
        return SeriesGuideContract.Episodes.COLLECTED;
    }

    @Override
    public String getConfirmationText(Context context) {
        String number = TextTools.getEpisodeNumber(context, season, -1);
        return context.getString(getFlagValue() == 1 ? R.string.trakt_collected
                : R.string.trakt_notcollected, number);
    }
}
