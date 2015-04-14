/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.dataliberation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.dataliberation.model.Episode;
import com.battlelancer.seriesguide.dataliberation.model.List;
import com.battlelancer.seriesguide.dataliberation.model.ListItem;
import com.battlelancer.seriesguide.dataliberation.model.Movie;
import com.battlelancer.seriesguide.dataliberation.model.Season;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.interfaces.OnTaskFinishedListener;
import com.battlelancer.seriesguide.interfaces.OnTaskProgressListener;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.stream.JsonWriter;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import timber.log.Timber;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;

/**
 * Export the show database to a human-readable JSON file on external storage. By default meta-data
 * like descriptions, ratings, actors, etc. will not be included.
 */
public class JsonExportTask extends AsyncTask<Void, Integer, Integer> {

    public static final String EXPORT_FOLDER = "SeriesGuide";
    public static final String EXPORT_FOLDER_AUTO = "SeriesGuide" + File.separator + "AutoBackup";
    public static final String EXPORT_JSON_FILE_SHOWS = "sg-shows-export.json";
    public static final String EXPORT_JSON_FILE_LISTS = "sg-lists-export.json";
    public static final String EXPORT_JSON_FILE_MOVIES = "sg-movies-export.json";

    private static final int SUCCESS = 1;
    private static final int ERROR_STORAGE_ACCESS = 0;
    private static final int ERROR = -1;

    /**
     * Show status used when exporting data. Compare with {@link com.battlelancer.seriesguide.util.ShowTools.Status}.
     */
    public interface ShowStatusExport {
        String CONTINUING = "continuing";
        String ENDED = "ended";
        String UNKNOWN = "unknown";
    }

    public interface ListItemTypesExport {
        String SHOW = "show";
        String SEASON = "season";
        String EPISODE = "episode";
    }

    private Context mContext;
    private OnTaskProgressListener mProgressListener;
    private OnTaskFinishedListener mListener;
    private boolean mIsFullDump;
    private boolean mIsAutoBackupMode;

    public static File getExportPath(boolean isAutoBackupMode) {
        return new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                isAutoBackupMode ? EXPORT_FOLDER_AUTO : EXPORT_FOLDER);
    }

    /**
     * Same as {@link JsonExportTask} but allows to set parameters.
     *
     * @param isFullDump Whether to also export meta-data like descriptions, ratings, actors, etc.
     * Increases file size about 2-4 times.
     * @param isSilentMode Whether to show result toasts.
     */
    public JsonExportTask(Context context, OnTaskProgressListener progressListener,
            OnTaskFinishedListener listener, boolean isFullDump,
            boolean isSilentMode) {
        mContext = context.getApplicationContext();
        mProgressListener = progressListener;
        mListener = listener;
        mIsFullDump = isFullDump;
        mIsAutoBackupMode = isSilentMode;
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    protected Integer doInBackground(Void... params) {
        // Ensure external storage is available
        if (!AndroidUtils.isExtStorageAvailable()) {
            return ERROR_STORAGE_ACCESS;
        }

        // Ensure the export directory exists
        File exportPath = getExportPath(mIsAutoBackupMode);
        exportPath.mkdirs();

        int result = exportShows(exportPath);
        if (result == ERROR || isCancelled()) {
            return ERROR;
        }

        result = exportLists(exportPath);
        if (result == ERROR || isCancelled()) {
            return ERROR;
        }

        result = exportMovies(exportPath);
        if (result == ERROR || isCancelled()) {
            return ERROR;
        }

        if (mIsAutoBackupMode) {
            // store current time = last backup time
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            prefs.edit().putLong(AdvancedSettings.KEY_LASTBACKUP, System.currentTimeMillis())
                    .commit();
        }

        return SUCCESS;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (mProgressListener != null) {
            mProgressListener.onProgressUpdate(values);
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (!mIsAutoBackupMode) {
            int messageId;
            switch (result) {
                case SUCCESS:
                    messageId = R.string.backup_success;
                    break;
                case ERROR_STORAGE_ACCESS:
                    messageId = R.string.backup_failed_nosd;
                    break;
                default:
                    messageId = R.string.backup_failed;
                    break;
            }
            Toast.makeText(mContext, messageId, Toast.LENGTH_LONG).show();
        }

        if (mListener != null) {
            mListener.onTaskFinished();
        }
    }

    private int exportShows(File exportPath) {
        final Cursor shows = mContext.getContentResolver().query(
                Shows.CONTENT_URI,
                mIsFullDump ? ShowsQuery.PROJECTION_FULL : ShowsQuery.PROJECTION,
                null, null, ShowsQuery.SORT);
        if (shows == null) {
            return ERROR;
        }
        if (shows.getCount() == 0) {
            // There are no shows? Done.
            shows.close();
            return SUCCESS;
        }

        publishProgress(shows.getCount(), 0);

        File backup = new File(exportPath, EXPORT_JSON_FILE_SHOWS);
        try {
            OutputStream out = new FileOutputStream(backup);

            writeJsonStreamShows(out, shows);
        } catch (JsonIOException | IOException e) {
            // Also catch IO exception as we want to know if exporting fails due
            // to a JsonSyntaxException
            Timber.e(e, "JSON shows export failed");
            return ERROR;
        } finally {
            shows.close();
        }

        return SUCCESS;
    }

    private void writeJsonStreamShows(OutputStream out, Cursor shows) throws IOException {
        int numTotal = shows.getCount();
        int numExported = 0;

        Gson gson = new Gson();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.beginArray();

        while (shows.moveToNext()) {
            if (isCancelled()) {
                break;
            }

            Show show = new Show();
            show.tvdbId = shows.getInt(ShowsQuery.ID);
            show.title = shows.getString(ShowsQuery.TITLE);
            show.favorite = shows.getInt(ShowsQuery.FAVORITE) == 1;
            show.hidden = shows.getInt(ShowsQuery.HIDDEN) == 1;
            show.release_time = shows.getInt(ShowsQuery.RELEASE_TIME);
            show.release_weekday = shows.getInt(ShowsQuery.RELEASE_WEEKDAY);
            show.release_timezone = shows.getString(ShowsQuery.RELEASE_TIMEZONE);
            show.country = shows.getString(ShowsQuery.RELEASE_COUNTRY);
            show.lastWatchedEpisode = shows.getInt(ShowsQuery.LASTWATCHEDID);
            show.poster = shows.getString(ShowsQuery.POSTER);
            show.contentRating = shows.getString(ShowsQuery.CONTENTRATING);
            show.status = DataLiberationTools.decodeShowStatus(shows.getInt(ShowsQuery.STATUS));
            show.runtime = shows.getInt(ShowsQuery.RUNTIME);
            show.network = shows.getString(ShowsQuery.NETWORK);
            show.imdbId = shows.getString(ShowsQuery.IMDBID);
            show.firstAired = shows.getString(ShowsQuery.FIRSTAIRED);
            show.rating_user = shows.getInt(ShowsQuery.RATING_USER);
            if (mIsFullDump) {
                show.overview = shows.getString(ShowsQuery.OVERVIEW);
                show.rating = shows.getDouble(ShowsQuery.RATING_GLOBAL);
                show.rating_votes = shows.getInt(ShowsQuery.RATING_VOTES);
                show.genres = shows.getString(ShowsQuery.GENRES);
                show.actors = shows.getString(ShowsQuery.ACTORS);
                show.lastUpdated = shows.getLong(ShowsQuery.LAST_UPDATED);
                show.lastEdited = shows.getLong(ShowsQuery.LAST_EDITED);
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
        final Cursor seasonsCursor = mContext.getContentResolver().query(
                Seasons.buildSeasonsOfShowUri(String.valueOf(show.tvdbId)),
                new String[] {
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
        final Cursor episodesCursor = mContext.getContentResolver().query(
                Episodes.buildEpisodesOfSeasonUri(String.valueOf(season.tvdbId)),
                mIsFullDump ? EpisodesQuery.PROJECTION_FULL : EpisodesQuery.PROJECTION, null, null,
                EpisodesQuery.SORT);

        if (episodesCursor == null) {
            return;
        }

        while (episodesCursor.moveToNext()) {
            Episode episode = new Episode();
            episode.tvdbId = episodesCursor.getInt(EpisodesQuery.ID);
            episode.episode = episodesCursor.getInt(EpisodesQuery.NUMBER);
            episode.episodeAbsolute = episodesCursor.getInt(EpisodesQuery.NUMBER_ABSOLUTE);
            episode.episodeDvd = episodesCursor.getDouble(EpisodesQuery.NUMBER_DVD);
            int episodeFlag = episodesCursor.getInt(EpisodesQuery.WATCHED);
            episode.watched = EpisodeTools.isWatched(episodeFlag);
            episode.skipped = EpisodeTools.isSkipped(episodeFlag);
            episode.collected = episodesCursor.getInt(EpisodesQuery.COLLECTED) == 1;
            episode.title = episodesCursor.getString(EpisodesQuery.TITLE);
            episode.firstAired = episodesCursor.getLong(EpisodesQuery.FIRSTAIRED);
            episode.imdbId = episodesCursor.getString(EpisodesQuery.IMDBID);
            episode.rating_user = episodesCursor.getInt(EpisodesQuery.RATING_USER);
            if (mIsFullDump) {
                episode.overview = episodesCursor.getString(EpisodesQuery.OVERVIEW);
                episode.image = episodesCursor.getString(EpisodesQuery.IMAGE);
                episode.writers = episodesCursor.getString(EpisodesQuery.WRITERS);
                episode.gueststars = episodesCursor.getString(EpisodesQuery.GUESTSTARS);
                episode.directors = episodesCursor.getString(EpisodesQuery.DIRECTORS);
                episode.rating = episodesCursor.getDouble(EpisodesQuery.RATING_GLOBAL);
                episode.rating_votes = episodesCursor.getInt(EpisodesQuery.RATING_VOTES);
                episode.lastEdited = episodesCursor.getLong(EpisodesQuery.LAST_EDITED);
            }

            season.episodes.add(episode);
        }

        episodesCursor.close();
    }

    private int exportLists(File exportPath) {
        final Cursor lists = mContext.getContentResolver()
                .query(SeriesGuideContract.Lists.CONTENT_URI,
                        ListsQuery.PROJECTION, null, null,
                        SeriesGuideContract.Lists.SORT_ORDER_THEN_NAME);
        if (lists == null) {
            return ERROR;
        }
        if (lists.getCount() == 0) {
            // There are no lists? Done.
            lists.close();
            return SUCCESS;
        }

        publishProgress(lists.getCount(), 0);

        File backupLists = new File(exportPath, EXPORT_JSON_FILE_LISTS);
        try {
            OutputStream out = new FileOutputStream(backupLists);

            writeJsonStreamLists(out, lists);
        } catch (JsonIOException | IOException e) {
            // Only catch IO exception as we want to know if exporting fails due
            // to a JsonSyntaxException
            Timber.e(e, "JSON lists export failed");
            return ERROR;
        } finally {
            lists.close();
        }

        return SUCCESS;
    }

    private void writeJsonStreamLists(OutputStream out, Cursor lists) throws IOException {
        int numTotal = lists.getCount();
        int numExported = 0;

        Gson gson = new Gson();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
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
        final Cursor listItems = mContext.getContentResolver().query(
                ListItems.CONTENT_URI, ListItemsQuery.PROJECTION,
                ListItemsQuery.SELECTION,
                new String[] {
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

    private int exportMovies(File exportPath) {
        final Cursor movies = mContext.getContentResolver()
                .query(Movies.CONTENT_URI,
                        MoviesQuery.PROJECTION, null, null, MoviesQuery.SORT_ORDER);
        if (movies == null) {
            return ERROR;
        }
        if (movies.getCount() == 0) {
            // There are no movies? Done.
            movies.close();
            return SUCCESS;
        }

        publishProgress(movies.getCount(), 0);

        File backupFile = new File(exportPath, EXPORT_JSON_FILE_MOVIES);
        try {
            OutputStream out = new FileOutputStream(backupFile);

            writeJsonStreamMovies(out, movies);
        } catch (JsonIOException | IOException e) {
            // Only catch IO exception as we want to know if exporting fails due
            // to a JsonSyntaxException
            Timber.e(e, "JSON movies export failed");
            return ERROR;
        } finally {
            movies.close();
        }

        return SUCCESS;
    }

    private void writeJsonStreamMovies(OutputStream out, Cursor movies) throws IOException {
        int numTotal = movies.getCount();
        int numExported = 0;

        Gson gson = new Gson();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
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
            if (mIsFullDump) {
                movie.overview = movies.getString(MoviesQuery.OVERVIEW);
            }

            gson.toJson(movie, Movie.class, writer);

            publishProgress(numTotal, ++numExported);
        }

        writer.endArray();
        writer.close();
    }

    public interface ShowsQuery {
        String[] PROJECTION = new String[] {
                Shows._ID,
                Shows.TITLE,
                Shows.FAVORITE,
                Shows.HIDDEN,
                Shows.RELEASE_TIME,
                Shows.RELEASE_WEEKDAY,
                Shows.RELEASE_TIMEZONE,
                Shows.RELEASE_COUNTRY,
                Shows.LASTWATCHEDID,
                Shows.POSTER,
                Shows.CONTENTRATING,
                Shows.STATUS,
                Shows.RUNTIME,
                Shows.NETWORK,
                Shows.IMDBID,
                Shows.FIRST_RELEASE,
                Shows.RATING_USER
        };
        String[] PROJECTION_FULL = new String[] {
                Shows._ID,
                Shows.TITLE,
                Shows.FAVORITE,
                Shows.HIDDEN,
                Shows.RELEASE_TIME,
                Shows.RELEASE_WEEKDAY,
                Shows.RELEASE_TIMEZONE,
                Shows.RELEASE_COUNTRY,
                Shows.LASTWATCHEDID,
                Shows.POSTER,
                Shows.CONTENTRATING,
                Shows.STATUS,
                Shows.RUNTIME,
                Shows.NETWORK,
                Shows.IMDBID,
                Shows.FIRST_RELEASE,
                Shows.RATING_USER,
                Shows.OVERVIEW,
                Shows.RATING_GLOBAL,
                Shows.RATING_VOTES,
                Shows.GENRES,
                Shows.ACTORS,
                Shows.LASTUPDATED,
                Shows.LASTEDIT
        };

        String SORT = Shows.TITLE + " COLLATE NOCASE ASC";

        int ID = 0;
        int TITLE = 1;
        int FAVORITE = 2;
        int HIDDEN = 3;
        int RELEASE_TIME = 4;
        int RELEASE_WEEKDAY = 5;
        int RELEASE_TIMEZONE = 6;
        int RELEASE_COUNTRY = 7;
        int LASTWATCHEDID = 8;
        int POSTER = 9;
        int CONTENTRATING = 10;
        int STATUS = 11;
        int RUNTIME = 12;
        int NETWORK = 13;
        int IMDBID = 14;
        int FIRSTAIRED = 15;
        int RATING_USER = 16;
        // Full dump only
        int OVERVIEW = 17;
        int RATING_GLOBAL = 18;
        int RATING_VOTES = 19;
        int GENRES = 20;
        int ACTORS = 21;
        int LAST_UPDATED = 22;
        int LAST_EDITED = 23;
    }

    public interface EpisodesQuery {
        String[] PROJECTION = new String[] {
                Episodes._ID,
                Episodes.NUMBER,
                Episodes.ABSOLUTE_NUMBER,
                Episodes.WATCHED,
                Episodes.COLLECTED,
                Episodes.TITLE,
                Episodes.FIRSTAIREDMS,
                Episodes.IMDBID,
                Episodes.DVDNUMBER,
                Episodes.RATING_USER
        };
        String[] PROJECTION_FULL = new String[] {
                Episodes._ID,
                Episodes.NUMBER,
                Episodes.ABSOLUTE_NUMBER,
                Episodes.WATCHED,
                Episodes.COLLECTED,
                Episodes.TITLE,
                Episodes.FIRSTAIREDMS,
                Episodes.IMDBID,
                Episodes.DVDNUMBER,
                Episodes.RATING_USER,
                // Full dump only
                Episodes.OVERVIEW,
                Episodes.IMAGE,
                Episodes.WRITERS,
                Episodes.GUESTSTARS,
                Episodes.DIRECTORS,
                Episodes.RATING_GLOBAL,
                Episodes.RATING_VOTES,
                Episodes.LAST_EDITED
        };

        String SORT = Episodes.NUMBER + " ASC";

        int ID = 0;
        int NUMBER = 1;
        int NUMBER_ABSOLUTE = 2;
        int WATCHED = 3;
        int COLLECTED = 4;
        int TITLE = 5;
        int FIRSTAIRED = 6;
        int IMDBID = 7;
        int NUMBER_DVD = 8;
        int RATING_USER = 9;
        // Full dump only
        int OVERVIEW = 10;
        int IMAGE = 11;
        int WRITERS = 12;
        int GUESTSTARS = 13;
        int DIRECTORS = 14;
        int RATING_GLOBAL = 15;
        int RATING_VOTES = 16;
        int LAST_EDITED = 17;
    }

    public interface ListsQuery {
        String[] PROJECTION = new String[] {
                SeriesGuideContract.Lists.LIST_ID,
                SeriesGuideContract.Lists.NAME,
                SeriesGuideContract.Lists.ORDER
        };

        int ID = 0;
        int NAME = 1;
        int ORDER = 2;
    }

    public interface ListItemsQuery {
        String[] PROJECTION = new String[] {
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
        String[] PROJECTION = new String[] {
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
        // only in FULL dump
        int OVERVIEW = 10;
    }
}
