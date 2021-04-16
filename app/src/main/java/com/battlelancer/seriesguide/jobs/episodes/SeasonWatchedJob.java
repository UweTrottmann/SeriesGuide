package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import android.text.format.DateUtils;
import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider;
import com.battlelancer.seriesguide.provider.SgEpisode2Helper;
import com.battlelancer.seriesguide.provider.SgEpisode2Numbers;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;
import com.battlelancer.seriesguide.util.TextTools;
import java.util.List;

public class SeasonWatchedJob extends SeasonBaseJob {

    private final long currentTimePlusOneHour;

    public SeasonWatchedJob(long seasonId, int episodeFlags, long currentTime) {
        super(seasonId, episodeFlags, JobAction.EPISODE_WATCHED_FLAG);
        this.currentTimePlusOneHour = currentTime + DateUtils.HOUR_IN_MILLIS;
    }

    private long getLastWatchedEpisodeId(Context context) {
        if (EpisodeTools.isUnwatched(getFlagValue())) {
            // unwatched season
            // just reset
            return 0;
        } else {
            // watched or skipped season

            // get the highest flagged episode of the season
            long highestWatchedId = SgRoomDatabase.getInstance(context)
                    .sgEpisode2Helper()
                    .getHighestWatchedEpisodeOfSeason(seasonId, currentTimePlusOneHour);
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
                rowsUpdated = helper.setSeasonSkipped(seasonId, currentTimePlusOneHour);
                break;
            case EpisodeFlags.WATCHED:
                rowsUpdated = helper
                        .setSeasonWatchedAndAddPlay(seasonId, currentTimePlusOneHour);
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
            // only mark episodes that have been released until within the hour
            return helper
                    .getNotWatchedOrSkippedEpisodeNumbersOfSeason(seasonId, currentTimePlusOneHour);
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

    @NonNull
    @Override
    public String getConfirmationText(Context context) {
        int actionResId;
        int flagValue = getFlagValue();
        if (EpisodeTools.isSkipped(flagValue)) {
            actionResId = R.string.action_skip;
        } else if (EpisodeTools.isWatched(flagValue)) {
            actionResId = R.string.action_watched;
        } else {
            actionResId = R.string.action_unwatched;
        }
        // format like '6x · Set watched'
        String number = TextTools.getEpisodeNumber(context, getSeason().getNumber(), -1);
        return TextTools.dotSeparate(number, context.getString(actionResId));
    }
}
