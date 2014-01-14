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

import com.google.myjson.Gson;
import com.google.myjson.JsonParseException;
import com.google.myjson.stream.JsonReader;

import com.battlelancer.seriesguide.dataliberation.JsonExportTask.ListItemTypesExport;
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.ShowStatusExport;
import com.battlelancer.seriesguide.dataliberation.model.Episode;
import com.battlelancer.seriesguide.dataliberation.model.List;
import com.battlelancer.seriesguide.dataliberation.model.ListItem;
import com.battlelancer.seriesguide.dataliberation.model.Season;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.EpisodeSearch;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.thetvdbapi.TheTVDB.ShowStatus;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Import a show database from a human-readable JSON file on external storage.
 * By default meta-data like descriptions, ratings, actors, etc. will not be
 * included.
 */
public class JsonImportTask extends AsyncTask<Void, Integer, Integer> {

    private static final String TAG = "Json Import";
    private static final int SUCCESS = 1;
    private static final int ERROR_STORAGE_ACCESS = 0;
    private static final int ERROR = -1;
    private static final int ERROR_LARGE_DB_OP = -2;
    private static final int ERROR_FILE_ACCESS = -3;
    private Context mContext;
    private OnTaskFinishedListener mListener;
    private boolean mIsAutoBackupMode;

    public JsonImportTask(Context context, OnTaskFinishedListener listener, boolean isAutoBackupMode) {
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
        File path = JsonExportTask.getExportPath(mIsAutoBackupMode);
        File backup = new File(path, JsonExportTask.EXPORT_JSON_FILE_SHOWS);
        if (!backup.exists() || !backup.canRead()) {
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
            InputStream in = new FileInputStream(backup);

            Gson gson = new Gson();

            JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
            reader.beginArray();

            while (reader.hasNext()) {
                Show show = gson.fromJson(reader, Show.class);
                addShowToDatabase(show);
            }

            reader.endArray();
            reader.close();

        } catch (JsonParseException e) {
            // the given Json might not be valid or unreadable
            Utils.trackExceptionAndLog(mContext, TAG, e);
            return ERROR;
        } catch (IOException e) {
            Utils.trackExceptionAndLog(mContext, TAG, e);
            return ERROR;
        }

        /*
         * Lists
         */
        File backupLists = new File(path, JsonExportTask.EXPORT_JSON_FILE_LISTS);
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

        } catch (JsonParseException e) {
            // the given Json might not be valid or unreadable
            Utils.trackExceptionAndLog(mContext, TAG, e);
            return ERROR;
        } catch (IOException e) {
            Utils.trackExceptionAndLog(mContext, TAG, e);
            return ERROR;
        }

        // Renew search table
        mContext.getContentResolver().query(EpisodeSearch.CONTENT_URI_RENEWFTSTABLE, null, null,
                null, null);

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

    private void addShowToDatabase(Show show) {
        // Insert the show
        ContentValues showValues = new ContentValues();
        showValues.put(Shows._ID, show.tvdbId);
        showValues.put(Shows.TITLE, show.title);
        showValues.put(Shows.FAVORITE, show.favorite);
        showValues.put(Shows.HIDDEN, show.hidden);
        showValues.put(Shows.AIRSTIME, show.airtime);
        showValues.put(Shows.AIRSDAYOFWEEK, show.airday);
        showValues.put(Shows.GETGLUEID, show.checkInGetGlueId);
        showValues.put(Shows.LASTWATCHEDID, show.lastWatchedEpisode);
        showValues.put(Shows.POSTER, show.poster);
        showValues.put(Shows.CONTENTRATING, show.contentRating);
        showValues.put(Shows.RUNTIME, show.runtime);
        showValues.put(Shows.NETWORK, show.network);
        showValues.put(Shows.IMDBID, show.imdbId);
        showValues.put(Shows.FIRSTAIRED, show.firstAired);
        int status;
        if (ShowStatusExport.CONTINUING.equals(show.status)) {
            status = ShowStatus.CONTINUING;
        } else if (ShowStatusExport.ENDED.equals(show.status)) {
            status = ShowStatus.ENDED;
        } else {
            status = ShowStatus.UNKNOWN;
        }
        showValues.put(Shows.STATUS, status);
        // Full dump values
        showValues.put(Shows.OVERVIEW, show.overview);
        showValues.put(Shows.RATING, show.rating);
        showValues.put(Shows.GENRES, show.genres);
        showValues.put(Shows.ACTORS, show.actors);
        showValues.put(Shows.LASTUPDATED, show.lastUpdated);
        showValues.put(Shows.LASTEDIT, show.lastEdited);

        mContext.getContentResolver().insert(Shows.CONTENT_URI, showValues);

        if (show.seasons == null) {
            return;
        }

        ContentValues[][] seasonsAndEpisodes = buildSeasonAndEpisodeBatches(show);

        // Insert all seasons
        mContext.getContentResolver().bulkInsert(Seasons.CONTENT_URI, seasonsAndEpisodes[0]);

        // Insert all episodes
        mContext.getContentResolver().bulkInsert(Episodes.CONTENT_URI, seasonsAndEpisodes[1]);
    }

    /**
     * Returns all seasons and episodes of this show in neat
     * {@link ContentValues} packages put into arrays. The first array returned
     * includes all seasons, the second array all episodes.
     */
    private static ContentValues[][] buildSeasonAndEpisodeBatches(Show show) {
        // Initialize arrays
        ContentValues[] seasonBatch = new ContentValues[show.seasons.size()];
        int episodesSize = 0;
        for (Season season : show.seasons) {
            if (season.episodes != null) {
                episodesSize += season.episodes.size();
            }
        }
        ContentValues[] episodeBatch = new ContentValues[episodesSize];

        // Populate arrays...
        int seasonIdx = 0;
        int episodeIdx = 0;
        for (Season season : show.seasons) {
            // ...with each season
            ContentValues seasonValues = new ContentValues();
            seasonValues.put(Seasons._ID, season.tvdbId);
            seasonValues.put(Shows.REF_SHOW_ID, show.tvdbId);
            seasonValues.put(Seasons.COMBINED, season.season);

            seasonBatch[seasonIdx] = seasonValues;
            seasonIdx++;

            if (season.episodes == null) {
                continue;
            }

            // ...and its episodes
            for (Episode episode : season.episodes) {
                ContentValues episodeValues = new ContentValues();
                episodeValues.put(Episodes._ID, episode.tvdbId);
                episodeValues.put(Shows.REF_SHOW_ID, show.tvdbId);
                episodeValues.put(Seasons.REF_SEASON_ID, season.tvdbId);
                episodeValues.put(Episodes.NUMBER, episode.episode);
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
                // Full dump values
                episodeValues.put(Episodes.DVDNUMBER, episode.episodeDvd);
                episodeValues.put(Episodes.OVERVIEW, episode.overview);
                episodeValues.put(Episodes.IMAGE, episode.image);
                episodeValues.put(Episodes.WRITERS, episode.writers);
                episodeValues.put(Episodes.GUESTSTARS, episode.gueststars);
                episodeValues.put(Episodes.DIRECTORS, episode.directors);
                episodeValues.put(Episodes.RATING, episode.rating);
                episodeValues.put(Episodes.LAST_EDITED, episode.lastEdited);

                episodeBatch[episodeIdx] = episodeValues;
                episodeIdx++;
            }
        }

        return new ContentValues[][] {
                seasonBatch, episodeBatch
        };
    }

    private void addListToDatabase(List list) {
        // Insert the list
        ContentValues values = new ContentValues();
        values.put(Lists.LIST_ID, list.listId);
        values.put(Lists.NAME, list.name);
        mContext.getContentResolver().insert(Lists.CONTENT_URI, values);

        if (list.items == null || list.items.isEmpty()) {
            return;
        }

        // Insert the lists items
        ArrayList<ContentValues> items = com.uwetrottmann.androidutils.Lists.newArrayList();
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
}
