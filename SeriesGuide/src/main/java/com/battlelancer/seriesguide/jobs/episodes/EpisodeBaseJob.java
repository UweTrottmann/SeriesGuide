package com.battlelancer.seriesguide.jobs.episodes;

import android.net.Uri;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;

/**
 * Flagging single episodes watched or collected.
 */
public abstract class EpisodeBaseJob extends BaseJob {

    protected int episodeTvdbId;
    protected int season;
    protected int episode;

    public EpisodeBaseJob(int showTvdbId, int episodeTvdbId, int season, int episode, int flagValue,
            JobAction action) {
        super(showTvdbId, flagValue, action);
        this.episodeTvdbId = episodeTvdbId;
        this.season = season;
        this.episode = episode;
    }

    @Override
    public Uri getDatabaseUri() {
        return SeriesGuideContract.Episodes.buildEpisodeUri(String.valueOf(episodeTvdbId));
    }

    @Override
    public String getDatabaseSelection() {
        return null;
    }
}
