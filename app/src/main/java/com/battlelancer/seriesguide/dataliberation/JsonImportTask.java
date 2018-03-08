package com.battlelancer.seriesguide.dataliberation;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.ListItemTypesExport;
import com.battlelancer.seriesguide.dataliberation.model.Episode;
import com.battlelancer.seriesguide.dataliberation.model.List;
import com.battlelancer.seriesguide.dataliberation.model.ListItem;
import com.battlelancer.seriesguide.dataliberation.model.Movie;
import com.battlelancer.seriesguide.dataliberation.model.Season;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.BackupSettings;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.TaskManager;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.uwetrottmann.androidutils.AndroidUtils;
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

    @SuppressLint("StaticFieldLeak") private Context context;
    private String[] languageCodes;
    private boolean isImportingAutoBackup;
    private boolean isUseDefaultFolders;
    private boolean isImportShows;
    private boolean isImportLists;
    private boolean isImportMovies;
    @Nullable private String errorCause;

    public JsonImportTask(Context context, boolean importShows, boolean importLists,
            boolean importMovies) {
        this.context = context.getApplicationContext();
        languageCodes = this.context.getResources().getStringArray(R.array.languageCodesShows);
        isImportingAutoBackup = false;
        isImportShows = importShows;
        isImportLists = importLists;
        isImportMovies = importMovies;
        // use Storage Access Framework on KitKat and up to select custom backup files,
        // on older versions use default folders
        // also auto backup by default uses default folders
        isUseDefaultFolders = !AndroidUtils.isKitKatOrHigher();
    }

    public JsonImportTask(Context context) {
        this(context, true, true, true);
        isImportingAutoBackup = true;
        // use Storage Access Framework on KitKat and up to select custom backup files,
        // on older versions use default folders
        // also auto backup by default uses default folders
        isUseDefaultFolders = !AndroidUtils.isKitKatOrHigher()
                || BackupSettings.isUseAutoBackupDefaultFiles(context);
    }

    @Override
    protected Integer doInBackground(Void... params) {
        // Ensure no large database ops are running
        TaskManager tm = TaskManager.getInstance();
        if (SgSyncAdapter.isSyncActive(context, false) || tm.isAddTaskRunning()) {
            return ERROR_LARGE_DB_OP;
        }

        File importPath = null;
        if (isUseDefaultFolders) {
            // Ensure external storage
            if (!AndroidUtils.isExtStorageAvailable()) {
                return ERROR_STORAGE_ACCESS;
            }
            importPath = JsonExportTask.getExportPath(isImportingAutoBackup);
        }

        // last chance to abort
        if (isCancelled()) {
            return ERROR;
        }

        int result;
        if (isImportShows) {
            result = importData(importPath, JsonExportTask.BACKUP_SHOWS);
            if (result != SUCCESS) {
                return result;
            }
            if (isCancelled()) {
                return ERROR;
            }
        }

        if (isImportLists) {
            result = importData(importPath, JsonExportTask.BACKUP_LISTS);
            if (result != SUCCESS) {
                return result;
            }
            if (isCancelled()) {
                return ERROR;
            }
        }

        if (isImportMovies) {
            result = importData(importPath, JsonExportTask.BACKUP_MOVIES);
            if (result != SUCCESS) {
                return result;
            }
            if (isCancelled()) {
                return ERROR;
            }
        }

        // Renew search table
        DBUtils.rebuildFtsTable(context);

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

    private int importData(File importPath, @JsonExportTask.BackupType int type) {
        // if using default files or non-user custom files the backup task will not create a file
        // if there is no data to export,
        // so make sure to not fail just because a default folder file is missing
        if (!isUseDefaultFolders) {
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

            clearExistingData(type);

            // Access JSON from backup file and try to import data
            FileInputStream in = new FileInputStream(pfd.getFileDescriptor());
            try {
                importFromJson(type, in);

                // let the document provider know we're done.
                pfd.close();
            } catch (JsonParseException | IOException | IllegalStateException e) {
                // the given Json might not be valid or unreadable
                Timber.e(e, "JSON import failed");
                errorCause = e.getMessage();
                return ERROR;
            }
        } else {
            // make sure we can access the backup file
            File backupFile = null;
            if (type == JsonExportTask.BACKUP_SHOWS) {
                backupFile = new File(importPath, JsonExportTask.EXPORT_JSON_FILE_SHOWS);
            } else if (type == JsonExportTask.BACKUP_LISTS) {
                backupFile = new File(importPath, JsonExportTask.EXPORT_JSON_FILE_LISTS);
            } else if (type == JsonExportTask.BACKUP_MOVIES) {
                backupFile = new File(importPath, JsonExportTask.EXPORT_JSON_FILE_MOVIES);
            }
            if (backupFile == null || !backupFile.canRead()) {
                return ERROR_FILE_ACCESS;
            }
            if (!backupFile.exists()) {
                // no backup file, so nothing to restore, skip it
                return SUCCESS;
            }

            FileInputStream in;
            try {
                in = new FileInputStream(backupFile);
            } catch (FileNotFoundException e) {
                Timber.e(e, "Backup file not found.");
                errorCause = e.getMessage();
                return ERROR_FILE_ACCESS;
            }

            clearExistingData(type);

            // Access JSON from backup file and try to import data
            try {
                importFromJson(type, in);
            } catch (JsonParseException | IOException | IllegalStateException e) {
                // the given Json might not be valid or unreadable
                Timber.e(e, "JSON show import failed");
                errorCause = e.getMessage();
                return ERROR;
            }
        }

        return SUCCESS;
    }

    @Nullable
    private Uri getDataBackupFile(@JsonExportTask.BackupType int type) {
        // use import URIs
        // if they are not set getFileUri will fall back to the export URI
        // for auto backup always use the URI data is configured to be exported to
        if (type == JsonExportTask.BACKUP_SHOWS) {
            return BackupSettings.getFileUri(context,
                    isImportingAutoBackup ? BackupSettings.KEY_AUTO_BACKUP_SHOWS_EXPORT_URI
                            : BackupSettings.KEY_SHOWS_IMPORT_URI);
        }
        if (type == JsonExportTask.BACKUP_LISTS) {
            return BackupSettings.getFileUri(context,
                    isImportingAutoBackup ? BackupSettings.KEY_AUTO_BACKUP_LISTS_EXPORT_URI
                            : BackupSettings.KEY_LISTS_IMPORT_URI);
        }
        if (type == JsonExportTask.BACKUP_MOVIES) {
            return BackupSettings.getFileUri(context,
                    isImportingAutoBackup ? BackupSettings.KEY_AUTO_BACKUP_MOVIES_EXPORT_URI
                            : BackupSettings.KEY_MOVIES_IMPORT_URI);
        }
        return null;
    }

    private void clearExistingData(@JsonExportTask.BackupType int type) {
        if (type == JsonExportTask.BACKUP_SHOWS) {
            context.getContentResolver().delete(Shows.CONTENT_URI, null, null);
            context.getContentResolver().delete(Seasons.CONTENT_URI, null, null);
            context.getContentResolver().delete(Episodes.CONTENT_URI, null, null);
        } else if (type == JsonExportTask.BACKUP_LISTS) {
            context.getContentResolver().delete(Lists.CONTENT_URI, null, null);
            context.getContentResolver().delete(ListItems.CONTENT_URI, null, null);
        } else if (type == JsonExportTask.BACKUP_MOVIES) {
            context.getContentResolver().delete(Movies.CONTENT_URI, null, null);
        }
    }

    private void importFromJson(@JsonExportTask.BackupType int type, FileInputStream in)
            throws JsonParseException, IOException, IllegalArgumentException {
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
                addMovieToDatabase(movie);
            }
        }

        reader.endArray();
        reader.close();
    }

    private void addShowToDatabase(Show show) {
        if (show.tvdb_id <= 0) {
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
        // ensure a show will be updated (last_updated might be far into the future)
        if (show.last_updated > System.currentTimeMillis()) {
            show.last_updated = 0;
        }

        ContentValues showValues = show.toContentValues(context, true);
        context.getContentResolver().insert(Shows.CONTENT_URI, showValues);

        if (show.seasons == null || show.seasons.isEmpty()) {
            // no seasons (or episodes)
            return;
        }

        ContentValues[][] seasonsAndEpisodes = buildSeasonAndEpisodeBatches(show);
        if (seasonsAndEpisodes[0] != null && seasonsAndEpisodes[1] != null) {
            // Insert all seasons
            context.getContentResolver().bulkInsert(Seasons.CONTENT_URI, seasonsAndEpisodes[0]);
            // Insert all episodes
            context.getContentResolver().bulkInsert(Episodes.CONTENT_URI, seasonsAndEpisodes[1]);
        }
    }

    /**
     * Returns all seasons and episodes of this show in neat {@link ContentValues} packages put into
     * arrays. The first array returned includes all seasons, the second array all episodes.
     */
    private static ContentValues[][] buildSeasonAndEpisodeBatches(Show show) {
        ArrayList<ContentValues> seasonBatch = new ArrayList<>();
        ArrayList<ContentValues> episodeBatch = new ArrayList<>();

        // Populate arrays...
        for (Season season : show.seasons) {
            if (season.tvdbId <= 0) {
                // valid id is required
                continue;
            }
            if (season.episodes == null || season.episodes.isEmpty()) {
                // episodes required
                continue;
            }

            // add the season...
            seasonBatch.add(season.toContentValues(show.tvdb_id));

            // ...and its episodes
            for (Episode episode : season.episodes) {
                if (episode.tvdbId <= 0) {
                    // valid id is required
                    continue;
                }

                ContentValues episodeValues = episode
                        .toContentValues(show.tvdb_id, season.tvdbId, season.season);
                episodeBatch.add(episodeValues);
            }
        }

        return new ContentValues[][]{
                seasonBatch.size() == 0 ? null
                        : seasonBatch.toArray(new ContentValues[seasonBatch.size()]),
                episodeBatch.size() == 0 ? null
                        : episodeBatch.toArray(new ContentValues[episodeBatch.size()])
        };
    }

    private void addListToDatabase(List list) {
        if (TextUtils.isEmpty(list.listId)) {
            if (TextUtils.isEmpty(list.name)) {
                return; // can't rebuild list id
            }
            list.listId = SeriesGuideContract.Lists.generateListId(list.name);
        }

        // Insert the list
        ContentValues values = new ContentValues();
        values.put(Lists.LIST_ID, list.listId);
        values.put(Lists.NAME, list.name);
        values.put(Lists.ORDER, list.order);
        context.getContentResolver().insert(Lists.CONTENT_URI, values);

        if (list.items == null || list.items.isEmpty()) {
            return;
        }

        // Insert the lists items
        ArrayList<ContentValues> items = new ArrayList<>();
        for (ListItem item : list.items) {
            int type;
            if (ListItemTypesExport.SHOW.equals(item.type)) {
                type = ListItemTypes.SHOW;
            } else if (ListItemTypesExport.SEASON.equals(item.type)) {
                type = ListItemTypes.SEASON;
            } else if (ListItemTypesExport.EPISODE.equals(item.type)) {
                type = ListItemTypes.EPISODE;
            } else {
                // Unknown item type, skip
                continue;
            }

            if (TextUtils.isEmpty(item.listItemId)) {
                if (item.tvdbId <= 0) {
                    continue; // can't rebuild item id
                }
                item.listItemId = SeriesGuideContract.ListItems.generateListItemId(item.tvdbId,
                        type, list.listId);
            }

            ContentValues itemValues = new ContentValues();
            itemValues.put(ListItems.LIST_ITEM_ID, item.listItemId);
            itemValues.put(Lists.LIST_ID, list.listId);
            itemValues.put(ListItems.ITEM_REF_ID, item.tvdbId);
            itemValues.put(ListItems.TYPE, type);

            items.add(itemValues);
        }

        ContentValues[] itemsArray = new ContentValues[items.size()];
        context.getContentResolver().bulkInsert(ListItems.CONTENT_URI, items.toArray(itemsArray));
    }

    private void addMovieToDatabase(Movie movie) {
        ContentValues values = new ContentValues();
        values.put(Movies.TMDB_ID, movie.tmdbId);
        values.put(Movies.IMDB_ID, movie.imdbId);
        values.put(Movies.TITLE, movie.title);
        values.put(Movies.TITLE_NOARTICLE, DBUtils.trimLeadingArticle(movie.title));
        values.put(Movies.RELEASED_UTC_MS, movie.releasedUtcMs);
        values.put(Movies.RUNTIME_MIN, movie.runtimeMin);
        values.put(Movies.POSTER, movie.poster);
        values.put(Movies.IN_COLLECTION, movie.inCollection);
        values.put(Movies.IN_WATCHLIST, movie.inWatchlist);
        values.put(Movies.WATCHED, movie.watched);
        // full dump values
        values.put(Movies.OVERVIEW, movie.overview);

        context.getContentResolver().insert(Movies.CONTENT_URI, values);
    }
}
