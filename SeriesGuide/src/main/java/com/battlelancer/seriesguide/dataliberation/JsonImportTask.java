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

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.ListItemTypesExport;
import com.battlelancer.seriesguide.dataliberation.model.Episode;
import com.battlelancer.seriesguide.dataliberation.model.List;
import com.battlelancer.seriesguide.dataliberation.model.ListItem;
import com.battlelancer.seriesguide.dataliberation.model.Movie;
import com.battlelancer.seriesguide.dataliberation.model.Season;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.interfaces.OnTaskFinishedListener;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.TaskManager;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import timber.log.Timber;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;

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
    private Context mContext;
    private OnTaskFinishedListener mListener;
    private boolean mIsAutoBackupMode;

    public JsonImportTask(Context context, OnTaskFinishedListener listener,
            boolean isAutoBackupMode) {
        mContext = context.getApplicationContext();
        mListener = listener;
        mIsAutoBackupMode = isAutoBackupMode;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        // Ensure external storage
        if (!AndroidUtils.isExtStorageAvailable()) {
            return ERROR_STORAGE_ACCESS;
        }

        // Ensure no large database ops are running
        TaskManager tm = TaskManager.getInstance(mContext);
        if (SgSyncAdapter.isSyncActive(mContext, false) || tm.isAddTaskRunning()) {
            return ERROR_LARGE_DB_OP;
        }

        // Ensure JSON file is available
        File importPath = JsonExportTask.getExportPath(mIsAutoBackupMode);

        int result = importShows(importPath);
        if (result == ERROR || isCancelled()) {
            return ERROR;
        }

        if (result == SUCCESS) { // only import lists, if show import was successful
            result = importLists(importPath);
            if (result == ERROR || isCancelled()) {
                return ERROR;
            }
        }

        result = importMovies(importPath);
        if (result == ERROR) {
            return ERROR;
        }

        // Renew search table
        DBUtils.rebuildFtsTable(mContext);

        return SUCCESS;
    }

    @Override
    protected void onPostExecute(Integer result) {
        int messageId;
        switch (result) {
            case SUCCESS:
                messageId = R.string.import_success;
                break;
            case ERROR_STORAGE_ACCESS:
                messageId = R.string.import_failed_nosd;
                break;
            case ERROR_FILE_ACCESS:
                messageId = R.string.import_failed_nofile;
                break;
            case ERROR_LARGE_DB_OP:
                messageId = R.string.update_inprogress;
                break;
            default:
                messageId = R.string.import_failed;
                break;
        }
        Toast.makeText(mContext, messageId, Toast.LENGTH_LONG).show();

        if (mListener != null) {
            mListener.onTaskFinished();
        }
    }

    private int importShows(File importPath) {
        File backupShows = new File(importPath, JsonExportTask.EXPORT_JSON_FILE_SHOWS);
        if (!backupShows.exists() || !backupShows.canRead()) {
            return ERROR_FILE_ACCESS;
        }

        // Clean out all existing tables
        mContext.getContentResolver().delete(Shows.CONTENT_URI, null, null);
        mContext.getContentResolver().delete(Seasons.CONTENT_URI, null, null);
        mContext.getContentResolver().delete(Episodes.CONTENT_URI, null, null);
        mContext.getContentResolver().delete(SeriesGuideContract.Lists.CONTENT_URI, null, null);
        mContext.getContentResolver().delete(ListItems.CONTENT_URI, null, null);

        // Access JSON from backup folder to create new database
        try {
            InputStream in = new FileInputStream(backupShows);

            Gson gson = new Gson();

            JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
            reader.beginArray();

            while (reader.hasNext()) {
                Show show = gson.fromJson(reader, Show.class);
                addShowToDatabase(show);
            }

            reader.endArray();
            reader.close();
        } catch (JsonParseException | IOException | IllegalStateException e) {
            // the given Json might not be valid or unreadable
            Timber.e(e, "JSON show import failed");
            return ERROR;
        }

        return SUCCESS;
    }

    private void addShowToDatabase(Show show) {
        if (show.tvdbId <= 0) {
            // valid id required
            return;
        }

        // Insert the show
        ContentValues showValues = new ContentValues();
        showValues.put(Shows._ID, show.tvdbId);
        showValues.put(Shows.TITLE, show.title == null ? "" : show.title);
        showValues.put(Shows.TITLE_NOARTICLE, DBUtils.trimLeadingArticle(show.title));
        showValues.put(Shows.FAVORITE, show.favorite);
        showValues.put(Shows.HIDDEN, show.hidden);
        showValues.put(Shows.RELEASE_TIME, show.release_time);
        if (show.release_weekday < -1 || show.release_weekday > 7) {
            show.release_weekday = -1;
        }
        showValues.put(Shows.RELEASE_WEEKDAY, show.release_weekday);
        showValues.put(Shows.RELEASE_TIMEZONE, show.release_timezone);
        showValues.put(Shows.RELEASE_COUNTRY, show.country);
        showValues.put(Shows.LASTWATCHEDID, show.lastWatchedEpisode);
        showValues.put(Shows.POSTER, show.poster);
        showValues.put(Shows.CONTENTRATING, show.contentRating);
        if (show.runtime < 0) {
            show.runtime = 0;
        }
        showValues.put(Shows.RUNTIME, show.runtime);
        showValues.put(Shows.NETWORK, show.network);
        showValues.put(Shows.IMDBID, show.imdbId);
        showValues.put(Shows.FIRST_RELEASE, show.firstAired);
        if (show.rating_user < 0 || show.rating_user > 10) {
            show.rating_user = 0;
        }
        showValues.put(Shows.RATING_USER, show.rating_user);
        showValues.put(Shows.STATUS, DataLiberationTools.encodeShowStatus(show.status));
        // Full dump values
        showValues.put(Shows.OVERVIEW, show.overview);
        if (show.rating < 0 || show.rating > 10) {
            show.rating = 0;
        }
        showValues.put(Shows.RATING_GLOBAL, show.rating);
        if (show.rating_votes < 0) {
            show.rating_votes = 0;
        }
        showValues.put(Shows.RATING_VOTES, show.rating_votes);
        showValues.put(Shows.GENRES, show.genres);
        showValues.put(Shows.ACTORS, show.actors);
        if (show.lastUpdated > System.currentTimeMillis()) {
            show.lastUpdated = 0;
        }
        showValues.put(Shows.LASTUPDATED, show.lastUpdated);
        showValues.put(Shows.LASTEDIT, show.lastEdited);

        mContext.getContentResolver().insert(Shows.CONTENT_URI, showValues);

        if (show.seasons == null || show.seasons.isEmpty()) {
            // no seasons (or episodes)
            return;
        }

        ContentValues[][] seasonsAndEpisodes = buildSeasonAndEpisodeBatches(show);
        if (seasonsAndEpisodes[0] != null && seasonsAndEpisodes[1] != null) {
            // Insert all seasons
            mContext.getContentResolver().bulkInsert(Seasons.CONTENT_URI, seasonsAndEpisodes[0]);
            // Insert all episodes
            mContext.getContentResolver().bulkInsert(Episodes.CONTENT_URI, seasonsAndEpisodes[1]);
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
            ContentValues seasonValues = new ContentValues();
            seasonValues.put(Seasons._ID, season.tvdbId);
            seasonValues.put(Shows.REF_SHOW_ID, show.tvdbId);
            if (season.season < 0) {
                season.season = 0;
            }
            seasonValues.put(Seasons.COMBINED, season.season);

            seasonBatch.add(seasonValues);

            // ...and its episodes
            for (Episode episode : season.episodes) {
                if (episode.tvdbId <= 0) {
                    // valid id is required
                    continue;
                }

                ContentValues episodeValues = new ContentValues();
                episodeValues.put(Episodes._ID, episode.tvdbId);
                episodeValues.put(Shows.REF_SHOW_ID, show.tvdbId);
                episodeValues.put(Seasons.REF_SEASON_ID, season.tvdbId);
                if (episode.episode < 0) {
                    episode.episode = 0;
                }
                episodeValues.put(Episodes.NUMBER, episode.episode);
                if (episode.episodeAbsolute < 0) {
                    episode.episodeAbsolute = 0;
                }
                episodeValues.put(Episodes.ABSOLUTE_NUMBER, episode.episodeAbsolute);
                episodeValues.put(Episodes.SEASON, season.season);
                episodeValues.put(Episodes.TITLE, episode.title);
                // watched/skipped represented internally in watched flag
                if (episode.skipped) {
                    episodeValues.put(Episodes.WATCHED, EpisodeFlags.SKIPPED);
                } else {
                    episodeValues.put(Episodes.WATCHED,
                            episode.watched ? EpisodeFlags.WATCHED : EpisodeFlags.UNWATCHED);
                }
                episodeValues.put(Episodes.COLLECTED, episode.collected);
                episodeValues.put(Episodes.FIRSTAIREDMS, episode.firstAired);
                episodeValues.put(Episodes.IMDBID, episode.imdbId);
                if (episode.rating_user < 0 || episode.rating_user > 10) {
                    episode.rating_user = 0;
                }
                episodeValues.put(Episodes.RATING_USER, episode.rating_user);
                // Full dump values
                if (episode.episodeDvd < 0) {
                    episode.episodeDvd = 0;
                }
                episodeValues.put(Episodes.DVDNUMBER, episode.episodeDvd);
                episodeValues.put(Episodes.OVERVIEW, episode.overview);
                episodeValues.put(Episodes.IMAGE, episode.image);
                episodeValues.put(Episodes.WRITERS, episode.writers);
                episodeValues.put(Episodes.GUESTSTARS, episode.gueststars);
                episodeValues.put(Episodes.DIRECTORS, episode.directors);
                if (episode.rating < 0 || episode.rating > 10) {
                    episode.rating = 0;
                }
                episodeValues.put(Episodes.RATING_GLOBAL, episode.rating);
                if (episode.rating_votes < 0) {
                    episode.rating_votes = 0;
                }
                episodeValues.put(Episodes.RATING_VOTES, episode.rating_votes);
                episodeValues.put(Episodes.LAST_EDITED, episode.lastEdited);

                episodeBatch.add(episodeValues);
            }
        }

        return new ContentValues[][] {
                seasonBatch.size() == 0 ? null
                        : seasonBatch.toArray(new ContentValues[seasonBatch.size()]),
                episodeBatch.size() == 0 ? null
                        : episodeBatch.toArray(new ContentValues[episodeBatch.size()])
        };
    }

    private int importLists(File importPath) {
        File backupLists = new File(importPath, JsonExportTask.EXPORT_JSON_FILE_LISTS);
        if (!backupLists.exists() || !backupLists.canRead()) {
            // Skip lists if the file is not accessible
            return SUCCESS;
        }

        // Access JSON from backup folder to create new database
        try {
            InputStream in = new FileInputStream(backupLists);

            Gson gson = new Gson();

            JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
            reader.beginArray();

            while (reader.hasNext()) {
                List list = gson.fromJson(reader, List.class);
                addListToDatabase(list);
            }

            reader.endArray();
            reader.close();
        } catch (JsonParseException | IOException | IllegalStateException e) {
            // the given Json might not be valid or unreadable
            Timber.e(e, "JSON lists import failed");
            return ERROR;
        }

        return SUCCESS;
    }

    private void addListToDatabase(List list) {
        // Insert the list
        ContentValues values = new ContentValues();
        values.put(Lists.LIST_ID, list.listId);
        values.put(Lists.NAME, list.name);
        values.put(Lists.ORDER, list.order);
        mContext.getContentResolver().insert(Lists.CONTENT_URI, values);

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
            ContentValues itemValues = new ContentValues();
            itemValues.put(ListItems.LIST_ITEM_ID, item.listItemId);
            itemValues.put(Lists.LIST_ID, list.listId);
            itemValues.put(ListItems.ITEM_REF_ID, item.tvdbId);
            itemValues.put(ListItems.TYPE, type);

            items.add(itemValues);
        }

        ContentValues[] itemsArray = new ContentValues[items.size()];
        mContext.getContentResolver().bulkInsert(ListItems.CONTENT_URI, items.toArray(itemsArray));
    }

    private int importMovies(File importPath) {
        mContext.getContentResolver().delete(Movies.CONTENT_URI, null, null);
        File backupMovies = new File(importPath, JsonExportTask.EXPORT_JSON_FILE_MOVIES);
        if (!backupMovies.exists() || !backupMovies.canRead()) {
            // Skip movies if the file is not available
            return SUCCESS;
        }

        // Access JSON from backup folder to create new database
        try {
            InputStream in = new FileInputStream(backupMovies);

            Gson gson = new Gson();

            JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
            reader.beginArray();

            while (reader.hasNext()) {
                Movie movie = gson.fromJson(reader, Movie.class);
                addMovieToDatabase(movie);
            }

            reader.endArray();
            reader.close();
        } catch (JsonParseException | IOException | IllegalStateException e) {
            // the given Json might not be valid or unreadable
            Timber.e(e, "JSON movies import failed");
            return ERROR;
        }

        return SUCCESS;
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

        mContext.getContentResolver().insert(Movies.CONTENT_URI, values);
    }
}
