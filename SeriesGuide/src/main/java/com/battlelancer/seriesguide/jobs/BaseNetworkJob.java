package com.battlelancer.seriesguide.jobs;

import android.app.PendingIntent;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.jobs.episodes.JobAction;
import com.battlelancer.seriesguide.sync.NetworkJobProcessor.JobResult;

public abstract class BaseNetworkJob implements NetworkJob {

    final JobAction action;
    final SgJobInfo jobInfo;

    public BaseNetworkJob(JobAction action, SgJobInfo jobInfo) {
        this.action = action;
        this.jobInfo = jobInfo;
    }

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
        jobResult.item = getItemTitle(context);
        jobResult.action = getActionDescription(context);
        jobResult.error = error;
        jobResult.contentIntent = getErrorIntent(context);
        return jobResult;
    }

    @Nullable
    protected abstract String getItemTitle(Context context);

    @Nullable
    protected abstract String getActionDescription(Context context);

    @NonNull
    protected abstract PendingIntent getErrorIntent(Context context);
}
