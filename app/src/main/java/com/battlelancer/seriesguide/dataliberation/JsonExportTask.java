package com.battlelancer.seriesguide.dataliberation;

import android.annotation.SuppressLint;
import android.content.Context;
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
import com.battlelancer.seriesguide.model.SgEpisode2;
import com.battlelancer.seriesguide.model.SgList;
import com.battlelancer.seriesguide.model.SgListItem;
import com.battlelancer.seriesguide.model.SgMovie;
import com.battlelancer.seriesguide.model.SgSeason2;
import com.battlelancer.seriesguide.model.SgShow2;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;
import com.battlelancer.seriesguide.ui.shows.ShowTools;
import com.battlelancer.seriesguide.util.Errors;
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
        String IN_PRODUCTION = "in_production";
        String PILOT = "pilot";
        String CANCELED = "canceled";
        String UPCOMING = "upcoming";
        String CONTINUING = "continuing";
        String ENDED = "ended";
        String UNKNOWN = "unknown";
    }

    public interface ListItemTypesExport {
        String SHOW = "show";
        String TMDB_SHOW = "tmdb-show";
        String SEASON = "season";
        String EPISODE = "episode";
    }

    @SuppressLint("StaticFieldLeak") private final Context context;
    private final OnTaskProgressListener progressListener;
    private final boolean isFullDump;
    private final boolean isAutoBackupMode;
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
                Errors.logAndReport("Unable to auto backup.", e);
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
                writeJsonStreamShows(out);
            } else if (type == BACKUP_LISTS) {
                writeJsonStreamLists(out);
            } else if (type == BACKUP_MOVIES) {
                writeJsonStreamMovies(out);
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
            // Only report unexpected errors.
            Errors.logAndReport("Backup failed.", e);
            errorCause = e.getMessage();
            return ERROR;
        }

        return SUCCESS;
    }

    @Nullable
    Uri getDataBackupFile(@BackupType int type) {
        return BackupSettings.getExportFileUri(context, type, isAutoBackupMode);
    }

    void removeBackupFileUri(@BackupType int type) {
        BackupSettings.storeExportFileUri(context, type, null, isAutoBackupMode);
    }

    void writeJsonStreamShows(OutputStream out) throws IOException {
        java.util.List<SgShow2> shows = SgRoomDatabase.getInstance(context).sgShow2Helper()
                .getShowsForExport();

        int numTotal = shows.size();
        int numExported = 0;

        publishProgress(numTotal, 0);

        Gson gson = new Gson();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        writer.beginArray();

        for (SgShow2 sgShow : shows) {
            if (isCancelled()) {
                break;
            }

            Show show = new Show();
            show.tmdb_id = sgShow.getTmdbId();
            show.tvdb_id = sgShow.getTvdbId();
            show.title = sgShow.getTitle();
            show.favorite = sgShow.getFavorite();
            show.notify = sgShow.getNotify();
            show.hidden = sgShow.getHidden();
            show.language = sgShow.getLanguage();
            show.release_time = sgShow.getReleaseTimeOrDefault();
            show.release_weekday = sgShow.getReleaseWeekDayOrDefault();
            show.release_timezone = sgShow.getReleaseTimeZone();
            show.country = sgShow.getReleaseCountry();
            show.last_watched_ms = sgShow.getLastWatchedMs();
            show.poster = sgShow.getPoster();
            show.content_rating = sgShow.getContentRating();
            show.status = DataLiberationTools.decodeShowStatus(sgShow.getStatusOrUnknown());
            show.runtime = sgShow.getRuntime() != null ? sgShow.getRuntime() : 0;
            show.network = sgShow.getNetwork();
            show.imdb_id = sgShow.getImdbId();
            show.trakt_id = sgShow.getTraktId();
            show.first_aired = sgShow.getFirstRelease();
            show.rating_user = sgShow.getRatingUser();
            if (isFullDump) {
                show.overview = sgShow.getOverview();
                show.rating = sgShow.getRatingGlobalOrZero();
                show.rating_votes = sgShow.getRatingVotesOrZero();
                show.genres = sgShow.getGenres();
            }

            addSeasons(show, sgShow.getId());

            gson.toJson(show, Show.class, writer);

            publishProgress(numTotal, ++numExported);
        }

        writer.endArray();
        writer.close();
    }

    private void addSeasons(Show show, long showId) {
        show.seasons = new ArrayList<>();

        java.util.List<SgSeason2> seasons = SgRoomDatabase.getInstance(context)
                .sgSeason2Helper().getSeasonsForExport(showId);
        for (SgSeason2 sgSeason : seasons) {
            Season season = new Season();
            season.tmdb_id = sgSeason.getTmdbId();
            season.tvdbId = sgSeason.getTvdbId();
            season.season = sgSeason.getNumber();

            addEpisodes(season, sgSeason.getId());

            show.seasons.add(season);
        }
    }

    private void addEpisodes(Season season, long seasonId) {
        season.episodes = new ArrayList<>();
        java.util.List<SgEpisode2> episodes = SgRoomDatabase.getInstance(context)
                .sgEpisode2Helper()
                .getEpisodesForExport(seasonId);

        for (SgEpisode2 episodeDb : episodes) {
            Episode episodeExport = new Episode();
            episodeExport.tmdb_id = episodeDb.getTmdbId();
            episodeExport.tvdbId = episodeDb.getTvdbId();
            episodeExport.episode = episodeDb.getNumber();
            episodeExport.episodeAbsolute = episodeDb.getAbsoluteNumber();
            episodeExport.episodeDvd = episodeDb.getDvdNumber();
            int episodeFlag = episodeDb.getWatched();
            episodeExport.watched = EpisodeTools.isWatched(episodeFlag);
            episodeExport.skipped = EpisodeTools.isSkipped(episodeFlag);
            episodeExport.plays = episodeDb.getPlaysOrZero();
            episodeExport.collected = episodeDb.getCollected();
            episodeExport.title = episodeDb.getTitle();
            episodeExport.firstAired = episodeDb.getFirstReleasedMs();
            episodeExport.imdbId = episodeDb.getImdbId();
            episodeExport.rating_user = episodeDb.getRatingUser();
            if (isFullDump) {
                episodeExport.overview = episodeDb.getOverview();
                episodeExport.image = episodeDb.getImage();
                episodeExport.writers = episodeDb.getWriters();
                episodeExport.gueststars = episodeDb.getGuestStars();
                episodeExport.directors = episodeDb.getDirectors();
                episodeExport.rating = episodeDb.getRatingGlobal();
                episodeExport.rating_votes = episodeDb.getRatingVotes();
            }

            season.episodes.add(episodeExport);
        }
    }

    void writeJsonStreamLists(OutputStream out) throws IOException {
        java.util.List<SgList> lists = SgRoomDatabase.getInstance(context).sgListHelper()
                .getListsForExport();

        int numTotal = lists.size();
        int numExported = 0;

        publishProgress(numTotal, 0);

        Gson gson = new Gson();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        writer.beginArray();

        for (SgList sgList : lists) {
            if (isCancelled()) {
                break;
            }

            List list = new List();
            list.listId = sgList.listId;
            list.name = sgList.name;
            list.order = sgList.getOrderOrDefault();

            addListItems(list);

            gson.toJson(list, List.class, writer);

            publishProgress(numTotal, ++numExported);
        }

        writer.endArray();
        writer.close();
    }

    private void addListItems(List list) {
        java.util.List<SgListItem> listItems = SgRoomDatabase.getInstance(context)
                .sgListHelper().getListItemsForExport(list.listId);

        list.items = new ArrayList<>();
        for (SgListItem listItem : listItems) {
            ListItem item = new ListItem();
            item.listItemId = listItem.listItemId;
            item.externalId = listItem.itemRefId;
            // Note: export legacy types so users can get to legacy data if they need to.
            switch (listItem.type) {
                case ListItemTypes.TVDB_SHOW:
                    item.type = ListItemTypesExport.SHOW;
                    break;
                case ListItemTypes.TMDB_SHOW:
                    item.type = ListItemTypesExport.TMDB_SHOW;
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
    }

    void writeJsonStreamMovies(OutputStream out) throws IOException {
        java.util.List<SgMovie> movies = SgRoomDatabase.getInstance(context).movieHelper()
                .getMoviesForExport();

        int numTotal = movies.size();
        int numExported = 0;

        publishProgress(numTotal, 0);

        Gson gson = new Gson();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        writer.beginArray();

        for (SgMovie sgMovie : movies) {
            if (isCancelled()) {
                break;
            }

            Movie movie = new Movie();
            movie.tmdbId = sgMovie.tmdbId;
            movie.imdbId = sgMovie.imdbId;
            movie.title = sgMovie.title;
            movie.releasedUtcMs = sgMovie.getReleasedMsOrDefault();
            movie.runtimeMin = sgMovie.getRuntimeMinOrDefault();
            movie.poster = sgMovie.poster;
            movie.inCollection = sgMovie.inCollection;
            movie.inWatchlist = sgMovie.inWatchlist;
            movie.watched = sgMovie.watched;
            movie.plays = sgMovie.plays;
            movie.lastUpdatedMs = sgMovie.getLastUpdatedOrDefault();

            if (isFullDump) {
                movie.overview = sgMovie.overview;
            }

            gson.toJson(movie, Movie.class, writer);

            publishProgress(numTotal, ++numExported);
        }

        writer.endArray();
        writer.close();
    }
}
