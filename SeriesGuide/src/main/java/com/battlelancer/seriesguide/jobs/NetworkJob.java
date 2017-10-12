package com.battlelancer.seriesguide.jobs;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.jobs.episodes.JobAction;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.sync.NetworkJobProcessor.JobResult;
import com.battlelancer.seriesguide.ui.OverviewActivity;
import com.battlelancer.seriesguide.ui.ShowsActivity;

public abstract class NetworkJob {

    public static final int SUCCESS = 0;
    /** Issue connecting or reading a response, should retry. */
    public static final int ERROR_CONNECTION = -1;
    public static final int ERROR_TRAKT_AUTH = -2;
    /** Issue with request, do not retry. */
    public static final int ERROR_TRAKT_CLIENT = -3;
    /** Issue with connection or server, do retry. */
    public static final int ERROR_TRAKT_SERVER = -4;
    /** Show, season or episode not found, do not retry, but notify. */
    public static final int ERROR_TRAKT_NOT_FOUND = -5;
    /** Issue with the request, do not retry. */
    public static final int ERROR_HEXAGON_CLIENT = -6;
    /** Issue with connection or server, should retry. */
    public static final int ERROR_HEXAGON_SERVER = -7;
    public static final int ERROR_HEXAGON_AUTH = -8;

    final JobAction action;
    final SgJobInfo jobInfo;

    public NetworkJob(JobAction action, SgJobInfo jobInfo) {
        this.action = action;
        this.jobInfo = jobInfo;
    }

    @NonNull
    public abstract JobResult execute(Context context);

    /**
     * @return JobResult.jobRemovable true if the job can be removed, false if it should be retried
     * later.
     */
    protected JobResult buildResult(Context context, Integer result) {
        String error;
        boolean removeJob;
        switch (result) {
            case NetworkJob.SUCCESS:
                return new JobResult(true, true);
            case NetworkJob.ERROR_CONNECTION:
            case NetworkJob.ERROR_HEXAGON_SERVER:
            case NetworkJob.ERROR_TRAKT_SERVER:
                return new JobResult(false, false);
            case NetworkJob.ERROR_HEXAGON_AUTH:
                // TODO ut better error message if auth is missing, or drop?
                error = context.getString(R.string.api_error_generic,
                        context.getString(R.string.hexagon));
                removeJob = false;
                break;
            case NetworkJob.ERROR_TRAKT_AUTH:
                error = context.getString(R.string.trakt_error_credentials);
                removeJob = false;
                break;
            case NetworkJob.ERROR_HEXAGON_CLIENT:
                error = context.getString(R.string.api_error_generic,
                        context.getString(R.string.hexagon));
                removeJob = true;
                break;
            case NetworkJob.ERROR_TRAKT_CLIENT:
                error = context.getString(R.string.api_error_generic,
                        context.getString(R.string.trakt));
                removeJob = true;
                break;
            case NetworkJob.ERROR_TRAKT_NOT_FOUND:
                // show not on trakt: notify, but complete successfully
                error = context.getString(R.string.trakt_notice_not_exists);
                removeJob = true;
                break;
            default:
                return new JobResult(true, true);
        }
        JobResult jobResult = new JobResult(false, removeJob);
        jobResult.item = getShowTitle(context);
        jobResult.action = getActionDescription(context);
        jobResult.error = error;
        jobResult.contentIntent = getErrorIntent(context);
        return jobResult;
    }

    @Nullable
    private String getShowTitle(Context context) {
        int showTvdbId = jobInfo.showTvdbId();
        Cursor query = context.getContentResolver()
                .query(SeriesGuideContract.Shows.buildShowUri(showTvdbId),
                        SeriesGuideContract.Shows.PROJECTION_TITLE, null,
                        null, null);
        if (query == null) {
            return null;
        }
        if (!query.moveToFirst()) {
            query.close();
            return null;
        }
        String title = query.getString(
                query.getColumnIndexOrThrow(SeriesGuideContract.Shows.TITLE));
        query.close();
        return title;
    }

    @Nullable
    private String getActionDescription(Context context) {
        int flagValue = jobInfo.flagValue();
        if (action == JobAction.EPISODE_COLLECTION) {
            boolean isRemoveAction = flagValue == 0;
            return context.getString(isRemoveAction
                    ? R.string.action_collection_remove : R.string.action_collection_add);
        } else if (action == JobAction.EPISODE_WATCHED_FLAG) {
            if (flagValue == EpisodeFlags.UNWATCHED) {
                return context.getString(R.string.action_unwatched);
            } else if (flagValue == EpisodeFlags.SKIPPED) {
                return context.getString(R.string.action_skip);
            } else if (flagValue == EpisodeFlags.WATCHED) {
                return context.getString(R.string.action_watched);
            }
        }
        return null;
    }

    @NonNull
    private PendingIntent getErrorIntent(Context context) {
        // tapping the notification should open the affected show
        return TaskStackBuilder.create(context)
                .addNextIntent(new Intent(context, ShowsActivity.class))
                .addNextIntent(OverviewActivity.intentShow(context, jobInfo.showTvdbId()))
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
