package com.battlelancer.seriesguide.sync;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
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
import com.battlelancer.seriesguide.settings.TraktCredentials;
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
            int typeId = query.getInt(1);
            JobAction action = JobAction.fromId(typeId);

            if (action != JobAction.UNKNOWN) {
                long createdAt = query.getLong(2);
                byte[] jobInfoArr = query.getBlob(3);
                ByteBuffer jobInfoBuffered = ByteBuffer.wrap(jobInfoArr);
                SgJobInfo jobInfo = SgJobInfo.getRootAsSgJobInfo(jobInfoBuffered);

                if (!doNetworkJob(action, jobInfo)) {
                    break; // abort to avoid ordering issues
                }
            }

            long jobId = query.getLong(0);
            jobsToRemove.add(jobId);
        }
        query.close();

        // remove completed jobs
        if (!jobsToRemove.isEmpty()) {
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
    }

    private boolean doNetworkJob(JobAction action, SgJobInfo jobInfo) {
        // upload to hexagon
        if (shouldSendToHexagon) {
            if (!AndroidUtils.isNetworkConnected(context)) {
                handleResult(NetworkJob.ERROR_NETWORK);
                return false;
            }
            HexagonTools hexagonTools = SgApp.getServicesComponent(context).hexagonTools();

            HexagonEpisodeJob hexagonJob = new HexagonEpisodeJob(hexagonTools, action, jobInfo);
            int result = hexagonJob.upload(context);
            if (result < 0) {
                handleResult(result);
                return false;
            }
        }

        // upload to trakt
        if (shouldSendToTrakt) {
            // Do not send if show has no trakt id (was not on trakt last time we checked).
            TraktEpisodeJob traktJob = new TraktEpisodeJob(action, jobInfo);
            boolean canSendToTrakt = traktJob.checkCanUpload(context);
            if (canSendToTrakt) {
                if (!AndroidUtils.isNetworkConnected(context)) {
                    handleResult(NetworkJob.ERROR_NETWORK);
                    return false;
                }

                int result = traktJob.upload(context);
                if (result < 0) {
                    handleResult(result);
                    return false;
                }
            } else {
                handleResult(NetworkJob.SUCCESS, false);
                return true;
            }
        }

        handleResult(NetworkJob.SUCCESS);
        return true;
    }

    private void handleResult(Integer result, boolean canSendToTrakt) {
        // handle errors
        String error = null;
        switch (result) {
            case NetworkJob.ERROR_NETWORK:
                error = context.getString(R.string.offline);
                break;
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
        }
        boolean isSuccessful = error == null;

        // post completed status
        String confirmationText;
        boolean displaySuccess;
        if (isSuccessful && shouldSendToTrakt && !canSendToTrakt) {
            // tell the user this change can not be sent to trakt for now
            confirmationText = context.getString(R.string.trakt_notice_not_exists);
            displaySuccess = false;
        } else {
//            confirmationText = isSuccessful ? job.getConfirmationText(context) : error;
            displaySuccess = isSuccessful;
        }

        // TODO show notification on error, offer to retry
    }

    private void handleResult(Integer result) {
        handleResult(result, true);
    }

}
