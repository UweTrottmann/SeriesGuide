package com.battlelancer.seriesguide.jobs.episodes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.ui.BaseNavDrawerActivity;
import com.battlelancer.seriesguide.util.EpisodeTools;
import org.greenrobot.eventbus.EventBus;

public class EpisodeJobAsyncTask extends AsyncTask<Void, Void, Void> {

    @SuppressLint("StaticFieldLeak") // using application context
    private final Context context;
    private final EpisodeFlagJob job;

    public EpisodeJobAsyncTask(Context context, EpisodeFlagJob job) {
        this.context = context.getApplicationContext();
        this.job = job;
    }

    @Override
    protected Void doInBackground(Void... params) {
        boolean shouldSendToHexagon = HexagonSettings.isEnabled(context);
        /* Do net send skipped episodes, this is not supported by trakt.
        However, if the skipped flag is removed
        this will be handled identical to flagging as unwatched. */
        boolean shouldSendToTrakt = TraktCredentials.get(context).hasCredentials()
                && !EpisodeTools.isSkipped(job.getFlagValue());
        boolean requiresNetworkJob = shouldSendToHexagon || shouldSendToTrakt;

        EventBus.getDefault().postSticky(new BaseNavDrawerActivity.ServiceActiveEvent(
                shouldSendToHexagon, shouldSendToTrakt));

        // update local database and possibly prepare network job
        boolean isSuccessful = job.applyLocalChanges(context, requiresNetworkJob);

        EventBus.getDefault().removeStickyEvent(BaseNavDrawerActivity.ServiceActiveEvent.class);

        EventBus.getDefault().post(
                new BaseNavDrawerActivity.ServiceCompletedEvent(job.getConfirmationText(context),
                        isSuccessful));
        EventBus.getDefault().post(new CompletedEvent(job, isSuccessful));

        if (requiresNetworkJob) {
            SgSyncAdapter.requestSyncJobsImmediate(context);
        }

        return null;
    }

    /**
     * Posted once the episode task has completed. It may not have been successful.
     */
    public static class CompletedEvent {
        public final EpisodeFlagJob job;
        public final boolean isSuccessful;

        public CompletedEvent(EpisodeFlagJob job, boolean isSuccessful) {
            this.job = job;
            this.isSuccessful = isSuccessful;
        }
    }
}
