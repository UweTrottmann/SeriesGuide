package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import android.net.Uri;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;

public abstract class ShowBaseJob extends BaseJob {

    public ShowBaseJob(int showTvdbId, int flagValue, JobAction action) {
        super(showTvdbId, flagValue, action);
    }

    @Override
    public Uri getDatabaseUri() {
        return SeriesGuideContract.Episodes.buildEpisodesOfShowUri(
                String.valueOf(getShowTvdbId()));
    }

    @Override
    public String getConfirmationText(Context context) {
        return null;
    }
}
