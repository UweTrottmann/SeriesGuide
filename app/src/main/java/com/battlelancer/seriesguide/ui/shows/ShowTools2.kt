package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.widget.Toast
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.enums.NetworkResult
import com.battlelancer.seriesguide.enums.Result
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.service.NotificationService
import com.battlelancer.seriesguide.sync.HexagonShowSync
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.seriesguide.backend.shows.model.Show
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

/**
 * Provides some show operations as (async) suspend functions, running within global scope.
 */
class ShowTools2(val showTools: ShowTools, val context: Context) {

    /**
     * Posted if a show is about to get removed.
     */
    data class OnRemovingShowEvent(val showId: Long)

    /**
     * Posted if show was just removed (or failure).
     */
    data class OnShowRemovedEvent(
        val showId: Long,
        /** One of [com.battlelancer.seriesguide.enums.NetworkResult]. */
        val resultCode: Int
    )

    /**
     * Removes a show and its seasons and episodes, including search docs. Sends isRemoved flag to
     * Hexagon so the show will not be auto-added on any device connected to Hexagon.
     *
     * Posts [OnRemovingShowEvent] when starting and [OnShowRemovedEvent] once completed.
     */
    fun removeShow(showId: Long) {
        SgApp.coroutineScope.launch(SgApp.SINGLE) {
            withContext(Dispatchers.Main) {
                EventBus.getDefault().post(OnRemovingShowEvent(showId))
            }

            val result = removeShowAsync(showId)

            withContext(Dispatchers.Main) {
                if (result == NetworkResult.OFFLINE) {
                    Toast.makeText(context, R.string.offline, Toast.LENGTH_LONG).show()
                } else if (result == NetworkResult.ERROR) {
                    Toast.makeText(context, R.string.delete_error, Toast.LENGTH_LONG).show()
                }
                EventBus.getDefault().post(OnShowRemovedEvent(showId, result))
            }
        }
    }

    /**
     * Returns [com.battlelancer.seriesguide.enums.NetworkResult].
     */
    private suspend fun removeShowAsync(showId: Long): Int {
        // Send to cloud.
        val isCloudFailed = withContext(Dispatchers.Default) {
            if (!HexagonSettings.isEnabled(context)) {
                return@withContext false
            }
            if (isNotConnected(context)) {
                return@withContext true
            }
            val showTvdbId =
                SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTvdbId(showId)
            if (showTvdbId == 0) {
                return@withContext true
            }

            // Sets the isRemoved flag of the given show on Hexagon, so the show will
            // not be auto-added on any device connected to Hexagon.
            val show = Show()
            show.tvdbId = showTvdbId
            show.isRemoved = true

            val success = uploadShowToCloudAsync(show)
            return@withContext !success
        }
        // Do not save to local database if sending to cloud has failed.
        if (isCloudFailed) return Result.ERROR

        return withContext(Dispatchers.IO) {
            // Remove database entries in stages, so if an earlier stage fails,
            // user can try again. Also saves memory by using smaller database transactions.
            val database = SgRoomDatabase.getInstance(context)

            var rowsUpdated = database.sgEpisode2Helper().deleteEpisodesOfShow(showId)
            if (rowsUpdated == -1) return@withContext Result.ERROR

            rowsUpdated = database.sgSeason2Helper().deleteSeasonsOfShow(showId)
            if (rowsUpdated == -1) return@withContext Result.ERROR

            rowsUpdated = database.sgShow2Helper().deleteShow(showId)
            if (rowsUpdated == -1) return@withContext Result.ERROR

            SeriesGuideDatabase.rebuildFtsTable(context)
            Result.SUCCESS
        }
    }

    /**
     * Saves new favorite flag to the local database and, if signed in, up into the cloud as well.
     */
    fun storeIsFavorite(showId: Long, isFavorite: Boolean) {
        SgApp.coroutineScope.launch {
            storeIsFavoriteAsync(showId, isFavorite)
        }
    }

    private suspend fun storeIsFavoriteAsync(showId: Long, isFavorite: Boolean) {
        // Send to cloud.
        val isCloudFailed = withContext(Dispatchers.Default) {
            if (!HexagonSettings.isEnabled(context)) {
                return@withContext false
            }
            if (isNotConnected(context)) {
                return@withContext true
            }
            val showTvdbId =
                SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTvdbId(showId)
            if (showTvdbId == 0) {
                return@withContext true
            }

            val show = Show()
            show.tvdbId = showTvdbId
            show.isFavorite = isFavorite

            val success = uploadShowToCloudAsync(show)
            return@withContext !success
        }
        // Do not save to local database if sending to cloud has failed.
        if (isCloudFailed) return

        // Save to local database.
        withContext(Dispatchers.IO) {
            SgRoomDatabase.getInstance(context).sgShow2Helper().setShowFavorite(showId, isFavorite)

            // FIXME also notify URIs used by search and lists
            context.contentResolver
                .notifyChange(SeriesGuideContract.Shows.CONTENT_URI_FILTER, null)
            context.contentResolver
                .notifyChange(SeriesGuideContract.ListItems.CONTENT_WITH_DETAILS_URI, null)
        }

        // display info toast
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context, context.getString(
                    if (isFavorite)
                        R.string.favorited
                    else
                        R.string.unfavorited
                ), Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Saves new hidden flag to the local database and, if signed in, up into the cloud as well.
     */
    fun storeIsHidden(showId: Long, isHidden: Boolean) {
        SgApp.coroutineScope.launch {
            storeIsHiddenAsync(showId, isHidden)
        }
    }

    private suspend fun storeIsHiddenAsync(showId: Long, isHidden: Boolean) {
        // Send to cloud.
        val isCloudFailed = withContext(Dispatchers.Default) {
            if (!HexagonSettings.isEnabled(context)) {
                return@withContext false
            }
            if (isNotConnected(context)) {
                return@withContext true
            }

            val showTvdbId =
                SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTvdbId(showId)
            if (showTvdbId == 0) {
                return@withContext true
            }

            val show = Show()
            show.tvdbId = showTvdbId
            show.isHidden = isHidden

            val success = uploadShowToCloudAsync(show)
            return@withContext !success
        }
        // Do not save to local database if sending to cloud has failed.
        if (isCloudFailed) return

        // Save to local database.
        withContext(Dispatchers.IO) {
            SgRoomDatabase.getInstance(context).sgShow2Helper().setShowHidden(showId, isHidden)

            // FIXME also notify filter URI used by search
            context.contentResolver
                .notifyChange(SeriesGuideContract.Shows.CONTENT_URI_FILTER, null)
        }

        // display info toast
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context, context.getString(
                    if (isHidden)
                        R.string.hidden
                    else
                        R.string.unhidden
                ), Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Saves new notify flag to the local database and, if signed in, up into the cloud as well.
     */
    fun storeNotify(showId: Long, notify: Boolean) {
        SgApp.coroutineScope.launch {
            storeNotifyAsync(showId, notify)
        }
    }

    private suspend fun storeNotifyAsync(showId: Long, notify: Boolean) {
        // Send to cloud.
        val isCloudFailed = withContext(Dispatchers.Default) {
            if (!HexagonSettings.isEnabled(context)) {
                return@withContext false
            }
            if (isNotConnected(context)) {
                return@withContext true
            }
            val showTvdbId =
                SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTvdbId(showId)
            if (showTvdbId == 0) {
                return@withContext true
            }

            val show = Show()
            show.tvdbId = showTvdbId
            show.notify = notify

            val success = uploadShowToCloudAsync(show)
            return@withContext !success
        }
        // Do not save to local database if sending to cloud has failed.
        if (isCloudFailed) return

        // Save to local database.
        withContext(Dispatchers.IO) {
            SgRoomDatabase.getInstance(context).sgShow2Helper().setShowNotify(showId, notify)
        }

        // new notify setting may determine eligibility for notifications
        withContext(Dispatchers.Default) {
            NotificationService.trigger(context)
        }
    }

    /**
     * Removes hidden flag from all hidden shows in the local database and, if signed in, sends to
     * the cloud as well.
     */
    fun storeAllHiddenVisible() {
        SgApp.coroutineScope.launch {
            // Send to cloud.
            val isCloudFailed = withContext(Dispatchers.Default) {
                if (!HexagonSettings.isEnabled(context)) {
                    return@withContext false
                }
                if (isNotConnected(context)) {
                    return@withContext true
                }

                val hiddenShowTvdbIds = withContext(Dispatchers.IO) {
                    SgRoomDatabase.getInstance(context).sgShow2Helper().getHiddenShowsTvdbIds()
                }

                val shows = hiddenShowTvdbIds.map { showTvdbId ->
                    val show = Show()
                    show.tvdbId = showTvdbId
                    show.isHidden = false
                    show
                }

                val success = uploadShowsToCloudAsync(shows)
                return@withContext !success
            }
            // Do not save to local database if sending to cloud has failed.
            if (isCloudFailed) return@launch

            // Save to local database.
            withContext(Dispatchers.IO) {
                SgRoomDatabase.getInstance(context).sgShow2Helper().makeHiddenVisible()
            }
        }
    }

    fun storeLanguage(showId: Long, languageCode: String) = SgApp.coroutineScope.launch {
        // Send to cloud.
        val isCloudFailed = withContext(Dispatchers.Default) {
            if (!HexagonSettings.isEnabled(context)) {
                return@withContext false
            }
            if (isNotConnected(context)) {
                return@withContext true
            }
            val showTvdbId =
                SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTvdbId(showId)
            if (showTvdbId == 0) {
                return@withContext true
            }

            val show = Show()
            show.tvdbId = showTvdbId
            show.language = languageCode

            val success = uploadShowToCloudAsync(show)
            return@withContext !success
        }
        // Do not save to local database if sending to cloud has failed.
        if (isCloudFailed) return@launch

        // Save to local database and schedule sync.
        withContext(Dispatchers.IO) {
            // change language
            val database = SgRoomDatabase.getInstance(context)
            database.sgShow2Helper().updateLanguage(showId, languageCode)
            // reset episode last update time so all get updated
            database.sgEpisode2Helper().resetLastUpdatedForShow(showId)
            // trigger update
            val showTvdbId = database.sgShow2Helper().getShowTvdbId(showId)
            SgSyncAdapter.requestSyncSingleImmediate(context, false, showTvdbId)
        }

        withContext(Dispatchers.Main) {
            // show immediate feedback, also if offline and sync won't go through
            if (AndroidUtils.isNetworkConnected(context)) {
                // notify about upcoming sync
                Toast.makeText(context, R.string.update_scheduled, Toast.LENGTH_SHORT).show()
            } else {
                // offline
                Toast.makeText(context, R.string.update_no_connection, Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun isNotConnected(context: Context): Boolean {
        val isConnected = AndroidUtils.isNetworkConnected(context)
        // display offline toast
        if (!isConnected) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, R.string.offline, Toast.LENGTH_LONG).show()
            }
        }
        return !isConnected
    }

    fun uploadShowToCloud(show: Show) {
        SgApp.coroutineScope.launch {
            uploadShowToCloudAsync(show)
        }
    }

    private suspend fun uploadShowToCloudAsync(show: Show): Boolean {
        return uploadShowsToCloudAsync(listOf(show))
    }

    private suspend fun uploadShowsToCloudAsync(shows: List<Show>): Boolean {
        return withContext(Dispatchers.IO) {
            HexagonShowSync(context, SgApp.getServicesComponent(context).hexagonTools())
                .upload(shows)
        }
    }

}