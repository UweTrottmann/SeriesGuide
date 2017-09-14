package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.uwetrottmann.seriesguide.backend.episodes.model.Episode;
import com.uwetrottmann.trakt5.entities.SyncSeason;
import java.util.LinkedList;
import java.util.List;

public class SeasonCollectedJob extends SeasonBaseJob {

    public SeasonCollectedJob(Context context, int showTvdbId, int seasonTvdbId, int season,
            boolean isCollected) {
        super(context, showTvdbId, seasonTvdbId, season, isCollected ? 1 : 0,
                JobAction.SEASON_COLLECTED);
    }

    @Override
    public String getDatabaseSelection() {
        // include all episodes of season
        return null;
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
    public List<SyncSeason> getEpisodesForTrakt() {
        // flag the whole season
        List<SyncSeason> seasons = new LinkedList<>();
        seasons.add(new SyncSeason().number(season));
        return seasons;
    }

    @Override
    public String getConfirmationText() {
        String number = TextTools.getEpisodeNumber(getContext(), season, -1);
        return getContext().getString(getFlagValue() == 1 ? R.string.trakt_collected
                : R.string.trakt_notcollected, number);
    }
}
