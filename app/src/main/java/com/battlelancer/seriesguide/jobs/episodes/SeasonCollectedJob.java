package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.TextTools;

public class SeasonCollectedJob extends SeasonBaseJob {

    private final boolean isCollected;

    public SeasonCollectedJob(int showTvdbId, int seasonTvdbId, int season, boolean isCollected) {
        super(showTvdbId, seasonTvdbId, season, isCollected ? 1 : 0,
                JobAction.EPISODE_COLLECTION);
        this.isCollected = isCollected;
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

    @NonNull
    @Override
    public String getConfirmationText(Context context) {
        String number = TextTools.getEpisodeNumber(context, season, -1);
        return TextTools.dotSeparate(number, context.getString(isCollected
                ? R.string.action_collection_add : R.string.action_collection_remove));
    }
}
