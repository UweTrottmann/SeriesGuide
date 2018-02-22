package com.battlelancer.seriesguide.jobs;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags;
import com.battlelancer.seriesguide.jobs.episodes.JobAction;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.ui.OverviewActivity;
import com.battlelancer.seriesguide.ui.ShowsActivity;

public abstract class BaseNetworkEpisodeJob extends BaseNetworkJob {

    public BaseNetworkEpisodeJob(JobAction action, SgJobInfo jobInfo) {
        super(action, jobInfo);
    }

    @Nullable
    protected String getItemTitle(Context context) {
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
    protected String getActionDescription(Context context) {
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
    protected PendingIntent getErrorIntent(Context context) {
        // tapping the notification should open the affected show
        return TaskStackBuilder.create(context)
                .addNextIntent(new Intent(context, ShowsActivity.class))
                .addNextIntent(OverviewActivity.intentShow(context, jobInfo.showTvdbId()))
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
