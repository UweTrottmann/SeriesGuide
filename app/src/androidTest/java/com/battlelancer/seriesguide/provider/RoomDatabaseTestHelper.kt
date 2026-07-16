// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2021 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.provider

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.battlelancer.seriesguide.movies.database.SgMovie
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables
import com.battlelancer.seriesguide.shows.database.SgEpisode2
import com.battlelancer.seriesguide.shows.database.SgSeason2
import com.battlelancer.seriesguide.shows.database.SgShow2
import com.battlelancer.seriesguide.shows.episodes.EpisodeFlags
import com.battlelancer.seriesguide.util.TextTools

/**
 * Helps insert data for different database schema versions to test migrations.
 */
object RoomDatabaseTestHelper {

    /**
     * Modeled after [SgShow2] table as it exists since
     * [SgRoomDatabase.VERSION_49_AUTO_ID_MIGRATION].
     */
    data class TestShow49(
        val tmdbId: Int,
        val title: String,
        val runtime: Int,
        val poster: String
    ) {
        fun toContentValues(): ContentValues {
            val values = ContentValues()
            values.put("series_tmdb_id", tmdbId)
            values.put("series_title", title)
            values.put("series_runtime", runtime)
            values.put("series_poster", poster)

            values.put("series_favorite", false)
            values.put("series_hidden", false)
            values.put("series_notify", true)
            values.put("series_syncenabled", true)
            values.put("series_lastupdate", 0)
            values.put("series_lastedit", 0)
            values.put("series_lastwatchedid", 0)
            values.put("series_lastwatched_ms", 0)
            values.put("series_unwatched_count", -1)
            return values
        }
    }


    /**
     * Modeled after [SgSeason2] table as it exists since
     * [SgRoomDatabase.VERSION_49_AUTO_ID_MIGRATION].
     */
    data class TestSeason49(
        val tmdbId: String,
        val number: Int
    ) {
        fun toContentValues(showId: Long): ContentValues {
            val values = ContentValues()
            values.put("series_id", showId)
            values.put("season_tmdb_id", tmdbId)
            values.put("season_number", number)
            values.put("season_order", number)
            return values
        }
    }

    /**
     * Modeled after [SgEpisode2] table as it exists since
     * [SgRoomDatabase.VERSION_49_AUTO_ID_MIGRATION].
     */
    data class TestEpisode49(
        val tmdbId: Int,
        val title: String,
        val number: Int
    ) {
        fun toContentValues(
            showId: Long,
            seasonId: Long,
            seasonNumber: Int
        ): ContentValues {
            val values = ContentValues()
            values.put("episode_tmdb_id", tmdbId)
            values.put("episode_title", title)
            values.put("episode_number", number)
            values.put("episode_order", number)

            values.put("season_id", seasonId)
            values.put("series_id", showId)
            values.put("episode_season_number", seasonNumber)
            values.put("episode_firstairedms", SgEpisode2.EPISODE_UNKNOWN_RELEASE)
            values.put("episode_watched", EpisodeFlags.UNWATCHED)
            values.put("episode_plays", 0)
            values.put("episode_collected", 0)
            values.put("episode_lastedit", 0)
            values.put("episode_lastupdate", 0)
            return values
        }
    }

    @JvmStatic
    fun insertShow49(show: TestShow49, db: SupportSQLiteDatabase): Long {
        return db.insert("sg_show", SQLiteDatabase.CONFLICT_REPLACE, show.toContentValues())
    }

    @JvmStatic
    fun insertSeason49(
        season: TestSeason49,
        showId: Long,
        db: SupportSQLiteDatabase
    ): Long {
        return db.insert(
            "sg_season",
            SQLiteDatabase.CONFLICT_REPLACE,
            season.toContentValues(showId)
        )
    }

    @JvmStatic
    fun insertEpisode49(
        episode: TestEpisode49,
        showId: Long,
        seasonId: Long,
        seasonNumber: Int,
        db: SupportSQLiteDatabase
    ) {
        db.insert(
            "sg_episode",
            SQLiteDatabase.CONFLICT_REPLACE,
            episode.toContentValues(showId, seasonId, seasonNumber)
        )
    }

    /**
     * Modeled after shows table as it existed up to [SgRoomDatabase.VERSION_49_AUTO_ID_MIGRATION].
     */
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

    /**
     * Modeled after seasons table as it existed up to
     * [SgRoomDatabase.VERSION_49_AUTO_ID_MIGRATION].
     */
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

    /**
     * Modeled after episodes table as it existed up to
     * [SgRoomDatabase.VERSION_49_AUTO_ID_MIGRATION].
     */
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
            SgEpisode2.EPISODE_UNKNOWN_RELEASE
        )
        if (watched) values.put("watched", EpisodeFlags.WATCHED)
        db.insert(Tables.EPISODES, SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    /**
     * Modeled after [SgMovie] table as it exists since the oldest supported database version.
     */
    data class TestMovie(
        val tmdbId: Int,
        val watched: Boolean,
        /** Starting with [SgRoomDatabase.VERSION_48_EPISODE_PLAYS] plays are 0 or greater. */
        val plays: Int?
    ) {

        // Use different values to spot if columns get mixed up during migration
        val imdbId = "test-imdbId"
        val title = "Test Movie"
        val titleNoArticle = TextTools.trimLeadingArticle(title)
        val poster = "test/poster.jpg"
        val overview = "Test overview"
        val releasedMs = SgMovie.RELEASED_MS_UNKNOWN
        val runtimeMin = 123
        val inCollection = true
        val inWatchlist = true
        val ratingTmdb = 8.5
        val ratingVotesTmdb = 1234
        val ratingTrakt = 8
        val ratingVotesTrakt = 4321
        val ratingUser = 9
        val lastUpdated = System.currentTimeMillis()

        fun toContentValues(): ContentValues {
            return ContentValues().apply {
                put("movies_tmdbid", tmdbId)
                put("movies_imdbid", imdbId)
                put("movies_title", title)
                put("movies_title_noarticle", titleNoArticle)
                put("movies_poster", poster)
                put("movies_overview", overview)
                put("movies_released", releasedMs)
                put("movies_runtime", runtimeMin)
                put("movies_incollection", inCollection)
                put("movies_inwatchlist", inWatchlist)
                put("movies_plays", plays)
                put("movies_watched", watched)
                put("movies_rating_tmdb", ratingTmdb)
                put("movies_rating_votes_tmdb", ratingVotesTmdb)
                put("movies_rating_trakt", ratingTrakt)
                put("movies_rating_votes_trakt", ratingVotesTrakt)
                put("movies_rating_user", ratingUser)
                put("movies_last_updated", lastUpdated)
            }
        }

        fun insertInto(db: SupportSQLiteDatabase) {
            db.insert(Tables.MOVIES, SQLiteDatabase.CONFLICT_REPLACE, toContentValues())
        }

    }

}