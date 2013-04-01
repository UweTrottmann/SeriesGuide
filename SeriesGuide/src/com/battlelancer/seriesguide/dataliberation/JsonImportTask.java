
package com.battlelancer.seriesguide.dataliberation;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import com.battlelancer.seriesguide.dataliberation.model.Episode;
import com.battlelancer.seriesguide.dataliberation.model.Season;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.provider.SeriesContract;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.util.Lists;
import com.battlelancer.seriesguide.util.TaskManager;
import com.google.myjson.Gson;
import com.google.myjson.stream.JsonReader;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

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
public class JsonImportTask extends AsyncTask<Void, Void, Integer> {

    private Context mContext;
    private OnTaskFinishedListener mListener;

    public JsonImportTask(Context context, OnTaskFinishedListener listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        if (!AndroidUtils.isExtStorageAvailable()) {
            return 0;
        }

        TaskManager tm = TaskManager.getInstance(mContext);
        if (tm.isUpdateTaskRunning(false) || tm.isAddTaskRunning()) {
            return -1;
        }

        // Clean out all existing tables
        mContext.getContentResolver().delete(Shows.CONTENT_URI, null, null);
        mContext.getContentResolver().delete(Seasons.CONTENT_URI, null, null);
        mContext.getContentResolver().delete(Episodes.CONTENT_URI, null, null);
        mContext.getContentResolver().delete(SeriesContract.Lists.CONTENT_URI, null, null);
        mContext.getContentResolver().delete(ListItems.CONTENT_URI, null, null);

        // Access JSON from backup folder to create new database
        File path = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                JsonExportTask.EXPORT_FOLDER);
        File backup = new File(path, JsonExportTask.EXPORT_JSON_FILE);

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

        } catch (IOException e) {
            return -1;
        }

        return 1;
    }

    @Override
    protected void onPostExecute(Integer result) {
        int messageId;
        switch (result) {
            case 1:
                messageId = R.string.import_success;
                break;
            case 0:
                messageId = R.string.import_failed_nosd;
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
        mContext.getContentResolver().insert(Shows.CONTENT_URI, showValues);

        final ArrayList<ContentValues> seasonBatch = Lists.newArrayList();
        final ArrayList<ContentValues> episodeBatch = Lists.newArrayList();
        buildSeasonAndEpisodeBatches(show, seasonBatch, episodeBatch);

        // Insert all seasons
        mContext.getContentResolver().bulkInsert(Seasons.CONTENT_URI,
                (ContentValues[]) seasonBatch.toArray());

        // Insert all episodes
        mContext.getContentResolver().bulkInsert(Episodes.CONTENT_URI,
                (ContentValues[]) episodeBatch.toArray());
    }

    private void buildSeasonAndEpisodeBatches(Show show, ArrayList<ContentValues> seasonBatch,
            ArrayList<ContentValues> episodeBatch) {
        for (Season season : show.seasons) {
            ContentValues seasonValues = new ContentValues();
            seasonValues.put(Seasons._ID, season.tvdbId);
            seasonValues.put(Seasons.COMBINED, season.season);

            seasonBatch.add(seasonValues);

            for (Episode episode : season.episodes) {
                ContentValues episodeValues = new ContentValues();
                episodeValues.put(Episodes._ID, episode.tvdbId);
                episodeValues.put(Episodes.NUMBER, episode.episode);
                episodeValues.put(Episodes.ABSOLUTE_NUMBER, episode.episodeAbsolute);
                episodeValues.put(Episodes.TITLE, episode.title);
                episodeValues.put(Episodes.WATCHED, episode.watched);
                episodeValues.put(Episodes.COLLECTED, episode.collected);
                episodeValues.put(Episodes.FIRSTAIREDMS, episode.firstAired);

                episodeBatch.add(episodeValues);
            }
        }
    }

}
