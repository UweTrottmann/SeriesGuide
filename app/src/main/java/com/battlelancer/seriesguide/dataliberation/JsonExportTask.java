package com.battlelancer.seriesguide.dataliberation;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.dataliberation.model.Episode;
import com.battlelancer.seriesguide.dataliberation.model.List;
import com.battlelancer.seriesguide.dataliberation.model.ListItem;
import com.battlelancer.seriesguide.dataliberation.model.Movie;
import com.battlelancer.seriesguide.dataliberation.model.Season;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.model.SgEpisode;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;
import com.battlelancer.seriesguide.ui.shows.ShowTools;
import com.battlelancer.seriesguide.util.TaskManager;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.greenrobot.eventbus.EventBus;
import timber.log.Timber;

/**
 * Export the show database to a human-readable JSON file on external storage. By default meta-data
 * like descriptions, ratings, actors, etc. will not be included.
 */
public class JsonExportTask extends AsyncTask<Void, Integer, Integer> {

    public interface OnTaskProgressListener {
        void onProgressUpdate(Integer... values);
    }

    public static final String EXPORT_JSON_FILE_SHOWS = "seriesguide-shows-backup.json";
    public static final String EXPORT_JSON_FILE_LISTS = "seriesguide-lists-backup.json";
    public static final String EXPORT_JSON_FILE_MOVIES = "seriesguide-movies-backup.json";

    public static final int BACKUP_SHOWS = 1;
    public static final int BACKUP_LISTS = 2;
    public static final int BACKUP_MOVIES = 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            BACKUP_SHOWS,
            BACKUP_LISTS,
            BACKUP_MOVIES
    })
    public @interface BackupType {
    }

    private static final int SUCCESS = 1;
    private static final int ERROR_FILE_ACCESS = 0;
    private static final int ERROR = -1;

    /**
     * Show status used when exporting data. Compare with {@link ShowTools.Status}.
     */
    public interface ShowStatusExport {
        String UPCOMING = "upcoming";
        String CONTINUING = "continuing";
        String ENDED = "ended";
        String UNKNOWN = "unknown";
    }

    public interface ListItemTypesExport {
        String SHOW = "show";
        String SEASON = "season";
        String EPISODE = "episode";
    }

    @SuppressLint("StaticFieldLeak") private Context context;
    private OnTaskProgressListener progressListener;
    private boolean isFullDump;
    private boolean isAutoBackupMode;
    @Nullable private final Integer type;
    @Nullable private String errorCause;

    /**
     * Same as {@link JsonExportTask} but allows to set parameters.
     *
     * @param isFullDump Whether to also export meta-data like descriptions, ratings, actors, etc.
     * Increases file size about 2-4 times.
     * @param isAutoBackupMode Whether to run an auto backup, also shows no result toasts.
     */
    public JsonExportTask(Context context, OnTaskProgressListener progressListener,
            boolean isFullDump, boolean isAutoBackupMode, @Nullable Integer type) {
        this.context = context.getApplicationContext();
        this.progressListener = progressListener;
        this.isFullDump = isFullDump;
        this.isAutoBackupMode = isAutoBackupMode;
        this.type = type;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        if (isAutoBackupMode) {
            // Auto backup mode.
            try {
                new AutoBackupTask(this, context).run();
                BackupSettings.setAutoBackupErrorOrNull(context, null);
                return SUCCESS;
            } catch (Exception e) {
                Timber.e(e, "Unable to auto backup.");
                BackupSettings.setAutoBackupErrorOrNull(context,
                        e.getClass().getSimpleName() + ": " + e.getMessage());
                return ERROR;
            }
        } else {
            // Manual backup mode.

            if (isCancelled()) {
                return ERROR;
            }

            int result = SUCCESS;
            if (type == null || type == BACKUP_SHOWS) {
                result = exportData(BACKUP_SHOWS);
                if (result != SUCCESS) {
                    return result;
                }
                if (isCancelled()) {
                    return ERROR;
                }
            }

            if (type == null || type == BACKUP_LISTS) {
                result = exportData(BACKUP_LISTS);
                if (result != SUCCESS) {
                    return result;
                }
                if (isCancelled()) {
                    return ERROR;
                }
            }

            if (type == null || type == BACKUP_MOVIES) {
                result = exportData(BACKUP_MOVIES);
            }

            return result;
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (progressListener != null) {
            progressListener.onProgressUpdate(values);
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        TaskManager.getInstance().releaseBackupTaskRef();

        if (!isAutoBackupMode) {
            int messageId;
            boolean showIndefinite;
            switch (result) {
                case SUCCESS:
                    messageId = R.string.backup_success;
                    showIndefinite = false;
                    break;
                case ERROR_FILE_ACCESS:
                    messageId = R.string.backup_failed_file_access;
                    showIndefinite = true;
                    break;
                default:
                    messageId = R.string.backup_failed;
                    showIndefinite = true;
                    break;
            }
            EventBus.getDefault()
                    .post(new DataLiberationFragment.LiberationResultEvent(
                            context.getString(messageId), errorCause, showIndefinite));
        } else {
            EventBus.getDefault().post(new DataLiberationFragment.LiberationResultEvent());
        }
    }

    private int exportData(@BackupType int type) {
        // check if there is any data to export
        Cursor data = getDataCursor(type);
        if (data == null) {
            // query failed
            return ERROR;
        }
        if (data.getCount() == 0) {
            // There is no data? Done.
            data.close();
            return SUCCESS;
        }

        publishProgress(data.getCount(), 0);

        // try to export all data
        try {
            // ensure the user has selected a backup file
            Uri backupFileUri = getDataBackupFile(type);
            if (backupFileUri == null) {
                return ERROR_FILE_ACCESS;
            }

            ParcelFileDescriptor pfd = context.getContentResolver()
                    .openFileDescriptor(backupFileUri, "w");
            if (pfd == null) {
                return ERROR_FILE_ACCESS;
            }
            FileOutputStream out = new FileOutputStream(pfd.getFileDescriptor());

            // Even though using streams and FileOutputStream does not append by
            // default, using Storage Access Framework just overwrites existing
            // bytes, potentially leaving old bytes hanging over:
            // so truncate the file first to clear any existing bytes.
            out.getChannel().truncate(0);

            if (type == BACKUP_SHOWS) {
                writeJsonStreamShows(out, data);
            } else if (type == BACKUP_LISTS) {
                writeJsonStreamLists(out, data);
            } else if (type == BACKUP_MOVIES) {
                writeJsonStreamMovies(out, data);
            }

            // let the document provider know we're done.
            pfd.close();
        } catch (FileNotFoundException e) {
            Timber.e(e, "Backup file not found.");
            removeBackupFileUri(type);
            errorCause = e.getMessage();
            return ERROR_FILE_ACCESS;
        } catch (IOException | SecurityException e) {
            Timber.e(e, "Could not access backup file.");
            removeBackupFileUri(type);
            errorCause = e.getMessage();
            return ERROR_FILE_ACCESS;
        } catch (JsonParseException e) {
            Timber.e(e, "JSON export failed.");
            errorCause = e.getMessage();
            return ERROR;
        } catch (Exception e) {
            Timber.e(e, "Backup failed.");
            errorCause = e.getMessage();
            return ERROR;
        } finally {
            data.close();
        }

        return SUCCESS;
    }

    @Nullable
    Cursor getDataCursor(@BackupType int type) {
        if (type == BACKUP_SHOWS) {
            return context.getContentResolver().query(
                    Shows.CONTENT_URI, ShowsQuery.PROJECTION_FULL,
                    null, null, Shows.SORT_TITLE);
        }
        if (type == BACKUP_LISTS) {
            return context.getContentResolver()
                    .query(SeriesGuideContract.Lists.CONTENT_URI,
                            ListsQuery.PROJECTION, null, null,
                            SeriesGuideContract.Lists.SORT_ORDER_THEN_NAME);
        }
        if (type == BACKUP_MOVIES) {
            return context.getContentResolver()
                    .query(Movies.CONTENT_URI,
                            MoviesQuery.PROJECTION, null, null, MoviesQuery.SORT_ORDER);
        }
        return null;
    }

    @Nullable
    Uri getDataBackupFile(@BackupType int type) {
        return BackupSettings.getExportFileUri(context, type, isAutoBackupMode);
    }

    void removeBackupFileUri(@BackupType int type) {
        BackupSettings.storeExportFileUri(context, type, null, isAutoBackupMode);
    }

    void writeJsonStreamShows(OutputStream out, Cursor shows) throws IOException {
        int numTotal = shows.getCount();
        int numExported = 0;

        Gson gson = new Gson();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        writer.beginArray();

        while (shows.moveToNext()) {
            if (isCancelled()) {
                break;
            }

            Show show = new Show();
            show.tvdb_id = shows.getInt(ShowsQuery.ID);
            show.tvdb_slug = shows.getString(ShowsQuery.SLUG);
            show.title = shows.getString(ShowsQuery.TITLE);
            show.favorite = shows.getInt(ShowsQuery.FAVORITE) == 1;
            show.notify = shows.getInt(ShowsQuery.NOTIFY) == 1;
            show.hidden = shows.getInt(ShowsQuery.HIDDEN) == 1;
            show.language = shows.getString(ShowsQuery.LANGUAGE);
            show.release_time = shows.getInt(ShowsQuery.RELEASE_TIME);
            show.release_weekday = shows.getInt(ShowsQuery.RELEASE_WEEKDAY);
            show.release_timezone = shows.getString(ShowsQuery.RELEASE_TIMEZONE);
            show.country = shows.getString(ShowsQuery.RELEASE_COUNTRY);
            show.last_watched_episode = shows.getInt(ShowsQuery.LASTWATCHEDID);
            show.last_watched_ms = shows.getLong(ShowsQuery.LASTWATCHED_MS);
            show.poster = shows.getString(ShowsQuery.POSTER);
            show.poster_small = shows.getString(ShowsQuery.POSTER_THUMBNAIL);
            show.content_rating = shows.getString(ShowsQuery.CONTENTRATING);
            show.status = DataLiberationTools.decodeShowStatus(shows.getInt(ShowsQuery.STATUS));
            show.runtime = shows.getInt(ShowsQuery.RUNTIME);
            show.network = shows.getString(ShowsQuery.NETWORK);
            show.imdb_id = shows.getString(ShowsQuery.IMDBID);
            show.trakt_id = shows.getInt(ShowsQuery.TRAKT_ID);
            show.first_aired = shows.getString(ShowsQuery.FIRSTAIRED);
            show.rating_user = shows.getInt(ShowsQuery.RATING_USER);
            if (isFullDump) {
                show.overview = shows.getString(ShowsQuery.OVERVIEW);
                show.rating = shows.getDouble(ShowsQuery.RATING_GLOBAL);
                show.rating_votes = shows.getInt(ShowsQuery.RATING_VOTES);
                show.genres = shows.getString(ShowsQuery.GENRES);
                show.last_updated = shows.getLong(ShowsQuery.LAST_UPDATED);
                show.last_edited = shows.getLong(ShowsQuery.LAST_EDITED);
            }

            addSeasons(show);

            gson.toJson(show, Show.class, writer);

            publishProgress(numTotal, ++numExported);
        }

        writer.endArray();
        writer.close();
    }

    private void addSeasons(Show show) {
        show.seasons = new ArrayList<>();
        final Cursor seasonsCursor = context.getContentResolver().query(
                Seasons.buildSeasonsOfShowUri(String.valueOf(show.tvdb_id)),
                new String[]{
                        Seasons._ID,
                        Seasons.COMBINED
                }, null, null, null
        );

        if (seasonsCursor == null) {
            return;
        }

        while (seasonsCursor.moveToNext()) {
            Season season = new Season();
            season.tvdbId = seasonsCursor.getInt(0);
            season.season = seasonsCursor.getInt(1);

            addEpisodes(season);

            show.seasons.add(season);
        }

        seasonsCursor.close();
    }

    private void addEpisodes(Season season) {
        season.episodes = new ArrayList<>();
        java.util.List<SgEpisode> episodes = SgRoomDatabase.getInstance(context)
                .episodeHelper()
                .getSeason(season.tvdbId);

        for (SgEpisode episodeDb : episodes) {
            Episode episodeExport = new Episode();
            episodeExport.tvdbId = episodeDb.tvdbId;
            episodeExport.episode = episodeDb.number;
            episodeExport.episodeAbsolute = episodeDb.absoluteNumber;
            episodeExport.episodeDvd = episodeDb.dvdNumber;
            int episodeFlag = episodeDb.watched;
            episodeExport.watched = EpisodeTools.isWatched(episodeFlag);
            episodeExport.skipped = EpisodeTools.isSkipped(episodeFlag);
            episodeExport.collected = episodeDb.collected;
            episodeExport.title = episodeDb.title;
            episodeExport.firstAired = episodeDb.firstReleasedMs;
            episodeExport.imdbId = episodeDb.imdbId;
            episodeExport.rating_user = episodeDb.ratingUser;
            if (isFullDump) {
                episodeExport.overview = episodeDb.overview;
                episodeExport.image = episodeDb.image;
                episodeExport.writers = episodeDb.writers;
                episodeExport.gueststars = episodeDb.guestStars;
                episodeExport.directors = episodeDb.directors;
                episodeExport.rating = episodeDb.ratingGlobal;
                episodeExport.rating_votes = episodeDb.ratingVotes;
                episodeExport.lastEdited = episodeDb.lastEditedSec;
            }

            season.episodes.add(episodeExport);
        }
    }

    void writeJsonStreamLists(OutputStream out, Cursor lists) throws IOException {
        int numTotal = lists.getCount();
        int numExported = 0;

        Gson gson = new Gson();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        writer.beginArray();

        while (lists.moveToNext()) {
            if (isCancelled()) {
                break;
            }

            List list = new List();
            list.listId = lists.getString(ListsQuery.ID);
            list.name = lists.getString(ListsQuery.NAME);
            list.order = lists.getInt(ListsQuery.ORDER);

            addListItems(list);

            gson.toJson(list, List.class, writer);

            publishProgress(numTotal, ++numExported);
        }

        writer.endArray();
        writer.close();
    }

    private void addListItems(List list) {
        final Cursor listItems = context.getContentResolver().query(
                ListItems.CONTENT_URI, ListItemsQuery.PROJECTION,
                ListItemsQuery.SELECTION,
                new String[]{
                        list.listId
                }, null
        );
        if (listItems == null) {
            return;
        }

        list.items = new ArrayList<>();
        while (listItems.moveToNext()) {
            ListItem item = new ListItem();
            item.listItemId = listItems.getString(ListItemsQuery.ID);
            item.tvdbId = listItems.getInt(ListItemsQuery.ITEM_REF_ID);
            switch (listItems.getInt(ListItemsQuery.TYPE)) {
                case ListItemTypes.SHOW:
                    item.type = ListItemTypesExport.SHOW;
                    break;
                case ListItemTypes.SEASON:
                    item.type = ListItemTypesExport.SEASON;
                    break;
                case ListItemTypes.EPISODE:
                    item.type = ListItemTypesExport.EPISODE;
                    break;
            }

            list.items.add(item);
        }

        listItems.close();
    }

    void writeJsonStreamMovies(OutputStream out, Cursor movies) throws IOException {
        int numTotal = movies.getCount();
        int numExported = 0;

        Gson gson = new Gson();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        writer.beginArray();

        while (movies.moveToNext()) {
            if (isCancelled()) {
                break;
            }

            Movie movie = new Movie();
            movie.tmdbId = movies.getInt(MoviesQuery.TMDB_ID);
            movie.imdbId = movies.getString(MoviesQuery.IMDB_ID);
            movie.title = movies.getString(MoviesQuery.TITLE);
            movie.releasedUtcMs = movies.getLong(MoviesQuery.RELEASED_UTC_MS);
            movie.runtimeMin = movies.getInt(MoviesQuery.RUNTIME_MIN);
            movie.poster = movies.getString(MoviesQuery.POSTER);
            movie.inCollection = movies.getInt(MoviesQuery.IN_COLLECTION) == 1;
            movie.inWatchlist = movies.getInt(MoviesQuery.IN_WATCHLIST) == 1;
            movie.watched = movies.getInt(MoviesQuery.WATCHED) == 1;
            movie.lastUpdatedMs = movies.getLong(MoviesQuery.LAST_UPDATED);

            if (isFullDump) {
                movie.overview = movies.getString(MoviesQuery.OVERVIEW);
            }

            gson.toJson(movie, Movie.class, writer);

            publishProgress(numTotal, ++numExported);
        }

        writer.endArray();
        writer.close();
    }

    public interface ShowsQuery {
        String[] PROJECTION_FULL = new String[]{
                Shows._ID,
                Shows.TITLE,
                Shows.FAVORITE,
                Shows.NOTIFY,
                Shows.HIDDEN,
                Shows.RELEASE_TIME,
                Shows.RELEASE_WEEKDAY,
                Shows.RELEASE_TIMEZONE,
                Shows.RELEASE_COUNTRY,
                Shows.LASTWATCHEDID,
                Shows.LASTWATCHED_MS,
                Shows.POSTER,
                Shows.POSTER_SMALL,
                Shows.CONTENTRATING,
                Shows.STATUS,
                Shows.RUNTIME,
                Shows.NETWORK,
                Shows.IMDBID,
                Shows.TRAKT_ID,
                Shows.FIRST_RELEASE,
                Shows.RATING_USER,
                Shows.LANGUAGE,
                Shows.OVERVIEW,
                Shows.RATING_GLOBAL,
                Shows.RATING_VOTES,
                Shows.GENRES,
                Shows.LASTUPDATED,
                Shows.LASTEDIT,
                Shows.SLUG
        };

        int ID = 0;
        int TITLE = 1;
        int FAVORITE = 2;
        int NOTIFY = 3;
        int HIDDEN = 4;
        int RELEASE_TIME = 5;
        int RELEASE_WEEKDAY = 6;
        int RELEASE_TIMEZONE = 7;
        int RELEASE_COUNTRY = 8;
        int LASTWATCHEDID = 9;
        int LASTWATCHED_MS = 10;
        int POSTER = 11;
        int POSTER_THUMBNAIL = 12;
        int CONTENTRATING = 13;
        int STATUS = 14;
        int RUNTIME = 15;
        int NETWORK = 16;
        int IMDBID = 17;
        int TRAKT_ID = 18;
        int FIRSTAIRED = 19;
        int RATING_USER = 20;
        int LANGUAGE = 21;
        int OVERVIEW = 22;
        int RATING_GLOBAL = 23;
        int RATING_VOTES = 24;
        int GENRES = 25;
        int LAST_UPDATED = 26;
        int LAST_EDITED = 27;
        int SLUG = 28;
    }

    public interface ListsQuery {
        String[] PROJECTION = new String[]{
                SeriesGuideContract.Lists.LIST_ID,
                SeriesGuideContract.Lists.NAME,
                SeriesGuideContract.Lists.ORDER
        };

        int ID = 0;
        int NAME = 1;
        int ORDER = 2;
    }

    public interface ListItemsQuery {
        String[] PROJECTION = new String[]{
                ListItems.LIST_ITEM_ID, SeriesGuideContract.Lists.LIST_ID, ListItems.ITEM_REF_ID,
                ListItems.TYPE
        };

        String SELECTION = SeriesGuideContract.Lists.LIST_ID + "=?";

        int ID = 0;
        int LIST_ID = 1;
        int ITEM_REF_ID = 2;
        int TYPE = 3;
    }

    public interface MoviesQuery {
        String[] PROJECTION = new String[]{
                Movies._ID,
                Movies.TMDB_ID,
                Movies.IMDB_ID,
                Movies.TITLE,
                Movies.RELEASED_UTC_MS,
                Movies.RUNTIME_MIN,
                Movies.POSTER,
                Movies.IN_COLLECTION,
                Movies.IN_WATCHLIST,
                Movies.WATCHED,
                Movies.LAST_UPDATED,
                Movies.OVERVIEW
        };

        String SORT_ORDER = Movies.TITLE + " COLLATE NOCASE ASC";

        int TMDB_ID = 1;
        int IMDB_ID = 2;
        int TITLE = 3;
        int RELEASED_UTC_MS = 4;
        int RUNTIME_MIN = 5;
        int POSTER = 6;
        int IN_COLLECTION = 7;
        int IN_WATCHLIST = 8;
        int WATCHED = 9;
        int LAST_UPDATED = 10;
        // only in FULL dump
        int OVERVIEW = 11;
    }
}
