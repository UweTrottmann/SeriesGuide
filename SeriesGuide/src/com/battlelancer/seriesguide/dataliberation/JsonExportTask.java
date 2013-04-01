
package com.battlelancer.seriesguide.dataliberation;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import com.battlelancer.seriesguide.dataliberation.model.Episode;
import com.battlelancer.seriesguide.dataliberation.model.Season;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
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

    public interface OnExportTaskFinishedListener {
        public void onExportTaskFinished();
    }

    private Context mContext;
    private OnExportTaskFinishedListener mListener;

    public JsonExportTask(Context context, OnExportTaskFinishedListener listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        if (!AndroidUtils.isExtStorageAvailable()) {
            return 0;
        }

        final Cursor shows = mContext.getContentResolver().query(Shows.CONTENT_URI, new String[] {
                Shows._ID, Shows.TITLE, Shows.FAVORITE, Shows.HIDDEN
        }, null, null, null);

        if (shows == null) {
            return -1;
        }

        File path = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "SeriesGuide");
        // Ensure the directory exists
        path.mkdirs();

        File backup = new File(path, "sg-database-export.json");

        try {
            OutputStream out = new FileOutputStream(backup);

            writeJsonStream(out, shows);

            shows.close();

            return 1;
        } catch (IOException e) {
            // Backup failed
        }

        return -1;
    }

    @Override
    protected void onPostExecute(Integer resultCode) {
        int messageId;
        switch (resultCode) {
            case 1:
                messageId = R.string.backup_success;
                break;
            case 0:
                messageId = R.string.backup_failed_nosd;
                break;
            default:
                messageId = R.string.backup_failed;
                break;
        }
        Toast.makeText(mContext, messageId, Toast.LENGTH_LONG).show();

        if (mListener != null) {
            mListener.onExportTaskFinished();
        }
    }

    private void writeJsonStream(OutputStream out, Cursor shows) throws IOException {
        Gson gson = new Gson();
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent("  ");
        writer.beginArray();

        while (shows.moveToNext()) {
            Show show = new Show();
            show.tvdbId = shows.getInt(0);
            show.title = shows.getString(1);
            show.favorite = shows.getInt(2) == 1;
            show.hidden = shows.getInt(3) == 1;

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

}
