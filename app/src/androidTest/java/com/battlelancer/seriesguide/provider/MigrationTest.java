package com.battlelancer.seriesguide.provider;

import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_42_43;
import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_43_44;
import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_44_45;
import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_45_46;
import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_46_47;
import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_47_48;
import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_48_49;
import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_49_50;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.Nullable;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.battlelancer.seriesguide.model.ActivityType;
import com.battlelancer.seriesguide.provider.RoomDatabaseTestHelper.TestEpisode;
import com.battlelancer.seriesguide.provider.RoomDatabaseTestHelper.TestSeason;
import com.battlelancer.seriesguide.provider.RoomDatabaseTestHelper.TestShow;
import com.battlelancer.seriesguide.ui.movies.MovieDetails;
import com.battlelancer.seriesguide.util.ImageTools;
import com.uwetrottmann.tmdb2.entities.Movie;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MigrationTest {

    private static final String TEST_DB_NAME = "test-db";

    private static final TestShow SHOW = new TestShow(
            21,
            "The No Answers Show",
            45,
            "example.jpg",
            "21"
    );
    private static final TestSeason SEASON = new TestSeason(
            21,
            "21",
            2
    );
    private static final TestEpisode EPISODE = new TestEpisode(
            21,
            "Episode Title",
            1
    );

    private static TestEpisode getTestEpisode(@Nullable Integer tvdbId) {
        return new TestEpisode(
                tvdbId != null ? tvdbId : 21,
                "Episode Title",
                1
        );
    }

    private static MovieDetails getTestMovieDetails(@Nullable Integer tmdbId) {
        MovieDetails movieDetails = new MovieDetails();
        Movie tmdbMovie = new Movie();
        if (tmdbId != null) {
            tmdbMovie.id = tmdbId;
        } else {
            tmdbMovie.id = 12;
        }
        movieDetails.tmdbMovie(tmdbMovie);
        return movieDetails;
    }

    // Helper for creating Room databases and migrations
    @Rule
    public MigrationTestHelper migrationTestHelper =
            new MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
                    SgRoomDatabase.class.getCanonicalName(),
                    new FrameworkSQLiteOpenHelperFactory());

    // Helper for creating SQLite database in version 42
    private SqliteTestDbOpenHelper sqliteTestDbHelper;

    @Before
    public void setUp() {
        // delete database file to ensure onCreate is called in SqliteTestDbOpenHelper
        ApplicationProvider.getApplicationContext().deleteDatabase(TEST_DB_NAME);

        // create the database with version 42 using SQLite API
        sqliteTestDbHelper = new SqliteTestDbOpenHelper(ApplicationProvider.getApplicationContext(),
                TEST_DB_NAME);
    }

    @After
    public void tearDown() {
        // close the database to minimize issues when deleting it in setUp()
        sqliteTestDbHelper.close();
    }

    @Test
    public void migrationFrom42To43_containsCorrectData() throws IOException {
        // Create the database with the initial version 42 schema and insert test data
        insertTestDataSqlite();

        // Re-open the database with version 43 and
        // provide MIGRATION_42_43 as the migration process.
        SupportSQLiteDatabase database = migrationTestHelper
                .runMigrationsAndValidate(TEST_DB_NAME, 43,
                        false /* adding FTS table ourselves */, MIGRATION_42_43);
        assertTestData_series_seasons_episodes(database);
    }

    @Test
    public void migrationFrom42To44_containsCorrectData() throws IOException {
        insertTestDataSqlite();

        migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, 44,
                false, MIGRATION_42_43, MIGRATION_43_44);

        // MigrationTestHelper automatically verifies the schema changes, but not the data validity
        // Validate that the data was migrated properly.
        assertTestData_series_seasons_episodes(getMigratedDatabase(44));
    }

    private void insertTestDataSqlite() {
        // Create the database with the initial version 42 schema and insert test data
        SQLiteDatabase db = sqliteTestDbHelper.getWritableDatabase();
        SqliteDatabaseTestHelper.insertShow(SHOW, db);
        SqliteDatabaseTestHelper.insertSeason(SEASON, db);
        SqliteDatabaseTestHelper
                .insertEpisode(EPISODE, SHOW.getTvdbId(), SEASON.getTvdbId(), SEASON.getNumber(),
                        db);
        db.close();
    }

    @Test
    public void migrationFrom43To44_containsCorrectData() throws IOException {
        // First version that uses Room, so can use migration test helper
        SupportSQLiteDatabase db = migrationTestHelper
                .createDatabase(TEST_DB_NAME, SgRoomDatabase.VERSION_43_ROOM);
        RoomDatabaseTestHelper.insertShow(SHOW, db);
        RoomDatabaseTestHelper.insertSeason(SEASON, db);
        RoomDatabaseTestHelper
                .insertEpisode(EPISODE, SHOW.getTvdbId(), SEASON.getTvdbId(), SEASON.getNumber(),
                        db);
        db.close();

        assertTestData_series_seasons_episodes(getMigratedDatabase(SgRoomDatabase.VERSION_44_RECREATE_SERIES_EPISODES));
    }

    @Test
    public void migrationFrom44To45_containsCorrectData() throws IOException {
        SupportSQLiteDatabase db = migrationTestHelper
                .createDatabase(TEST_DB_NAME, SgRoomDatabase.VERSION_44_RECREATE_SERIES_EPISODES);
        RoomDatabaseTestHelper.insertShow(SHOW, db);
        RoomDatabaseTestHelper.insertSeason(SEASON, db);
        RoomDatabaseTestHelper
                .insertEpisode(EPISODE, SHOW.getTvdbId(), SEASON.getTvdbId(), SEASON.getNumber(),
                        db);
        db.close();

        assertTestData_series_seasons_episodes(getMigratedDatabase(SgRoomDatabase.VERSION_45_RECREATE_SEASONS));
    }

    @Test
    public void migrationFrom45To46_containsCorrectData() throws IOException {
        SupportSQLiteDatabase db = migrationTestHelper
                .createDatabase(TEST_DB_NAME, SgRoomDatabase.VERSION_45_RECREATE_SEASONS);
        RoomDatabaseTestHelper.insertShow(SHOW, db);
        RoomDatabaseTestHelper.insertSeason(SEASON, db);
        RoomDatabaseTestHelper
                .insertEpisode(EPISODE, SHOW.getTvdbId(), SEASON.getTvdbId(), SEASON.getNumber(),
                        db);
        db.close();

        db = getMigratedDatabase(SgRoomDatabase.VERSION_46_SERIES_SLUG);
        assertTestData_series_seasons_episodes(db);
        queryAndAssert(db, "SELECT series_slug FROM series",
                seriesQuery -> assertThat(seriesQuery.isNull(0)).isTrue());
    }

    @Test
    public void migrationFrom46To47_containsCorrectData() throws IOException {
        SupportSQLiteDatabase db = migrationTestHelper
                .createDatabase(TEST_DB_NAME, SgRoomDatabase.VERSION_46_SERIES_SLUG);
        RoomDatabaseTestHelper.insertShow(SHOW, db);
        RoomDatabaseTestHelper.insertSeason(SEASON, db);
        RoomDatabaseTestHelper
                .insertEpisode(EPISODE, SHOW.getTvdbId(), SEASON.getTvdbId(), SEASON.getNumber(),
                        db);
        db.close();

        db = getMigratedDatabase(SgRoomDatabase.VERSION_47_SERIES_POSTER_THUMB);
        assertTestData_series_seasons_episodes(db);
        queryAndAssert(db, "SELECT series_poster_small, poster FROM series",
                series -> assertThat(series.getString(0))
                        .isEqualTo(ImageTools.TVDB_LEGACY_CACHE_PREFIX + series.getString(1)));
    }

    @Test
    public void migrationFrom47To48_containsCorrectData() throws IOException {
        int v47 = SgRoomDatabase.VERSION_47_SERIES_POSTER_THUMB;
        SupportSQLiteDatabase db = migrationTestHelper.createDatabase(TEST_DB_NAME, v47);
        RoomDatabaseTestHelper.insertShow(SHOW, db);
        RoomDatabaseTestHelper.insertSeason(SEASON, db);

        TestEpisode testEpisode = getTestEpisode(21);
        RoomDatabaseTestHelper.insertEpisode(db, testEpisode, SHOW.getTvdbId(), SEASON.getTvdbId(),
                SEASON.getNumber(), true);

        testEpisode = getTestEpisode(22);
        RoomDatabaseTestHelper.insertEpisode(db, testEpisode, SHOW.getTvdbId(), SEASON.getTvdbId(),
                SEASON.getNumber(), false);

        MovieDetails testMovieDetails = getTestMovieDetails(12);
        testMovieDetails.setWatched(true);
        RoomDatabaseTestHelper.insertMovie(db, testMovieDetails);

        testMovieDetails = getTestMovieDetails(13);
        testMovieDetails.setWatched(false);
        RoomDatabaseTestHelper.insertMovie(db, testMovieDetails);
        db.close();

        db = getMigratedDatabase(SgRoomDatabase.VERSION_48_EPISODE_PLAYS);
        assertTestData_series_seasons_episodes(db);

        // Watched episode should have 1 play.
        queryAndAssert(db, "SELECT plays FROM episodes WHERE _id=21",
                episodeWatched -> assertThat(episodeWatched.getInt(0)).isEqualTo(1));

        queryAndAssert(db, "SELECT plays FROM episodes WHERE _id=22",
                episodeNotWatched -> assertThat(episodeNotWatched.getInt(0)).isEqualTo(0));

        // Watched movie should have 1 play.
        queryAndAssert(db, "SELECT movies_plays FROM movies WHERE movies_tmdbid=12",
                movieWatched -> assertThat(movieWatched.getInt(0)).isEqualTo(1));

        queryAndAssert(db, "SELECT movies_plays FROM movies WHERE movies_tmdbid=13",
                movieNotWatched -> assertThat(movieNotWatched.getInt(0)).isEqualTo(0));
    }

    private void assertTestData_series_seasons_episodes(SupportSQLiteDatabase db) {
        // MigrationTestHelper automatically verifies the schema changes, but not the data validity.
        // Validate that the data was migrated properly.
        queryAndAssert(db, "SELECT _id, seriestitle, runtime, poster FROM series",
                dbShow -> {
                    assertThat(dbShow.getInt(0)).isEqualTo(SHOW.getTvdbId());
                    assertThat(dbShow.getString(1)).isEqualTo(SHOW.getTitle());
                    assertThat(dbShow.getInt(2)).isEqualTo(SHOW.getRuntime());
                    assertThat(dbShow.getString(3)).isEqualTo(SHOW.getPoster());
                });

        queryAndAssert(db, "SELECT _id, series_id, combinednr FROM seasons",
                dbSeason -> {
                    assertThat(dbSeason.getInt(0)).isEqualTo(SEASON.getTvdbId());
                    assertThat(dbSeason.getString(1)).isEqualTo(SEASON.getShowTvdbId());
                    assertThat(dbSeason.getInt(2)).isEqualTo(SEASON.getNumber());
                });

        queryAndAssert(db,
                "SELECT _id, series_id, season_id, episodetitle, episodenumber, season FROM episodes",
                dbEpisode -> {
                    assertThat(dbEpisode.getInt(0)).isEqualTo(EPISODE.getTvdbId());
                    assertThat(dbEpisode.getInt(1)).isEqualTo(SHOW.getTvdbId());
                    assertThat(dbEpisode.getInt(2)).isEqualTo(SEASON.getTvdbId());
                    assertThat(dbEpisode.getString(3)).isEqualTo(EPISODE.getName());
                    assertThat(dbEpisode.getInt(4)).isEqualTo(EPISODE.getNumber());
                    assertThat(dbEpisode.getInt(5)).isEqualTo(SEASON.getNumber());
                });
    }

    @Test
    public void migrationFrom48To49_containsCorrectData() throws IOException {
        SupportSQLiteDatabase dbOld = migrationTestHelper
                .createDatabase(TEST_DB_NAME, SgRoomDatabase.VERSION_48_EPISODE_PLAYS);
        RoomDatabaseTestHelper.insertShow(SHOW, dbOld);
        RoomDatabaseTestHelper.insertSeason(SEASON, dbOld);
        RoomDatabaseTestHelper.insertEpisode(dbOld, EPISODE, SHOW.getTvdbId(), SEASON.getTvdbId(),
                SEASON.getNumber(), true);
        // Insert activity
        dbOld.execSQL("INSERT INTO activity (activity_episode, activity_show, activity_time) VALUES (21, 42, 123456789)");
        dbOld.close();

        final SupportSQLiteDatabase db = getMigratedDatabase(
                SgRoomDatabase.VERSION_49_AUTO_ID_MIGRATION);
        // Old tables should still exist, data should remain.
        assertTestData_series_seasons_episodes(db);

        // New tables have different structure.
        queryAndAssert(db, "SELECT _id, series_tvdb_id, series_tmdb_id, series_title, series_runtime, series_poster, series_next FROM sg_show",
                dbShow -> {
                    // Row id should be auto-generated.
                    assertThat(dbShow.getLong(0)).isNotEqualTo(SHOW.getTvdbId());
                    // TVDB id should be in new column.
                    assertThat(dbShow.getInt(1)).isEqualTo(SHOW.getTvdbId());
                    // TMDB id should not be set, but exist.
                    assertThat(dbShow.isNull(2)).isTrue();
                    // Some other values that should have moved to other columns.
                    assertThat(dbShow.getString(3)).isEqualTo(SHOW.getTitle());
                    assertThat(dbShow.getInt(4)).isEqualTo(SHOW.getRuntime());
                    assertThat(dbShow.getString(5)).isEqualTo(SHOW.getPoster());
                    // Next episode changed from TVDB to row ID, so reset to default value.
                    assertThat(dbShow.getString(6)).isEmpty();
                });

        Cursor showIdQuery = db.query("SELECT _id FROM sg_show");
        showIdQuery.moveToFirst();
        long showId = showIdQuery.getLong(0);
        showIdQuery.close();

        queryAndAssert(db, "SELECT _id, series_id, season_tmdb_id, season_tvdb_id, season_number, season_order FROM sg_season",
                dbSeason -> {
                    // Row id should be auto-generated.
                    assertThat(dbSeason.getLong(0)).isNotEqualTo(SEASON.getTvdbId());
                    // Show ID should now be internal ID, not TVDB ID.
                    assertThat(dbSeason.getInt(1)).isEqualTo(showId);
                    // TMDB id should not be set, but exist.
                    assertThat(dbSeason.isNull(2)).isTrue();
                    // TVDB ID should be in new column.
                    assertThat(dbSeason.getInt(3)).isEqualTo(SEASON.getTvdbId());
                    assertThat(dbSeason.getInt(4)).isEqualTo(SEASON.getNumber());
                    // order is new, should be the number.
                    assertThat(dbSeason.getInt(5)).isEqualTo(SEASON.getNumber());
                });

        Cursor seasonIdQuery = db.query("SELECT _id FROM sg_season");
        seasonIdQuery.moveToFirst();
        long seasonId = seasonIdQuery.getLong(0);
        seasonIdQuery.close();

        queryAndAssert(db,
                "SELECT _id, series_id, season_id, episode_tmdb_id, episode_tvdb_id, episode_title, episode_number, episode_order, episode_season_number FROM sg_episode",
                dbEpisode -> {
                    // Row id should be auto-generated.
                    assertThat(dbEpisode.getLong(0)).isNotEqualTo(EPISODE.getTvdbId());
                    // Show and season ID should now be internal ID, not TVDB ID.
                    assertThat(dbEpisode.getLong(1)).isEqualTo(showId);
                    assertThat(dbEpisode.getLong(2)).isEqualTo(seasonId);
                    // TMDB id should not be set, but exist.
                    assertThat(dbEpisode.isNull(3)).isTrue();
                    assertThat(dbEpisode.getInt(4)).isEqualTo(EPISODE.getTvdbId());
                    assertThat(dbEpisode.getString(5)).isEqualTo(EPISODE.getName());
                    assertThat(dbEpisode.getInt(6)).isEqualTo(EPISODE.getNumber());
                    // order is new, should be the number.
                    assertThat(dbEpisode.getInt(7)).isEqualTo(EPISODE.getNumber());
                    assertThat(dbEpisode.getInt(8)).isEqualTo(SEASON.getNumber());
                });

        // Ensure new type column was populated.
        queryAndAssert(db, "SELECT * FROM activity", cursor -> {
            int activity_type = cursor.getColumnIndex("activity_type");
            assertThat(cursor.getInt(activity_type)).isEqualTo(ActivityType.TVDB_ID);
        });
        // Ensure unique index now includes type column by inserting same IDs, but different type.
        String activityStmt =
                "INSERT INTO activity (activity_episode, activity_show, activity_time, activity_type)"
                        + " VALUES (21, 42, 123456789, " + ActivityType.TMDB_ID + ")";
        db.execSQL(activityStmt);
        SQLiteConstraintException constraintException = assertThrows(
                SQLiteConstraintException.class, () -> db.execSQL(activityStmt));
        assertThat(constraintException).hasMessageThat().contains("UNIQUE constraint");
    }

    private SupportSQLiteDatabase getMigratedDatabase(int version) throws IOException {
        return migrationTestHelper.runMigrationsAndValidate(
                TEST_DB_NAME, version, false /* adding FTS table ourselves */,
                MIGRATION_42_43,
                MIGRATION_43_44,
                MIGRATION_44_45,
                MIGRATION_45_46,
                MIGRATION_46_47,
                MIGRATION_47_48,
                MIGRATION_48_49,
                MIGRATION_49_50 // not tested, just adds a new table
        );
    }

    private interface CursorAsserter {
        void assertCursor(Cursor cursor);
    }

    private void queryAndAssert(SupportSQLiteDatabase database, String query,
            CursorAsserter asserter) {
        Cursor cursor = database.query(query);
        assertThat(cursor.moveToFirst()).isTrue();
        asserter.assertCursor(cursor);
        cursor.close();
    }
}