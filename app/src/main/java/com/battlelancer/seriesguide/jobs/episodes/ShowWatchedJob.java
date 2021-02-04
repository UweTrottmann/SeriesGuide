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
import java.util.List;

public class ShowWatchedJob extends ShowBaseJob {

    private final long currentTimePlusOneHour;

    public ShowWatchedJob(long showId, int flagValue, long currentTime) {
        super(showId, flagValue, JobAction.EPISODE_WATCHED_FLAG);
        this.currentTimePlusOneHour = currentTime + DateUtils.HOUR_IN_MILLIS;
    }

    @Override
    public boolean applyLocalChanges(Context context, boolean requiresNetworkJob) {
        if (!super.applyLocalChanges(context, requiresNetworkJob)) {
            return false;
        }

        int lastWatchedEpisodeTvdbId = EpisodeTools.isUnwatched(getFlagValue())
                ? 0 /* just reset */
                : -1 /* we don't care */;

        // set a new last watched episode
        // set last watched time to now if marking as watched or skipped
        updateLastWatched(context, lastWatchedEpisodeTvdbId,
                !EpisodeTools.isUnwatched(getFlagValue()));

        ListWidgetProvider.notifyDataChanged(context);

        return true;
    }

    @Override
    protected boolean applyDatabaseChanges(@NonNull Context context) {
        SgEpisode2Helper helper = SgRoomDatabase.getInstance(context).sgEpisode2Helper();

        int rowsUpdated;
        switch (getFlagValue()) {
            case EpisodeFlags.UNWATCHED:
                rowsUpdated = helper.setShowNotWatchedAndRemovePlays(getShowId());
                break;
            case EpisodeFlags.WATCHED:
                rowsUpdated = helper.setShowWatchedAndAddPlay(getShowId(), currentTimePlusOneHour);
                break;
            default:
                // Note: Skip not supported for whole show.
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
            return helper.getWatchedOrSkippedEpisodeNumbersOfShow(getShowId());
        } else {
            // set watched or skipped
            // do NOT mark watched episodes again to avoid trakt adding a new watch
            // only mark episodes that have been released until within the hour
            return helper.getNotWatchedOrSkippedEpisodeNumbersOfShow(getShowId(),
                    currentTimePlusOneHour);
        }
    }

    /**
     * Note: this should mirror the planned database changes in {@link #applyDatabaseChanges(Context)}.
     */
    @Override
    protected int getPlaysForNetworkJob(int plays) {
        switch (getFlagValue()) {
            case EpisodeFlags.WATCHED:
                return plays + 1;
            case EpisodeFlags.UNWATCHED:
                return 0;
            default:
                // Note: Skip not supported for whole show.
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
        return context.getString(actionResId);
    }
}
