// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2025 Uwe Trottmann

package com.battlelancer.seriesguide.dataliberation

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.annotation.IntDef
import androidx.annotation.VisibleForTesting
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.dataliberation.DataLiberationFragment.LiberationResultEvent
import com.battlelancer.seriesguide.dataliberation.model.Episode
import com.battlelancer.seriesguide.dataliberation.model.ListItem
import com.battlelancer.seriesguide.dataliberation.model.Movie
import com.battlelancer.seriesguide.dataliberation.model.Season
import com.battlelancer.seriesguide.dataliberation.model.Show
import com.battlelancer.seriesguide.lists.database.SgListHelper
import com.battlelancer.seriesguide.movies.database.MovieHelper
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgEpisode2Helper
import com.battlelancer.seriesguide.shows.database.SgSeason2Helper
import com.battlelancer.seriesguide.shows.database.SgShow2Helper
import com.battlelancer.seriesguide.shows.episodes.EpisodeTools
import com.battlelancer.seriesguide.util.Errors
import com.battlelancer.seriesguide.util.TextTools
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import kotlin.collections.List
import com.battlelancer.seriesguide.dataliberation.model.List as ExportList

/**
 * Export the show database to a human-readable JSON file on external storage. By default meta-data
 * like descriptions, ratings, actors, etc. will not be included.
 *
 * @param isFullDump Whether to also export meta-data like descriptions, ratings, actors, etc.
 * Increases file size about 2-4 times.
 * @param isAutoBackupMode Whether to run an auto backup, also shows no result toasts.
 */
@Suppress("BlockingMethodInNonBlockingContext")
class JsonExportTask(
    context: Context,
    private val progressListener: OnTaskProgressListener?,
    private val isFullDump: Boolean,
    private val isAutoBackupMode: Boolean,
    private val type: Int?,
    private val sgShow2Helper: SgShow2Helper,
    private val sgSeason2Helper: SgSeason2Helper,
    private val sgEpisode2Helper: SgEpisode2Helper,
    private val sgListHelper: SgListHelper,
    private val movieHelper: MovieHelper
) {

    constructor(
        context: Context,
        progressListener: OnTaskProgressListener?,
        isFullDump: Boolean,
        isAutoBackupMode: Boolean,
        type: Int?,
    ) : this(
        context,
        progressListener,
        isFullDump,
        isAutoBackupMode,
        type,
        SgRoomDatabase.getInstance(context).sgShow2Helper(),
        SgRoomDatabase.getInstance(context).sgSeason2Helper(),
        SgRoomDatabase.getInstance(context).sgEpisode2Helper(),
        SgRoomDatabase.getInstance(context).sgListHelper(),
        SgRoomDatabase.getInstance(context).movieHelper()
    )

    private val context: Context = context.applicationContext

    @VisibleForTesting
    var errorCause: String? = null
        private set

    /**
     * If set will use this file instead of opening via URI, which seems broken with Robolectric
     * (openFileDescriptor throws FileNotFoundException).
     */
    @VisibleForTesting
    var testExportFile: File? = null

    /**
     * Wraps [run] so it can be called from Java code.
     */
    fun launch(): Job {
        return SgApp.coroutineScope.launch {
            run()
        }
    }

    suspend fun run(): Int {
        return withContext(Dispatchers.IO) {
            val result = doInBackground(this)
            onPostExecute(result)
            return@withContext result
        }
    }

    private suspend fun doInBackground(coroutineScope: CoroutineScope): Int {
        return if (isAutoBackupMode) {
            // Auto backup mode.
            try {
                AutoBackupTask(this, context).run(coroutineScope)
                BackupSettings.setAutoBackupErrorOrNull(context, null)
                SUCCESS
            } catch (e: Exception) {
                Errors.logAndReport("Unable to auto backup.", e)
                BackupSettings.setAutoBackupErrorOrNull(
                    context,
                    e.javaClass.simpleName + ": " + e.message
                )
                ERROR
            }
        } else {
            // Manual backup mode.
            if (!coroutineScope.isActive) {
                return ERROR
            }

            var result = SUCCESS
            if (type == null || type == EXPORT_SHOWS) {
                result = exportData(coroutineScope, EXPORT_SHOWS)
                if (result != SUCCESS) {
                    return result
                }
                if (!coroutineScope.isActive) {
                    return ERROR
                }
            }

            if (type == null || type == EXPORT_LISTS) {
                result = exportData(coroutineScope, EXPORT_LISTS)
                if (result != SUCCESS) {
                    return result
                }
                if (!coroutineScope.isActive) {
                    return ERROR
                }
            }

            if (type == null || type == EXPORT_MOVIES) {
                result = exportData(coroutineScope, EXPORT_MOVIES)
            }

            result
        }
    }

    private suspend fun onProgressUpdate(total: Int, completed: Int) {
        withContext(Dispatchers.Main) {
            progressListener?.onProgressUpdate(total, completed)
        }
    }

    private fun onPostExecute(result: Int) {
        if (!isAutoBackupMode) {
            val message: String
            val showIndefinite: Boolean
            when (result) {
                SUCCESS -> {
                    message = context.getString(R.string.status_successful)
                    showIndefinite = false
                }

                ERROR_FILE_ACCESS -> {
                    message = TextTools.dotSeparate(
                        context,
                        R.string.status_failure,
                        R.string.status_failed_file_access
                    )
                    showIndefinite = true
                }

                else -> {
                    message = context.getString(R.string.status_failure)
                    showIndefinite = true
                }
            }
            EventBus.getDefault()
                .post(
                    LiberationResultEvent(
                        context, message, errorCause, showIndefinite
                    )
                )
        } else {
            EventBus.getDefault().post(LiberationResultEvent())
        }
    }

    private suspend fun exportData(coroutineScope: CoroutineScope, @ExportType type: Int): Int {
        // try to export all data
        try {
            val testExportFile = testExportFile
            var pfd: ParcelFileDescriptor? = null
            val out = if (testExportFile == null) {
                // ensure the user has selected a file
                val exportFileUri = getExportFileUri(type)
                    ?: return ERROR_FILE_ACCESS

                pfd = context.contentResolver.openFileDescriptor(exportFileUri, "w")
                    ?: return ERROR_FILE_ACCESS

                FileOutputStream(pfd.fileDescriptor)
            } else {
                FileOutputStream(testExportFile)
            }

            // Even though using streams and FileOutputStream does not append by
            // default, using Storage Access Framework just overwrites existing
            // bytes, potentially leaving old bytes hanging over:
            // so truncate the file first to clear any existing bytes.
            out.channel.truncate(0)

            when (type) {
                EXPORT_SHOWS -> {
                    writeJsonStreamShows(coroutineScope, out)
                }

                EXPORT_LISTS -> {
                    writeJsonStreamLists(coroutineScope, out)
                }

                EXPORT_MOVIES -> {
                    writeJsonStreamMovies(coroutineScope, out)
                }
            }

            // let the document provider know we're done.
            pfd?.close()
        } catch (e: FileNotFoundException) {
            Timber.e(e, "File not found.")
            removeExportFileUri(type)
            errorCause = e.message
            return ERROR_FILE_ACCESS
        } catch (e: IOException) {
            Timber.e(e, "Failed to write to file.")
            removeExportFileUri(type)
            errorCause = e.message
            return ERROR_FILE_ACCESS
        } catch (e: SecurityException) {
            Timber.e(e, "No permission to access file.")
            removeExportFileUri(type)
            errorCause = e.message
            return ERROR_FILE_ACCESS
        } catch (e: JsonParseException) {
            Timber.e(e, "Failed to create JSON export.")
            errorCause = e.message
            return ERROR
        } catch (e: Exception) {
            // Only report unexpected errors.
            Errors.logAndReport("Export failed unexpectedly.", e)
            errorCause = e.message
            return ERROR
        }

        return SUCCESS
    }

    fun getExportFileUri(@ExportType type: Int): Uri? {
        return BackupSettings.getExportFileUri(context, type, isAutoBackupMode)
    }

    fun removeExportFileUri(@ExportType type: Int) {
        BackupSettings.storeExportFileUri(context, type, null, isAutoBackupMode)
    }

    @Throws(IOException::class)
    suspend fun writeJsonStreamShows(coroutineScope: CoroutineScope, out: OutputStream) {
        val shows = sgShow2Helper.getShowsForExport()

        val numTotal = shows.size
        var numExported = 0

        onProgressUpdate(numTotal, 0)

        val gson = Gson()
        val writer = JsonWriter(OutputStreamWriter(out, StandardCharsets.UTF_8))
        writer.beginArray()

        for (sgShow in shows) {
            if (!coroutineScope.isActive) {
                break
            }

            val show = Show()
            show.tmdb_id = sgShow.tmdbId
            show.tvdb_id = sgShow.tvdbId
            show.title = sgShow.title
            show.favorite = sgShow.favorite
            show.notify = sgShow.notify
            show.hidden = sgShow.hidden
            // Note: not mapping legacy language codes so backups can be imported in older versions,
            // which would drop the language if it isn't recognized.
            show.language = sgShow.language
            show.release_time = sgShow.releaseTimeOrDefault
            show.release_weekday = sgShow.releaseWeekDayOrDefault
            show.release_timezone = sgShow.releaseTimeZone
            show.country = sgShow.releaseCountry
            // Note: do net set default values for custom time if never configured, set to null
            // instead. This avoids restoring a backup overwriting values in Cloud on next sync.
            show.custom_release_time = sgShow.customReleaseTime
            show.custom_release_day_offset = sgShow.customReleaseDayOffset
            show.custom_release_timezone = sgShow.customReleaseTimeZone
            show.last_watched_ms = sgShow.lastWatchedMs
            show.poster = sgShow.poster
            show.content_rating = sgShow.contentRating
            show.status = DataLiberationTools.decodeShowStatus(sgShow.statusOrUnknown)
            show.runtime = sgShow.runtime ?: 0
            show.network = sgShow.network
            show.imdb_id = sgShow.imdbId
            show.trakt_id = sgShow.traktId
            show.first_aired = sgShow.firstRelease
            show.rating_user = sgShow.ratingUser
            // Do not export empty or blank text (blank text should never get saved, but don't
            // trust what's in the database) to keep file small and imports fast.
            show.user_note = sgShow.userNote?.ifBlank { null }
            show.user_note_trakt_id = sgShow.userNoteTraktId
            if (isFullDump) {
                show.overview = sgShow.overview
                show.rating_tmdb = sgShow.ratingTmdb
                show.rating_tmdb_votes = sgShow.ratingTmdbVotes
                show.rating = sgShow.ratingTrakt
                show.rating_votes = sgShow.ratingTraktVotes
                show.genres = sgShow.genres
            }

            show.seasons = getSeasons(sgShow.id)

            gson.toJson(show, Show::class.java, writer)

            onProgressUpdate(numTotal, ++numExported)
        }

        writer.endArray()
        writer.close()
    }

    /**
     * Returns possibly empty list of seasons with episodes.
     */
    private fun getSeasons(showId: Long): List<Season> {
        val list = ArrayList<Season>()

        val seasons = sgSeason2Helper.getSeasonsForExport(showId)

        for (sgSeason in seasons) {
            val season = Season()
            season.tmdb_id = sgSeason.tmdbId
            season.tvdb_id = sgSeason.tvdbId
            season.season = sgSeason.number

            season.episodes = getEpisodes(sgSeason.id)

            // Do not export season to JSON if it has no episodes
            if (season.episodes.isNotEmpty()) {
                list.add(season)
            }
        }
        return list
    }

    /**
     * Returns possibly empty list of episodes for season.
     */
    private fun getEpisodes(seasonId: Long): List<Episode> {
        val list = ArrayList<Episode>()
        val episodes = sgEpisode2Helper.getEpisodesForExport(seasonId)

        for (episodeDb in episodes) {
            val episodeExport = Episode()
            episodeExport.tmdb_id = episodeDb.tmdbId
            episodeExport.tvdb_id = episodeDb.tvdbId
            episodeExport.episode = episodeDb.number
            episodeExport.episode_absolute = episodeDb.absoluteNumber
            episodeExport.episode_dvd = episodeDb.dvdNumber
            val episodeFlag = episodeDb.watched
            episodeExport.watched = EpisodeTools.isWatched(episodeFlag)
            episodeExport.skipped = EpisodeTools.isSkipped(episodeFlag)
            episodeExport.plays = episodeDb.playsOrZero
            episodeExport.collected = episodeDb.collected
            episodeExport.title = episodeDb.title
            episodeExport.first_aired = episodeDb.firstReleasedMs
            episodeExport.rating_user = episodeDb.ratingUser
            if (isFullDump) {
                episodeExport.overview = episodeDb.overview
                episodeExport.image = episodeDb.image
                episodeExport.writers = episodeDb.writers
                episodeExport.gueststars = episodeDb.guestStars
                episodeExport.directors = episodeDb.directors
                episodeExport.rating_tmdb = episodeDb.ratingTmdb
                episodeExport.rating_tmdb_votes = episodeDb.ratingTmdbVotes
                episodeExport.rating = episodeDb.ratingTrakt
                episodeExport.rating_votes = episodeDb.ratingTraktVotes
            }

            list.add(episodeExport)
        }
        return list
    }

    @Throws(IOException::class)
    suspend fun writeJsonStreamLists(coroutineScope: CoroutineScope, out: OutputStream) {
        val lists = sgListHelper.getListsForExport()

        val numTotal = lists.size
        var numExported = 0

        onProgressUpdate(numTotal, 0)

        val gson = Gson()
        val writer = JsonWriter(OutputStreamWriter(out, StandardCharsets.UTF_8))
        writer.beginArray()

        for (sgList in lists) {
            if (!coroutineScope.isActive) {
                break
            }

            val list = ExportList()
            list.list_id = sgList.listId
            list.name = sgList.name
            list.order = sgList.orderOrDefault

            addListItems(list)

            gson.toJson(list, ExportList::class.java, writer)

            onProgressUpdate(numTotal, ++numExported)
        }

        writer.endArray()
        writer.close()
    }

    private fun addListItems(list: ExportList) {
        val listItems = sgListHelper.getListItemsForExport(list.list_id)

        list.items = ArrayList()
        for (listItem in listItems) {
            val item = ListItem()
            item.list_item_id = listItem.listItemId
            item.externalId = listItem.itemRefId
            // Note: export legacy types so users can get to legacy data if they need to.
            when (listItem.type) {
                ListItemTypes.TVDB_SHOW -> item.type = ListItemTypesExport.SHOW
                ListItemTypes.TMDB_SHOW -> item.type = ListItemTypesExport.TMDB_SHOW
                ListItemTypes.SEASON -> item.type = ListItemTypesExport.SEASON
                ListItemTypes.EPISODE -> item.type = ListItemTypesExport.EPISODE
            }

            list.items.add(item)
        }
    }

    @Throws(IOException::class)
    suspend fun writeJsonStreamMovies(coroutineScope: CoroutineScope, out: OutputStream) {
        val movies = movieHelper.getMoviesForExport()

        val numTotal = movies.size
        var numExported = 0

        onProgressUpdate(numTotal, 0)

        val gson = Gson()
        val writer = JsonWriter(OutputStreamWriter(out, StandardCharsets.UTF_8))
        writer.beginArray()

        for (sgMovie in movies) {
            if (!coroutineScope.isActive) {
                break
            }
            val movie = Movie()
            movie.tmdb_id = sgMovie.tmdbId
            movie.imdb_id = sgMovie.imdbId
            movie.title = sgMovie.title
            movie.released_utc_ms = sgMovie.releasedMsOrDefault
            movie.runtime_min = sgMovie.runtimeMinOrDefault
            movie.poster = sgMovie.poster
            movie.in_collection = sgMovie.inCollectionOrDefault
            movie.in_watchlist = sgMovie.inWatchlistOrDefault
            movie.watched = sgMovie.watchedOrDefault
            movie.plays = sgMovie.playsOrDefault
            movie.last_updated_ms = sgMovie.lastUpdatedOrDefault
            if (isFullDump) {
                movie.overview = sgMovie.overview
            }

            gson.toJson(movie, Movie::class.java, writer)

            onProgressUpdate(numTotal, ++numExported)
        }

        writer.endArray()
        writer.close()
    }

    companion object {
        const val EXPORT_JSON_FILE_SHOWS = "seriesguide-shows-backup.json"
        const val EXPORT_JSON_FILE_LISTS = "seriesguide-lists-backup.json"
        const val EXPORT_JSON_FILE_MOVIES = "seriesguide-movies-backup.json"

        const val EXPORT_SHOWS = 1
        const val EXPORT_LISTS = 2
        const val EXPORT_MOVIES = 3

        const val SUCCESS = 1
        private const val ERROR_FILE_ACCESS = 0
        private const val ERROR = -1
    }

    interface OnTaskProgressListener {
        fun onProgressUpdate(total: Int, completed: Int)
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(EXPORT_SHOWS, EXPORT_LISTS, EXPORT_MOVIES)
    annotation class ExportType

    sealed class Export(val name: String, @ExportType val type: Int) {
        object Shows : Export("seriesguide-shows", EXPORT_SHOWS)
        object Lists : Export("seriesguide-lists", EXPORT_LISTS)
        object Movies : Export("seriesguide-movies", EXPORT_MOVIES)
    }

    /**
     * Show status used when exporting data.
     * Compare with [com.battlelancer.seriesguide.shows.tools.ShowStatus].
     */
    object ShowStatusExport {
        const val IN_PRODUCTION = "in_production"
        const val PILOT = "pilot"
        const val CANCELED = "canceled"
        const val UPCOMING = "upcoming"
        const val CONTINUING = "continuing"
        const val ENDED = "ended"
        const val UNKNOWN = "unknown"
    }

    object ListItemTypesExport {
        const val SHOW = "show"
        const val TMDB_SHOW = "tmdb-show"
        const val SEASON = "season"
        const val EPISODE = "episode"
    }

}