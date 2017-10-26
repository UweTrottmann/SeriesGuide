package com.battlelancer.seriesguide.jobs.movies;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.jobs.FlagJob;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.ui.BaseNavDrawerActivity;
import org.greenrobot.eventbus.EventBus;

public class MovieJobAsyncTask extends AsyncTask<Void, Void, Void> {

    @SuppressLint("StaticFieldLeak") // using application context
    private final Context context;
    private final FlagJob job;

    public MovieJobAsyncTask(Context context, FlagJob job) {
        this.context = context.getApplicationContext();
        this.job = job;
    }

    @Override
    protected Void doInBackground(Void... params) {
        boolean shouldSendToHexagon = job.supportsHexagon()
                && HexagonSettings.isEnabled(context);
        boolean shouldSendToTrakt = job.supportsTrakt()
                && TraktCredentials.get(context).hasCredentials();
        boolean requiresNetworkJob = shouldSendToHexagon || shouldSendToTrakt;

        // set send flags to false to avoid showing 'Sending to...' message
        EventBus.getDefault().postSticky(new BaseNavDrawerActivity.ServiceActiveEvent(
                false, false));

        // update local database and possibly prepare network job
        boolean isSuccessful = job.applyLocalChanges(context, requiresNetworkJob);

        EventBus.getDefault().removeStickyEvent(BaseNavDrawerActivity.ServiceActiveEvent.class);

        EventBus.getDefault().post(
                new BaseNavDrawerActivity.ServiceCompletedEvent(isSuccessful
                        ? job.getConfirmationText(context)
                        : context.getString(R.string.database_error),
                        isSuccessful, job));

        if (requiresNetworkJob) {
            SgSyncAdapter.requestSyncJobsImmediate(context);
        }

        return null;
    }
}
