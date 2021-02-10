package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.widget.Toast
import androidx.collection.SparseArrayCompat
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.enums.NetworkResult
import com.battlelancer.seriesguide.enums.Result
import com.battlelancer.seriesguide.model.SgShow2
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.service.NotificationService
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.sync.HexagonEpisodeSync
import com.battlelancer.seriesguide.sync.HexagonShowSync
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.battlelancer.seriesguide.traktapi.TraktTools2
import com.battlelancer.seriesguide.ui.shows.ShowTools.Status
import com.battlelancer.seriesguide.util.DBUtils
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.trakt5.entities.BaseShow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import com.uwetrottmann.seriesguide.backend.shows.model.Show as CloudShow

/**
 * Provides some show operations as (async) suspend functions, running within global scope.
 */
class ShowTools2(val showTools: ShowTools, val context: Context) {

    enum class ShowResult {
        SUCCESS,
        IN_DATABASE,
        DOES_NOT_EXIST,
        TMDB_ERROR,
        TRAKT_ERROR,
        HEXAGON_ERROR,
        DATABASE_ERROR
    }

    /**
     * Gets row ID of a show by TMDB id first, then if given by TVDB id. Null if not in database.
     */
    fun getShowId(showTmdbId: Int, showTvdbId: Int?): Long? {
        val helper = SgRoomDatabase.getInstance(context).sgShow2Helper()

        val showIdByTmdbId = helper.getShowIdByTmdbId(showTmdbId)
        if (showIdByTmdbId > 0) return showIdByTmdbId

        if (showTvdbId != null) {
            val showIdByTvdbId = helper.getShowIdByTvdbId(showTvdbId)
            if (showIdByTvdbId > 0) return showIdByTvdbId
        }

        return null
    }

    fun getShowDetails(showTmdbId: Int, desiredLanguage: String): Pair<SgShow2?, ShowResult> {
        val tmdbResult = TmdbTools2().getShowAndExternalIds(showTmdbId, desiredLanguage, context)
        var tmdbShow = tmdbResult.first ?: return Pair(
            null,
            if (tmdbResult.second) ShowResult.DOES_NOT_EXIST else ShowResult.TMDB_ERROR
        )

        val noTranslation = tmdbShow.overview.isNullOrEmpty()
        if (noTranslation) {
            val tmdbResultFallback = TmdbTools2().getShowAndExternalIds(
                showTmdbId,
                DisplaySettings.getShowsLanguageFallback(context),
                context
            )
            tmdbShow = tmdbResultFallback.first ?: return Pair(
                null,
                if (tmdbResultFallback.second) ShowResult.DOES_NOT_EXIST else ShowResult.TMDB_ERROR
            )
        }

        val traktResult = TraktTools2.getShowByTmdbId(showTmdbId, context)
        // Fail if looking up Trakt details failed to avoid removing them for existing shows.
        if (traktResult.failed) return Pair(null, ShowResult.TRAKT_ERROR)
        val traktShow = traktResult.show
        if (traktShow == null) {
            Timber.w("getShowDetails: no Trakt show found, using default values.")
        }

        val title = if (tmdbShow.name.isNullOrEmpty()) {
            context.getString(R.string.no_translation_title)
        } else {
            tmdbShow.name
        }

        val overview = if (noTranslation || tmdbShow.overview.isNullOrEmpty()) {
            // add note about non-translated or non-existing overview
            var overview = context.getString(
                R.string.no_translation,
                LanguageTools.getShowLanguageStringFor(context, desiredLanguage),
                context.getString(R.string.tmdb)
            )
            // if there is one, append non-translated overview
            if (!tmdbShow.overview.isNullOrEmpty()) {
                overview += "\n\n${tmdbShow.overview}"
            }
            overview
        } else {
            tmdbShow.overview
        }

        val rating = traktShow?.rating?.let { if (it in 0.0..10.0) it else 0.0 }

        return Pair(
            SgShow2(
                tmdbId = tmdbShow.id,
                tvdbId = tmdbShow.external_ids?.tvdb_id ?: 0,
                traktId = traktShow?.ids?.trakt,
                title = title,
                titleNoArticle = DBUtils.trimLeadingArticle(tmdbShow.name),
                overview = overview,
                releaseTime = TimeTools.parseShowReleaseTime(traktShow?.airs?.time),
                releaseWeekDay = TimeTools.parseShowReleaseWeekDay(traktShow?.airs?.day),
                releaseCountry = traktShow?.country,
                releaseTimeZone = traktShow?.airs?.timezone,
                firstRelease = TimeTools.parseShowFirstRelease(traktShow?.first_aired),
                ratingGlobal = rating ?: 0.0,
                genres = TextTools.mendTvdbStrings(tmdbShow.genres?.map { genre -> genre.name }),
                network = tmdbShow.networks?.firstOrNull()?.name ?: "",
                imdbId = tmdbShow.external_ids?.imdb_id ?: "",
                runtime = tmdbShow.episode_run_time?.first() ?: 45, // estimate 45 minutes if none.
                status = when (tmdbShow.status) {
                    "Returning Series" -> Status.CONTINUING
                    "Planned" -> Status.UPCOMING
                    "Pilot" -> Status.PILOT
                    "Ended" -> Status.ENDED
                    "Canceled" -> Status.CANCELED
                    "In Production" -> Status.IN_PRODUCTION
                    else -> Status.UNKNOWN
                },
                poster = tmdbShow.poster_path ?: "",
                posterSmall = tmdbShow.poster_path ?: "",
                // set desired language, might not be the content language if fallback used above.
                language = desiredLanguage
            ), ShowResult.SUCCESS
        )
    }

    /**
     * Decodes the [ShowTools.Status] and returns the localized text representation.
     * May be `null` if status is unknown.
     */
    fun getStatus(encodedStatus: Int): String? {
        return when (encodedStatus) {
            Status.IN_PRODUCTION -> context.getString(R.string.show_status_in_production)
            Status.PILOT -> context.getString(R.string.show_status_pilot)
            Status.CANCELED -> context.getString(R.string.show_status_canceled)
            Status.UPCOMING -> context.getString(R.string.show_isUpcoming)
            Status.CONTINUING -> context.getString(R.string.show_isalive)
            Status.ENDED -> context.getString(R.string.show_isnotalive)
            else -> {
                // status unknown, display nothing
                null
            }
        }
    }

    fun addShow(
        showTmdbId: Int,
        desiredLanguage: String?,
        traktCollection: Map<Int, BaseShow>?,
        traktWatched: Map<Int, BaseShow>?,
        hexagonEpisodeSync: HexagonEpisodeSync
    ): ShowResult {
        // Do nothing if TMDB ID already in database.
        if (getShowId(showTmdbId, null) != null) return ShowResult.IN_DATABASE

        val language = desiredLanguage ?: DisplaySettings.LANGUAGE_EN

        val result = getShowDetails(showTmdbId, language)
        if (result.second != ShowResult.SUCCESS) return result.second
        val show = result.first!!

        // Check again if in database using TVDB id, show might not have TMDB id, yet.
        if (getShowId(showTmdbId, show.tvdbId) != null) return ShowResult.IN_DATABASE

        // Restore properties from Hexagon
        if (show.tvdbId != null && HexagonSettings.isEnabled(context)) {
            val hexagonResult = SgApp.getServicesComponent(context).hexagonTools()
                .getShow(show.tvdbId)
            if (!hexagonResult.second) return ShowResult.HEXAGON_ERROR
            val hexagonShow = hexagonResult.first
            if (hexagonShow != null) {
                if (hexagonShow.isFavorite != null) {
                    show.favorite = hexagonShow.isFavorite
                }
                if (hexagonShow.notify != null) {
                    show.notify = hexagonShow.notify
                }
                if (hexagonShow.isHidden != null) {
                    show.hidden = hexagonShow.isHidden
                }
            }
        }

        // TODO Get episodes; store to database; restore episode flags from Cloud/Trakt
        val showId = SgRoomDatabase.getInstance(context).sgShow2Helper().insertShow(show)
        if (showId == -1L) return ShowResult.DATABASE_ERROR

        // Calculate next episode
        DBUtils.updateLatestEpisode(context, showId)

        return ShowResult.SUCCESS
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
            val showTvdbId =
                SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTvdbId(showId)
            if (showTvdbId == 0) {
                return@withContext true
            }

            // Sets the isRemoved flag of the given show on Hexagon, so the show will
            // not be auto-added on any device connected to Hexagon.
            val show = CloudShow()
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

            val show = CloudShow()
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

            val showTvdbId =
                SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTvdbId(showId)
            if (showTvdbId == 0) {
                return@withContext true
            }

            val show = CloudShow()
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

            val show = CloudShow()
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
                    val show = CloudShow()
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

            val show = CloudShow()
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

    fun uploadShowToCloud(show: CloudShow) {
        SgApp.coroutineScope.launch {
            uploadShowToCloudAsync(show)
        }
    }

    private suspend fun uploadShowToCloudAsync(show: CloudShow): Boolean {
        return uploadShowsToCloudAsync(listOf(show))
    }

    private suspend fun uploadShowsToCloudAsync(shows: List<CloudShow>): Boolean {
        return withContext(Dispatchers.IO) {
            HexagonShowSync(context, SgApp.getServicesComponent(context).hexagonTools())
                .upload(shows)
        }
    }

    fun getTmdbIdsToPoster(context: Context): SparseArrayCompat<String> {
        val shows = SgRoomDatabase.getInstance(context).sgShow2Helper().getShowsMinimal()
        val map = SparseArrayCompat<String>()
        shows.forEach {
            if (it.tmdbId != null && it.tmdbId != 0) {
                map.put(it.tmdbId, it.posterSmall)
            }
        }
        return map
    }

}