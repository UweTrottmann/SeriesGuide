package com.battlelancer.seriesguide.jobs

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.TaskStackBuilder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.jobs.episodes.JobAction
import com.battlelancer.seriesguide.provider.SgRoomDatabase.Companion.getInstance
import com.battlelancer.seriesguide.ui.MoviesActivity
import com.battlelancer.seriesguide.ui.ShowsActivity
import com.battlelancer.seriesguide.ui.movies.MovieDetailsActivity
import com.battlelancer.seriesguide.util.PendingIntentCompat

abstract class BaseNetworkMovieJob(
    action: JobAction,
    jobInfo: SgJobInfo
) : BaseNetworkJob(action, jobInfo) {

    override fun getItemTitle(context: Context): String? = getInstance(context)
        .movieHelper()
        .getMovieTitle(jobInfo.movieTmdbId())

    override fun getActionDescription(context: Context): String? {
        return when (action) {
            JobAction.MOVIE_COLLECTION_ADD -> context.getString(R.string.action_collection_add)
            JobAction.MOVIE_COLLECTION_REMOVE -> context.getString(R.string.action_collection_remove)
            JobAction.MOVIE_WATCHLIST_ADD -> context.getString(R.string.watchlist_add)
            JobAction.MOVIE_WATCHLIST_REMOVE -> context.getString(R.string.watchlist_remove)
            JobAction.MOVIE_WATCHED_SET -> context.getString(R.string.action_watched)
            JobAction.MOVIE_WATCHED_REMOVE -> context.getString(R.string.action_unwatched)
            else -> null
        }
    }

    /**
     * Returns intent with task stack that opens the affected movie.
     */
    override fun getErrorIntent(context: Context): PendingIntent {
        return TaskStackBuilder.create(context)
            .addNextIntent(Intent(context, ShowsActivity::class.java))
            .addNextIntent(Intent(context, MoviesActivity::class.java))
            .addNextIntent(MovieDetailsActivity.intentMovie(context, jobInfo.movieTmdbId()))
            .getPendingIntent(
                0,
                PendingIntentCompat.flagImmutable or PendingIntent.FLAG_UPDATE_CURRENT
            )!!
    }
}