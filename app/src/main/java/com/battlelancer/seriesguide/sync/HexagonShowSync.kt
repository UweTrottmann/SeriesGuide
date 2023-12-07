// SPDX-License-Identifier: Apache-2.0
// Copyright 2017-2023 Uwe Trottmann

package com.battlelancer.seriesguide.sync

import android.content.Context
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.backend.HexagonTools
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.modules.ApplicationContext
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgShow2CloudUpdate
import com.battlelancer.seriesguide.shows.search.discover.SearchResult
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.battlelancer.seriesguide.util.Errors.Companion.logAndReportHexagon
import com.battlelancer.seriesguide.util.LanguageTools
import com.github.michaelbull.result.getOrElse
import com.google.api.client.util.DateTime
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.seriesguide.backend.shows.model.SgCloudShow
import com.uwetrottmann.seriesguide.backend.shows.model.SgCloudShowList
import com.uwetrottmann.seriesguide.backend.shows.model.Show
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.util.LinkedList
import javax.inject.Inject
import kotlin.collections.set

class HexagonShowSync @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val hexagonTools: HexagonTools
) {

    /**
     * Downloads shows from Hexagon and updates existing shows with new property values. Any
     * shows not yet in the local database, determined by the given TMDB ID map, will be added
     * to the given map.
     *
     * When merging shows (e.g. just signed in) also downloads legacy cloud shows.
     */
    fun download(
        tmdbIdsToShowIds: Map<Int, Long>,
        toAdd: HashMap<Int, SearchResult>,
        hasMergedShows: Boolean
    ): Boolean {
        val updates: MutableList<SgShow2CloudUpdate> = ArrayList()
        val toUpdate = mutableSetOf<Long>()
        val removed = mutableSetOf<Int>()
        val currentTime = System.currentTimeMillis()
        val lastSyncTime = DateTime(HexagonSettings.getLastShowsSyncTime(context))

        if (hasMergedShows) {
            Timber.d("download: changed shows since %s", lastSyncTime)
        } else {
            Timber.d("download: all shows")
        }

        val success = downloadShows(
            updates, toUpdate, removed, toAdd, tmdbIdsToShowIds,
            hasMergedShows, lastSyncTime
        )
        if (!success) return false
        // When just signed in, try to get legacy cloud shows. Get changed shows only via TMDB ID
        // to encourage users to update the app.
        if (!hasMergedShows) {
            val successLegacy =
                downloadLegacyShows(updates, toUpdate, removed, toAdd, tmdbIdsToShowIds)
            if (!successLegacy) return false
        }

        // Apply all updates
        SgRoomDatabase.getInstance(context).sgShow2Helper().updateForCloudUpdate(updates)
        if (hasMergedShows) {
            // set new last sync time
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong(HexagonSettings.KEY_LAST_SYNC_SHOWS, currentTime)
                .apply()
        }
        return true
    }

    private fun downloadShows(
        updates: MutableList<SgShow2CloudUpdate>,
        toUpdate: MutableSet<Long>,
        removed: MutableSet<Int>,
        toAdd: HashMap<Int, SearchResult>,
        tmdbIdsToShowIds: Map<Int, Long>,
        hasMergedShows: Boolean,
        lastSyncTime: DateTime
    ): Boolean {
        var cursor: String? = null
        var hasMoreShows = true
        while (hasMoreShows) {
            // abort if connection is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                Timber.e("download: no network connection")
                return false
            }

            var shows: List<SgCloudShow>?
            try {
                // get service each time to check if auth was removed
                val showsService = hexagonTools.showsService ?: return false
                val request = showsService.sgShows // use default server limit
                if (hasMergedShows) {
                    // only get changed shows (otherwise returns all)
                    request.updatedSince = lastSyncTime
                }
                if (!cursor.isNullOrEmpty()) {
                    request.cursor = cursor
                }
                val response = request.execute()
                if (response == null) {
                    // If empty API sends status 200 and empty list, so no body is a failure.
                    Timber.e("download: response was null")
                    return false
                }
                shows = response.shows

                // check for more items
                if (response.cursor != null) {
                    cursor = response.cursor
                } else {
                    hasMoreShows = false
                }
            } catch (e: IOException) {
                logAndReportHexagon("get shows", e)
                return false
            } catch (e: IllegalArgumentException) {
                // Note: JSON parser may throw IllegalArgumentException.
                logAndReportHexagon("get shows", e)
                return false
            }
            if (shows == null || shows.isEmpty()) {
                // nothing to do here
                break
            }

            // append updates for received shows if there isn't one,
            // or appends shows not added locally
            appendShowUpdates(
                updates,
                toUpdate,
                removed,
                toAdd,
                shows,
                tmdbIdsToShowIds,
                !hasMergedShows
            )
        }
        return true
    }

    private fun downloadLegacyShows(
        updates: MutableList<SgShow2CloudUpdate>,
        toUpdate: MutableSet<Long>,
        removed: MutableSet<Int>,
        toAdd: HashMap<Int, SearchResult>,
        tmdbIdsToShowIds: Map<Int, Long>
    ): Boolean {
        var cursor: String? = null
        var hasMoreShows = true
        while (hasMoreShows) {
            // abort if connection is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                Timber.e("download: no network connection")
                return false
            }

            var legacyShows: List<Show>?
            try {
                // get service each time to check if auth was removed
                val showsService = hexagonTools.showsService ?: return false
                val request = showsService.get() // use default server limit
                if (!cursor.isNullOrEmpty()) {
                    request.cursor = cursor
                }
                val response = request.execute()
                if (response == null) {
                    // If empty API sends status 200 and empty list, so no body is a failure.
                    Timber.e("download: response was null")
                    return false
                }
                legacyShows = response.shows

                // check for more items
                if (response.cursor != null) {
                    cursor = response.cursor
                } else {
                    hasMoreShows = false
                }
            } catch (e: IOException) {
                logAndReportHexagon("get legacy shows", e)
                return false
            } catch (e: IllegalArgumentException) {
                // Note: JSON parser may throw IllegalArgumentException.
                logAndReportHexagon("get legacy shows", e)
                return false
            }
            if (legacyShows == null || legacyShows.isEmpty()) {
                // nothing to do here
                break
            }
            val shows = mapLegacyShows(legacyShows) ?: return false

            // append updates for received shows if there isn't one,
            // or appends shows not added locally
            appendShowUpdates(
                updates,
                toUpdate,
                removed,
                toAdd,
                shows,
                tmdbIdsToShowIds,
                true
            )
        }
        return true
    }

    /**
     * Returns null on network error while looking up TMDB ID.
     */
    private fun mapLegacyShows(legacyShows: List<Show>): List<SgCloudShow>? {
        val shows: MutableList<SgCloudShow> = ArrayList()
        for (legacyShow in legacyShows) {
            val showTvdbId = legacyShow.tvdbId
            if (showTvdbId == null || showTvdbId <= 0) {
                continue
            }
            val showTmdbIdOrNull = TmdbTools2().findShowTmdbId(context, showTvdbId)
                .getOrElse {
                    // Network or API error, abort.
                    return null
                }
            // Only add if TMDB id found
            if (showTmdbIdOrNull != null) {
                val show = SgCloudShow()
                show.tmdbId = showTmdbIdOrNull
                show.isRemoved = legacyShow.isRemoved
                show.isFavorite = legacyShow.isFavorite
                show.notify = legacyShow.notify
                show.isHidden = legacyShow.isHidden
                show.language = legacyShow.language
                shows.add(show)
            }
        }
        return shows
    }

    private fun appendShowUpdates(
        updates: MutableList<SgShow2CloudUpdate>,
        toUpdate: MutableSet<Long>,
        removed: MutableSet<Int>,
        toAdd: MutableMap<Int, SearchResult>,
        shows: List<SgCloudShow>,
        tmdbIdsToShowIds: Map<Int, Long>,
        mergeValues: Boolean
    ) {
        for (show in shows) {
            // schedule to add shows not in local database
            val showTmdbId = show.tmdbId ?: continue // Invalid data.

            val showIdOrNull = tmdbIdsToShowIds[showTmdbId]
            if (showIdOrNull == null) {
                // ...but do NOT add shows marked as removed
                if (removed.contains(showTmdbId)) {
                    continue
                }
                if (show.isRemoved != null && show.isRemoved) {
                    removed.add(showTmdbId)
                    continue
                }
                if (!toAdd.containsKey(showTmdbId)) {
                    val item = SearchResult()
                    item.tmdbId = showTmdbId
                    item.language = show.language?.let { LanguageTools.mapLegacyShowCode(it) }
                    item.title = ""
                    toAdd[showTmdbId] = item
                }
            } else if (!toUpdate.contains(showIdOrNull)) {
                // Create update if there isn't already one.
                val update = SgRoomDatabase.getInstance(context)
                    .sgShow2Helper()
                    .getForCloudUpdate(showIdOrNull)
                if (update != null) {
                    // The main use case considered for a merge here is one device not being
                    // connected to Cloud for some time (credentials expired, explicitly
                    // disconnected): in this case try to keep changes that have less bad
                    // side-effects (e.g. do not turn off notifications) and are easy to revert
                    // (e.g. remove one of a few favorites).
                    var hasUpdates = false
                    if (show.isFavorite != null) {
                        // When merging, add a show to favorites, but never remove it:
                        // assuming there are only a few favorite shows and many others,
                        // losing a favorite show is bad and it's easy to remove a few again.
                        if (!mergeValues || show.isFavorite) {
                            update.favorite = show.isFavorite
                            hasUpdates = true
                        }
                    }
                    if (show.notify != null) {
                        // When merging, enable notifications, but never disable them:
                        // assuming a user does not regularly check the notify setting,
                        // missing notifications is bad and it's easy to turn them off again.
                        if (!mergeValues || show.notify) {
                            update.notify = show.notify
                            hasUpdates = true
                        }
                    }
                    if (show.isHidden != null) {
                        // When merging, un-hide shows, but never hide them:
                        // assuming a user does not regularly check which shows are hidden,
                        // hiding a show is bad and it's easy to hide one again.
                        if (!mergeValues || !show.isHidden) {
                            update.hidden = show.isHidden
                            hasUpdates = true
                        }
                    }
                    // If below properties have changed (new value != old value), should
                    // trigger a sync for this show.
                    var scheduleShowUpdate = false
                    if (!show.language.isNullOrEmpty() && show.language != update.language) {
                        // Always overwrite with hexagon language value.
                        // Local shows might use a default value and it's easy to change.
                        update.language = show.language
                        scheduleShowUpdate = true
                    }
                    if (show.customReleaseTime != null
                        && show.customReleaseTime != update.customReleaseTime) {
                        // Always overwrite with hexagon custom time value.
                        // Local shows might use a default value.
                        update.customReleaseTime = show.customReleaseTime
                        scheduleShowUpdate = true
                    }
                    if (show.customReleaseDayOffset != null
                        && show.customReleaseDayOffset != update.customReleaseDayOffset) {
                        // Always overwrite with hexagon custom time value.
                        // Local shows might use a default value.
                        update.customReleaseDayOffset = show.customReleaseDayOffset
                        scheduleShowUpdate = true
                    }
                    if (show.customReleaseTimeZone != null
                        && show.customReleaseTimeZone != update.customReleaseTimeZone) {
                        // Always overwrite with hexagon custom time value.
                        // Local shows might use a default value.
                        update.customReleaseTimeZone = show.customReleaseTimeZone
                        scheduleShowUpdate = true
                    }
                    if (scheduleShowUpdate) {
                        // Mark the show as never updated so it will be on the next sync.
                        update.lastUpdatedMs = 0
                        hasUpdates = true
                    }
                    if (hasUpdates) {
                        updates.add(update)
                        toUpdate.add(showIdOrNull)
                    }
                }
            }
        }
    }

    /**
     * Uploads all local shows to Hexagon.
     */
    fun uploadAll(): Boolean {
        Timber.d("uploadAll: uploading all shows")
        val forCloudUpdate = SgRoomDatabase.getInstance(context)
            .sgShow2Helper()
            .getForCloudUpdate()
        val shows: MutableList<SgCloudShow> = LinkedList()
        for (localShow in forCloudUpdate) {
            val tmdbId = localShow.tmdbId ?: continue
            val cloudShow = SgCloudShow()
            cloudShow.tmdbId = tmdbId
            cloudShow.isFavorite = localShow.favorite
            cloudShow.notify = localShow.favorite
            cloudShow.isHidden = localShow.hidden
            cloudShow.language = localShow.language
            cloudShow.customReleaseTime = localShow.customReleaseTime
            cloudShow.customReleaseDayOffset = localShow.customReleaseDayOffset
            cloudShow.customReleaseTimeZone = localShow.customReleaseTimeZone
            shows.add(cloudShow)
        }
        if (shows.size == 0) {
            Timber.d("uploadAll: no shows to upload")
            // nothing to upload
            return true
        }
        return upload(shows)
    }

    /**
     * Uploads the given list of shows to Hexagon.
     */
    fun upload(shows: List<SgCloudShow>): Boolean {
        if (shows.isEmpty()) {
            Timber.d("upload: no shows to upload")
            return true
        }
        // Issues with some requests failing at Cloud due to
        // EOFException: Unexpected end of ZLIB input stream
        // Using info log to report sizes that are uploaded to determine
        // if there is need for batching.
        // https://github.com/UweTrottmann/SeriesGuide/issues/781
        Timber.i("upload: %d shows", shows.size)

        // Upload in batches
        shows.chunked(MAX_BATCH_SIZE).forEach { chunk ->
            val wrapper = SgCloudShowList()
            wrapper.shows = chunk
            try {
                // get service each time to check if auth was removed
                val showsService = hexagonTools.showsService ?: return false
                showsService.saveSgShows(wrapper).execute()
            } catch (e: IOException) {
                logAndReportHexagon("save shows", e)
                return false
            }
        }

        return true
    }

    suspend fun upload(show: SgCloudShow): Boolean {
        return withContext(Dispatchers.IO) {
            upload(listOf(show))
        }
    }

    companion object {
        // See API documentation on list size limit.
        const val MAX_BATCH_SIZE = 500
    }

}
