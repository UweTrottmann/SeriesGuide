package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.TextTools;

public class EpisodeCollectedJob extends EpisodeBaseJob {

    public EpisodeCollectedJob(int showTvdbId, int episodeTvdbId, int season,
            int episode, boolean isCollected) {
        super(showTvdbId, episodeTvdbId, season, episode, isCollected ? 1 : 0,
                JobAction.EPISODE_COLLECTION);
    }

    @Override
    protected String getDatabaseColumnToUpdate() {
        return SeriesGuideContract.Episodes.COLLECTED;
    }

    @Override
    public String getConfirmationText(Context context) {
        String number = TextTools.getEpisodeNumber(context, season, episode);
        return context.getString(getFlagValue() == 1 ? R.string.trakt_collected
                : R.string.trakt_notcollected, number);
    }
}
