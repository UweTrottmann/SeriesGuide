package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider;
import com.battlelancer.seriesguide.provider.SgActivityHelper;
import com.battlelancer.seriesguide.provider.SgEpisode2Helper;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;
import com.battlelancer.seriesguide.util.TextTools;

public class EpisodeWatchedJob extends EpisodeBaseJob {

    public EpisodeWatchedJob(long episodeId, int episodeFlags) {
        super(episodeId, episodeFlags, JobAction.EPISODE_WATCHED_FLAG);
    }

    private long getLastWatchedEpisodeId(Context context) {
        if (!EpisodeTools.isUnwatched(getFlagValue())) {
            // watched or skipped episode
            return episodeId;
        } else {
            // changed episode to not watched
            long lastWatchedId = -1; // don't change last watched episode by default

            // if modified episode is identical to last watched one (e.g. was just watched),
            // find an appropriate last watched episode
            SgRoomDatabase database = SgRoomDatabase.getInstance(context);
            long lastWatchedEpisodeId = database.sgShow2Helper()
                    .getShowLastWatchedEpisodeId(getShowId());
            // identical to last watched episode?
            if (episodeId == lastWatchedEpisodeId) {
                if (getEpisode().getSeason() == 0) {
                    // keep last watched (= this episode) if we got a special
                    return -1;
                }
                lastWatchedId = 0; // re-set if we don't find one

                // get newest watched before this one
                long previousWatchedEpisodeId = database.sgEpisode2Helper()
                        .getPreviousWatchedEpisodeOfShow(getShowId(), getEpisode().getSeason(),
                                getEpisode().getEpisodenumber());
                if (previousWatchedEpisodeId > 0) {
                    lastWatchedId = previousWatchedEpisodeId;
                }
            }

            return lastWatchedId;
        }
    }

    @Override
    public boolean applyLocalChanges(Context context, boolean requiresNetworkJob) {
        if (!super.applyLocalChanges(context, requiresNetworkJob)) {
            return false;
        }

        // set a new last watched episode
        // set last watched time to now if marking as watched or skipped
        boolean unwatched = EpisodeTools.isUnwatched(getFlagValue());
        updateLastWatched(context, getLastWatchedEpisodeId(context), !unwatched);

        if (EpisodeTools.isWatched(getFlagValue())) {
            // create activity entry for watched episode
            SgActivityHelper.addActivity(context, episodeId, getShowId());
        } else if (unwatched) {
            // remove any previous activity entries for this episode
            // use case: user accidentally toggled watched flag
            SgActivityHelper.removeActivity(context, episodeId);
        }

        ListWidgetProvider.notifyDataChanged(context);

        return true;
    }

    @Override
    protected boolean applyDatabaseChanges(@NonNull Context context) {
        SgEpisode2Helper episodeHelper = SgRoomDatabase.getInstance(context).sgEpisode2Helper();
        int flagValue = getFlagValue();

        int rowsUpdated;
        switch (flagValue) {
            case EpisodeFlags.SKIPPED:
                rowsUpdated = episodeHelper.setSkipped(episodeId);
                break;
            case EpisodeFlags.WATCHED:
                rowsUpdated = episodeHelper.setWatchedAndAddPlay(episodeId);
                break;
            case EpisodeFlags.UNWATCHED:
                rowsUpdated = episodeHelper.setNotWatchedAndRemovePlays(episodeId);
                break;
            default:
                throw new IllegalArgumentException("Flag value not supported");
        }

        return rowsUpdated == 1;
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
        // format like '6x42 · Set watched'
        String number = TextTools.getEpisodeNumber(context, getEpisode().getSeason(),
                getEpisode().getEpisodenumber());
        return TextTools.dotSeparate(number, context.getString(actionResId));
    }
}
