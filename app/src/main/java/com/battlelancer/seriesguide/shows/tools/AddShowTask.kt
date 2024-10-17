// SPDX-License-Identifier: Apache-2.0
// Copyright 2011-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.tools

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.backend.settings.HexagonSettings.isEnabled
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase
import com.battlelancer.seriesguide.shows.tools.AddUpdateShowTools.ShowResult
import com.battlelancer.seriesguide.sync.HexagonEpisodeSync
import com.battlelancer.seriesguide.traktapi.TraktCredentials.Companion.get
import com.battlelancer.seriesguide.traktapi.TraktSettings
import com.battlelancer.seriesguide.traktapi.TraktTools2
import com.battlelancer.seriesguide.traktapi.TraktTools2.ServiceResult
import com.battlelancer.seriesguide.util.Errors
import com.battlelancer.seriesguide.util.TaskManager
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.trakt5.entities.BaseShow
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.util.LinkedList

/**
 * Adds shows to the local database, tries to get watched and collected episodes if a trakt account
 * is connected.
 */
class AddShowTask(
    context: Context,
    shows: List<Show>,
    isSilentMode: Boolean,
    isMergingShows: Boolean
) : AsyncTask<Void?, String, Void?>() {

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

    @SuppressLint("StaticFieldLeak")
    private val context: Context = context.applicationContext
    private val addQueue = LinkedList<Show>()

    private var isFinishedAddingShows = false
    private var isSilentMode: Boolean
    private var isMergingShows: Boolean

    init {
        addQueue.addAll(shows)
        this.isSilentMode = isSilentMode
        this.isMergingShows = isMergingShows
    }

    /**
     * Adds shows to the add queue. If this returns false, the shows were not added because the task
     * is finishing up. Create a new one instead.
     */
    fun addShows(
        shows: List<Show>,
        isSilentMode: Boolean,
        isMergingShows: Boolean
    ): Boolean {
        if (isFinishedAddingShows) {
            Timber.d("addShows: failed, already finishing up.")
            return false
        } else {
            this.isSilentMode = isSilentMode
            // never reset isMergingShows once true, so merged flag is correctly set on completion
            this.isMergingShows = this.isMergingShows || isMergingShows
            addQueue.addAll(shows)
            Timber.d("addShows: added shows to queue.")
            return true
        }
    }

    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg params: Void?): Void? {
        Timber.d("Starting to add shows...")

        val firstShow = addQueue.peek()
        if (firstShow == null) {
            Timber.d("Finished. Queue was empty.")
            return null
        }

        if (!AndroidUtils.isNetworkConnected(context)) {
            Timber.d("Finished. No internet connection.")
            publishProgress(RESULT_OFFLINE, firstShow.tmdbId, firstShow.title)
            return null
        }

        if (isCancelled) {
            Timber.d("Finished. Cancelled.")
            return null
        }

        // if not connected to Hexagon, get episodes from trakt
        var traktCollection: Map<Int, BaseShow>? = null
        var traktWatched: Map<Int, BaseShow>? = null
        if (!isEnabled(context) && get(context).hasCredentials()) {
            Timber.d("Getting watched and collected episodes from trakt.")
            // get collection
            traktCollection = getTraktShows(true)
                ?: return null // can not get collected state from trakt, give up.
            // get watched
            traktWatched = getTraktShows(false)
                ?: return null // can not get watched state from trakt, give up.
        }

        val services = SgApp.getServicesComponent(context)
        val hexagonEpisodeSync = HexagonEpisodeSync(context, services.hexagonTools())
        val showTools = services.addUpdateShowTools()

        var result: Int
        var addedAtLeastOneShow = false
        var failedMergingShows = false
        while (!addQueue.isEmpty()) {
            Timber.d("Starting to add next show...")
            if (isCancelled) {
                Timber.d("Finished. Cancelled.")
                // only cancelled on config change, so don't rebuild fts
                // table yet
                return null
            }

            val nextShow = addQueue.removeFirst()
            // set values required for progress update
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

                    // Only fail a hexagon merge if show can not be added due to network error,
                    // not because it does not (longer) exist.
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

        isFinishedAddingShows = true

        // when merging shows down from Hexagon, set success flag
        if (isMergingShows && !failedMergingShows) {
            HexagonSettings.setHasMergedShows(context, true)
        }

        if (addedAtLeastOneShow) {
            // make sure the next sync will download all ratings
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putLong(TraktSettings.KEY_LAST_SHOWS_RATED_AT, 0)
                .putLong(TraktSettings.KEY_LAST_EPISODES_RATED_AT, 0)
                .apply()

            // renew FTS3 table
            Timber.d("Renewing search table.")
            SeriesGuideDatabase.rebuildFtsTable(context)
        }

        Timber.d("Finished adding shows.")
        return null
    }

    @Deprecated("Deprecated in Java")
    override fun onProgressUpdate(vararg values: String) {
        if (isSilentMode) {
            Timber.d("SILENT MODE: do not show progress toast")
            return
        }

        // Not catching format/null exceptions, should not occur if values correctly passed.
        val result = values[0].toInt()
        val showTmdbId = values[1].toInt()
        val showTitle = values[2]
        val event = when (result) {
            PROGRESS_SUCCESS ->
                // do nothing, user will see show added to show list
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
                context.getString(R.string.api_error_generic, context.getString(R.string.hexagon))
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

    @Deprecated("Deprecated in Java")
    override fun onPostExecute(aVoid: Void?) {
        TaskManager.releaseAddTaskRef()
    }

    private fun publishProgress(result: Int) {
        publishProgress(result.toString(), "0", "")
    }

    private fun publishProgress(result: Int, showTmdbId: Int, showTitle: String) {
        publishProgress(result.toString(), showTmdbId.toString(), showTitle)
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
