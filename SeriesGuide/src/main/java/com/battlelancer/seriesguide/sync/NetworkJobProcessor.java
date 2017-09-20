package com.battlelancer.seriesguide.sync;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.jobs.HexagonEpisodeJob;
import com.battlelancer.seriesguide.jobs.NetworkJob;
import com.battlelancer.seriesguide.jobs.SgJobInfo;
import com.battlelancer.seriesguide.jobs.TraktEpisodeJob;
import com.battlelancer.seriesguide.jobs.episodes.JobAction;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Jobs;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.OverviewActivity;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.util.DBUtils;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

public class NetworkJobProcessor {

    private final Context context;
    private final boolean shouldSendToHexagon;
    private final boolean shouldSendToTrakt;

    public NetworkJobProcessor(Context context) {
        shouldSendToHexagon = HexagonSettings.isEnabled(context);
        shouldSendToTrakt = TraktCredentials.get(context).hasCredentials();
        this.context = context;
    }

    public void process() {
        // query for jobs
        Cursor query = context.getContentResolver()
                .query(Jobs.CONTENT_URI, Jobs.PROJECTION, null, null, Jobs.SORT_OLDEST);
        if (query == null) {
            return; // query failed
        }

        // process jobs, starting with oldest
        List<Long> jobsToRemove = new ArrayList<>();
        while (query.moveToNext()) {
            long jobId = query.getLong(0);
            int typeId = query.getInt(1);
            JobAction action = JobAction.fromId(typeId);

            if (action != JobAction.UNKNOWN) {
                long createdAt = query.getLong(2);
                byte[] jobInfoArr = query.getBlob(3);
                ByteBuffer jobInfoBuffered = ByteBuffer.wrap(jobInfoArr);
                SgJobInfo jobInfo = SgJobInfo.getRootAsSgJobInfo(jobInfoBuffered);

                if (!doNetworkJob(jobId, action, createdAt, jobInfo)) {
                    break; // abort to avoid ordering issues
                }
            }

            jobsToRemove.add(jobId);
        }
        query.close();

        // remove completed jobs
        if (!jobsToRemove.isEmpty()) {
            removeJobs(jobsToRemove);
        }
    }

    /**
     * @return true if the job can be removed, false if it should be retried later.
     */
    private boolean doNetworkJob(long jobId, JobAction action, long createdAt, SgJobInfo jobInfo) {
        // upload to hexagon
        if (shouldSendToHexagon) {
            if (!AndroidUtils.isNetworkConnected(context)) {
                return false;
            }
            HexagonTools hexagonTools = SgApp.getServicesComponent(context).hexagonTools();

            HexagonEpisodeJob hexagonJob = new HexagonEpisodeJob(hexagonTools, action, jobInfo);
            int result = hexagonJob.upload(context);
            if (result < 0) {
                return handleResult(jobId, jobInfo, result);
            }
        }

        // upload to trakt
        if (shouldSendToTrakt) {
            // Do not send if show has no trakt id (was not on trakt last time we checked).
            TraktEpisodeJob traktJob = new TraktEpisodeJob(action, jobInfo, createdAt);
            boolean canSendToTrakt = traktJob.checkCanUpload(context);
            if (canSendToTrakt) {
                if (!AndroidUtils.isNetworkConnected(context)) {
                    return false;
                }

                int result = traktJob.upload(context);
                if (result < 0) {
                    return handleResult(jobId, jobInfo, result);
                }
            } else {
                // show not on trakt: notify, but complete successfully
                showCanNotSendToTraktNotification(jobId, jobInfo);
                return true;
            }
        }

        return true;
    }

    /**
     * @return true if the job can be removed, false if it should be retried later.
     */
    private boolean handleResult(long jobId, @NonNull SgJobInfo jobInfo, Integer result) {
        String error;
        switch (result) {
            case NetworkJob.ERROR_TRAKT_AUTH:
                error = context.getString(R.string.trakt_error_credentials);
                break;
            case NetworkJob.ERROR_TRAKT_API:
                error = context.getString(R.string.api_error_generic,
                        context.getString(R.string.trakt));
                break;
            case NetworkJob.ERROR_HEXAGON_API:
                error = context.getString(R.string.api_error_generic,
                        context.getString(R.string.hexagon));
                break;
            default:
                return true; // unknown error, remove job
        }
        showNotification(jobId, jobInfo, error);
        return true;
    }

    private void showCanNotSendToTraktNotification(long jobId, @NonNull SgJobInfo jobInfo) {
        showNotification(jobId, jobInfo, context.getString(R.string.trakt_notice_not_exists));
    }

    private void showNotification(long jobId, @NonNull SgJobInfo jobInfo, @NonNull String error) {
        // get affected show title
        int showTvdbId = jobInfo.showTvdbId();
        Cursor query = context.getContentResolver()
                .query(Shows.buildShowUri(showTvdbId),
                        Shows.PROJECTION_TITLE, null,
                        null, null);
        if (query == null) {
            return;
        }
        if (!query.moveToFirst()) {
            query.close();
            return;
        }
        String title = query.getString(query.getColumnIndexOrThrow(Shows.TITLE));
        query.close();

        // tapping the notification should open the affected show
        PendingIntent contentIntent = TaskStackBuilder.create(context)
                .addNextIntent(new Intent(context, ShowsActivity.class))
                .addNextIntent(OverviewActivity.intentShow(context, showTvdbId))
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context);
        nb.setSmallIcon(R.drawable.ic_notification);
        nb.setContentTitle(title);
        nb.setContentText(error);
        nb.setContentIntent(contentIntent);
        nb.setAutoCancel(true);
        nb.setColor(ContextCompat.getColor(context, R.color.accent_primary));
        nb.setPriority(NotificationCompat.PRIORITY_HIGH);
        nb.setCategory(NotificationCompat.CATEGORY_ERROR);

        NotificationManager nm = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            // notification for each job
            nm.notify(String.valueOf(jobId), SgApp.NOTIFICATION_JOB_ID, nb.build());
        }
    }

    private void removeJobs(List<Long> jobsToRemove) {
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        for (Long jobId : jobsToRemove) {
            batch.add(ContentProviderOperation.newDelete(Jobs.buildJobUri(jobId)).build());
        }
        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "process: failed to delete completed jobs");
        }
    }

    /**
     * If neither trakt or Cloud are connected, clears all remaining jobs.
     */
    public void removeObsoleteJobs() {
        if (shouldSendToHexagon || shouldSendToTrakt) {
            return; // still signed in to either service, do not clear jobs
        }
        context.getContentResolver().delete(Jobs.CONTENT_URI, null, null);
    }
}
