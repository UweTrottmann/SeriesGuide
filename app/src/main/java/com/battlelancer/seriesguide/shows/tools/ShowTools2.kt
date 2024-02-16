// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.tools

import android.content.Context
import android.widget.Toast
import androidx.collection.SparseArrayCompat
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.enums.NetworkResult
import com.battlelancer.seriesguide.modules.ApplicationContext
import com.battlelancer.seriesguide.notifications.NotificationService
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.sync.HexagonShowSync
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.seriesguide.backend.shows.model.SgCloudShow
import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject
import kotlin.collections.set
import com.battlelancer.seriesguide.enums.Result as SgResult

/**
 * Provides some show operations as (async) suspend functions, running within global scope.
 */
class ShowTools2 @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val hexagonShowSync: Lazy<HexagonShowSync>
) {

    /**
     * Gets row ID of a show by TMDB id first, then if given by TVDB id and null TMDB id (show is
     * not migrated, yet). Null if not in database or matched by TVDB id, but has different TMDB id.
     */
    fun getShowId(showTmdbId: Int, showTvdbId: Int?): Long? {
        val helper = SgRoomDatabase.getInstance(context).sgShow2Helper()

        val showIdByTmdbId = helper.getShowIdByTmdbId(showTmdbId)
        if (showIdByTmdbId > 0) return showIdByTmdbId

        if (showTvdbId != null) {
            // Note: TVDB might have a single show that is split into two or more shows on TMDB,
            // so on TMDB the same TVDB is linked for both. To not prevent adding the second one,
            // only return show ID if it was not migrated to avoid adding a duplicate show.
            val showIdByTvdbId = helper.getShowIdByTvdbIdWithNullTmdbId(showTvdbId)
            if (showIdByTvdbId > 0) return showIdByTvdbId
        }

        return null
    }

    /**
     * Returns the Trakt id of a show, or `null` if it is invalid or there is none.
     */
    fun getShowTraktId(showId: Long): Int? {
        val traktIdOrZero = SgRoomDatabase.getInstance(context).sgShow2Helper()
            .getShowTraktId(showId)
        return if (traktIdOrZero <= 0) {
            null
        } else {
            traktIdOrZero
        }
    }

    /**
     * Posted if a show is about to get removed.
     */
    data class OnRemovingShowEvent(val showId: Long)

    /**
     * Posted if show was just removed (or failure).
     */
    data class OnShowRemovedEvent(
        val showTmdbId: Int,
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

            // Get TMDB id for OnShowRemovedEvent before removing show.
            val showTmdbId = withContext(Dispatchers.IO) {
                SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTmdbId(showId)
            }

            val result = removeShowAsync(showId)

            withContext(Dispatchers.Main) {
                if (result == NetworkResult.OFFLINE) {
                    Toast.makeText(context, R.string.offline, Toast.LENGTH_LONG).show()
                } else if (result == NetworkResult.ERROR) {
                    Toast.makeText(context, R.string.delete_error, Toast.LENGTH_LONG).show()
                }
                EventBus.getDefault().post(OnShowRemovedEvent(showTmdbId, result))
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

            val showTmdbId =
                SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTmdbId(showId)
            if (showTmdbId == 0) {
                // If this is a legacy show (has TVDB ID but no TMDB ID), still allow removal
                // from local database. Do not send to Cloud but pretend no failure.
                val showTvdbId =
                    SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTvdbId(showId)
                return@withContext showTvdbId == 0
            }

            // Sets the isRemoved flag of the given show on Hexagon, so the show will
            // not be auto-added on any device connected to Hexagon.
            val show = SgCloudShow()
            show.tmdbId = showTmdbId
            show.isRemoved = true

            val success = uploadShowToCloud(show)
            return@withContext !success
        }
        // Do not save to local database if sending to cloud has failed.
        if (isCloudFailed) return SgResult.ERROR

        return withContext(Dispatchers.IO) {
            // Remove database entries in stages, so if an earlier stage fails,
            // user can try again. Also saves memory by using smaller database transactions.
            val database = SgRoomDatabase.getInstance(context)

            var rowsUpdated = database.sgEpisode2Helper().deleteEpisodesOfShow(showId)
            if (rowsUpdated == -1) return@withContext SgResult.ERROR

            rowsUpdated = database.sgSeason2Helper().deleteSeasonsOfShow(showId)
            if (rowsUpdated == -1) return@withContext SgResult.ERROR

            rowsUpdated = database.sgShow2Helper().deleteShow(showId)
            if (rowsUpdated == -1) return@withContext SgResult.ERROR

            database.sgWatchProviderHelper().deleteShowMappings(showId)

            SeriesGuideDatabase.rebuildFtsTable(context)
            SgResult.SUCCESS
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

            val showTmdbId =
                SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTmdbId(showId)
            if (showTmdbId == 0) {
                return@withContext true
            }

            val show = SgCloudShow()
            show.tmdbId = showTmdbId
            show.isFavorite = isFavorite

            val success = uploadShowToCloud(show)
            return@withContext !success
        }
        // Do not save to local database if sending to cloud has failed.
        if (isCloudFailed) return

        // Save to local database.
        withContext(Dispatchers.IO) {
            SgRoomDatabase.getInstance(context).sgShow2Helper().setShowFavorite(showId, isFavorite)

            // Also notify URI used by lists.
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

            val showTmdbId =
                SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTmdbId(showId)
            if (showTmdbId == 0) {
                return@withContext true
            }

            val show = SgCloudShow()
            show.tmdbId = showTmdbId
            show.isHidden = isHidden

            val success = uploadShowToCloud(show)
            return@withContext !success
        }
        // Do not save to local database if sending to cloud has failed.
        if (isCloudFailed) return

        // Save to local database.
        withContext(Dispatchers.IO) {
            SgRoomDatabase.getInstance(context).sgShow2Helper().setShowHidden(showId, isHidden)
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

            val showTmdbId =
                SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTmdbId(showId)
            if (showTmdbId == 0) {
                return@withContext true
            }

            val show = SgCloudShow()
            show.tmdbId = showTmdbId
            show.notify = notify

            val success = uploadShowToCloud(show)
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

                val hiddenShowTmdbIds = withContext(Dispatchers.IO) {
                    SgRoomDatabase.getInstance(context).sgShow2Helper().getHiddenShowsTmdbIds()
                }

                val shows = hiddenShowTmdbIds.map { tmdbId ->
                    val show = SgCloudShow()
                    show.tmdbId = tmdbId
                    show.isHidden = false
                    show
                }

                val success = withContext(Dispatchers.IO) {
                    hexagonShowSync.get().upload(shows)
                }
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
            val showTmdbId =
                SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTmdbId(showId)
            if (showTmdbId == 0) {
                return@withContext true
            }

            val show = SgCloudShow()
            show.tmdbId = showTmdbId
            show.language = languageCode

            val success = uploadShowToCloud(show)
            return@withContext !success
        }
        // Do not save to local database if sending to cloud has failed.
        if (isCloudFailed) return@launch

        // Save to local database and schedule sync.
        withContext(Dispatchers.IO) {
            // change language
            SgRoomDatabase.getInstance(context).sgShow2Helper().updateLanguage(showId, languageCode)
            ShowSync.triggerFullSync(context, showId)
        }

        notifyAboutSyncing()
    }

    /**
     * Uploads to Cloud and on success saves to local database.
     * Does not sanitize the given values.
     */
    fun storeCustomReleaseTime(
        showId: Long, customReleaseTime: Int,
        customReleaseDayOffset: Int,
        customReleaseTimeZone: String
    ) = SgApp.coroutineScope.launch {
        // Send to Cloud.
        val isCloudFailed = withContext(Dispatchers.Default) {
            if (!HexagonSettings.isEnabled(context)) {
                return@withContext false
            }
            if (isNotConnected(context)) {
                return@withContext true
            }
            val showTmdbId =
                SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTmdbId(showId)
            if (showTmdbId == 0) {
                return@withContext true
            }

            val show = SgCloudShow()
            show.tmdbId = showTmdbId
            show.customReleaseTime = customReleaseTime
            show.customReleaseDayOffset = customReleaseDayOffset
            show.customReleaseTimeZone = customReleaseTimeZone

            val success = uploadShowToCloud(show)
            return@withContext !success
        }
        // Do not save to local database if sending to cloud has failed.
        if (isCloudFailed) return@launch

        // Save to local database and schedule sync.
        withContext(Dispatchers.IO) {
            // Change custom release time values
            SgRoomDatabase.getInstance(context).sgShow2Helper().updateCustomReleaseTime(
                showId,
                customReleaseTime,
                customReleaseDayOffset,
                customReleaseTimeZone
            )
            ShowSync.triggerFullSync(context, showId)
        }

        notifyAboutSyncing()
    }

    private suspend fun notifyAboutSyncing() {
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

    private suspend fun uploadShowToCloud(show: SgCloudShow): Boolean {
        return hexagonShowSync.get().upload(show)
    }

    fun getTmdbIdsToPoster(): SparseArrayCompat<String> {
        val shows = SgRoomDatabase.getInstance(context).sgShow2Helper().getShowsMinimal()
        val map = SparseArrayCompat<String>()
        shows.forEach {
            if (it.tmdbId != null && it.tmdbId != 0) {
                map.put(it.tmdbId, it.posterSmall)
            }
        }
        return map
    }

    fun getTmdbIdsToShowIds(): Map<Int, Long> {
        val showIds = SgRoomDatabase.getInstance(context).sgShow2Helper().getShowIds()
        val map = mutableMapOf<Int, Long>()
        showIds.forEach {
            if (it.tmdbId != null) map[it.tmdbId] = it.id
        }
        return map
    }

}
