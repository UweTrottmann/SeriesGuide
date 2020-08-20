package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider;
import com.battlelancer.seriesguide.provider.EpisodeHelper;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;
import com.battlelancer.seriesguide.util.ActivityTools;
import com.battlelancer.seriesguide.util.TextTools;

public class EpisodeWatchedJob extends EpisodeBaseJob {

    public EpisodeWatchedJob(int showTvdbId, int episodeTvdbId, int season, int episode,
            int episodeFlags) {
        super(showTvdbId, episodeTvdbId, season, episode, episodeFlags,
                JobAction.EPISODE_WATCHED_FLAG);
    }

    @Override
    protected String getDatabaseColumnToUpdate() {
        return SeriesGuideContract.Episodes.WATCHED;
    }

    private int getLastWatchedEpisodeTvdbId(Context context) {
        if (!EpisodeTools.isUnwatched(getFlagValue())) {
            return episodeTvdbId; // watched or skipped episode
        } else {
            // unwatched episode
            int lastWatchedId = -1; // don't change last watched episode by default

            // if modified episode is identical to last watched one (e.g. was just watched),
            // find an appropriate last watched episode
            final Cursor show = context.getContentResolver().query(
                    SeriesGuideContract.Shows.buildShowUri(String.valueOf(getShowTvdbId())),
                    new String[]{
                            SeriesGuideContract.Shows._ID,
                            SeriesGuideContract.Shows.LASTWATCHEDID
                    }, null, null, null
            );
            if (show != null) {
                // identical to last watched episode?
                if (show.moveToFirst() && show.getInt(1) == episodeTvdbId) {
                    if (season == 0) {
                        // keep last watched (= this episode) if we got a special
                        show.close();
                        return -1;
                    }
                    lastWatchedId = 0; // re-set if we don't find one

                    // get latest watched before this one
                    String season = String.valueOf(this.season);
                    final Cursor latestWatchedEpisode = context.getContentResolver()
                            .query(SeriesGuideContract.Episodes.buildEpisodesOfShowUri(String
                                            .valueOf(getShowTvdbId())),
                                    BaseEpisodesJob.PROJECTION_EPISODE,
                                    SeriesGuideContract.Episodes.SELECTION_PREVIOUS_WATCHED,
                                    new String[]{
                                            season, season, String.valueOf(episode)
                                    }, SeriesGuideContract.Episodes.SORT_PREVIOUS_WATCHED
                            );
                    if (latestWatchedEpisode != null) {
                        if (latestWatchedEpisode.moveToFirst()) {
                            lastWatchedId = latestWatchedEpisode.getInt(0);
                        }

                        latestWatchedEpisode.close();
                    }
                }

                show.close();
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
        updateLastWatched(context, getLastWatchedEpisodeTvdbId(context), !unwatched);

        if (EpisodeTools.isWatched(getFlagValue())) {
            // create activity entry for watched episode
            ActivityTools.addActivity(context, episodeTvdbId, getShowTvdbId());
        } else if (unwatched) {
            // remove any previous activity entries for this episode
            // use case: user accidentally toggled watched flag
            ActivityTools.removeActivity(context, episodeTvdbId);
        }

        ListWidgetProvider.notifyDataChanged(context);

        return true;
    }

    @Override
    protected boolean applyDatabaseChanges(Context context, Uri uri) {
        EpisodeHelper episodeHelper = SgRoomDatabase.getInstance(context).episodeHelper();
        int flagValue = getFlagValue();

        int rowsUpdated;
        switch (flagValue) {
            case EpisodeFlags.SKIPPED:
                rowsUpdated = episodeHelper.setSkipped(episodeTvdbId);
                break;
            case EpisodeFlags.WATCHED:
                rowsUpdated = episodeHelper.setWatchedAndAddPlay(episodeTvdbId);
                break;
            case EpisodeFlags.UNWATCHED:
                rowsUpdated = episodeHelper.setNotWatchedAndRemovePlays(episodeTvdbId);
                break;
            default:
                throw new IllegalArgumentException("Flag value not supported");
        }

        return rowsUpdated == 1;
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
        String number = TextTools.getEpisodeNumber(context, season, episode);
        return TextTools.dotSeparate(number, context.getString(actionResId));
    }
}
