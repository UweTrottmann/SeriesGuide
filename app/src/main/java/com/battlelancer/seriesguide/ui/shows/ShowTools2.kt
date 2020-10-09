package com.battlelancer.seriesguide.ui.shows

import android.content.ContentValues
import android.content.Context
import android.widget.Toast
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.service.NotificationService
import com.battlelancer.seriesguide.sync.HexagonShowSync
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.seriesguide.backend.shows.model.Show
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Provides some show operations as (async) suspend functions, running within global scope.
 */
class ShowTools2(val showTools: ShowTools, val context: Context) {

    /**
     * Saves new favorite flag to the local database and, if signed in, up into the cloud as well.
     */
    fun storeIsFavorite(showTvdbId: Int, isFavorite: Boolean) {
        SgApp.coroutineScope.launch {
            storeIsFavoriteAsync(showTvdbId, isFavorite)
        }
    }

    private suspend fun storeIsFavoriteAsync(showTvdbId: Int, isFavorite: Boolean) {
        // Send to cloud.
        val isCloudFailed = withContext(Dispatchers.Default) {
            if (!HexagonSettings.isEnabled(context)) {
                return@withContext false
            }
            if (isNotConnected(context)) {
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
            val values = ContentValues()
            values.put(SeriesGuideContract.Shows.FAVORITE, if (isFavorite) 1 else 0)
            context.contentResolver.update(
                SeriesGuideContract.Shows.buildShowUri(showTvdbId), values, null, null
            )

            // also notify URIs used by search and lists
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
    fun storeIsHidden(showTvdbId: Int, isHidden: Boolean) {
        SgApp.coroutineScope.launch {
            storeIsHiddenAsync(showTvdbId, isHidden)
        }
    }

    private suspend fun storeIsHiddenAsync(showTvdbId: Int, isHidden: Boolean) {
        // Send to cloud.
        val isCloudFailed = withContext(Dispatchers.Default) {
            if (!HexagonSettings.isEnabled(context)) {
                return@withContext false
            }
            if (isNotConnected(context)) {
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
            val values = ContentValues()
            values.put(SeriesGuideContract.Shows.HIDDEN, if (isHidden) 1 else 0)
            context.contentResolver.update(
                SeriesGuideContract.Shows.buildShowUri(showTvdbId), values, null, null
            )

            // also notify filter URI used by search
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
    fun storeNotify(showTvdbId: Int, notify: Boolean) {
        SgApp.coroutineScope.launch {
            storeNotifyAsync(showTvdbId, notify)
        }
    }

    private suspend fun storeNotifyAsync(showTvdbId: Int, notify: Boolean) {
        // Send to cloud.
        val isCloudFailed = withContext(Dispatchers.Default) {
            if (!HexagonSettings.isEnabled(context)) {
                return@withContext false
            }
            if (isNotConnected(context)) {
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
            val values = ContentValues()
            values.put(SeriesGuideContract.Shows.NOTIFY, if (notify) 1 else 0)
            context.contentResolver.update(
                SeriesGuideContract.Shows.buildShowUri(showTvdbId), values, null, null
            )
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
                    SgRoomDatabase.getInstance(context).showHelper().hiddenShowsTvdbIds
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
                SgRoomDatabase.getInstance(context).showHelper().makeHiddenVisible()
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