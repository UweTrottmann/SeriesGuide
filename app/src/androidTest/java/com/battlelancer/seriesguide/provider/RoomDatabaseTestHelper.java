package com.battlelancer.seriesguide.provider;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.model.SgSeason;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.thetvdbapi.TvdbEpisodeTools;
import com.uwetrottmann.thetvdb.entities.Episode;

/**
 * Helper class for working with the SupportSQLiteDatabase using SQLite APIs (using Room).
 */
public class RoomDatabaseTestHelper {

    public static void insertShow(Show show, SupportSQLiteDatabase db) {
        ContentValues values = show.toContentValues(InstrumentationRegistry.getTargetContext(),
                true);

        db.insert(Tables.SHOWS, SQLiteDatabase.CONFLICT_REPLACE, values);
    }

    public static void insertSeason(SgSeason season, SupportSQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(Seasons._ID, season.tvdbId);
        values.put(Shows.REF_SHOW_ID, season.showTvdbId);
        values.put(Seasons.COMBINED, season.number);

        db.insert(Tables.SEASONS, SQLiteDatabase.CONFLICT_REPLACE, values);
    }

    public static void insertEpisode(Episode episode, int showTvdbId, int seasonTvdbId,
            int seasonNumber, SupportSQLiteDatabase db) {
        ContentValues values = new ContentValues();
        TvdbEpisodeTools.toContentValues(episode, values,
                episode.id, seasonTvdbId, showTvdbId, seasonNumber,
                Constants.EPISODE_UNKNOWN_RELEASE, true);

        db.insert(Tables.EPISODES, SQLiteDatabase.CONFLICT_REPLACE, values);
    }
}
