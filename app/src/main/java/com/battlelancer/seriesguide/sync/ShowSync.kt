package com.battlelancer.seriesguide.sync

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.sync.SgSyncAdapter.UpdateResult
import com.battlelancer.seriesguide.sync.SyncOptions.SyncType
import com.battlelancer.seriesguide.ui.shows.ShowTools
import com.battlelancer.seriesguide.ui.shows.ShowTools2
import com.battlelancer.seriesguide.ui.shows.ShowTools2.UpdateResult.ApiErrorRetry
import com.battlelancer.seriesguide.ui.shows.ShowTools2.UpdateResult.ApiErrorStop
import com.battlelancer.seriesguide.ui.shows.ShowTools2.UpdateResult.DatabaseError
import com.battlelancer.seriesguide.ui.shows.ShowTools2.UpdateResult.DoesNotExist
import com.battlelancer.seriesguide.ui.shows.ShowTools2.UpdateResult.Success
import com.battlelancer.seriesguide.util.TimeTools
import com.uwetrottmann.androidutils.AndroidUtils
import timber.log.Timber
import kotlin.math.pow
import kotlin.random.Random

/**
 * Updates show data from the show data source.
 * If updating a single show, should supply its row ID.
 */
class ShowSync(
    private val syncType: SyncType,
    private val singleShowId: Long
) {

    private var hasUpdatedShows = false

    /**
     * Update shows based on the sync type. Returns
     *
     * - null if the given show ID is not found
     * - [SgSyncAdapter.UpdateResult.INCOMPLETE] if a show has failed to update or the update
     * process should be retried later
     * - [SgSyncAdapter.UpdateResult.SUCCESS] if all desired shows were updated
     *
     * Considers shows that no longer exist at the source to be updated.
     * On network errors retries a few times to update a show before failing.
     */
    @SuppressLint("TimberExceptionLogging")
    fun sync(
        context: Context,
        showTools: ShowTools,
        currentTime: Long,
        progress: SyncProgress
    ): UpdateResult? {
        hasUpdatedShows = false

        val showsToUpdate = getShowsToUpdate(context, currentTime) ?: return null
        Timber.d("Updating %d show(s)...", showsToUpdate.size)

        var networkErrors = 0
        for (showId in showsToUpdate) {
            // Try to update this show.
            var result: ShowTools2.UpdateResult
            do {
                // Shortcut to stop updating if connectivity is lost.
                if (!AndroidUtils.isNetworkConnected(context)) {
                    return UpdateResult.INCOMPLETE
                }

                // This can fail due to
                // - network error (not connected, unknown host, time out) => abort and try again
                // - API error (parsing error, other error) => abort, report and try again later
                // - show does no longer exist => ignore and continue
                // - database error => abort, report and try again later
                // Note: reporting is done where the exception occurs.
                result = showTools.updateShow(showId)

                if (result is ApiErrorRetry) {
                    networkErrors++
                    if (networkErrors == 3) {
                        // Stop updating after multiple network errors
                        // (for timeouts around 3 * 15/20 seconds)
                        val service = context.getString(result.service.nameResId)
                        Timber.e("Too many network errors, last one with $service, trying again later.")
                        progress.setImportantErrorIfNone("Failed to talk to $service, trying again later.")
                        return UpdateResult.INCOMPLETE
                    } else {
                        // Back off, then try again.
                        try {
                            // Wait for 2^n seconds + random milliseconds,
                            // with n starting at 0 (so 1 s + random ms)
                            val n = networkErrors - 1
                            Thread.sleep(
                                (2.0.pow(n)).toLong() * DateUtils.SECOND_IN_MILLIS
                                        + Random.nextInt(0, 1000)
                            )
                        } catch (e: InterruptedException) {
                            // This can happen if the system has decided to interrupt the sync
                            // thread (see AbstractThreadedSyncAdapter class documentation),
                            // just try again later.
                            Timber.v("Wait for retry interrupted by system, trying again later.")
                            progress.setImportantErrorIfNone("Interrupted by system, trying again later.")
                            return UpdateResult.INCOMPLETE
                        }
                    }
                } else if (networkErrors > 0) {
                    // Reduce counter on each successful update.
                    networkErrors--
                }
            } while (result is ApiErrorRetry)

            // Handle update result.
            when (result) {
                Success -> hasUpdatedShows = true
                DoesNotExist -> {
                    // Continue with other shows, assume existing data is latest.
                    // TODO Add permanent hint to user the show can no longer be updated.
                    //  Currently, if multiple shows do not exist only the first to be tried
                    //  is displayed to the user.
                    setImportantMessageIfNone(
                        context,
                        progress,
                        showId,
                        "Show '%s' removed from TMDB (id %s), maybe search for a replacement and remove it."
                    )
                }
                is ApiErrorRetry -> throw IllegalStateException("Should retry and not handle result.")
                is ApiErrorStop -> {
                    // API error, do not continue and try again later.
                    setImportantMessageIfNone(
                        context,
                        progress,
                        showId,
                        "Could not update show '%s' (TMDB id %s) due to issue with ${
                            context.getString(result.service.nameResId)
                        }, trying again later."
                    )
                    return UpdateResult.INCOMPLETE
                }
                DatabaseError -> {
                    // Database error, do not continue and try again later.
                    setImportantMessageIfNone(
                        context,
                        progress,
                        showId,
                        "Could not update show '%s' (TMDB id %s) due to a database error, trying again later."
                    )
                    return UpdateResult.INCOMPLETE
                }
            }
        }
        return UpdateResult.SUCCESS
    }

    private fun setImportantMessageIfNone(
        context: Context,
        progress: SyncProgress,
        showId: Long,
        messageTemplate: String
    ) {
        val helper = SgRoomDatabase.getInstance(context).sgShow2Helper()
        val showTitle = helper.getShowTitle(showId)
        val showTmdbId = helper.getShowTmdbId(showId)
        val message = String.format(
            messageTemplate,
            showTitle, showTmdbId
        )
        progress.setImportantErrorIfNone(message)
        Timber.e(message)
    }

    /**
     * Returns an array of show ids to update.
     */
    private fun getShowsToUpdate(context: Context, currentTime: Long): List<Long>? {
        return when (syncType) {
            SyncType.SINGLE -> {
                val showId = singleShowId
                if (showId == 0L) {
                    Timber.e("Syncing...ABORT_INVALID_SHOW_TVDB_ID")
                    return null
                }
                listOf(showId)
            }
            SyncType.FULL -> {
                // get all show IDs for a full update
                SgRoomDatabase.getInstance(context).sgShow2Helper().getShowIdsLong()
            }
            SyncType.DELTA -> getShowsToDeltaUpdate(context, currentTime)
            else -> throw IllegalArgumentException("Sync type $syncType is not supported.")
        }
    }

    /**
     * Return list of show IDs that have not been updated for a certain time.
     */
    private fun getShowsToDeltaUpdate(context: Context, currentTime: Long): List<Long> {
        // get existing show ids
        val shows = SgRoomDatabase.getInstance(context)
            .sgShow2Helper().getShowsUpdateInfo()

        val updatableShowIds: MutableList<Long> = ArrayList()
        for ((id, lastUpdatedTime, releaseWeekDay) in shows) {
            val isDailyShow = releaseWeekDay == TimeTools.RELEASE_WEEKDAY_DAILY
            // update daily shows more frequently than weekly shows
            if (currentTime - lastUpdatedTime >
                (if (isDailyShow) UPDATE_THRESHOLD_DAILYS_MS else UPDATE_THRESHOLD_WEEKLYS_MS)) {
                // add shows that are due for updating
                updatableShowIds.add(id)
            }
        }
        return updatableShowIds
    }

    val isSyncMultiple: Boolean = syncType == SyncType.DELTA || syncType == SyncType.FULL

    fun hasUpdatedShows(): Boolean {
        return hasUpdatedShows
    }

    companion object {
        // Values based on the assumption that sync runs about every 24 hours
        private const val UPDATE_THRESHOLD_WEEKLYS_MS = 6 * DateUtils.DAY_IN_MILLIS +
                12 * DateUtils.HOUR_IN_MILLIS
        private const val UPDATE_THRESHOLD_DAILYS_MS = (DateUtils.DAY_IN_MILLIS
                + 12 * DateUtils.HOUR_IN_MILLIS)
    }
}