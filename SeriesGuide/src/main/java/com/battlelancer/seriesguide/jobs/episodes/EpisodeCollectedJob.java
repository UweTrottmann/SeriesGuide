package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.TextTools;

public class EpisodeCollectedJob extends EpisodeBaseJob {

    private final boolean isCollected;

    public EpisodeCollectedJob(int showTvdbId, int episodeTvdbId, int season,
            int episode, boolean isCollected) {
        super(showTvdbId, episodeTvdbId, season, episode, isCollected ? 1 : 0,
                JobAction.EPISODE_COLLECTION);
        this.isCollected = isCollected;
    }

    @Override
    protected String getDatabaseColumnToUpdate() {
        return SeriesGuideContract.Episodes.COLLECTED;
    }

    @NonNull
    @Override
    public String getConfirmationText(Context context) {
        String number = TextTools.getEpisodeNumber(context, season, episode);
        return TextTools.dotSeparate(number, context.getString(isCollected
                ? R.string.action_collection_add : R.string.action_collection_remove));
    }
}
