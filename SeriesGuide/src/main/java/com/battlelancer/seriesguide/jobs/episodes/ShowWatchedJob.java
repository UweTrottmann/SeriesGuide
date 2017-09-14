package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.uwetrottmann.seriesguide.backend.episodes.model.Episode;
import com.uwetrottmann.trakt5.entities.SyncSeason;
import java.util.List;

public class ShowWatchedJob extends ShowBaseJob {

    private final long currentTime;

    public ShowWatchedJob(Context context, int showTvdbId, int flagValue) {
        super(context, showTvdbId, flagValue, JobAction.SHOW_WATCHED);
        currentTime = TimeTools.getCurrentTime(context);
    }

    @Override
    public String getDatabaseSelection() {
        if (EpisodeTools.isUnwatched(getFlagValue())) {
            // set unwatched
            // include watched or skipped episodes
            return SeriesGuideContract.Episodes.SELECTION_WATCHED_OR_SKIPPED
                    + " AND " + SeriesGuideContract.Episodes.SELECTION_NO_SPECIALS;
        } else {
            // set watched or skipped
            // do NOT mark watched episodes again to avoid trakt adding a new watch
            // only mark episodes that have been released until within the hour
            return SeriesGuideContract.Episodes.FIRSTAIREDMS + "<=" + (currentTime
                    + DateUtils.HOUR_IN_MILLIS)
                    + " AND " + SeriesGuideContract.Episodes.SELECTION_HAS_RELEASE_DATE
                    + " AND " + SeriesGuideContract.Episodes.SELECTION_UNWATCHED_OR_SKIPPED
                    + " AND " + SeriesGuideContract.Episodes.SELECTION_NO_SPECIALS;
        }
    }

    @Override
    protected void setHexagonFlag(Episode episode) {
        episode.setWatchedFlag(getFlagValue());
    }

    @Override
    protected String getDatabaseColumnToUpdate() {
        return SeriesGuideContract.Episodes.WATCHED;
    }

    @Override
    public List<SyncSeason> getEpisodesForTrakt() {
        return buildTraktEpisodeList();
    }

    @Override
    public boolean applyLocalChanges() {
        if (!super.applyLocalChanges()) {
            return false;
        }

        int lastWatchedEpisodeTvdbId = EpisodeTools.isUnwatched(getFlagValue())
                ? 0 /* just reset */
                : -1 /* we don't care */;

        // set a new last watched episode
        // set last watched time to now if marking as watched or skipped
        updateLastWatched(lastWatchedEpisodeTvdbId,
                !EpisodeTools.isUnwatched(getFlagValue()));

        ListWidgetProvider.notifyAllAppWidgetsViewDataChanged(getContext());

        return true;
    }
}
