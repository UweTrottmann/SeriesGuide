package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import android.net.Uri;
import android.text.format.DateUtils;
import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider;
import com.battlelancer.seriesguide.provider.EpisodeHelper;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;

public class ShowWatchedJob extends ShowBaseJob {

    private final long currentTimePlusOneHour;

    public ShowWatchedJob(int showTvdbId, int flagValue, long currentTime) {
        super(showTvdbId, flagValue, JobAction.EPISODE_WATCHED_FLAG);
        this.currentTimePlusOneHour = currentTime + DateUtils.HOUR_IN_MILLIS;
    }

    @Override
    public String getDatabaseSelection() {
        if (EpisodeTools.isUnwatched(getFlagValue())) {
            // set unwatched
            // include watched or skipped episodes
            return Episodes.SELECTION_WATCHED_OR_SKIPPED
                    + " AND " + Episodes.SELECTION_NO_SPECIALS;
        } else {
            // set watched or skipped
            // do NOT mark watched episodes again to avoid trakt adding a new watch
            // only mark episodes that have been released until within the hour
            return Episodes.FIRSTAIREDMS + "<=" + currentTimePlusOneHour
                    + " AND " + Episodes.SELECTION_HAS_RELEASE_DATE
                    + " AND " + Episodes.SELECTION_UNWATCHED_OR_SKIPPED
                    + " AND " + Episodes.SELECTION_NO_SPECIALS;
        }
    }

    @Override
    protected String getDatabaseColumnToUpdate() {
        return Episodes.WATCHED;
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
    protected boolean applyDatabaseChanges(@NonNull Context context, @NonNull Uri uri) {
        EpisodeHelper episodeHelper = SgRoomDatabase.getInstance(context).episodeHelper();

        int rowsUpdated;
        switch (getFlagValue()) {
            case EpisodeFlags.UNWATCHED:
                rowsUpdated = episodeHelper.setShowNotWatchedAndRemovePlays(getShowTvdbId());
                break;
            case EpisodeFlags.WATCHED:
                rowsUpdated = episodeHelper
                        .setShowWatchedAndAddPlay(getShowTvdbId(), currentTimePlusOneHour);
                break;
            default:
                // Note: Skip not supported for whole show.
                throw new IllegalArgumentException("Flag value not supported");
        }
        return rowsUpdated >= 0; // -1 means error.
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
