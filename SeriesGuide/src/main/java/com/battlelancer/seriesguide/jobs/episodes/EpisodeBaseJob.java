package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import android.net.Uri;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.uwetrottmann.seriesguide.backend.episodes.model.Episode;
import com.uwetrottmann.trakt5.entities.SyncEpisode;
import com.uwetrottmann.trakt5.entities.SyncSeason;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Flagging single episodes watched or collected.
 */
public abstract class EpisodeBaseJob extends BaseJob {

    protected int episodeTvdbId;
    protected int season;
    protected int episode;

    public EpisodeBaseJob(Context context, int showTvdbId, int episodeTvdbId, int season,
            int episode, int flagValue, JobAction action) {
        super(context, showTvdbId, flagValue, action);
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

    @Override
    public List<Episode> getEpisodesForHexagon() {
        List<Episode> episodes = new ArrayList<>();

        Episode episode = new Episode();
        setHexagonFlag(episode);
        episode.setSeasonNumber(season);
        episode.setEpisodeNumber(this.episode);
        episodes.add(episode);

        return episodes;
    }

    @Override
    public List<SyncSeason> getEpisodesForTrakt() {
        // flag a single episode
        List<SyncSeason> seasons = new LinkedList<>();
        seasons.add(new SyncSeason().number(season)
                .episodes(new SyncEpisode().number(episode)));
        return seasons;
    }
}
