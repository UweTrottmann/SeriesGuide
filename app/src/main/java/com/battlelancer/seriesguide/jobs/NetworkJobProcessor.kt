package com.battlelancer.seriesguide.jobs;

import android.app.NotificationManager;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.text.format.DateUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.jobs.episodes.JobAction;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Jobs;
import com.battlelancer.seriesguide.settings.NotificationSettings;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
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
                Timber.d("Running job %d %s", jobId, action);

                long createdAt = query.getLong(2);
                byte[] jobInfoArr = query.getBlob(3);
                ByteBuffer jobInfoBuffered = ByteBuffer.wrap(jobInfoArr);
                SgJobInfo jobInfo = SgJobInfo.getRootAsSgJobInfo(jobInfoBuffered);

                if (!doNetworkJob(jobId, action, createdAt, jobInfo)) {
                    Timber.e("Job %d failed, will retry.", jobId);
                    break; // abort to avoid ordering issues
                }
                Timber.d("Job %d completed, will remove.", jobId);
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

            NetworkJob hexagonJob = getHexagonJobForAction(hexagonTools, action, jobInfo);
            if (hexagonJob != null) {
                JobResult result = hexagonJob.execute(context);
                if (!result.getSuccessful()) {
                    showNotification(jobId, createdAt, result);
                    return result.getJobRemovable();
                }
            }
        }

        // upload to trakt
        if (shouldSendToTrakt) {
            if (!AndroidUtils.isNetworkConnected(context)) {
                return false;
            }

            NetworkJob traktJob = getTraktJobForAction(action, jobInfo, createdAt);
            if (traktJob != null) {
                JobResult result = traktJob.execute(context);
                // may need to show notification if successful (for not found error)
                showNotification(jobId, createdAt, result);
                if (!result.getSuccessful()) {
                    return result.getJobRemovable();
                }
            }
        }

        return true;
    }

    @Nullable
    private NetworkJob getHexagonJobForAction(HexagonTools hexagonTools, JobAction action,
            SgJobInfo jobInfo) {
        switch (action) {
            case EPISODE_COLLECTION:
            case EPISODE_WATCHED_FLAG:
                return new HexagonEpisodeJob(hexagonTools, action, jobInfo);
            case MOVIE_COLLECTION_ADD:
            case MOVIE_COLLECTION_REMOVE:
            case MOVIE_WATCHLIST_ADD:
            case MOVIE_WATCHLIST_REMOVE:
            case MOVIE_WATCHED_SET:
            case MOVIE_WATCHED_REMOVE:
                return new HexagonMovieJob(hexagonTools, action, jobInfo);
            default:
                return null; // action not supported by hexagon
        }
    }

    @Nullable
    private NetworkJob getTraktJobForAction(JobAction action, SgJobInfo jobInfo, long createdAt) {
        switch (action) {
            case EPISODE_COLLECTION:
            case EPISODE_WATCHED_FLAG:
                return new TraktEpisodeJob(action, jobInfo, createdAt);
            case MOVIE_COLLECTION_ADD:
            case MOVIE_COLLECTION_REMOVE:
            case MOVIE_WATCHLIST_ADD:
            case MOVIE_WATCHLIST_REMOVE:
            case MOVIE_WATCHED_SET:
            case MOVIE_WATCHED_REMOVE:
                return new TraktMovieJob(action, jobInfo, createdAt);
            default:
                return null; // action not supported by trakt
        }
    }

    private void showNotification(long jobId, long jobCreatedAt, @NonNull JobResult result) {
        if (result.getAction() == null || result.getError() == null || result.getItem() == null) {
            return; // missing required values
        }

        NotificationCompat.Builder nb =
                new NotificationCompat.Builder(context, SgApp.NOTIFICATION_CHANNEL_ERRORS);
        NotificationSettings.setDefaultsForChannelErrors(context, nb);

        nb.setSmallIcon(R.drawable.ic_notification);
        // like: 'Failed: Remove from collection · BoJack Horseman'
        nb.setContentTitle(
                context.getString(R.string.api_failed,
                        result.getAction() + " · " + result.getItem()));
        nb.setContentText(result.getError());
        nb.setStyle(new NotificationCompat.BigTextStyle().bigText(
                getErrorDetails(result.getItem(), result.getError(), result.getAction(),
                        jobCreatedAt)));
        nb.setContentIntent(result.getContentIntent());
        nb.setAutoCancel(true);

        NotificationManager nm = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            // notification for each failed job
            nm.notify(String.valueOf(jobId), SgApp.NOTIFICATION_JOB_ID, nb.build());
        }
    }

    @NonNull
    private String getErrorDetails(@NonNull String item, @NonNull String error,
            @NonNull String action, long jobCreatedAt) {
        StringBuilder builder = new StringBuilder();
        // build message like:
        // 'Could not talk to server.
        // BoJack Horseman · Set watched · 5 sec ago'
        builder.append(error).append("\n").append(item).append(" · ").append(action);

        // append time if job is executed a while after it was created
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - jobCreatedAt > 3 * DateUtils.SECOND_IN_MILLIS) {
            builder.append(" · ");
            builder.append(DateUtils.getRelativeTimeSpanString(jobCreatedAt,
                    currentTimeMillis, DateUtils.SECOND_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_ALL));
        }

        return builder.toString();
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
    public void removeObsoleteJobs(boolean ignoreHexagonState) {
        if ((!ignoreHexagonState && shouldSendToHexagon) || shouldSendToTrakt) {
            return; // still signed in to either service, do not clear jobs
        }
        context.getContentResolver().delete(Jobs.CONTENT_URI, null, null);
    }
}
