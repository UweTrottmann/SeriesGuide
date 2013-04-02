/*
 * Copyright 2013 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.dataliberation;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import com.battlelancer.seriesguide.dataliberation.model.Episode;
import com.battlelancer.seriesguide.dataliberation.model.List;
import com.battlelancer.seriesguide.dataliberation.model.ListItem;
import com.battlelancer.seriesguide.dataliberation.model.Season;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.provider.SeriesContract;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.util.Lists;
import com.battlelancer.thetvdbapi.TheTVDB.ShowStatus;
import com.google.myjson.Gson;
import com.google.myjson.stream.JsonWriter;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Export the show database to a human-readable JSON file on external storage.
 * By default meta-data like descriptions, ratings, actors, etc. will not be
 * included.
 */
public class JsonExportTask extends AsyncTask<Void, Void, Integer> {

    public static final String EXPORT_FOLDER = "SeriesGuide";
    public static final String EXPORT_JSON_FILE_SHOWS = "sg-shows-export.json";
    public static final String EXPORT_JSON_FILE_LISTS = "sg-lists-export.json";

    private static final int SUCCESS = 1;
    private static final int ERROR_STORAGE_ACCESS = 0;
    private static final int ERROR = -1;

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
    private OnTaskFinishedListener mListener;
    private boolean mIsFullDump;
    private boolean mIsSilentMode;

    /**
     * Exports the show and lists database to a JSON file each into the
     * Downloads folder on external storage. By default dumps only minimum
     * required data and shows result toasts.
     */
    public JsonExportTask(Context context, OnTaskFinishedListener listener) {
        mContext = context;
        mListener = listener;
    }

    /**
     * Same as {@link JsonExportTask} but allows to set parameters.
     * 
     * @param isFullDump Whether to also export meta-data like descriptions,
     *            ratings, actors, etc. Increases file size about 2-4 times.
     * @param isSilentMode Whether to show result toasts.
     */
    public JsonExportTask(Context context, OnTaskFinishedListener listener, boolean isFullDump,
            boolean isSilentMode) {
        this(context, listener);
        mIsFullDump = isFullDump;
        mIsSilentMode = isSilentMode;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        // Ensure external storage is available
        if (!AndroidUtils.isExtStorageAvailable()) {
            return ERROR_STORAGE_ACCESS;
        }

        // Ensure the export directory exists
        File path = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                EXPORT_FOLDER);
        path.mkdirs();

        /*
         * Export shows.
         */
        final Cursor shows = mContext.getContentResolver().query(
                Shows.CONTENT_URI,
                mIsFullDump ? ShowsQuery.PROJECTION_FULL : ShowsQuery.PROJECTION,
                null, null, ShowsQuery.SORT);
        if (shows == null) {
            return ERROR;
        }
        if (shows.getCount() == 0) {
            // There are no shows? Done.
            return SUCCESS;
        }

        File backup = new File(path, EXPORT_JSON_FILE_SHOWS);
        try {
            OutputStream out = new FileOutputStream(backup);

            writeJsonStreamShows(out, shows);
        } catch (IOException e) {
            // Backup failed
            return ERROR;
        } finally {
            shows.close();
        }

        /*
         * Export lists.
         */
        final Cursor lists = mContext.getContentResolver().query(SeriesContract.Lists.CONTENT_URI,
                ListsQuery.PROJECTION, null, null, ListsQuery.SORT);
        if (lists == null) {
            return ERROR;
        }
        if (lists.getCount() == 0) {
            // There are no lists? Done.
            return SUCCESS;
        }

        File backupLists = new File(path, EXPORT_JSON_FILE_LISTS);
        try {
            OutputStream out = new FileOutputStream(backupLists);

            writeJsonStreamLists(out, lists);
        } catch (IOException e) {
            return ERROR;
        } finally {
            lists.close();
        }

        return SUCCESS;
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (!mIsSilentMode) {
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

    private void writeJsonStreamShows(OutputStream out, Cursor shows) throws IOException {
        Gson gson = new Gson();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent("  ");
        writer.beginArray();

        while (shows.moveToNext()) {
            Show show = new Show();
            show.tvdbId = shows.getInt(ShowsQuery.ID);
            show.title = shows.getString(ShowsQuery.TITLE);
            show.favorite = shows.getInt(ShowsQuery.FAVORITE) == 1;
            show.hidden = shows.getInt(ShowsQuery.HIDDEN) == 1;
            show.airtime = shows.getLong(ShowsQuery.AIRTIME);
            show.airday = shows.getString(ShowsQuery.AIRDAY);
            show.checkInGetGlueId = shows.getString(ShowsQuery.GETGLUEID);
            show.lastWatchedEpisode = shows.getInt(ShowsQuery.LASTWATCHEDID);
            show.poster = shows.getString(ShowsQuery.POSTER);
            show.contentRating = shows.getString(ShowsQuery.CONTENTRATING);
            switch (shows.getInt(ShowsQuery.STATUS)) {
                case ShowStatus.CONTINUING:
                    show.status = ShowStatusExport.CONTINUING;
                    break;
                case ShowStatus.ENDED:
                    show.status = ShowStatusExport.ENDED;
                    break;
                default:
                    show.status = ShowStatusExport.UNKNOWN;
                    break;
            }
            show.runtime = shows.getInt(ShowsQuery.RUNTIME);
            show.network = shows.getString(ShowsQuery.NETWORK);
            show.imdbId = shows.getString(ShowsQuery.IMDBID);
            show.firstAired = shows.getString(ShowsQuery.FIRSTAIRED);
            if (mIsFullDump) {
                show.overview = shows.getString(ShowsQuery.OVERVIEW);
                show.rating = shows.getDouble(ShowsQuery.RATING);
                show.genres = shows.getString(ShowsQuery.GENRES);
                show.actors = shows.getString(ShowsQuery.ACTORS);
                show.lastUpdated = shows.getLong(ShowsQuery.LAST_UPDATED);
                show.lastEdited = shows.getLong(ShowsQuery.LAST_EDITED);
            }

            addSeasons(show);

            gson.toJson(show, Show.class, writer);
        }

        writer.endArray();
        writer.close();
    }

    private void addSeasons(Show show) {
        show.seasons = Lists.newArrayList();
        final Cursor seasonsCursor = mContext.getContentResolver().query(
                Seasons.buildSeasonsOfShowUri(String.valueOf(show.tvdbId)),
                new String[] {
                        Seasons._ID,
                        Seasons.COMBINED
                }, null, null, null);

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
        season.episodes = Lists.newArrayList();
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
            episode.watched = episodesCursor.getInt(EpisodesQuery.WATCHED) == 1;
            episode.collected = episodesCursor.getInt(EpisodesQuery.COLLECTED) == 1;
            episode.title = episodesCursor.getString(EpisodesQuery.TITLE);
            episode.firstAired = episodesCursor.getLong(EpisodesQuery.FIRSTAIRED);
            episode.imdbId = episodesCursor.getString(EpisodesQuery.IMDBID);
            if (mIsFullDump) {
                episode.episodeDvd = episodesCursor.getDouble(EpisodesQuery.NUMBER_DVD);
                episode.overview = episodesCursor.getString(EpisodesQuery.OVERVIEW);
                episode.image = episodesCursor.getString(EpisodesQuery.IMAGE);
                episode.writers = episodesCursor.getString(EpisodesQuery.WRITERS);
                episode.gueststars = episodesCursor.getString(EpisodesQuery.GUESTSTARS);
                episode.directors = episodesCursor.getString(EpisodesQuery.DIRECTORS);
                episode.rating = episodesCursor.getDouble(EpisodesQuery.RATING);
                episode.lastEdited = episodesCursor.getLong(EpisodesQuery.LAST_EDITED);
            }

            season.episodes.add(episode);
        }

        episodesCursor.close();
    }

    private void writeJsonStreamLists(OutputStream out, Cursor lists) throws IOException {
        Gson gson = new Gson();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent("  ");
        writer.beginArray();

        while (lists.moveToNext()) {
            List list = new List();
            list.listId = lists.getString(ListsQuery.ID);
            list.name = lists.getString(ListsQuery.NAME);

            addListItems(list);

            gson.toJson(list, List.class, writer);
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
                }, null);
        if (listItems == null) {
            return;
        }

        list.items = Lists.newArrayList();
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

    public interface ShowsQuery {
        String[] PROJECTION = new String[] {
                Shows._ID, Shows.TITLE, Shows.FAVORITE, Shows.HIDDEN, Shows.AIRSTIME,
                Shows.AIRSDAYOFWEEK, Shows.GETGLUEID, Shows.LASTWATCHEDID,
                Shows.POSTER, Shows.CONTENTRATING, Shows.STATUS, Shows.RUNTIME, Shows.NETWORK,
                Shows.IMDBID, Shows.SYNCENABLED, Shows.FIRSTAIRED,
        };
        String[] PROJECTION_FULL = new String[] {
                Shows._ID, Shows.TITLE, Shows.FAVORITE, Shows.HIDDEN, Shows.AIRSTIME,
                Shows.AIRSDAYOFWEEK, Shows.GETGLUEID, Shows.LASTWATCHEDID,
                Shows.POSTER, Shows.CONTENTRATING, Shows.STATUS, Shows.RUNTIME, Shows.NETWORK,
                Shows.IMDBID, Shows.SYNCENABLED, Shows.FIRSTAIRED,
                Shows.OVERVIEW, Shows.RATING, Shows.GENRES, Shows.ACTORS,
                Shows.LASTUPDATED, Shows.LASTEDIT
        };

        String SORT = Shows.TITLE + " COLLATE NOCASE ASC";

        int ID = 0;
        int TITLE = 1;
        int FAVORITE = 2;
        int HIDDEN = 3;
        int AIRTIME = 4;
        int AIRDAY = 5;
        int GETGLUEID = 6;
        int LASTWATCHEDID = 7;
        int POSTER = 8;
        int CONTENTRATING = 9;
        int STATUS = 10;
        int RUNTIME = 11;
        int NETWORK = 12;
        int IMDBID = 13;
        int SYNC = 14;
        int FIRSTAIRED = 15;
        // Full dump only
        int OVERVIEW = 16;
        int RATING = 17;
        int GENRES = 18;
        int ACTORS = 19;
        int LAST_UPDATED = 20;
        int LAST_EDITED = 21;
    }

    public interface EpisodesQuery {
        String[] PROJECTION = new String[] {
                Episodes._ID, Episodes.NUMBER, Episodes.ABSOLUTE_NUMBER, Episodes.WATCHED,
                Episodes.COLLECTED, Episodes.TITLE, Episodes.FIRSTAIREDMS, Episodes.IMDBID
        };
        String[] PROJECTION_FULL = new String[] {
                Episodes._ID, Episodes.NUMBER, Episodes.ABSOLUTE_NUMBER, Episodes.WATCHED,
                Episodes.COLLECTED, Episodes.TITLE, Episodes.FIRSTAIREDMS, Episodes.IMDBID,
                Episodes.DVDNUMBER, Episodes.OVERVIEW, Episodes.IMAGE, Episodes.WRITERS,
                Episodes.GUESTSTARS, Episodes.DIRECTORS, Episodes.RATING, Episodes.LASTEDIT
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
        // Full dump only
        int NUMBER_DVD = 8;
        int OVERVIEW = 9;
        int IMAGE = 10;
        int WRITERS = 11;
        int GUESTSTARS = 12;
        int DIRECTORS = 13;
        int RATING = 14;
        int LAST_EDITED = 15;

    }

    public interface ListsQuery {
        String[] PROJECTION = new String[] {
                SeriesContract.Lists.LIST_ID, SeriesContract.Lists.NAME
        };

        String SORT = SeriesContract.Lists.NAME + " COLLATE NOCASE ASC";

        int ID = 0;
        int NAME = 1;
    }

    public interface ListItemsQuery {
        String[] PROJECTION = new String[] {
                ListItems.LIST_ITEM_ID, SeriesContract.Lists.LIST_ID, ListItems.ITEM_REF_ID,
                ListItems.TYPE
        };

        String SELECTION = SeriesContract.Lists.LIST_ID + "=?";

        int ID = 0;
        int LIST_ID = 1;
        int ITEM_REF_ID = 2;
        int TYPE = 3;
    }

}
