package com.battlelancer.seriesguide.provider

import android.database.sqlite.SQLiteDatabase
import com.battlelancer.seriesguide.Constants
import com.battlelancer.seriesguide.provider.RoomDatabaseTestHelper.TestSeason
import com.battlelancer.seriesguide.provider.RoomDatabaseTestHelper.TestShow

/**
 * Helper class for working with the SQLiteDatabase using SQLite APIs (before Room).
 */
object SqliteDatabaseTestHelper {

    @JvmStatic
    fun insertShow(show: TestShow, db: SQLiteDatabase) {
        db.insertWithOnConflict(
            "series",
            null,
            show.toContentValuesDb48OrLower(),
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    @JvmStatic
    fun insertSeason(season: TestSeason, db: SQLiteDatabase) {
        db.insertWithOnConflict(
            "seasons",
            null,
            season.toContentValuesDb48OrLower(),
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    @JvmStatic
    fun insertEpisode(
        episode: RoomDatabaseTestHelper.TestEpisode,
        showTvdbId: Int,
        seasonTvdbId: Int,
        seasonNumber: Int,
        db: SQLiteDatabase
    ) {
        val values = episode.toContentValuesDb48OrLower(
            seasonTvdbId,
            showTvdbId,
            seasonNumber,
            Constants.EPISODE_UNKNOWN_RELEASE.toLong()
        )
        db.insertWithOnConflict(
            SeriesGuideDatabase.Tables.EPISODES, null, values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }
}