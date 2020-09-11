package com.battlelancer.seriesguide.provider;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.model.SgSeason;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.thetvdbapi.TvdbEpisodeTools;
import com.uwetrottmann.thetvdb.entities.Episode;

/**
 * Helper class for working with the SQLiteDatabase using SQLite APIs (before Room).
 */
public class SqliteDatabaseTestHelper {

    public static void insertShow(Show show, SQLiteDatabase db) {
        ContentValues values = show.toContentValues(ApplicationProvider.getApplicationContext(),
                true);

        // Remove columns added after version 42 (last version before Room).
        // Also check RoomDatabaseTestHelper!
        values.remove(Shows.SLUG);
        values.remove(Shows.POSTER_SMALL);

        db.insertWithOnConflict(Tables.SHOWS, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static void insertSeason(SgSeason season, SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(Seasons._ID, season.tvdbId);
        values.put(Shows.REF_SHOW_ID, season.showTvdbId);
        values.put(Seasons.COMBINED, season.number);

        db.insertWithOnConflict(Tables.SEASONS, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static void insertEpisode(Episode episode, int showTvdbId, int seasonTvdbId,
            int seasonNumber, SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        TvdbEpisodeTools.toContentValues(episode, values,
                episode.id, seasonTvdbId, showTvdbId, seasonNumber,
                Constants.EPISODE_UNKNOWN_RELEASE, true);

        // Remove columns added after version 42 (last version before Room).
        // Also check RoomDatabaseTestHelper!
        values.remove(Episodes.PLAYS);

        db.insertWithOnConflict(Tables.EPISODES, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
    }
}
