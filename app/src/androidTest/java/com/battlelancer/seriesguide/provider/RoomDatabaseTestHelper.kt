package com.battlelancer.seriesguide.provider

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.battlelancer.seriesguide.Constants
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags
import com.battlelancer.seriesguide.ui.movies.MovieDetails

object RoomDatabaseTestHelper {

    data class TestShow(
        val tvdbId: Int,
        val title: String,
        val runtime: Int,
        val poster: String,
        val nextEpisode: String
    ) {
        fun toContentValuesDb48OrLower(): ContentValues {
            val values = ContentValues()
            values.put("_id", tvdbId)
            values.put("seriestitle", title)
            values.put("runtime", runtime)
            values.put("poster", poster)
            values.put("next", nextEpisode)

            values.put("series_favorite", false)
            values.put("series_syncenabled", true)
            values.put("series_hidden", false)
            values.put("series_lastupdate", 0)
            values.put("series_lastedit", 0)
            values.put("series_lastwatchedid", 0)
            values.put("series_lastwatched_ms", 0)
            values.put("series_unwatched_count", -1)
            values.put("series_notify", true)
            return values
        }
    }

    data class TestSeason(
        val tvdbId: Int,
        val showTvdbId: String,
        val number: Int
    ) {
        fun toContentValuesDb48OrLower(): ContentValues {
            val values = ContentValues()
            values.put("_id", tvdbId)
            values.put("series_id", showTvdbId)
            values.put("combinednr", number)
            return values
        }
    }

    data class TestEpisode(
        val tvdbId: Int,
        val name: String,
        val number: Int
    ) {
        fun toContentValuesDb48OrLower(
            seasonTvdbId: Int,
            showTvdbId: Int,
            seasonNumber: Int,
            releaseDateTime: Long
        ): ContentValues {
            val values = ContentValues()
            values.put("_id", tvdbId)
            values.put("episodetitle", name)
            values.put("episodenumber", number)

            values.put("season_id", seasonTvdbId)
            values.put("series_id", showTvdbId)
            values.put("season", seasonNumber)
            values.put("episode_firstairedms", releaseDateTime)
            values.put("watched", EpisodeFlags.UNWATCHED)
            values.put("episode_collected", 0)
            values.put("episode_lastedit", 0)
            values.put("episode_lastupdate", 0)
            return values
        }
    }

    @JvmStatic
    fun insertShow(show: TestShow, db: SupportSQLiteDatabase) {
        db.insert("series", SQLiteDatabase.CONFLICT_REPLACE, show.toContentValuesDb48OrLower())
    }

    @JvmStatic
    fun insertSeason(season: TestSeason, db: SupportSQLiteDatabase) {
        db.insert("seasons", SQLiteDatabase.CONFLICT_REPLACE, season.toContentValuesDb48OrLower())
    }

    @JvmStatic
    fun insertEpisode(
        episode: TestEpisode,
        showTvdbId: Int,
        seasonTvdbId: Int,
        seasonNumber: Int,
        db: SupportSQLiteDatabase
    ) {
        // Note: use version 47 as no changes before that.
        insertEpisode(
            db, episode, showTvdbId, seasonTvdbId, seasonNumber, false
        )
    }

    @JvmStatic
    fun insertEpisode(
        db: SupportSQLiteDatabase,
        episode: TestEpisode,
        showTvdbId: Int,
        seasonTvdbId: Int,
        seasonNumber: Int,
        watched: Boolean
    ) {
        val values = episode.toContentValuesDb48OrLower(
            seasonTvdbId,
            showTvdbId,
            seasonNumber,
            Constants.EPISODE_UNKNOWN_RELEASE.toLong()
        )
        if (watched) values.put("watched", EpisodeFlags.WATCHED)
        db.insert(Tables.EPISODES, SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    @JvmStatic
    fun insertMovie(db: SupportSQLiteDatabase, movieDetails: MovieDetails) {
        db.insert(
            Tables.MOVIES, SQLiteDatabase.CONFLICT_REPLACE,
            movieDetails.toContentValuesInsert()
        )
    }

}