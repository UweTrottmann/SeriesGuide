// SPDX-License-Identifier: Apache-2.0
// Copyright 2017, 2018, 2020-2023 Uwe Trottmann

package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.shows.database.SgEpisode2Helper;
import com.battlelancer.seriesguide.shows.database.SgEpisode2Numbers;
import com.battlelancer.seriesguide.shows.episodes.EpisodeFlags;
import com.battlelancer.seriesguide.shows.episodes.EpisodeTools;
import java.util.List;

/**
 * Sets *all* episodes of a season watched, skipped or not watched. Includes also episodes that do
 * not have a release date, which is common for special episodes. Also it is unexpected if all does
 * not mean all (previously this only marked episodes with release date up to current time + 1 hour).
 */
public class SeasonWatchedJob extends SeasonBaseJob {

    public SeasonWatchedJob(long seasonId, int episodeFlags) {
        super(seasonId, episodeFlags, JobAction.EPISODE_WATCHED_FLAG);
    }

    private long getLastWatchedEpisodeId(Context context) {
        if (EpisodeTools.isUnwatched(getFlagValue())) {
            // unwatched season
            // just reset
            return 0;
        } else {
            // watched or skipped season

            // Get the highest episode of the season.
            long highestWatchedId = SgRoomDatabase.getInstance(context)
                    .sgEpisode2Helper()
                    .getHighestEpisodeOfSeason(seasonId);
            if (highestWatchedId != 0) {
                return highestWatchedId;
            } else {
                return -1; // do not change
            }
        }
    }

    @Override
    public boolean applyLocalChanges(Context context, boolean requiresNetworkJob) {
        if (!super.applyLocalChanges(context, requiresNetworkJob)) {
            return false;
        }

        // set a new last watched episode
        // set last watched time to now if marking as watched or skipped
        updateLastWatched(context, getLastWatchedEpisodeId(context),
                !EpisodeTools.isUnwatched(getFlagValue()));

        ListWidgetProvider.notifyDataChanged(context);

        return true;
    }

    @Override
    protected boolean applyDatabaseChanges(@NonNull Context context) {
        SgEpisode2Helper helper = SgRoomDatabase.getInstance(context).sgEpisode2Helper();

        int rowsUpdated;
        switch (getFlagValue()) {
            case EpisodeFlags.SKIPPED:
                rowsUpdated = helper.setSeasonSkipped(seasonId);
                break;
            case EpisodeFlags.WATCHED:
                rowsUpdated = helper.setSeasonWatchedAndAddPlay(seasonId);
                break;
            case EpisodeFlags.UNWATCHED:
                rowsUpdated = helper.setSeasonNotWatchedAndRemovePlays(seasonId);
                break;
            default:
                throw new IllegalArgumentException("Flag value not supported");
        }
        return rowsUpdated >= 0; // -1 means error.
    }

    @NonNull
    @Override
    protected List<SgEpisode2Numbers> getEpisodesForNetworkJob(@NonNull Context context) {
        SgEpisode2Helper helper = SgRoomDatabase.getInstance(context).sgEpisode2Helper();
        if (EpisodeTools.isUnwatched(getFlagValue())) {
            // set unwatched
            // include watched or skipped episodes
            return helper.getWatchedOrSkippedEpisodeNumbersOfSeason(seasonId);
        } else {
            // set watched or skipped
            // do NOT mark watched episodes again to avoid Trakt adding a new watch
            return helper.getNotWatchedOrSkippedEpisodeNumbersOfSeason(seasonId);
        }
    }

    /**
     * Note: this should mirror the planned database changes in {@link #applyDatabaseChanges(Context)}.
     */
    @Override
    protected int getPlaysForNetworkJob(int plays) {
        switch (getFlagValue()) {
            case EpisodeFlags.SKIPPED:
                return plays;
            case EpisodeFlags.WATCHED:
                return plays + 1;
            case EpisodeFlags.UNWATCHED:
                return 0;
            default:
                throw new IllegalArgumentException("Flag value not supported");
        }
    }
}
