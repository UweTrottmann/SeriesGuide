package com.battlelancer.seriesguide.jobs;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.jobs.episodes.JobAction;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;

public abstract class BaseJob {

    private final JobAction action;

    public BaseJob(JobAction action) {
        this.action = action;
    }

    protected boolean persistNetworkJob(Context context, @NonNull byte[] jobInfo) {
        ContentValues values = new ContentValues();
        values.put(SeriesGuideContract.Jobs.TYPE, action.id);
        values.put(SeriesGuideContract.Jobs.CREATED_MS, System.currentTimeMillis());
        values.put(SeriesGuideContract.Jobs.EXTRAS, jobInfo);

        Uri insert = context.getContentResolver().insert(SeriesGuideContract.Jobs.CONTENT_URI, values);

        return insert != null;
    }
}
