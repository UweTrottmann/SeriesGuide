package com.battlelancer.seriesguide.dataliberation

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.content.OperationApplicationException
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.annotation.VisibleForTesting
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.dataliberation.DataLiberationFragment.LiberationResultEvent
import com.battlelancer.seriesguide.dataliberation.ImportTools.toSgEpisodeForImport
import com.battlelancer.seriesguide.dataliberation.ImportTools.toSgSeasonForImport
import com.battlelancer.seriesguide.dataliberation.ImportTools.toSgShowForImport
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.BackupType
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.ListItemTypesExport
import com.battlelancer.seriesguide.dataliberation.model.List
import com.battlelancer.seriesguide.dataliberation.model.Movie
import com.battlelancer.seriesguide.dataliberation.model.Season
import com.battlelancer.seriesguide.dataliberation.model.Show
import com.battlelancer.seriesguide.model.SgEpisode2
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase
import com.battlelancer.seriesguide.provider.SgEpisode2Helper
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.provider.SgSeason2Helper
import com.battlelancer.seriesguide.provider.SgShow2Helper
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.util.DBUtils
import com.battlelancer.seriesguide.util.Errors.Companion.logAndReport
import com.battlelancer.seriesguide.util.TaskManager
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.util.ArrayList

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
    private val sgEpisode2Helper: SgEpisode2Helper
) {

    private val context: Context = context.applicationContext
    private val languageCodes: Array<String> =
        this.context.resources.getStringArray(R.array.languageCodesShows)
    private var isImportingAutoBackup: Boolean
    private val isImportShows: Boolean
    private val isImportLists: Boolean
    private val isImportMovies: Boolean

    @VisibleForTesting
    var errorCause: String? = null
        private set

    /**
     * If set will use this file instead of opening via URI, which seems broken with Robolectric
     * (openFileDescriptor throws FileNotFoundException).
     */
    @VisibleForTesting
    var testBackupFile: File? = null

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
        SgRoomDatabase.getInstance(context).sgEpisode2Helper()
    )

    constructor(context: Context) : this(context, true, true, true) {
        isImportingAutoBackup = true
    }

    suspend fun run(): Int {
        return withContext(Dispatchers.IO) {
            val result = doInBackground(this)
            onPostExecute(result)
            return@withContext result
        }
    }

    private fun doInBackground(coroutineScope: CoroutineScope): Int {
        // Ensure no large database ops are running
        val tm = TaskManager.getInstance()
        if (SgSyncAdapter.isSyncActive(context, false) || tm.isAddTaskRunning) {
            return ERROR_LARGE_DB_OP
        }

        // last chance to abort
        if (!coroutineScope.isActive) {
            return ERROR
        }

        var result: Int
        if (isImportShows) {
            result = importData(JsonExportTask.BACKUP_SHOWS)
            if (result != SUCCESS) {
                return result
            }
            if (!coroutineScope.isActive) {
                return ERROR
            }
        }

        if (isImportLists) {
            result = importData(JsonExportTask.BACKUP_LISTS)
            if (result != SUCCESS) {
                return result
            }
            if (!coroutineScope.isActive) {
                return ERROR
            }
        }

        if (isImportMovies) {
            result = importData(JsonExportTask.BACKUP_MOVIES)
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

    private fun onPostExecute(result: Int) {
        val messageId: Int
        val showIndefinite: Boolean
        when (result) {
            SUCCESS -> {
                messageId = R.string.import_success
                showIndefinite = false
            }
            ERROR_STORAGE_ACCESS -> {
                messageId = R.string.import_failed_nosd
                showIndefinite = true
            }
            ERROR_FILE_ACCESS -> {
                messageId = R.string.import_failed_nofile
                showIndefinite = true
            }
            ERROR_LARGE_DB_OP -> {
                messageId = R.string.update_inprogress
                showIndefinite = false
            }
            else -> {
                messageId = R.string.import_failed
                showIndefinite = true
            }
        }
        EventBus.getDefault().post(
            LiberationResultEvent(
                context.getString(messageId), errorCause, showIndefinite
            )
        )
    }

    private fun importData(@BackupType type: Int): Int {
        if (!isImportingAutoBackup) {
            val testBackupFile = testBackupFile
            var pfd: ParcelFileDescriptor? = null
            if (testBackupFile == null) {
                // make sure we have a file uri...
                val backupFileUri = getDataBackupFile(type) ?: return ERROR_FILE_ACCESS
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
                }
                if (pfd == null) {
                    Timber.e("File descriptor is null.")
                    return ERROR_FILE_ACCESS
                }
            }

            if (!clearExistingData(type)) {
                return ERROR
            }

            // Access JSON from backup file and try to import data
            val inputStream = if (testBackupFile == null) {
                FileInputStream(pfd!!.fileDescriptor)
            } else FileInputStream(testBackupFile)
            try {
                importFromJson(type, inputStream)

                // let the document provider know we're done.
                pfd?.close()
            } catch (e: JsonParseException) {
                // the given Json might not be valid or unreadable
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
                logAndReport("Import failed", e)
                errorCause = e.message
                return ERROR
            }
        } else {
            // Restoring latest auto backup.
            val (backupFile) = AutoBackupTools.getLatestBackupOrNull(type, context)
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

            // Only clear data after backup file could be opened.
            if (!clearExistingData(type)) {
                return ERROR
            }

            // Access JSON from backup file and try to import data
            try {
                importFromJson(type, inputStream)
            } catch (e: JsonParseException) {
                // the given Json might not be valid or unreadable
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
                logAndReport("Import failed", e)
                errorCause = e.message
                return ERROR
            }
        }
        return SUCCESS
    }

    private fun getDataBackupFile(@BackupType type: Int): Uri? {
        return BackupSettings.getImportFileUriOrExportFileUri(context, type)
    }

    private fun clearExistingData(@BackupType type: Int): Boolean {
        val batch = ArrayList<ContentProviderOperation>()
        when (type) {
            JsonExportTask.BACKUP_SHOWS -> {
                database.runInTransaction {
                    // delete episodes and seasons first to prevent violating foreign key constraints
                    sgEpisode2Helper.deleteAllEpisodes()
                    sgSeason2Helper.deleteAllSeasons()
                    sgShow2Helper.deleteAllShows()
                }
            }
            JsonExportTask.BACKUP_LISTS -> {
                // delete list items before lists to prevent violating foreign key constraints
                batch.add(
                    ContentProviderOperation.newDelete(ListItems.CONTENT_URI).build()
                )
                batch.add(
                    ContentProviderOperation.newDelete(SeriesGuideContract.Lists.CONTENT_URI)
                        .build()
                )
            }
            JsonExportTask.BACKUP_MOVIES -> {
                batch.add(
                    ContentProviderOperation.newDelete(SeriesGuideContract.Movies.CONTENT_URI)
                        .build()
                )
            }
        }
        try {
            DBUtils.applyInSmallBatches(context, batch)
        } catch (e: OperationApplicationException) {
            errorCause = e.message
            Timber.e(e, "clearExistingData")
            return false
        }
        return true
    }

    @Throws(JsonParseException::class, IOException::class, IllegalArgumentException::class)
    private fun importFromJson(@BackupType type: Int, inputStream: FileInputStream) {
        if (inputStream.channel.size() == 0L) {
            Timber.i("Backup file is empty, nothing to import.")
            inputStream.close()
            return  // File is empty, nothing to import.
        }

        val gson = Gson()
        val reader = JsonReader(InputStreamReader(inputStream, "UTF-8"))
        reader.beginArray()
        when (type) {
            JsonExportTask.BACKUP_SHOWS -> {
                while (reader.hasNext()) {
                    val show = gson.fromJson<Show>(reader, Show::class.java)
                    addShowToDatabase(show)
                }
            }
            JsonExportTask.BACKUP_LISTS -> {
                while (reader.hasNext()) {
                    val list = gson.fromJson<List>(reader, List::class.java)
                    addListToDatabase(list)
                }
            }
            JsonExportTask.BACKUP_MOVIES -> {
                while (reader.hasNext()) {
                    val movie = gson.fromJson<Movie>(reader, Movie::class.java)
                    context.contentResolver.insert(
                        SeriesGuideContract.Movies.CONTENT_URI,
                        movie.toContentValues()
                    )
                }
            }
        }
        reader.endArray()
        reader.close()
    }

    private fun addShowToDatabase(show: Show) {
        if ((show.tmdb_id == null || show.tmdb_id!! <= 0)
            && (show.tvdb_id == null || show.tvdb_id!! <= 0)) {
            // valid id required
            return
        }

        // reset language if it is not supported
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
                && (season.tvdbId == null || season.tvdbId!! <= 0)) {
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
                && (episode.tvdbId == null || episode.tvdbId!! <= 0)) {
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
        if (list.listId.isNullOrEmpty()) {
            // rebuild from name
            list.listId = SeriesGuideContract.Lists.generateListId(list.name)
        }

        // Insert the list
        context.contentResolver.insert(
            SeriesGuideContract.Lists.CONTENT_URI,
            list.toContentValues()
        )

        if (list.items == null || list.items.isEmpty()) {
            return
        }

        // Insert the lists items
        val items = ArrayList<ContentValues>()
        for (item in list.items) {
            // Note: DO import legacy types (seasons and episodes),
            // as e.g. older backups can still contain legacy show data to allow displaying them.
            val type: Int = if (ListItemTypesExport.SHOW == item.type) {
                ListItemTypes.TVDB_SHOW
            } else if (ListItemTypesExport.TMDB_SHOW == item.type) {
                ListItemTypes.TMDB_SHOW
            } else if (ListItemTypesExport.SEASON == item.type) {
                ListItemTypes.SEASON
            } else if (ListItemTypesExport.EPISODE == item.type) {
                ListItemTypes.EPISODE
            } else {
                // Unknown item type, skip
                continue
            }

            var externalId: String? = null
            if (item.externalId != null && item.externalId.isNotEmpty()) {
                externalId = item.externalId
            } else if (item.tvdbId > 0) {
                externalId = item.tvdbId.toString()
            }
            if (externalId == null) continue  // No external ID, skip

            // Generate list item ID from values, do not trust given item ID
            // (e.g. encoded list ID might not match)
            item.listItemId = ListItems.generateListItemId(externalId, type, list.listId)

            val itemValues = ContentValues()
            itemValues.put(ListItems.LIST_ITEM_ID, item.listItemId)
            itemValues.put(SeriesGuideContract.Lists.LIST_ID, list.listId)
            itemValues.put(ListItems.ITEM_REF_ID, externalId)
            itemValues.put(ListItems.TYPE, type)

            items.add(itemValues)
        }

        context.contentResolver.bulkInsert(ListItems.CONTENT_URI, items.toTypedArray())
    }

    companion object {
        const val SUCCESS = 1
        private const val ERROR_STORAGE_ACCESS = 0
        private const val ERROR = -1
        private const val ERROR_LARGE_DB_OP = -2
        private const val ERROR_FILE_ACCESS = -3
    }
}