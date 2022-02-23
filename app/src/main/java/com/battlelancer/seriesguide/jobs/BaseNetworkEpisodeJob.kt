package com.battlelancer.seriesguide.jobs

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.TaskStackBuilder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.jobs.episodes.JobAction
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.ui.OverviewActivity.Companion.intentShow
import com.battlelancer.seriesguide.ui.ShowsActivity
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags
import com.battlelancer.seriesguide.util.PendingIntentCompat

abstract class BaseNetworkEpisodeJob(
    action: JobAction,
    jobInfo: SgJobInfo
) : BaseNetworkJob(action, jobInfo) {

    override fun getItemTitle(context: Context): String? {
        return SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTitle(jobInfo.showId())
    }

    override fun getActionDescription(context: Context): String? {
        val flagValue = jobInfo.flagValue()
        if (action == JobAction.EPISODE_COLLECTION) {
            val isRemoveAction = flagValue == 0
            return context.getString(
                if (isRemoveAction) {
                    R.string.action_collection_remove
                } else {
                    R.string.action_collection_add
                }
            )
        } else if (action == JobAction.EPISODE_WATCHED_FLAG) {
            when (flagValue) {
                EpisodeFlags.UNWATCHED -> {
                    return context.getString(R.string.action_unwatched)
                }
                EpisodeFlags.SKIPPED -> {
                    return context.getString(R.string.action_skip)
                }
                EpisodeFlags.WATCHED -> {
                    return context.getString(R.string.action_watched)
                }
            }
        }
        return null
    }

    override fun getErrorIntent(context: Context): PendingIntent {
        // tapping the notification should open the affected show
        return TaskStackBuilder.create(context)
            .addNextIntent(Intent(context, ShowsActivity::class.java))
            .addNextIntent(intentShow(context, jobInfo.showId()))
            .getPendingIntent(
                0,
                PendingIntentCompat.flagImmutable or PendingIntent.FLAG_UPDATE_CURRENT
            )!!
    }
}