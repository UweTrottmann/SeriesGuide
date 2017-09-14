package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import android.net.Uri;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;

public abstract class ShowBaseJob extends BaseJob {

    public ShowBaseJob(Context context, int showTvdbId, int flagValue, JobAction action) {
        super(context, showTvdbId, flagValue, action);
    }

    @Override
    public Uri getDatabaseUri() {
        return SeriesGuideContract.Episodes.buildEpisodesOfShowUri(
                String.valueOf(getShowTvdbId()));
    }

    @Override
    public String getConfirmationText() {
        return null;
    }
}
