// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2013 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.dataliberation

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.annotation.VisibleForTesting
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.dataliberation.ImportTools.toSgEpisodeForImport
import com.battlelancer.seriesguide.dataliberation.ImportTools.toSgListForImport
import com.battlelancer.seriesguide.dataliberation.ImportTools.toSgListItemForImport
import com.battlelancer.seriesguide.dataliberation.ImportTools.toSgMovieForImport
import com.battlelancer.seriesguide.dataliberation.ImportTools.toSgSeasonForImport
import com.battlelancer.seriesguide.dataliberation.ImportTools.toSgShowForImport
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.Export
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.ListItemTypesExport
import com.battlelancer.seriesguide.dataliberation.model.List
import com.battlelancer.seriesguide.dataliberation.model.Movie
import com.battlelancer.seriesguide.dataliberation.model.Season
import com.battlelancer.seriesguide.dataliberation.model.Show
import com.battlelancer.seriesguide.lists.ListsTools
import com.battlelancer.seriesguide.lists.database.SgListHelper
import com.battlelancer.seriesguide.lists.database.SgListItem
import com.battlelancer.seriesguide.movies.MoviesSettings
import com.battlelancer.seriesguide.movies.database.MovieHelper
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgEpisode2
import com.battlelancer.seriesguide.shows.database.SgEpisode2Helper
import com.battlelancer.seriesguide.shows.database.SgSeason2Helper
import com.battlelancer.seriesguide.shows.database.SgShow2Helper
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.tmdbapi.TmdbFindTools
import com.battlelancer.seriesguide.tmdbapi.TmdbTools4.TmdbErrorResponse
import com.battlelancer.seriesguide.tmdbapi.TmdbTools4.TmdbNonNullResponse
import com.battlelancer.seriesguide.util.Errors
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.TaskManager
import com.battlelancer.seriesguide.util.TextTools
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.stream.JsonReader
import com.uwetrottmann.tmdb2.entities.BaseMovie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader

/**
 * Imports shows, lists or movies from a human-readable JSON file replacing existing data.
 */
class JsonImportTask(
    context: Context,
    importShows: Boolean,
    importLists: Boolean,
    importMovies: Boolean,
    private val database: SgRoomDatabase,
    private val sgShow2Helper: SgShow2Helper,
    private val sgSeason2Helper: SgSeason2Helper,
    private val sgEpisode2Helper: SgEpisode2Helper,
    private val sgListHelper: SgListHelper,
    private val sgMovieHelper: MovieHelper,
    private val tmdbFindTools: TmdbFindTools
) {

    private val context: Context = context.applicationContext
    private val languageCodes: Array<String> =
        this.context.resources.getStringArray(R.array.content_languages)
    private var isImportingAutoBackup: Boolean
    private val isImportShows: Boolean
    private val isImportLists: Boolean
    private val isImportMovies: Boolean

    private var importedMovies: Int = 0
    private val skippedMovies: MutableList<Movie> = mutableListOf()

    @VisibleForTesting
    var errorCause: String? = null
        private set

    // Note: Path or NIO APIs require Android 8 (API 26) or coreLibraryDesugaring
    private var testBackupFileShows: File? = null
    private var testBackupFileMovies: File? = null
    private var testBackupFileLists: File? = null

    init {
        isImportingAutoBackup = false
        isImportShows = importShows
        isImportLists = importLists
        isImportMovies = importMovies
    }

    constructor(
        context: Context,
        importShows: Boolean,
        importLists: Boolean,
        importMovies: Boolean
    ) : this(
        context,
        importShows,
        importLists,
        importMovies,
        SgRoomDatabase.getInstance(context),
        SgRoomDatabase.getInstance(context).sgShow2Helper(),
        SgRoomDatabase.getInstance(context).sgSeason2Helper(),
        SgRoomDatabase.getInstance(context).sgEpisode2Helper(),
        SgRoomDatabase.getInstance(context).sgListHelper(),
        SgRoomDatabase.getInstance(context).movieHelper(),
        TmdbFindTools(
            SgApp.getServicesComponent(context).tmdb().findService(),
            MoviesSettings.getMoviesLanguage(context)
        )
    )

    constructor(context: Context) : this(context, true, true, true) {
        isImportingAutoBackup = true
    }

    data class Result(
        val message: String,
        val isError: Boolean = false
    )

    suspend fun run(): Result {
        return withContext(Dispatchers.IO) {
            val resultCode = if (SgSyncAdapter.isSyncActive(context, false)) {
                // Do not import if an update task is running
                ERROR_LARGE_DB_OP
            } else {
                // Do not import until an add or backup task are finished
                TaskManager.addShowOrBackupSemaphore.withPermit {
                    doInBackground(this)
                }
            }
            return@withContext buildResult(resultCode)
        }
    }

    private fun doInBackground(coroutineScope: CoroutineScope): Int {
        // last chance to abort
        if (!coroutineScope.isActive) {
            return ERROR
        }

        var result: Int
        if (isImportShows) {
            result = openFilesAndImport(Export.Shows)
            if (result != SUCCESS) {
                return result
            }
            if (!coroutineScope.isActive) {
                return ERROR
            }
        }

        if (isImportMovies) {
            result = openFilesAndImport(Export.Movies)
            if (result != SUCCESS) {
                return result
            }
            if (!coroutineScope.isActive) {
                return ERROR
            }
        }

        // Import lists last so imdb-movie list items can be mapped
        if (isImportLists) {
            result = openFilesAndImport(Export.Lists)
            if (result != SUCCESS) {
                return result
            }
            if (!coroutineScope.isActive) {
                return ERROR
            }
        }

        // Renew search table
        SeriesGuideDatabase.rebuildFtsTable(context)

        return SUCCESS
    }

    private fun buildResult(resultCode: Int): Result {
        return when (resultCode) {
            SUCCESS -> {
                val skippedMoviesSummary = skippedMovies.joinToString(separator = "\n") {
                    "tmdb_id = ${it.tmdb_id}, imdb_id = ${it.imdb_id}, title = ${it.title}"
                }

                // TODO Add other types, localize
                val summary = """
                ${context.getString(R.string.status_successful)}
                
                Movies: $importedMovies imported, ${skippedMovies.size} skipped
                
                Skipped movies:
                $skippedMoviesSummary
                """.trimIndent()

                Result(summary)
            }

            ERROR_FILE_ACCESS -> Result(
                TextTools.dotSeparate(
                    context,
                    TextTools.dotSeparate(
                        context,
                        R.string.status_failure,
                        R.string.status_failed_file_access
                    ),
                    errorCause
                ),
                isError = true
            )

            ERROR_LARGE_DB_OP -> Result(
                TextTools.dotSeparate(
                    context,
                    context.getString(R.string.update_inprogress),
                    errorCause
                ),
                isError = true
            )

            else -> Result(
                TextTools.dotSeparate(
                    context,
                    context.getString(R.string.status_failure),
                    errorCause
                ),
                isError = true
            )
        }
    }

    /**
     * If set, will use these files instead of opening via URI, which seems broken with Robolectric
     * (openFileDescriptor throws FileNotFoundException).
     */
    fun setTestBackupFiles(
        fileShows: File? = null,
        fileMovies: File? = null,
        fileLists: File? = null
    ) {
        this.testBackupFileShows = fileShows
        this.testBackupFileMovies = fileMovies
        this.testBackupFileLists = fileLists
    }

    private fun getTestBackupFile(export: Export): File? {
        return when (export) {
            Export.Shows -> testBackupFileShows
            Export.Lists -> testBackupFileLists
            Export.Movies -> testBackupFileMovies
        }
    }

    private fun openFilesAndImport(export: Export): Int {
        if (!isImportingAutoBackup) {

            val testBackupFile = getTestBackupFile(export)

            var pfd: ParcelFileDescriptor? = null
            if (testBackupFile == null) {
                // Make sure a file is configured...
                val backupFileUri = getDataBackupFile(export) ?: return ERROR_FILE_ACCESS
                // ...and the file actually exists
                try {
                    pfd = context.contentResolver.openFileDescriptor(backupFileUri, "r")
                } catch (e: FileNotFoundException) {
                    Timber.e(e, "Backup file not found.")
                    errorCause = e.message
                    return ERROR_FILE_ACCESS
                } catch (e: SecurityException) {
                    Timber.e(e, "Backup file not found.")
                    errorCause = e.message
                    return ERROR_FILE_ACCESS
                } catch (e: Exception) {
                    // Only report unexpected errors.
                    Errors.logAndReport("Backup file not found.", e)
                    errorCause = e.message
                    return ERROR_FILE_ACCESS
                }
                if (pfd == null) {
                    Timber.e("File descriptor is null.")
                    return ERROR_FILE_ACCESS
                }
            }

            // Access JSON from backup file and try to import data
            val inputStream = if (testBackupFile == null) {
                FileInputStream(pfd!!.fileDescriptor)
            } else FileInputStream(testBackupFile)

            try {
                return clearDataAndImportInTransaction(export, inputStream)
            } finally {
                try {
                    // Let the document provider know this is done.
                    pfd?.close()
                } catch (e: Exception) {
                    // Import is already done, don't fail if closing the descriptor fails, but report it
                    Errors.logAndReport("Import failed", e)
                }
            }
        } else {
            // Restoring latest auto backup.
            val (backupFile) = AutoBackupTools.getLatestBackupOrNull(export, context)
                ?: // There is no backup file to restore from.
                return ERROR_FILE_ACCESS
            val inputStream: FileInputStream // Closed by reader after importing.
            try {
                if (!backupFile.canRead()) {
                    return ERROR_FILE_ACCESS
                }
                inputStream = FileInputStream(backupFile)
            } catch (e: Exception) {
                Timber.e(e, "Unable to open backup file.")
                errorCause = e.message
                return ERROR_FILE_ACCESS
            }

            return clearDataAndImportInTransaction(export, inputStream)
        }
    }

    private fun getDataBackupFile(export: Export): Uri? {
        return BackupSettings.getImportFileUriOrExportFileUri(context, export)
    }

    /**
     * [inputStream] will be closed when this returns.
     */
    private fun clearDataAndImportInTransaction(export: Export, inputStream: FileInputStream): Int {
        try {
            // Wrap in transaction, so if anything goes wrong existing data is restored
            database.runInTransaction {
                clearExistingData(export)
                // Note: takes care of closing input stream
                importFromJson(export, inputStream)
            }
            return SUCCESS
        } catch (e: JsonParseException) {
            // The given Json might not be valid or unreadable
            Timber.e(e, "Import failed")
            errorCause = e.message
            return ERROR
        } catch (e: IOException) {
            Timber.e(e, "Import failed")
            errorCause = e.message
            return ERROR
        } catch (e: IllegalStateException) {
            Timber.e(e, "Import failed")
            errorCause = e.message
            return ERROR
        } catch (e: Exception) {
            // Only report unexpected errors.
            Errors.logAndReport("Import failed", e)
            errorCause = e.message
            return ERROR
        }
    }

    /**
     * Assumes this is run within a database transaction.
     */
    private fun clearExistingData(export: Export) {
        when (export) {
            Export.Shows -> {
                // delete episodes and seasons first to prevent violating foreign key constraints
                sgEpisode2Helper.deleteAllEpisodes()
                sgSeason2Helper.deleteAllSeasons()
                sgShow2Helper.deleteAllShows()
            }

            Export.Lists -> {
                // Delete list items before lists to prevent violating foreign key constraints
                sgListHelper.deleteAllListItems()
                sgListHelper.deleteAllLists()
            }

            Export.Movies -> {
                sgMovieHelper.deleteAllMovies()
            }
        }
    }

    /**
     * Takes care of closing the given [inputStream].
     */
    @Throws(JsonParseException::class, IOException::class, IllegalArgumentException::class)
    private fun importFromJson(export: Export, inputStream: FileInputStream) {
        if (inputStream.channel.size() == 0L) {
            Timber.i("Backup file is empty, nothing to import.")
            inputStream.close()
            return  // File is empty, nothing to import.
        }

        val gson = Gson()

        JsonReader(InputStreamReader(inputStream, "UTF-8"))
            .use { reader ->
                reader.beginArray()
                when (export) {
                    Export.Shows -> {
                        while (reader.hasNext()) {
                            val show = gson.fromJson<Show>(reader, Show::class.java)
                            addShowToDatabase(show)
                        }
                    }

                    Export.Lists -> {
                        while (reader.hasNext()) {
                            val list = gson.fromJson<List>(reader, List::class.java)
                            addListToDatabase(list)
                        }
                    }

                    Export.Movies -> {
                        while (reader.hasNext()) {
                            val movie = gson.fromJson<Movie>(reader, Movie::class.java)
                            addMovieToDatabase(movie)
                        }
                    }
                }
                reader.endArray()
            }
    }

    /**
     * Throws [IOException] if looking up via IMDB ID fails due to an API or network error.
     */
    @Throws(IOException::class)
    private fun addMovieToDatabase(movie: Movie) {
        if (movie.tmdb_id <= 0) {
            // Try to look up via IMDB ID
            if (movie.imdb_id.isNullOrBlank()) {
                skippedMovies.add(movie)
                return // Skip, needs IMDB ID for look-up
            }
            val tmdbIdResponse = runBlocking {
                tmdbFindTools.findMovieByImdbId(movie.imdb_id)
            }
            if (tmdbIdResponse is TmdbErrorResponse.Other) {
                // If the API or network has issues, stop importing to avoid skipping movies
                throw IOException("Failed to look up movie: TMDB API or network error (title = ${movie.title}, imdb_id = ${movie.imdb_id})")
            }
            if (tmdbIdResponse is TmdbNonNullResponse.Success) {
                val tmdbMovie = tmdbIdResponse.data
                val tmdbId = tmdbMovie.id
                if (tmdbId != null) {
                    movie.tmdb_id = tmdbId
                    // Already made an API call to TMDB, so fill in data and prevent from updating
                    // right away.
                    tmdbMovie.enrichAndSetUpdated(movie)
                }
            }
            if (movie.tmdb_id <= 0) {
                Timber.i(
                    "Failed to look up movie (title = %s, imdb_id = %s)",
                    movie.imdb_id,
                    movie.title
                )
                skippedMovies.add(movie)
                return // Skip, TMDB ID required
            }
        }
        sgMovieHelper.insertMovie(movie.toSgMovieForImport())
        importedMovies++
    }

    /**
     * Sets last updated to avoid hitting TMDB again soon after importing.
     */
    private fun BaseMovie.enrichAndSetUpdated(toImport: Movie) {
        title?.let { toImport.title = it }
        release_date?.time?.let { toImport.released_utc_ms = it }
        poster_path?.let { toImport.poster = it }
        overview?.let { toImport.overview = it }

        toImport.last_updated_ms = System.currentTimeMillis()
    }

    private fun addShowToDatabase(show: Show) {
        if ((show.tmdb_id == null || show.tmdb_id!! <= 0)
            && (show.tvdb_id == null || show.tvdb_id!! <= 0)) {
            // valid id required
            return
        }

        // Map legacy language codes.
        if (!show.language.isNullOrEmpty()) {
            show.language = LanguageTools.mapLegacyShowCode(show.language)
        }
        // Reset language if it is not supported.
        val languageSupported = languageCodes.find { it == show.language } != null
        if (!languageSupported) {
            show.language = null
        }

        val sgShow = show.toSgShowForImport()
        val showId = sgShow2Helper.insertShow(sgShow)
        if (showId == -1L) {
            return  // Insert failed.
        }

        if (show.seasons == null || show.seasons.isEmpty()) {
            // no seasons (or episodes)
            return
        }

        // Parse and insert seasons and episodes.
        insertSeasonsAndEpisodes(show, showId)
    }

    private fun insertSeasonsAndEpisodes(show: Show, showId: Long) {
        for (season in show.seasons) {
            if ((season.tmdb_id == null || season.tmdb_id!!.isEmpty())
                && (season.tvdb_id == null || season.tvdb_id!! <= 0)) {
                // valid id is required
                continue
            }
            if (season.episodes == null || season.episodes.isEmpty()) {
                // episodes required
                continue
            }

            // Insert season.
            val sgSeason = season.toSgSeasonForImport(showId)
            val seasonId = sgSeason2Helper.insertSeason(sgSeason)

            // If inserted, insert episodes.
            if (seasonId != -1L) {
                val episodes = buildEpisodeBatch(season, showId, seasonId)
                sgEpisode2Helper.insertEpisodes(episodes)
            }
        }
    }

    private fun buildEpisodeBatch(
        season: Season,
        showId: Long,
        seasonId: Long
    ): ArrayList<SgEpisode2> {
        val episodeBatch = ArrayList<SgEpisode2>()
        for (episode in season.episodes) {
            if ((episode.tmdb_id == null || episode.tmdb_id!! <= 0)
                && (episode.tvdb_id == null || episode.tvdb_id!! <= 0)) {
                // valid id is required
                continue
            }
            episodeBatch.add(episode.toSgEpisodeForImport(showId, seasonId, season.season))
        }
        return episodeBatch
    }

    private fun addListToDatabase(list: List) {
        if (list.name.isNullOrEmpty()) {
            return // required
        }
        if (list.list_id.isNullOrEmpty()) {
            // rebuild from name
            list.list_id = ListsTools.generateListId(list.name)
                ?: return
        }

        // Insert the list
        val sgList = list.toSgListForImport()
        sgListHelper.insertList(sgList)

        if (list.items == null || list.items.isEmpty()) {
            return
        }

        // Insert the list items
        val items = ArrayList<SgListItem>()
        for (item in list.items) {

            // Special type for movies: map to TMDB ID if movie with that IMDB ID is in the database
            if (ListItemTypesExport.IMDB_MOVIE == item.type) {
                val tmdbIdOrNull = sgMovieHelper.getTmdbIdByImdbId(item.externalId)
                if (tmdbIdOrNull == null) {
                    Timber.i("Skipping imdb-movie list item: no movie in database with IMDB ID ${item.externalId}")
                    continue
                }
                item.externalId = tmdbIdOrNull.toString()
                item.type = ListItemTypesExport.MOVIE
            }

            item.toSgListItemForImport(sgList.listId)
                ?.let { items.add(it) }
        }

        sgListHelper.insertListItems(items)
    }

    companion object {
        const val SUCCESS = 1
        private const val ERROR = -1
        private const val ERROR_LARGE_DB_OP = -2
        private const val ERROR_FILE_ACCESS = -3
    }
}