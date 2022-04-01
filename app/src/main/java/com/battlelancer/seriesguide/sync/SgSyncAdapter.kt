package com.battlelancer.seriesguide.sync

import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.content.SyncResult
import android.os.Bundle
import android.text.format.DateUtils
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.HexagonTools
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase
import com.battlelancer.seriesguide.service.NotificationService
import com.battlelancer.seriesguide.settings.UpdateSettings
import com.battlelancer.seriesguide.sync.SyncOptions.SyncType
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.lists.ListsTools2.migrateTvdbShowListItemsToTmdbIds
import com.battlelancer.seriesguide.ui.movies.MovieTools
import com.battlelancer.seriesguide.ui.shows.ShowTools
import com.battlelancer.seriesguide.util.TaskManager
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.tmdb2.services.ConfigurationService
import com.uwetrottmann.trakt5.services.Sync
import dagger.Lazy
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.pow
import kotlin.random.Random

/**
 * [AbstractThreadedSyncAdapter] which updates show and movie data and sends data to
 * or syncs with Trakt and Cloud.
 */
class SgSyncAdapter(context: Context) : AbstractThreadedSyncAdapter(context, true, false) {

    enum class UpdateResult {
        SUCCESS, INCOMPLETE
    }

    @Inject
    lateinit var hexagonTools: Lazy<HexagonTools>

    @Inject
    lateinit var traktSync: Lazy<Sync>

    @Inject
    lateinit var movieTools: Lazy<MovieTools>

    @Inject
    lateinit var tmdbConfigService: Lazy<ConfigurationService>

    init {
        Timber.d("Creating sync adapter")
        SgApp.getServicesComponent(context).inject(this)
    }

    override fun onPerformSync(
        account: Account,
        extras: Bundle,
        authority: String,
        provider: ContentProviderClient,
        syncResult: SyncResult
    ) {
        // determine type of sync
        val options = SyncOptions(extras)
        Timber.i(
            "Syncing: %s%s",
            options.syncType,
            if (options.syncImmediately) "_IMMEDIATE" else "_REGULAR"
        )

        // JOBS
        if (options.syncType == SyncType.JOBS || options.syncType == SyncType.DELTA) {
            // Note: will not process further jobs if one fails, safe to call again to retry.
            NetworkJobProcessor(context).process()
            if (options.syncType == SyncType.JOBS) {
                return  // do nothing else
            }
        }

        // SYNC
        // should we sync?
        val showSync = ShowSync(options.syncType, options.singleShowId)
        val currentTime = System.currentTimeMillis()
        if (!options.syncImmediately && showSync.isSyncMultiple) {
            if (!isTimeForSync(context, currentTime)) {
                Timber.d("Syncing: ABORT_DID_JUST_SYNC")
                return
            }
        }

        // from here on we need more sophisticated abort handling, so keep track of errors
        val progress = SyncProgress()
        progress.publish(SyncProgress.Step.TMDB)

        // Get latest TMDb configuration.
        // No need to abort on failure, can use default or last fetched config.
        val tmdbSync = TmdbSync(context, tmdbConfigService.get(), movieTools.get())
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (!tmdbSync.updateConfiguration(prefs)) {
            progress.recordError()
        }

        // Update show data.
        // If failed for at least one show, do not proceed with other sync steps to avoid
        // syncing with outdated show data.
        // Note: it is still NOT guaranteed show data is up-to-date before syncing because a show
        // does not get updated if it was recently (see ShowSync selecting which shows to update).
        val showTools = SgApp.getServicesComponent(context).showTools()
        var resultCode = showSync.sync(context, showTools, currentTime, progress)
        Timber.d("Syncing: TMDB shows...DONE")
        if (resultCode == null || resultCode == UpdateResult.INCOMPLETE) {
            progress.recordError()
            progress.publishFinished()
            if (showSync.isSyncMultiple) {
                updateTimeAndFailedCounter(prefs, resultCode)
            }
            return // Try again later.
        }

        // do some more things if this is not a quick update
        if (showSync.isSyncMultiple) {
            // update data of to be released movies
            if (!tmdbSync.updateMovies(progress)) {
                progress.recordError()
            }
            Timber.d("Syncing: TMDB...DONE")

            // sync with hexagon
            var hasAddedShows = false
            val isHexagonEnabled = HexagonSettings.isEnabled(context)
            if (isHexagonEnabled) {
                val resultHexagonSync = HexagonSync(
                    context,
                    hexagonTools.get(), movieTools.get(), progress
                ).sync()
                hasAddedShows = resultHexagonSync.hasAddedShows
                // don't overwrite failure
                if (resultCode == UpdateResult.SUCCESS) {
                    resultCode = if (resultHexagonSync.success) {
                        UpdateResult.SUCCESS
                    } else {
                        UpdateResult.INCOMPLETE
                    }
                }
                Timber.d("Syncing: Hexagon...DONE")
            } else {
                Timber.d("Syncing: Hexagon...SKIP")
            }

            // Migrate legacy list items
            // Note: might send to Hexagon, so make sure to sync lists with Hexagon before
            migrateTvdbShowListItemsToTmdbIds(context)

            // sync with trakt (only ratings if hexagon is enabled)
            if (TraktCredentials.get(context).hasCredentials()) {
                val resultTraktSync = TraktSync(
                    context, movieTools.get(),
                    traktSync.get(), progress
                ).sync(currentTime, isHexagonEnabled)
                // don't overwrite failure
                if (resultCode == UpdateResult.SUCCESS) {
                    resultCode = resultTraktSync
                }
                Timber.d("Syncing: trakt...DONE")
            } else {
                Timber.d("Syncing: trakt...SKIP")
            }

            // renew search table if shows were updated and it will not be renewed by add task
            if (showSync.hasUpdatedShows() && !hasAddedShows) {
                SeriesGuideDatabase.rebuildFtsTable(context)
            }

            // update next episodes for all shows
            TaskManager.getInstance().tryNextEpisodeUpdateTask(context)

            updateTimeAndFailedCounter(prefs, resultCode)
        }

        // There could have been new episodes added after an update
        NotificationService.trigger(context)

        Timber.i("Syncing: %s", resultCode.toString())
        progress.publishFinished()
    }

    private fun updateTimeAndFailedCounter(
        prefs: SharedPreferences,
        resultCode: UpdateResult?
    ) {
        // store time of update, set retry counter on failure
        val currentTime = System.currentTimeMillis()
        if (resultCode == UpdateResult.SUCCESS) {
            // we were successful, reset failed counter
            prefs.edit()
                .putLong(UpdateSettings.KEY_LASTUPDATE, currentTime)
                .putInt(UpdateSettings.KEY_FAILED_COUNTER, 0)
                .apply()
        } else {
            val failed = UpdateSettings.getFailedNumberOfUpdates(context) + 1

            val backOffExponent = failed.coerceAtMost(5)
            // Back off by 2**(failures) minutes + random milliseconds
            // (random to have more spread across all installs).
            // Currently: 2min, 4min, 8min, 16min and max. 32min + random ms
            val backOffMinutes = 2.0.pow(backOffExponent).toLong()
            Timber.d("Syncing: backing off for %d minutes", backOffMinutes)
            val backOffMs = backOffMinutes * DateUtils.MINUTE_IN_MILLIS +
                    Random.nextLong(1000)

            /*
             * Purposely set a fake last update time, because the next update will be triggered
             * SYNC_INTERVAL_MINIMUM_MINUTES minutes after the last update time.
             * This will trigger sync earlier/later than the default (5min) interval.
             */
            val fakeLastUpdateTime = currentTime - SYNC_INTERVAL_MINIMUM_MINUTES + backOffMs

            prefs.edit()
                .putLong(UpdateSettings.KEY_LASTUPDATE, fakeLastUpdateTime)
                .putInt(UpdateSettings.KEY_FAILED_COUNTER, failed)
                .apply()
        }
    }

    companion object {
        /** Should never be outside 4-32 so back-off works as expected.  */
        private const val SYNC_INTERVAL_MINIMUM_MINUTES = 5

        /**
         * One of [SyncType].
         */
        const val EXTRA_SYNC_TYPE = "com.battlelancer.seriesguide.sync_type"

        /**
         * If [EXTRA_SYNC_TYPE] is [SyncType.SINGLE], the row id of the show
         * to sync.
         */
        const val EXTRA_SYNC_SHOW_ID = "com.battlelancer.seriesguide.sync_show"

        /**
         * Whether the sync should occur despite time or backoff limits.
         */
        const val EXTRA_SYNC_IMMEDIATE = "com.battlelancer.seriesguide.sync_immediate"

        private fun isTimeForSync(context: Context, currentTime: Long): Boolean {
            val previousUpdateTime = UpdateSettings.getLastAutoUpdateTime(context)
            return currentTime - previousUpdateTime >
                    SYNC_INTERVAL_MINIMUM_MINUTES * DateUtils.MINUTE_IN_MILLIS
        }

        /**
         * Calls [requestSyncIfConnected] if there is no pending sync.
         */
        @JvmStatic
        fun requestSyncIfTime(context: Context) {
            // guard against scheduling too many sync requests
            val account = AccountUtils.getAccount(context)
            if (account == null ||
                ContentResolver.isSyncPending(account, SgApp.CONTENT_AUTHORITY)) {
                return
            }
            if (!isTimeForSync(context, System.currentTimeMillis())) {
                return
            }
            requestSyncIfConnected(context, SyncType.DELTA, 0)
        }

        /**
         * Schedules a sync for a single show if [ShowTools.shouldUpdateShow] returns true.
         *
         * *Note: Runs a content provider op, so you should do this on a background thread.*
         */
        @JvmStatic
        fun requestSyncIfTime(context: Context, showId: Long) {
            if (SgApp.getServicesComponent(context).showTools().shouldUpdateShow(showId)) {
                requestSyncIfConnected(context, SyncType.SINGLE, showId)
            }
        }

        /**
         * Schedules a sync. Will only queue a sync request if there is a network connection and
         * auto-sync is enabled.
         *
         * @param showId If using [SyncType.SINGLE], the row id of a show.
         */
        private fun requestSyncIfConnected(context: Context, syncType: SyncType, showId: Long) {
            if (!AndroidUtils.isNetworkConnected(context) || !isSyncAutomatically(context)) {
                // offline or auto-sync disabled: abort
                return
            }
            val args = Bundle()
            args.putInt(EXTRA_SYNC_TYPE, syncType.id)
            args.putLong(EXTRA_SYNC_SHOW_ID, showId)
            requestSync(context, args)
        }

        fun requestSyncJobsImmediate(context: Context) {
            val args = Bundle()
            args.putBoolean(EXTRA_SYNC_IMMEDIATE, true)
            args.putInt(EXTRA_SYNC_TYPE, SyncType.JOBS.id)

            // ignore sync settings and backoff
            args.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
            // push to front of sync queue
            args.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)

            requestSync(context, args)
        }

        /**
         * Schedules an immediate sync even if auto-sync is disabled, it runs as soon as there is a
         * connection.
         *
         * @param showStatusToast If set, shows a status toast and aborts if offline.
         */
        fun requestSyncDeltaImmediate(context: Context, showStatusToast: Boolean) {
            requestSyncImmediate(context, SyncType.DELTA, 0, showStatusToast)
        }

        /**
         * @see .requestSyncDeltaImmediate
         */
        @JvmStatic
        fun requestSyncSingleImmediate(context: Context, showStatusToast: Boolean, showId: Long) {
            requestSyncImmediate(context, SyncType.SINGLE, showId, showStatusToast)
        }

        /**
         * @see .requestSyncDeltaImmediate
         */
        fun requestSyncFullImmediate(context: Context, showStatusToast: Boolean) {
            requestSyncImmediate(context, SyncType.FULL, 0, showStatusToast)
        }

        private fun requestSyncImmediate(
            context: Context,
            syncType: SyncType,
            showId: Long,
            showStatusToast: Boolean
        ) {
            if (showStatusToast) {
                if (!AndroidUtils.isNetworkConnected(context)) {
                    // offline: notify and abort
                    Toast.makeText(context, R.string.update_no_connection, Toast.LENGTH_LONG).show()
                    return
                }
                // notify about upcoming sync
                Toast.makeText(context, R.string.update_scheduled, Toast.LENGTH_SHORT).show()
            }

            val args = Bundle()
            args.putBoolean(EXTRA_SYNC_IMMEDIATE, true)
            args.putInt(EXTRA_SYNC_TYPE, syncType.id)
            args.putLong(EXTRA_SYNC_SHOW_ID, showId)

            // ignore sync settings and backoff
            args.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
            // push to front of sync queue
            args.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)

            requestSync(context, args)
        }

        /**
         * Schedules a sync with the given arguments.
         */
        private fun requestSync(context: Context, args: Bundle) {
            val account = AccountUtils.getAccount(context) ?: return
            ContentResolver.requestSync(account, SgApp.CONTENT_AUTHORITY, args)
        }

        /**
         * Returns true if there is currently a sync operation for the given account or authority in the
         * pending list, or actively being processed.
         */
        fun isSyncActive(context: Context, isDisplayWarning: Boolean): Boolean {
            val account = AccountUtils.getAccount(context) ?: return false
            val isSyncActive = ContentResolver.isSyncActive(
                account,
                SgApp.CONTENT_AUTHORITY
            )
            if (isSyncActive && isDisplayWarning) {
                Toast.makeText(context, R.string.update_inprogress, Toast.LENGTH_LONG).show()
            }
            return isSyncActive
        }

        /**
         * Check if the provider should be synced when a network tickle is received.
         */
        fun isSyncAutomatically(context: Context): Boolean {
            val account = AccountUtils.getAccount(context)
            return account != null && ContentResolver.getSyncAutomatically(
                account,
                SgApp.CONTENT_AUTHORITY
            )
        }

        /**
         * Set whether or not the provider is synced when it receives a network tickle.
         */
        fun setSyncAutomatically(context: Context, sync: Boolean) {
            val account = AccountUtils.getAccount(context) ?: return
            ContentResolver.setSyncAutomatically(account, SgApp.CONTENT_AUTHORITY, sync)
        }
    }

}