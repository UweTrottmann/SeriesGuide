package com.battlelancer.seriesguide.provider;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.model.SgSeason;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.thetvdbapi.TvdbEpisodeTools;
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags;
import com.battlelancer.seriesguide.ui.movies.MovieDetails;
import com.uwetrottmann.thetvdb.entities.Episode;

/**
 * Helper class for working with the SupportSQLiteDatabase using SQLite APIs (using Room).
 */
public class RoomDatabaseTestHelper {

    public static void insertShow(Show show, SupportSQLiteDatabase db, int version) {
        ContentValues values = show.toContentValues(ApplicationProvider.getApplicationContext(),
                true);

        // Remove columns added in newer versions.
        // Also check SqliteDatabaseTestHelper!
        if (version < SgRoomDatabase.VERSION_46_SERIES_SLUG) {
            values.remove(Shows.SLUG);
        }
        if (version < SgRoomDatabase.VERSION_47_SERIES_POSTER_THUMB) {
            values.remove(Shows.POSTER_SMALL);
        }

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
        // Note: use version 47 as no changes before that.
        insertEpisode(db, SgRoomDatabase.VERSION_47_SERIES_POSTER_THUMB,
                episode, showTvdbId, seasonTvdbId, seasonNumber, false);
    }

    public static void insertEpisode(
            SupportSQLiteDatabase db,
            int version,
            Episode episode,
            int showTvdbId,
            int seasonTvdbId,
            int seasonNumber,
            boolean watched
    ) {
        ContentValues values = new ContentValues();
        TvdbEpisodeTools.toContentValues(episode, values,
                episode.id, seasonTvdbId, showTvdbId, seasonNumber,
                Constants.EPISODE_UNKNOWN_RELEASE, true);

        if (watched) values.put(Episodes.WATCHED, EpisodeFlags.WATCHED);

        // Remove columns added in newer versions.
        // Also check SqliteDatabaseTestHelper!
        if (version < SgRoomDatabase.VERSION_48_EPISODE_PLAYS) {
            values.remove(Episodes.PLAYS);
        }

        db.insert(Tables.EPISODES, SQLiteDatabase.CONFLICT_REPLACE, values);
    }

    public static void insertMovie(SupportSQLiteDatabase db, MovieDetails movieDetails) {
        db.insert(Tables.MOVIES, SQLiteDatabase.CONFLICT_REPLACE,
                movieDetails.toContentValuesInsert());
    }
}
