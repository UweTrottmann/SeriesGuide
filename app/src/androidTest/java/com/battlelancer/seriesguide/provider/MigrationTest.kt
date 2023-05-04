package com.battlelancer.seriesguide.provider

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.battlelancer.seriesguide.movies.details.MovieDetails
import com.battlelancer.seriesguide.provider.RoomDatabaseTestHelper.TestEpisode
import com.battlelancer.seriesguide.provider.RoomDatabaseTestHelper.TestSeason
import com.battlelancer.seriesguide.provider.RoomDatabaseTestHelper.TestShow
import com.battlelancer.seriesguide.shows.history.ActivityType
import com.battlelancer.seriesguide.util.ImageTools
import com.google.common.truth.Truth.assertThat
import com.uwetrottmann.tmdb2.entities.Movie
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test Room database migrations.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    // Helper for creating Room databases and migrations
    @get:Rule
    var migrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SgRoomDatabase::class.java
    )

    // Helper for creating SQLite database in version 42
    private lateinit var sqliteTestDbHelper: SqliteTestDbOpenHelper

    @Before
    fun setUp() {
        // delete database file to ensure onCreate is called in SqliteTestDbOpenHelper
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(TEST_DB_NAME)

        // create the database with version 42 using SQLite API
        sqliteTestDbHelper = SqliteTestDbOpenHelper(
            ApplicationProvider.getApplicationContext(),
            TEST_DB_NAME
        )
    }

    @After
    fun tearDown() {
        // close the database to minimize issues when deleting it in setUp()
        sqliteTestDbHelper.close()
    }

    @Test
    fun migrationFrom42To43_containsCorrectData() {
        // Create the database with the initial version 42 schema and insert test data
        insertTestDataSqlite()

        // Re-open the database with version 43 and
        // provide MIGRATION_42_43 as the migration process.
        val database = migrationTestHelper
            .runMigrationsAndValidate(
                TEST_DB_NAME, SgRoomDatabase.VERSION_43_ROOM,
                false /* FTS table added manually */,
                SgRoomDatabase.MIGRATION_42_43
            )
        assertTestData_series_seasons_episodes(database)
    }

    @Test
    fun migrationFrom42To44_containsCorrectData() {
        insertTestDataSqlite()

        val version = SgRoomDatabase.VERSION_44_RECREATE_SERIES_EPISODES
        migrationTestHelper.runMigrationsAndValidate(
            TEST_DB_NAME, version,
            false,
            SgRoomDatabase.MIGRATION_42_43,
            SgRoomDatabase.MIGRATION_43_44
        )

        // MigrationTestHelper automatically verifies the schema changes, but not the data validity
        // Validate that the data was migrated properly.
        assertTestData_series_seasons_episodes(getMigratedDatabase(version))
    }

    /**
     * Create the database with the initial version 42 schema and insert test data.
     */
    private fun insertTestDataSqlite() {
        val db = sqliteTestDbHelper.writableDatabase
        SqliteDatabaseTestHelper.insertShow(SHOW, db)
        SqliteDatabaseTestHelper.insertSeason(SEASON, db)
        SqliteDatabaseTestHelper.insertEpisode(
            EPISODE,
            SHOW.tvdbId,
            SEASON.tvdbId,
            SEASON.number,
            db
        )
        db.close()
    }

    @Test
    fun migrationFrom43To44_containsCorrectData() {
        // First version that uses Room, so can use migration test helper
        val db = migrationTestHelper
            .createDatabase(TEST_DB_NAME, SgRoomDatabase.VERSION_43_ROOM)
        RoomDatabaseTestHelper.insertShow(SHOW, db)
        RoomDatabaseTestHelper.insertSeason(SEASON, db)
        RoomDatabaseTestHelper.insertEpisode(EPISODE, SHOW.tvdbId, SEASON.tvdbId, SEASON.number, db)
        db.close()

        assertTestData_series_seasons_episodes(getMigratedDatabase(SgRoomDatabase.VERSION_44_RECREATE_SERIES_EPISODES))
    }

    @Test
    fun migrationFrom44To45_containsCorrectData() {
        val db = migrationTestHelper
            .createDatabase(TEST_DB_NAME, SgRoomDatabase.VERSION_44_RECREATE_SERIES_EPISODES)
        RoomDatabaseTestHelper.insertShow(SHOW, db)
        RoomDatabaseTestHelper.insertSeason(SEASON, db)
        RoomDatabaseTestHelper.insertEpisode(EPISODE, SHOW.tvdbId, SEASON.tvdbId, SEASON.number, db)
        db.close()

        assertTestData_series_seasons_episodes(getMigratedDatabase(SgRoomDatabase.VERSION_45_RECREATE_SEASONS))
    }

    @Test
    fun migrationFrom45To46_containsCorrectData() {
        var db = migrationTestHelper
            .createDatabase(TEST_DB_NAME, SgRoomDatabase.VERSION_45_RECREATE_SEASONS)
        RoomDatabaseTestHelper.insertShow(SHOW, db)
        RoomDatabaseTestHelper.insertSeason(SEASON, db)
        RoomDatabaseTestHelper.insertEpisode(EPISODE, SHOW.tvdbId, SEASON.tvdbId, SEASON.number, db)
        db.close()

        db = getMigratedDatabase(SgRoomDatabase.VERSION_46_SERIES_SLUG)
        assertTestData_series_seasons_episodes(db)
        queryAndAssert(db, "SELECT series_slug FROM series") { seriesQuery: Cursor ->
            assertThat(seriesQuery.isNull(0)).isTrue()
        }
    }

    @Test
    fun migrationFrom46To47_containsCorrectData() {
        var db = migrationTestHelper
            .createDatabase(TEST_DB_NAME, SgRoomDatabase.VERSION_46_SERIES_SLUG)
        RoomDatabaseTestHelper.insertShow(SHOW, db)
        RoomDatabaseTestHelper.insertSeason(SEASON, db)
        RoomDatabaseTestHelper.insertEpisode(EPISODE, SHOW.tvdbId, SEASON.tvdbId, SEASON.number, db)
        db.close()

        db = getMigratedDatabase(SgRoomDatabase.VERSION_47_SERIES_POSTER_THUMB)
        assertTestData_series_seasons_episodes(db)
        queryAndAssert(db, "SELECT series_poster_small, poster FROM series") {
            assertThat(it.getString(0))
                .isEqualTo(ImageTools.TVDB_LEGACY_CACHE_PREFIX + it.getString(1))
        }
    }

    @Test
    fun migrationFrom47To48_containsCorrectData() {
        var db = migrationTestHelper
            .createDatabase(TEST_DB_NAME, SgRoomDatabase.VERSION_47_SERIES_POSTER_THUMB)
        RoomDatabaseTestHelper.insertShow(SHOW, db)
        RoomDatabaseTestHelper.insertSeason(SEASON, db)

        var testEpisode = getTestEpisode(21)
        RoomDatabaseTestHelper.insertEpisode(
            db,
            testEpisode,
            SHOW.tvdbId,
            SEASON.tvdbId,
            SEASON.number,
            true
        )

        testEpisode = getTestEpisode(22)
        RoomDatabaseTestHelper.insertEpisode(
            db,
            testEpisode,
            SHOW.tvdbId,
            SEASON.tvdbId,
            SEASON.number,
            false
        )

        var testMovieDetails = getTestMovieDetails(12)
        testMovieDetails.isWatched = true
        RoomDatabaseTestHelper.insertMovie(db, testMovieDetails)

        testMovieDetails = getTestMovieDetails(13)
        testMovieDetails.isWatched = false
        RoomDatabaseTestHelper.insertMovie(db, testMovieDetails)

        db.close()

        db = getMigratedDatabase(SgRoomDatabase.VERSION_48_EPISODE_PLAYS)
        assertTestData_series_seasons_episodes(db)

        // Watched episode should have 1 play.
        queryAndAssert(db, "SELECT plays FROM episodes WHERE _id=21") {
            assertThat(it.getInt(0)).isEqualTo(1)
        }
        queryAndAssert(db, "SELECT plays FROM episodes WHERE _id=22") {
            assertThat(it.getInt(0)).isEqualTo(0)
        }

        // Watched movie should have 1 play.
        queryAndAssert(db, "SELECT movies_plays FROM movies WHERE movies_tmdbid=12") {
            assertThat(it.getInt(0)).isEqualTo(1)
        }
        queryAndAssert(db, "SELECT movies_plays FROM movies WHERE movies_tmdbid=13") {
            assertThat(it.getInt(0)).isEqualTo(0)
        }
    }

    /**
     * Validate test data for version [SgRoomDatabase.VERSION_48_EPISODE_PLAYS] or lower.
     *
     * MigrationTestHelper automatically verifies the schema changes, but not the data validity.
     * Validate that the data was migrated properly.
     */
    private fun assertTestData_series_seasons_episodes(db: SupportSQLiteDatabase) {
        queryAndAssert(db, "SELECT _id, seriestitle, runtime, poster FROM series") {
            assertThat(it.getInt(0)).isEqualTo(SHOW.tvdbId)
            assertThat(it.getString(1)).isEqualTo(SHOW.title)
            assertThat(it.getInt(2)).isEqualTo(SHOW.runtime)
            assertThat(it.getString(3)).isEqualTo(SHOW.poster)
        }
        queryAndAssert(db, "SELECT _id, series_id, combinednr FROM seasons") {
            assertThat(it.getInt(0)).isEqualTo(SEASON.tvdbId)
            assertThat(it.getString(1)).isEqualTo(SEASON.showTvdbId)
            assertThat(it.getInt(2)).isEqualTo(SEASON.number)
        }
        queryAndAssert(
            db,
            "SELECT _id, series_id, season_id, episodetitle, episodenumber, season FROM episodes"
        ) {
            assertThat(it.getInt(0)).isEqualTo(EPISODE.tvdbId)
            assertThat(it.getInt(1)).isEqualTo(SHOW.tvdbId)
            assertThat(it.getInt(2)).isEqualTo(SEASON.tvdbId)
            assertThat(it.getString(3)).isEqualTo(EPISODE.name)
            assertThat(it.getInt(4)).isEqualTo(EPISODE.number)
            assertThat(it.getInt(5)).isEqualTo(SEASON.number)
        }
    }

    @Test
    fun migrationFrom48To49_containsCorrectData() {
        val dbOld = migrationTestHelper
            .createDatabase(TEST_DB_NAME, SgRoomDatabase.VERSION_48_EPISODE_PLAYS)
        RoomDatabaseTestHelper.insertShow(SHOW, dbOld)
        RoomDatabaseTestHelper.insertSeason(SEASON, dbOld)
        RoomDatabaseTestHelper.insertEpisode(
            dbOld,
            EPISODE,
            SHOW.tvdbId,
            SEASON.tvdbId,
            SEASON.number,
            true
        )
        // Insert activity
        dbOld.execSQL("INSERT INTO activity (activity_episode, activity_show, activity_time) VALUES (21, 42, 123456789)")
        dbOld.close()

        val db = getMigratedDatabase(SgRoomDatabase.VERSION_49_AUTO_ID_MIGRATION)
        // Old tables should still exist, data should remain.
        assertTestData_series_seasons_episodes(db)

        // New tables have different structure.
        queryAndAssert(
            db,
            "SELECT _id, series_tvdb_id, series_tmdb_id, series_title, series_runtime, series_poster, series_next FROM sg_show"
        ) {
            // Row id should be auto-generated.
            assertThat(it.getLong(0)).isNotEqualTo(SHOW.tvdbId)
            // TVDB id should be in new column.
            assertThat(it.getInt(1)).isEqualTo(SHOW.tvdbId)
            // TMDB id should not be set, but exist.
            assertThat(it.isNull(2)).isTrue()
            // Some other values that should have moved to other columns.
            assertThat(it.getString(3)).isEqualTo(SHOW.title)
            assertThat(it.getInt(4)).isEqualTo(SHOW.runtime)
            assertThat(it.getString(5)).isEqualTo(SHOW.poster)
            // Next episode changed from TVDB to row ID, so reset to default value.
            assertThat(it.getString(6)).isEmpty()
        }

        val showIdQuery = db.query("SELECT _id FROM sg_show")
        showIdQuery.moveToFirst()
        val showId = showIdQuery.getLong(0)
        showIdQuery.close()

        queryAndAssert(
            db,
            "SELECT _id, series_id, season_tmdb_id, season_tvdb_id, season_number, season_order FROM sg_season"
        ) {
            // Row id should be auto-generated.
            assertThat(it.getLong(0)).isNotEqualTo(SEASON.tvdbId)
            // Show ID should now be internal ID, not TVDB ID.
            assertThat(it.getInt(1)).isEqualTo(showId)
            // TMDB id should not be set, but exist.
            assertThat(it.isNull(2)).isTrue()
            // TVDB ID should be in new column.
            assertThat(it.getInt(3)).isEqualTo(SEASON.tvdbId)
            assertThat(it.getInt(4)).isEqualTo(SEASON.number)
            // order is new, should be the number.
            assertThat(it.getInt(5)).isEqualTo(SEASON.number)
        }

        val seasonIdQuery = db.query("SELECT _id FROM sg_season")
        seasonIdQuery.moveToFirst()
        val seasonId = seasonIdQuery.getLong(0)
        seasonIdQuery.close()

        queryAndAssert(
            db,
            "SELECT _id, series_id, season_id, episode_tmdb_id, episode_tvdb_id, episode_title, episode_number, episode_order, episode_season_number FROM sg_episode"
        ) {
            // Row id should be auto-generated.
            assertThat(it.getLong(0)).isNotEqualTo(EPISODE.tvdbId)
            // Show and season ID should now be internal ID, not TVDB ID.
            assertThat(it.getLong(1)).isEqualTo(showId)
            assertThat(it.getLong(2)).isEqualTo(seasonId)
            // TMDB id should not be set, but exist.
            assertThat(it.isNull(3)).isTrue()
            assertThat(it.getInt(4)).isEqualTo(EPISODE.tvdbId)
            assertThat(it.getString(5)).isEqualTo(EPISODE.name)
            assertThat(it.getInt(6)).isEqualTo(EPISODE.number)
            // order is new, should be the number.
            assertThat(it.getInt(7)).isEqualTo(EPISODE.number)
            assertThat(it.getInt(8)).isEqualTo(SEASON.number)
        }

        // Ensure new type column was populated.
        queryAndAssert(db, "SELECT * FROM activity") { cursor ->
            val activity_type = cursor.getColumnIndex("activity_type")
            assertThat(cursor.getInt(activity_type)).isEqualTo(ActivityType.TVDB_ID)
        }
        // Ensure unique index now includes type column by inserting same IDs, but different type.
        val activityStmt =
            ("INSERT INTO activity (activity_episode, activity_show, activity_time, activity_type)"
                    + " VALUES (21, 42, 123456789, " + ActivityType.TMDB_ID + ")")
        db.execSQL(activityStmt)
        val constraintException = assertThrows(SQLiteConstraintException::class.java) {
            db.execSQL(activityStmt)
        }
        assertThat(constraintException).hasMessageThat().contains("UNIQUE constraint")
    }

    private fun getMigratedDatabase(version: Int): SupportSQLiteDatabase {
        return migrationTestHelper.runMigrationsAndValidate(
            TEST_DB_NAME, version, false /* FTS table added manually */,
            SgRoomDatabase.MIGRATION_42_43,
            SgRoomDatabase.MIGRATION_43_44,
            SgRoomDatabase.MIGRATION_44_45,
            SgRoomDatabase.MIGRATION_45_46,
            SgRoomDatabase.MIGRATION_46_47,
            SgRoomDatabase.MIGRATION_47_48,
            SgRoomDatabase.MIGRATION_48_49,
            SgRoomDatabase.MIGRATION_49_50 // not tested, just adds a new table
        )
    }

    private fun queryAndAssert(
        database: SupportSQLiteDatabase, query: String,
        assertCursor: (Cursor) -> Unit
    ) {
        val cursor = database.query(query)
        assertThat(cursor.moveToFirst()).isTrue()
        assertCursor(cursor)
        cursor.close()
    }

    companion object {
        private const val TEST_DB_NAME = "test-db"
        private val SHOW = TestShow(
            21,
            "The No Answers Show",
            45,
            "example.jpg",
            "21"
        )
        private val SEASON = TestSeason(
            21,
            "21",
            2
        )
        private val EPISODE = TestEpisode(
            21,
            "Episode Title",
            1
        )

        private fun getTestEpisode(tvdbId: Int?): TestEpisode {
            return TestEpisode(
                tvdbId ?: 21,
                "Episode Title",
                1
            )
        }

        private fun getTestMovieDetails(tmdbId: Int?): MovieDetails {
            val movieDetails = MovieDetails()
            val tmdbMovie = Movie()
            if (tmdbId != null) {
                tmdbMovie.id = tmdbId
            } else {
                tmdbMovie.id = 12
            }
            movieDetails.tmdbMovie(tmdbMovie)
            return movieDetails
        }
    }
}