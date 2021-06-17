package com.battlelancer.seriesguide.dataliberation

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import androidx.annotation.IntDef
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.dataliberation.DataLiberationFragment.LiberationResultEvent
import com.battlelancer.seriesguide.dataliberation.model.Episode
import com.battlelancer.seriesguide.dataliberation.model.ListItem
import com.battlelancer.seriesguide.dataliberation.model.Movie
import com.battlelancer.seriesguide.dataliberation.model.Season
import com.battlelancer.seriesguide.dataliberation.model.Show
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools
import com.battlelancer.seriesguide.util.Errors
import com.battlelancer.seriesguide.util.TaskManager
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.stream.JsonWriter
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import com.battlelancer.seriesguide.dataliberation.model.List as ExportList

/**
 * Export the show database to a human-readable JSON file on external storage. By default meta-data
 * like descriptions, ratings, actors, etc. will not be included.
 *
 * @param isFullDump Whether to also export meta-data like descriptions, ratings, actors, etc.
 * Increases file size about 2-4 times.
 * @param isAutoBackupMode Whether to run an auto backup, also shows no result toasts.
 */
class JsonExportTask(
    context: Context,
    private val progressListener: OnTaskProgressListener?,
    private val isFullDump: Boolean,
    private val isAutoBackupMode: Boolean,
    private val type: Int?
) : AsyncTask<Void?, Int?, Int>() {

    @SuppressLint("StaticFieldLeak")
    private val context: Context = context.applicationContext
    private var errorCause: String? = null

    override fun doInBackground(vararg params: Void?): Int {
        return if (isAutoBackupMode) {
            // Auto backup mode.
            try {
                AutoBackupTask(this, context).run()
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
            if (isCancelled) {
                return ERROR
            }

            var result = SUCCESS
            if (type == null || type == BACKUP_SHOWS) {
                result = exportData(BACKUP_SHOWS)
                if (result != SUCCESS) {
                    return result
                }
                if (isCancelled) {
                    return ERROR
                }
            }

            if (type == null || type == BACKUP_LISTS) {
                result = exportData(BACKUP_LISTS)
                if (result != SUCCESS) {
                    return result
                }
                if (isCancelled) {
                    return ERROR
                }
            }

            if (type == null || type == BACKUP_MOVIES) {
                result = exportData(BACKUP_MOVIES)
            }

            result
        }
    }

    override fun onProgressUpdate(vararg values: Int?) {
        progressListener?.onProgressUpdate(*values)
    }

    override fun onPostExecute(result: Int) {
        TaskManager.getInstance().releaseBackupTaskRef()

        if (!isAutoBackupMode) {
            val messageId: Int
            val showIndefinite: Boolean
            when (result) {
                SUCCESS -> {
                    messageId = R.string.backup_success
                    showIndefinite = false
                }
                ERROR_FILE_ACCESS -> {
                    messageId = R.string.backup_failed_file_access
                    showIndefinite = true
                }
                else -> {
                    messageId = R.string.backup_failed
                    showIndefinite = true
                }
            }
            EventBus.getDefault()
                .post(
                    LiberationResultEvent(
                        context.getString(messageId), errorCause, showIndefinite
                    )
                )
        } else {
            EventBus.getDefault().post(LiberationResultEvent())
        }
    }

    private fun exportData(@BackupType type: Int): Int {
        // try to export all data
        try {
            // ensure the user has selected a backup file
            val backupFileUri = getDataBackupFile(type)
                ?: return ERROR_FILE_ACCESS

            val pfd = context.contentResolver.openFileDescriptor(backupFileUri, "w")
                ?: return ERROR_FILE_ACCESS
            val out = FileOutputStream(pfd.fileDescriptor)

            // Even though using streams and FileOutputStream does not append by
            // default, using Storage Access Framework just overwrites existing
            // bytes, potentially leaving old bytes hanging over:
            // so truncate the file first to clear any existing bytes.
            out.channel.truncate(0)

            when (type) {
                BACKUP_SHOWS -> {
                    writeJsonStreamShows(out)
                }
                BACKUP_LISTS -> {
                    writeJsonStreamLists(out)
                }
                BACKUP_MOVIES -> {
                    writeJsonStreamMovies(out)
                }
            }

            // let the document provider know we're done.
            pfd.close()
        } catch (e: FileNotFoundException) {
            Timber.e(e, "Backup file not found.")
            removeBackupFileUri(type)
            errorCause = e.message
            return ERROR_FILE_ACCESS
        } catch (e: IOException) {
            Timber.e(e, "Could not access backup file.")
            removeBackupFileUri(type)
            errorCause = e.message
            return ERROR_FILE_ACCESS
        } catch (e: SecurityException) {
            Timber.e(e, "Could not access backup file.")
            removeBackupFileUri(type)
            errorCause = e.message
            return ERROR_FILE_ACCESS
        } catch (e: JsonParseException) {
            Timber.e(e, "JSON export failed.")
            errorCause = e.message
            return ERROR
        } catch (e: Exception) {
            // Only report unexpected errors.
            Errors.logAndReport("Backup failed.", e)
            errorCause = e.message
            return ERROR
        }

        return SUCCESS
    }

    fun getDataBackupFile(@BackupType type: Int): Uri? {
        return BackupSettings.getExportFileUri(context, type, isAutoBackupMode)
    }

    fun removeBackupFileUri(@BackupType type: Int) {
        BackupSettings.storeExportFileUri(context, type, null, isAutoBackupMode)
    }

    @Throws(IOException::class)
    fun writeJsonStreamShows(out: OutputStream) {
        val shows = SgRoomDatabase.getInstance(context).sgShow2Helper().getShowsForExport()

        val numTotal = shows.size
        var numExported = 0

        publishProgress(numTotal, 0)

        val gson = Gson()
        val writer = JsonWriter(OutputStreamWriter(out, StandardCharsets.UTF_8))
        writer.beginArray()

        for (sgShow in shows) {
            if (isCancelled) {
                break
            }

            val show = Show()
            show.tmdb_id = sgShow.tmdbId
            show.tvdb_id = sgShow.tvdbId
            show.title = sgShow.title
            show.favorite = sgShow.favorite
            show.notify = sgShow.notify
            show.hidden = sgShow.hidden
            show.language = sgShow.language
            show.release_time = sgShow.releaseTimeOrDefault
            show.release_weekday = sgShow.releaseWeekDayOrDefault
            show.release_timezone = sgShow.releaseTimeZone
            show.country = sgShow.releaseCountry
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
            if (isFullDump) {
                show.overview = sgShow.overview
                show.rating = sgShow.ratingGlobalOrZero
                show.rating_votes = sgShow.ratingVotesOrZero
                show.genres = sgShow.genres
            }

            show.seasons = getSeasons(sgShow.id)

            gson.toJson(show, Show::class.java, writer)

            publishProgress(numTotal, ++numExported)
        }

        writer.endArray()
        writer.close()
    }

    /**
     * Returns possibly empty list of seasons with episodes.
     */
    private fun getSeasons(showId: Long): List<Season> {
        val list = ArrayList<Season>()

        val seasons = SgRoomDatabase.getInstance(context)
            .sgSeason2Helper()
            .getSeasonsForExport(showId)

        for (sgSeason in seasons) {
            val season = Season()
            season.tmdb_id = sgSeason.tmdbId
            season.tvdbId = sgSeason.tvdbId
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
        val episodes = SgRoomDatabase.getInstance(context)
            .sgEpisode2Helper()
            .getEpisodesForExport(seasonId)

        for (episodeDb in episodes) {
            val episodeExport = Episode()
            episodeExport.tmdb_id = episodeDb.tmdbId
            episodeExport.tvdbId = episodeDb.tvdbId
            episodeExport.episode = episodeDb.number
            episodeExport.episodeAbsolute = episodeDb.absoluteNumber
            episodeExport.episodeDvd = episodeDb.dvdNumber
            val episodeFlag = episodeDb.watched
            episodeExport.watched = EpisodeTools.isWatched(episodeFlag)
            episodeExport.skipped = EpisodeTools.isSkipped(episodeFlag)
            episodeExport.plays = episodeDb.playsOrZero
            episodeExport.collected = episodeDb.collected
            episodeExport.title = episodeDb.title
            episodeExport.firstAired = episodeDb.firstReleasedMs
            episodeExport.imdbId = episodeDb.imdbId
            episodeExport.rating_user = episodeDb.ratingUser
            if (isFullDump) {
                episodeExport.overview = episodeDb.overview
                episodeExport.image = episodeDb.image
                episodeExport.writers = episodeDb.writers
                episodeExport.gueststars = episodeDb.guestStars
                episodeExport.directors = episodeDb.directors
                episodeExport.rating = episodeDb.ratingGlobal
                episodeExport.rating_votes = episodeDb.ratingVotes
            }

            list.add(episodeExport)
        }
        return list
    }

    @Throws(IOException::class)
    fun writeJsonStreamLists(out: OutputStream) {
        val lists = SgRoomDatabase.getInstance(context)
            .sgListHelper()
            .getListsForExport()

        val numTotal = lists.size
        var numExported = 0

        publishProgress(numTotal, 0)

        val gson = Gson()
        val writer = JsonWriter(OutputStreamWriter(out, StandardCharsets.UTF_8))
        writer.beginArray()

        for (sgList in lists) {
            if (isCancelled) {
                break
            }

            val list = ExportList()
            list.listId = sgList.listId
            list.name = sgList.name
            list.order = sgList.orderOrDefault

            addListItems(list)

            gson.toJson(list, ExportList::class.java, writer)

            publishProgress(numTotal, ++numExported)
        }

        writer.endArray()
        writer.close()
    }

    private fun addListItems(list: ExportList) {
        val listItems = SgRoomDatabase.getInstance(context)
            .sgListHelper().getListItemsForExport(list.listId)

        list.items = ArrayList()
        for (listItem in listItems) {
            val item = ListItem()
            item.listItemId = listItem.listItemId
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
    fun writeJsonStreamMovies(out: OutputStream) {
        val movies = SgRoomDatabase.getInstance(context)
            .movieHelper()
            .getMoviesForExport()

        val numTotal = movies.size
        var numExported = 0

        publishProgress(numTotal, 0)

        val gson = Gson()
        val writer = JsonWriter(OutputStreamWriter(out, StandardCharsets.UTF_8))
        writer.beginArray()

        for (sgMovie in movies) {
            if (isCancelled) {
                break
            }
            val movie = Movie()
            movie.tmdbId = sgMovie.tmdbId
            movie.imdbId = sgMovie.imdbId
            movie.title = sgMovie.title
            movie.releasedUtcMs = sgMovie.releasedMsOrDefault
            movie.runtimeMin = sgMovie.runtimeMinOrDefault
            movie.poster = sgMovie.poster
            movie.inCollection = sgMovie.inCollection
            movie.inWatchlist = sgMovie.inWatchlist
            movie.watched = sgMovie.watched
            movie.plays = sgMovie.plays
            movie.lastUpdatedMs = sgMovie.lastUpdatedOrDefault
            if (isFullDump) {
                movie.overview = sgMovie.overview
            }

            gson.toJson(movie, Movie::class.java, writer)

            publishProgress(numTotal, ++numExported)
        }

        writer.endArray()
        writer.close()
    }

    companion object {
        const val EXPORT_JSON_FILE_SHOWS = "seriesguide-shows-backup.json"
        const val EXPORT_JSON_FILE_LISTS = "seriesguide-lists-backup.json"
        const val EXPORT_JSON_FILE_MOVIES = "seriesguide-movies-backup.json"

        const val BACKUP_SHOWS = 1
        const val BACKUP_LISTS = 2
        const val BACKUP_MOVIES = 3

        private const val SUCCESS = 1
        private const val ERROR_FILE_ACCESS = 0
        private const val ERROR = -1
    }

    interface OnTaskProgressListener {
        fun onProgressUpdate(vararg values: Int?)
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(BACKUP_SHOWS, BACKUP_LISTS, BACKUP_MOVIES)
    annotation class BackupType

    /**
     * Show status used when exporting data.
     * Compare with [com.battlelancer.seriesguide.ui.shows.ShowTools.Status].
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