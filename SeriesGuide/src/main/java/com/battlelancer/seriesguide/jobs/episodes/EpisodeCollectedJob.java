package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.uwetrottmann.seriesguide.backend.episodes.model.Episode;

public class EpisodeCollectedJob extends EpisodeBaseJob {

    public EpisodeCollectedJob(Context context, int showTvdbId, int episodeTvdbId, int season,
            int episode, boolean isCollected) {
        super(context, showTvdbId, episodeTvdbId, season, episode, isCollected ? 1 : 0,
                JobAction.EPISODE_COLLECTED);
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
    public String getConfirmationText() {
        String number = TextTools.getEpisodeNumber(getContext(), season, episode);
        return getContext().getString(getFlagValue() == 1 ? R.string.trakt_collected
                : R.string.trakt_notcollected, number);
    }
}
