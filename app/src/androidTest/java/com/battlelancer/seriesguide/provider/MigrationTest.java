package com.battlelancer.seriesguide.provider;

import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_42_43;
import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_43_44;
import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_44_45;
import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_45_46;
import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_46_47;
import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_47_48;
import static com.google.common.truth.Truth.assertThat;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.Nullable;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.battlelancer.seriesguide.provider.RoomDatabaseTestHelper.TestEpisode;
import com.battlelancer.seriesguide.provider.RoomDatabaseTestHelper.TestSeason;
import com.battlelancer.seriesguide.provider.RoomDatabaseTestHelper.TestShow;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.battlelancer.seriesguide.ui.movies.MovieDetails;
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
            "example.jpg"
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
        assertTestData(database);
    }

    @Test
    public void migrationFrom42To44_containsCorrectData() throws IOException {
        insertTestDataSqlite();

        migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, 44,
                false, MIGRATION_42_43, MIGRATION_43_44);

        // MigrationTestHelper automatically verifies the schema changes, but not the data validity
        // Validate that the data was migrated properly.
        assertTestData(getMigratedDatabase(44));
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

        assertTestData(getMigratedDatabase(SgRoomDatabase.VERSION_44_RECREATE_SERIES_EPISODES));
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

        assertTestData(getMigratedDatabase(SgRoomDatabase.VERSION_45_RECREATE_SEASONS));
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
        assertTestData(db);
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
        assertTestData(db);
        queryAndAssert(db, "SELECT series_poster_small, poster FROM series",
                series -> assertThat(series.getString(0))
                        .isEqualTo(TvdbImageTools.TVDB_LEGACY_CACHE_PREFIX + series.getString(1)));
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
        assertTestData(db);

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

    private void assertTestData(SupportSQLiteDatabase db) {
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

    private SupportSQLiteDatabase getMigratedDatabase(int version) throws IOException {
        return migrationTestHelper.runMigrationsAndValidate(
                TEST_DB_NAME, version, false /* adding FTS table ourselves */,
                MIGRATION_42_43,
                MIGRATION_43_44,
                MIGRATION_44_45,
                MIGRATION_45_46,
                MIGRATION_46_47,
                MIGRATION_47_48
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