package com.battlelancer.seriesguide.service;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.Nullable;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.jobs.episodes.EpisodeJobAsyncTask;
import com.battlelancer.seriesguide.jobs.episodes.EpisodeWatchedJob;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.BaseNavDrawerActivity;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.LatestEpisodeUpdateTask;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.jobs.episodes.EpisodeFlagJob;
import com.uwetrottmann.androidutils.AndroidUtils;
import org.greenrobot.eventbus.EventBus;

public class EpisodeTask {

    private static final int SUCCESS = 0;
    private static final int ERROR_NETWORK = -1;
    private static final int ERROR_TRAKT_AUTH = -2;
    private static final int ERROR_TRAKT_API = -3;
    private static final int ERROR_HEXAGON_API = -4;

    private final Context context;
    private final EpisodeFlagJob job;

    private boolean shouldSendToTrakt;
    private boolean canSendToTrakt;

    @Nullable
    public static EpisodeTask typeChangeFlag(Context context, int episodeTvdbId, int flag) {
        EpisodeDetails details = getEpisodeDetails(context, episodeTvdbId);
        if (details == null) {
            return null;
        }
        EpisodeWatchedJob taskType
                = new EpisodeWatchedJob(context, details.showTvdbId,
                episodeTvdbId, details.season, details.episode, flag);
        return new EpisodeTask(context, taskType);
    }

    private EpisodeTask(Context context, EpisodeFlagJob job) {
        this.context = context;
        this.job = job;
    }

    public void execute() {
        boolean shouldSendToHexagon = HexagonSettings.isEnabled(context);
        shouldSendToTrakt = TraktCredentials.get(context).hasCredentials()
                && !EpisodeTools.isSkipped(job.getFlagValue());

        EventBus.getDefault().postSticky(new BaseNavDrawerActivity.ServiceActiveEvent(
                shouldSendToHexagon, shouldSendToTrakt));

        // upload to hexagon
        if (shouldSendToHexagon) {
            if (!AndroidUtils.isNetworkConnected(context)) {
                handleWorkResult(ERROR_NETWORK);
                return;
            }

            HexagonTools hexagonTools = SgApp.getServicesComponent(context).hexagonTools();
            int result = EpisodeJobAsyncTask.uploadToHexagon(context, hexagonTools,
                    job.getShowTvdbId(), job.getEpisodesForHexagon());
            if (result < 0) {
                handleWorkResult(result);
                return;
            }
        }

        // upload to trakt
        /*
          Do net send skipped episodes, this is not supported by trakt.
          However, if the skipped flag is removed this will be handled identical
          to flagging as unwatched.
         */
        if (shouldSendToTrakt) {
            // Do not send if show has no trakt id (was not on trakt last time we checked).
            Integer traktId = ShowTools.getShowTraktId(context, job.getShowTvdbId());
            canSendToTrakt = traktId != null;
            if (canSendToTrakt) {
                if (!AndroidUtils.isNetworkConnected(context)) {
                    handleWorkResult(ERROR_NETWORK);
                    return;
                }

                int result = EpisodeJobAsyncTask.uploadToTrakt(context, job, traktId);
                if (result < 0) {
                    handleWorkResult(result);
                    return;
                }
            }
        }

        // update local database (if uploading went smoothly or not uploading at all)
        job.applyLocalChanges();

        handleWorkResult(SUCCESS);
    }

    private void handleWorkResult(int result) {
        EventBus.getDefault().removeStickyEvent(BaseNavDrawerActivity.ServiceActiveEvent.class);

        // handle errors
        String error = null;
        switch (result) {
            case ERROR_NETWORK:
                error = context.getString(R.string.offline);
                break;
            case ERROR_TRAKT_AUTH:
                error = context.getString(R.string.trakt_error_credentials);
                break;
            case ERROR_TRAKT_API:
                error = context.getString(R.string.api_error_generic,
                        context.getString(R.string.trakt));
                break;
            case ERROR_HEXAGON_API:
                error = context.getString(R.string.api_error_generic,
                        context.getString(R.string.hexagon));
                break;
        }
        boolean isSuccessful = error == null;

        if (isSuccessful) {
            // update latest episode for the changed show
            LatestEpisodeUpdateTask.updateLatestEpisodeFor(context, job.getShowTvdbId());
        }

        // post completed status
        String confirmationText;
        boolean displaySuccess;
        if (isSuccessful && shouldSendToTrakt && !canSendToTrakt) {
            // tell the user this change can not be sent to trakt for now
            confirmationText = context.getString(R.string.trakt_notice_not_exists);
            displaySuccess = false;
        } else {
            confirmationText = isSuccessful ? job.getConfirmationText() : error;
            displaySuccess = isSuccessful;
        }
        EventBus.getDefault()
                .post(new BaseNavDrawerActivity.ServiceCompletedEvent(confirmationText,
                        displaySuccess));
        EventBus.getDefault()
                .post(new EpisodeJobAsyncTask.CompletedEvent(job, isSuccessful));
    }

    @Nullable
    private static EpisodeDetails getEpisodeDetails(Context context, int episodeTvdbvId) {
        Cursor query = context.getContentResolver()
                .query(SeriesGuideContract.Episodes.buildEpisodeWithShowUri(episodeTvdbvId),
                        new String[] {
                                SeriesGuideContract.Shows.REF_SHOW_ID,
                                SeriesGuideContract.Episodes.SEASON,
                                SeriesGuideContract.Episodes.NUMBER }, null, null, null);
        if (query == null) {
            return null;
        }

        EpisodeDetails episodeDetails = null;
        if (query.moveToFirst()) {
            episodeDetails = new EpisodeDetails(
                    query.getInt(0),
                    query.getInt(1),
                    query.getInt(2)
            );
        }

        query.close();

        return episodeDetails;
    }

    private static class EpisodeDetails {
        public int showTvdbId;
        public int season;
        public int episode;

        public EpisodeDetails(int showTvdbId, int season, int episode) {
            this.showTvdbId = showTvdbId;
            this.season = season;
            this.episode = episode;
        }
    }
}
