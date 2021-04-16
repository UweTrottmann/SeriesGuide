package com.battlelancer.seriesguide.dataliberation;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;

import android.annotation.SuppressLint;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.ListItemTypesExport;
import com.battlelancer.seriesguide.dataliberation.model.Episode;
import com.battlelancer.seriesguide.dataliberation.model.List;
import com.battlelancer.seriesguide.dataliberation.model.ListItem;
import com.battlelancer.seriesguide.dataliberation.model.Movie;
import com.battlelancer.seriesguide.dataliberation.model.Season;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.model.SgEpisode2;
import com.battlelancer.seriesguide.model.SgSeason2;
import com.battlelancer.seriesguide.model.SgShow2;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.Errors;
import com.battlelancer.seriesguide.util.TaskManager;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import org.greenrobot.eventbus.EventBus;
import timber.log.Timber;

/**
 * Import a show database from a human-readable JSON file on external storage. By default meta-data
 * like descriptions, ratings, actors, etc. will not be included.
 */
public class JsonImportTask extends AsyncTask<Void, Integer, Integer> {

    private static final int SUCCESS = 1;
    private static final int ERROR_STORAGE_ACCESS = 0;
    private static final int ERROR = -1;
    private static final int ERROR_LARGE_DB_OP = -2;
    private static final int ERROR_FILE_ACCESS = -3;

    @SuppressLint("StaticFieldLeak") private final Context context;
    private final String[] languageCodes;
    private boolean isImportingAutoBackup;
    private final boolean isImportShows;
    private final boolean isImportLists;
    private final boolean isImportMovies;
    @Nullable private String errorCause;

    public JsonImportTask(Context context, boolean importShows, boolean importLists,
            boolean importMovies) {
        this.context = context.getApplicationContext();
        languageCodes = this.context.getResources().getStringArray(R.array.languageCodesShows);
        isImportingAutoBackup = false;
        isImportShows = importShows;
        isImportLists = importLists;
        isImportMovies = importMovies;
    }

    public JsonImportTask(Context context) {
        this(context, true, true, true);
        isImportingAutoBackup = true;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        // Ensure no large database ops are running
        TaskManager tm = TaskManager.getInstance();
        if (SgSyncAdapter.isSyncActive(context, false) || tm.isAddTaskRunning()) {
            return ERROR_LARGE_DB_OP;
        }

        // last chance to abort
        if (isCancelled()) {
            return ERROR;
        }

        int result;
        if (isImportShows) {
            result = importData(JsonExportTask.BACKUP_SHOWS);
            if (result != SUCCESS) {
                return result;
            }
            if (isCancelled()) {
                return ERROR;
            }
        }

        if (isImportLists) {
            result = importData(JsonExportTask.BACKUP_LISTS);
            if (result != SUCCESS) {
                return result;
            }
            if (isCancelled()) {
                return ERROR;
            }
        }

        if (isImportMovies) {
            result = importData(JsonExportTask.BACKUP_MOVIES);
            if (result != SUCCESS) {
                return result;
            }
            if (isCancelled()) {
                return ERROR;
            }
        }

        // Renew search table
        SeriesGuideDatabase.rebuildFtsTable(context);

        return SUCCESS;
    }

    @Override
    protected void onPostExecute(Integer result) {
        int messageId;
        boolean showIndefinite;
        switch (result) {
            case SUCCESS:
                messageId = R.string.import_success;
                showIndefinite = false;
                break;
            case ERROR_STORAGE_ACCESS:
                messageId = R.string.import_failed_nosd;
                showIndefinite = true;
                break;
            case ERROR_FILE_ACCESS:
                messageId = R.string.import_failed_nofile;
                showIndefinite = true;
                break;
            case ERROR_LARGE_DB_OP:
                messageId = R.string.update_inprogress;
                showIndefinite = false;
                break;
            default:
                messageId = R.string.import_failed;
                showIndefinite = true;
                break;
        }
        EventBus.getDefault()
                .post(new DataLiberationFragment.LiberationResultEvent(
                        context.getString(messageId), errorCause, showIndefinite));
    }

    private int importData(@JsonExportTask.BackupType int type) {
        if (!isImportingAutoBackup) {
            // make sure we have a file uri...
            Uri backupFileUri = getDataBackupFile(type);
            if (backupFileUri == null) {
                return ERROR_FILE_ACCESS;
            }
            // ...and the file actually exists
            ParcelFileDescriptor pfd;
            try {
                pfd = context.getContentResolver().openFileDescriptor(backupFileUri, "r");
            } catch (FileNotFoundException | SecurityException e) {
                Timber.e(e, "Backup file not found.");
                errorCause = e.getMessage();
                return ERROR_FILE_ACCESS;
            }
            if (pfd == null) {
                Timber.e("File descriptor is null.");
                return ERROR_FILE_ACCESS;
            }

            if (!clearExistingData(type)) {
                return ERROR;
            }

            // Access JSON from backup file and try to import data
            FileInputStream in = new FileInputStream(pfd.getFileDescriptor());
            try {
                importFromJson(type, in);

                // let the document provider know we're done.
                pfd.close();
            } catch (JsonParseException | IOException | IllegalStateException e) {
                // the given Json might not be valid or unreadable
                Timber.e(e, "Import failed");
                errorCause = e.getMessage();
                return ERROR;
            } catch (Exception e) {
                // Only report unexpected errors.
                Errors.logAndReport("Import failed", e);
                errorCause = e.getMessage();
                return ERROR;
            }
        } else {
            // Restoring latest auto backup.

            AutoBackupTools.BackupFile backupFileOrNull =
                    AutoBackupTools.getLatestBackupOrNull(type, context);

            if (backupFileOrNull == null) {
                // There is no backup file to restore from.
                return ERROR_FILE_ACCESS;
            }

            File backupFile = backupFileOrNull.getFile();
            FileInputStream in; // Closed by reader after importing.
            try {
                if (!backupFile.canRead()) {
                    return ERROR_FILE_ACCESS;
                }
                in = new FileInputStream(backupFile);
            } catch (Exception e) {
                Timber.e(e, "Unable to open backup file.");
                errorCause = e.getMessage();
                return ERROR_FILE_ACCESS;
            }

            // Only clear data after backup file could be opened.
            if (!clearExistingData(type)) {
                return ERROR;
            }

            // Access JSON from backup file and try to import data
            try {
                importFromJson(type, in);
            } catch (JsonParseException | IOException | IllegalStateException e) {
                // the given Json might not be valid or unreadable
                Timber.e(e, "Import failed");
                errorCause = e.getMessage();
                return ERROR;
            } catch (Exception e) {
                // Only report unexpected errors.
                Errors.logAndReport("Import failed", e);
                errorCause = e.getMessage();
                return ERROR;
            }
        }

        return SUCCESS;
    }

    @Nullable
    private Uri getDataBackupFile(@JsonExportTask.BackupType int type) {
        return BackupSettings.getImportFileUriOrExportFileUri(context, type);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean clearExistingData(@JsonExportTask.BackupType int type) {
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        if (type == JsonExportTask.BACKUP_SHOWS) {
            SgRoomDatabase database = SgRoomDatabase.getInstance(context);
            database.runInTransaction(() -> {
                // delete episodes and seasons first to prevent violating foreign key constraints
                database.sgEpisode2Helper().deleteAllEpisodes();
                database.sgSeason2Helper().deleteAllSeasons();
                database.sgShow2Helper().deleteAllShows();
            });
        } else if (type == JsonExportTask.BACKUP_LISTS) {
            // delete list items before lists to prevent violating foreign key constraints
            batch.add(ContentProviderOperation.newDelete(ListItems.CONTENT_URI).build());
            batch.add(ContentProviderOperation.newDelete(Lists.CONTENT_URI).build());
        } else if (type == JsonExportTask.BACKUP_MOVIES) {
            batch.add(ContentProviderOperation.newDelete(Movies.CONTENT_URI).build());
        }

        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            errorCause = e.getMessage();
            Timber.e(e, "clearExistingData");
            return false;
        }

        return true;
    }

    private void importFromJson(@JsonExportTask.BackupType int type, FileInputStream in)
            throws JsonParseException, IOException, IllegalArgumentException {
        if (in.getChannel().size() == 0) {
            Timber.i("Backup file is empty, nothing to import.");
            in.close();
            return; // File is empty, nothing to import.
        }

        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        reader.beginArray();

        if (type == JsonExportTask.BACKUP_SHOWS) {
            while (reader.hasNext()) {
                Show show = gson.fromJson(reader, Show.class);
                addShowToDatabase(show);
            }
        } else if (type == JsonExportTask.BACKUP_LISTS) {
            while (reader.hasNext()) {
                List list = gson.fromJson(reader, List.class);
                addListToDatabase(list);
            }
        } else if (type == JsonExportTask.BACKUP_MOVIES) {
            while (reader.hasNext()) {
                Movie movie = gson.fromJson(reader, Movie.class);
                context.getContentResolver().insert(Movies.CONTENT_URI, movie.toContentValues());
            }
        }

        reader.endArray();
        reader.close();
    }

    private void addShowToDatabase(Show show) {
        if ((show.tmdb_id == null || show.tmdb_id <= 0)
                && (show.tvdb_id == null || show.tvdb_id <= 0)) {
            // valid id required
            return;
        }

        // reset language if it is not supported
        boolean languageSupported = false;
        for (int i = 0, size = languageCodes.length; i < size; i++) {
            if (languageCodes[i].equals(show.language)) {
                languageSupported = true;
                break;
            }
        }
        if (!languageSupported) {
            show.language = null;
        }

        SgRoomDatabase database = SgRoomDatabase.getInstance(context);

        SgShow2 sgShow = ImportTools.toSgShowForImport(show);
        long showId = database.sgShow2Helper().insertShow(sgShow);
        if (showId == -1) {
            return; // Insert failed.
        }

        if (show.seasons == null || show.seasons.isEmpty()) {
            // no seasons (or episodes)
            return;
        }

        // Parse and insert seasons and episodes.
        insertSeasonsAndEpisodes(show, showId);
    }

    private void insertSeasonsAndEpisodes(Show show, long showId) {
        SgRoomDatabase database = SgRoomDatabase.getInstance(context);

        for (Season season : show.seasons) {
            if ((season.tmdb_id == null || season.tmdb_id.isEmpty())
                    && (season.tvdbId == null || season.tvdbId <= 0)) {
                // valid id is required
                continue;
            }
            if (season.episodes == null || season.episodes.isEmpty()) {
                // episodes required
                continue;
            }

            // Insert season.
            SgSeason2 sgSeason = ImportTools.toSgSeasonForImport(season, showId);
            long seasonId = database.sgSeason2Helper().insertSeason(sgSeason);

            // If inserted, insert episodes.
            if (seasonId != -1) {
                ArrayList<SgEpisode2> episodes = buildEpisodeBatch(season, showId, seasonId);
                database.sgEpisode2Helper().insertEpisodes(episodes);
            }
        }
    }

    private static ArrayList<SgEpisode2> buildEpisodeBatch(Season season, long showId,
            long seasonId) {
        ArrayList<SgEpisode2> episodeBatch = new ArrayList<>();

        for (Episode episode : season.episodes) {
            if ((episode.tmdb_id == null || episode.tmdb_id <= 0)
                    && (episode.tvdbId == null || episode.tvdbId <= 0)) {
                // valid id is required
                continue;
            }

            episodeBatch.add(ImportTools
                    .toSgEpisodeForImport(episode, showId, seasonId, season.season));
        }

        return episodeBatch;
    }

    private void addListToDatabase(List list) {
        if (TextUtils.isEmpty(list.name)) {
            return; // required
        }
        if (TextUtils.isEmpty(list.listId)) {
            // rebuild from name
            list.listId = SeriesGuideContract.Lists.generateListId(list.name);
        }

        // Insert the list
        context.getContentResolver().insert(Lists.CONTENT_URI, list.toContentValues());

        if (list.items == null || list.items.isEmpty()) {
            return;
        }

        // Insert the lists items
        ArrayList<ContentValues> items = new ArrayList<>();
        for (ListItem item : list.items) {
            // Note: do not import legacy types (seasons and episodes).
            int type;
            if (ListItemTypesExport.SHOW.equals(item.type)) {
                type = ListItemTypes.TVDB_SHOW;
            } else if (ListItemTypesExport.TMDB_SHOW.equals(item.type)) {
                type = ListItemTypes.TMDB_SHOW;
            } else {
                // Unknown item type, skip
                continue;
            }

            if (TextUtils.isEmpty(item.listItemId)) continue;

            String externalId = null;
            if (item.externalId != null && !item.externalId.isEmpty()) {
                externalId = item.externalId;
            } else if (item.tvdbId > 0) {
                externalId = String.valueOf(item.tvdbId);
            }
            if (externalId == null) continue;

            ContentValues itemValues = new ContentValues();
            itemValues.put(ListItems.LIST_ITEM_ID, item.listItemId);
            itemValues.put(Lists.LIST_ID, list.listId);
            itemValues.put(ListItems.ITEM_REF_ID, externalId);
            itemValues.put(ListItems.TYPE, type);

            items.add(itemValues);
        }

        ContentValues[] itemsArray = new ContentValues[items.size()];
        context.getContentResolver().bulkInsert(ListItems.CONTENT_URI, items.toArray(itemsArray));
    }
}
