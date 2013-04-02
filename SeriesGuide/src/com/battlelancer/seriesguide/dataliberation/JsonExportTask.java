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

    public interface ListItemTypesExport {
        String SHOW = "show";
        String SEASON = "season";
        String EPISODE = "episode";
    }

    private Context mContext;
    private OnTaskFinishedListener mListener;

    public JsonExportTask(Context context, OnTaskFinishedListener listener) {
        mContext = context;
        mListener = listener;
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
                ShowsQuery.PROJECTION, null, null, ShowsQuery.SORT);
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

            writeJsonStream(out, shows);
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

        if (mListener != null) {
            mListener.onTaskFinished();
        }
    }

    private void writeJsonStream(OutputStream out, Cursor shows) throws IOException {
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
                new String[] {
                        Episodes._ID, Episodes.NUMBER, Episodes.ABSOLUTE_NUMBER, Episodes.WATCHED,
                        Episodes.COLLECTED, Episodes.TITLE, Episodes.FIRSTAIREDMS
                }, null, null, null);

        if (episodesCursor == null) {
            return;
        }

        while (episodesCursor.moveToNext()) {
            Episode episode = new Episode();
            episode.tvdbId = episodesCursor.getInt(0);
            episode.episode = episodesCursor.getInt(1);
            episode.episodeAbsolute = episodesCursor.getInt(2);
            episode.watched = episodesCursor.getInt(3) == 1;
            episode.collected = episodesCursor.getInt(4) == 1;
            episode.title = episodesCursor.getString(5);
            episode.firstAired = episodesCursor.getLong(6);

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
                Shows.AIRSDAYOFWEEK, Shows.GETGLUEID, Shows.LASTWATCHEDID
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
