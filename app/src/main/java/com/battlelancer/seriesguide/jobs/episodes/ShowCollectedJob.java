package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;

public class ShowCollectedJob extends ShowBaseJob {

    private final boolean isCollected;

    public ShowCollectedJob(int showTvdbId, boolean isCollected) {
        super(showTvdbId, isCollected ? 1 : 0, JobAction.EPISODE_COLLECTION);
        this.isCollected = isCollected;
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

    @NonNull
    @Override
    public String getConfirmationText(Context context) {
        return context.getString(isCollected
                ? R.string.action_collection_add : R.string.action_collection_remove);
    }

}
