// SPDX-License-Identifier: Apache-2.0
// Copyright 2011-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.tools

import android.content.Context
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase
import com.battlelancer.seriesguide.shows.tools.AddShowTask.OnShowAddedEvent
import com.battlelancer.seriesguide.shows.tools.AddUpdateShowTools.ShowResult
import com.battlelancer.seriesguide.sync.HexagonEpisodeSync
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.traktapi.TraktSettings
import com.battlelancer.seriesguide.traktapi.TraktTools2
import com.battlelancer.seriesguide.traktapi.TraktTools2.ServiceResult
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.trakt5.entities.BaseShow
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.util.LinkedList

/**
 * Adds shows to the local database using [AddUpdateShowTools.addShow].
 *
 * Set [isSilentMode] to not send [OnShowAddedEvent] events, even on failure.
 *
 * Set [isMergingShows] to set [HexagonSettings.setHasMergedShows] if all shows were added
 * successfully.
 */
class AddShowTask(
    context: Context,
    shows: List<Show>,
    private val isSilentMode: Boolean,
    private val isMergingShows: Boolean
) {

    /**
     * [tmdbId] and [languageCode] are passed to [AddUpdateShowTools.addShow]. The [title] is only
     * used for notifying the user, so it can be empty if the task is running in silent mode.
     */
    data class Show(
        val tmdbId: Int,
        val languageCode: String,
        val title: String
    )

    class OnShowAddedEvent private constructor(
        /**
         * Is -1 if add task was aborted.
         */
        val showTmdbId: Int,
        private val message: String?,
        val successful: Boolean
    ) {
        fun handle(context: Context?) {
            if (message != null) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }

        companion object {
            fun successful(showTmdbId: Int): OnShowAddedEvent {
                return OnShowAddedEvent(showTmdbId, null, true)
            }

            fun exists(context: Context, showTmdbId: Int, showTitle: String?): OnShowAddedEvent {
                return OnShowAddedEvent(
                    showTmdbId,
                    context.getString(R.string.add_already_exists, showTitle),
                    true
                )
            }

            fun failed(context: Context, showTmdbId: Int, showTitle: String?): OnShowAddedEvent {
                return OnShowAddedEvent(
                    showTmdbId,
                    context.getString(R.string.add_error, showTitle),
                    false
                )
            }

            fun failedDetails(
                context: Context,
                showTmdbId: Int,
                showTitle: String?,
                details: String?
            ): OnShowAddedEvent {
                return OnShowAddedEvent(
                    showTmdbId,
                    String.format(
                        "%s %s", context.getString(R.string.add_error, showTitle),
                        details
                    ),
                    false
                )
            }

            fun aborted(message: String): OnShowAddedEvent {
                return OnShowAddedEvent(-1, message, false)
            }
        }
    }

    private val context: Context = context.applicationContext
    private val addQueue = LinkedList<Show>()

    init {
        addQueue.addAll(shows)
    }

    fun run() {
        Timber.d("Starting to add shows...")

        val firstShow = addQueue.peek()
        if (firstShow == null) {
            Timber.d("Finished. Queue was empty.")
            return
        }

        if (!AndroidUtils.isNetworkConnected(context)) {
            Timber.d("Finished. No internet connection.")
            publishProgress(RESULT_OFFLINE, firstShow.tmdbId, firstShow.title)
            return
        }

        // If not connected to Hexagon, get episodes from Trakt
        var traktCollection: Map<Int, BaseShow>? = null
        var traktWatched: Map<Int, BaseShow>? = null
        if (!HexagonSettings.isEnabled(context) && TraktCredentials.get(context).hasCredentials()) {
            Timber.d("Getting watched and collected episodes from trakt.")
            // get collection
            traktCollection = getTraktShows(true)
                ?: return // can not get collected state, give up
            // get watched
            traktWatched = getTraktShows(false)
                ?: return // can not get watched state, give up
        }

        val services = SgApp.getServicesComponent(context)
        val hexagonEpisodeSync = HexagonEpisodeSync(context, services.hexagonTools())
        val showTools = services.addUpdateShowTools()

        var result: Int
        var addedAtLeastOneShow = false
        var failedMergingShows = false
        while (!addQueue.isEmpty()) {
            Timber.d("Starting to add next show...")
            val nextShow = addQueue.removeFirst()
            val currentShowName = nextShow.title
            val currentShowTmdbId = nextShow.tmdbId

            if (currentShowTmdbId <= 0) {
                // Invalid ID, should never have been passed, report.
                // Background: Hexagon gets requests with ID 0.
                val invalidIdException =
                    IllegalStateException("Show id invalid: $currentShowTmdbId, silentMode=$isSilentMode, merging=$isMergingShows")
                Errors.logAndReport("Add show", invalidIdException)
                continue
            }

            if (!AndroidUtils.isNetworkConnected(context)) {
                Timber.d("Finished. No connection.")
                publishProgress(RESULT_OFFLINE, currentShowTmdbId, currentShowName)
                failedMergingShows = true
                break
            }

            val addResult = showTools.addShow(
                nextShow.tmdbId,
                nextShow.languageCode,
                traktCollection, traktWatched,
                hexagonEpisodeSync
            )
            when (addResult) {
                ShowResult.SUCCESS -> {
                    result = PROGRESS_SUCCESS
                    addedAtLeastOneShow = true
                }

                ShowResult.IN_DATABASE -> {
                    result = PROGRESS_EXISTS
                }

                else -> {
                    Timber.e("Adding show failed: %s", addResult)

                    // Only fail a Hexagon merge if a show can not be added due to a network error,
                    // not because it does not or no longer exist at the source.
                    if (isMergingShows && addResult != ShowResult.DOES_NOT_EXIST) {
                        failedMergingShows = true
                    }

                    result = when (addResult) {
                        ShowResult.DOES_NOT_EXIST -> PROGRESS_ERROR_DOES_NOT_EXIST
                        ShowResult.TMDB_ERROR -> PROGRESS_ERROR_TMDB
                        ShowResult.HEXAGON_ERROR -> PROGRESS_ERROR_HEXAGON
                        ShowResult.DATABASE_ERROR -> PROGRESS_ERROR_DATA
                        else -> PROGRESS_ERROR
                    }
                }
            }
            publishProgress(result, currentShowTmdbId, currentShowName)
            Timber.d("Finished adding show. (Result code: %s)", result)
        }

        // When merging shows down from Hexagon, set success flag
        if (isMergingShows && !failedMergingShows) {
            HexagonSettings.setHasMergedShows(context, true)
        }

        if (addedAtLeastOneShow) {
            // Make sure the next sync will download all ratings
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putLong(TraktSettings.KEY_LAST_SHOWS_RATED_AT, 0)
                .putLong(TraktSettings.KEY_LAST_EPISODES_RATED_AT, 0)
                .apply()

            Timber.d("Renewing search table.")
            SeriesGuideDatabase.rebuildFtsTable(context)
        }

        Timber.d("Finished adding shows.")
    }

    private fun publishProgress(
        result: Int,
        showTmdbId: Int,
        showTitle: String
    ) {
        if (isSilentMode) {
            Timber.d("SILENT MODE: do not show progress toast")
            return
        }

        // Not catching format/null exceptions, should not occur if values correctly passed.
        val event = when (result) {
            PROGRESS_SUCCESS ->
                // Do nothing, user will see show added to show list
                OnShowAddedEvent.successful(showTmdbId)

            PROGRESS_EXISTS -> OnShowAddedEvent.exists(context, showTmdbId, showTitle)

            PROGRESS_ERROR -> OnShowAddedEvent.failed(context, showTmdbId, showTitle)

            PROGRESS_ERROR_TMDB -> OnShowAddedEvent.failedDetails(
                context, showTmdbId, showTitle,
                context.getString(R.string.api_error_generic, context.getString(R.string.tmdb))
            )

            PROGRESS_ERROR_DOES_NOT_EXIST -> OnShowAddedEvent.failedDetails(
                context, showTmdbId, showTitle,
                context.getString(R.string.tvdb_error_does_not_exist)
            )

            PROGRESS_ERROR_HEXAGON -> OnShowAddedEvent.failedDetails(
                context, showTmdbId, showTitle,
                context.getString(
                    R.string.api_error_generic,
                    context.getString(R.string.hexagon)
                )
            )

            PROGRESS_ERROR_DATA -> OnShowAddedEvent.failedDetails(
                context, showTmdbId, showTitle, context.getString(R.string.database_error)
            )

            RESULT_OFFLINE -> OnShowAddedEvent.aborted(context.getString(R.string.offline))

            RESULT_TRAKT_API_ERROR -> OnShowAddedEvent.aborted(
                context.getString(R.string.api_error_generic, context.getString(R.string.trakt))
            )

            RESULT_TRAKT_AUTH_ERROR -> OnShowAddedEvent.aborted(
                context.getString(R.string.trakt_error_credentials)
            )

            else -> null
        }

        if (event != null) {
            EventBus.getDefault().post(event)
        }
    }

    private fun publishProgress(result: Int) {
        publishProgress(result, 0, "")
    }

    private fun getTraktShows(isCollectionNotWatched: Boolean): Map<Int, BaseShow>? {
        val result: Pair<Map<Int, BaseShow>?, ServiceResult> =
            TraktTools2.getCollectedOrWatchedShows(isCollectionNotWatched, context)
        if (result.second == ServiceResult.AUTH_ERROR) {
            publishProgress(RESULT_TRAKT_AUTH_ERROR)
        } else if (result.second == ServiceResult.API_ERROR) {
            publishProgress(RESULT_TRAKT_API_ERROR)
        }
        return result.first
    }

    companion object {
        private const val PROGRESS_EXISTS = 0
        private const val PROGRESS_SUCCESS = 1
        private const val PROGRESS_ERROR = 2
        private const val PROGRESS_ERROR_TMDB = 3
        private const val PROGRESS_ERROR_DOES_NOT_EXIST = 4
        private const val PROGRESS_ERROR_HEXAGON = 6
        private const val PROGRESS_ERROR_DATA = 7
        private const val RESULT_OFFLINE = 8
        private const val RESULT_TRAKT_API_ERROR = 9
        private const val RESULT_TRAKT_AUTH_ERROR = 10
    }
}
