package com.battlelancer.seriesguide.provider;

import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_42_43;
import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_43_44;
import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_44_45;
import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_45_46;
import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_46_47;
import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_47_48;
import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_48_49;
import static com.google.common.truth.Truth.assertThat;

import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.Nullable;
import androidx.room.Room;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.battlelancer.seriesguide.model.SgEpisode;
import com.battlelancer.seriesguide.model.SgMovie;
import com.battlelancer.seriesguide.model.SgSeason;
import com.battlelancer.seriesguide.model.SgShow;
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
        migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, 43,
                false /* adding FTS table ourselves */, MIGRATION_42_43);
        assertTestData(getMigratedRoomDatabase());
    }

    @Test
    public void migrationFrom42To44_containsCorrectData() throws IOException {
        insertTestDataSqlite();

        migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, 44,
                false, MIGRATION_42_43, MIGRATION_43_44);

        // MigrationTestHelper automatically verifies the schema changes, but not the data validity
        // Validate that the data was migrated properly.
        assertTestData(getMigratedRoomDatabase());
    }

    private void insertTestDataSqlite() {
        // Create the database with the initial version 42 schema and insert test data
        SQLiteDatabase db = sqliteTestDbHelper.getWritableDatabase();
        SqliteDatabaseTestHelper.insertShow(SHOW, db);
        SqliteDatabaseTestHelper.insertSeason(SEASON, db);
        SqliteDatabaseTestHelper
                .insertEpisode(EPISODE, SHOW.getTvdbId(), SEASON.getTvdbId(), SEASON.getNumber(), db);
        db.close();
    }

    @Test
    public void migrationFrom43To44_containsCorrectData() throws IOException {
        // First version that uses Room, so can use migration test helper
        SupportSQLiteDatabase db = migrationTestHelper.createDatabase(TEST_DB_NAME, 43);
        RoomDatabaseTestHelper.insertShow(SHOW, db, 43);
        RoomDatabaseTestHelper.insertSeason(SEASON, db);
        RoomDatabaseTestHelper
                .insertEpisode(EPISODE, SHOW.getTvdbId(), SEASON.getTvdbId(), SEASON.getNumber(), db);
        db.close();

        assertTestData(getMigratedRoomDatabase());
    }

    @Test
    public void migrationFrom44To45_containsCorrectData() throws IOException {
        SupportSQLiteDatabase db = migrationTestHelper.createDatabase(TEST_DB_NAME, 44);
        RoomDatabaseTestHelper.insertShow(SHOW, db, 44);
        RoomDatabaseTestHelper.insertSeason(SEASON, db);
        RoomDatabaseTestHelper
                .insertEpisode(EPISODE, SHOW.getTvdbId(), SEASON.getTvdbId(), SEASON.getNumber(), db);
        db.close();

        assertTestData(getMigratedRoomDatabase());
    }

    @Test
    public void migrationFrom45To46_containsCorrectData() throws IOException {
        SupportSQLiteDatabase db = migrationTestHelper.createDatabase(TEST_DB_NAME, 45);
        RoomDatabaseTestHelper.insertShow(SHOW, db, 45);
        RoomDatabaseTestHelper.insertSeason(SEASON, db);
        RoomDatabaseTestHelper
                .insertEpisode(EPISODE, SHOW.getTvdbId(), SEASON.getTvdbId(), SEASON.getNumber(), db);
        db.close();

        SgRoomDatabase database = getMigratedRoomDatabase();
        assertTestData(database);
        SgShow dbShow = database.showHelper().getShow();
        assertThat(dbShow.slug).isNull();
    }

    @Test
    public void migrationFrom46To47_containsCorrectData() throws IOException {
        SupportSQLiteDatabase db = migrationTestHelper.createDatabase(TEST_DB_NAME, 46);
        RoomDatabaseTestHelper.insertShow(SHOW, db, 46);
        RoomDatabaseTestHelper.insertSeason(SEASON, db);
        RoomDatabaseTestHelper
                .insertEpisode(EPISODE, SHOW.getTvdbId(), SEASON.getTvdbId(), SEASON.getNumber(), db);
        db.close();

        SgRoomDatabase database = getMigratedRoomDatabase();
        assertTestData(database);
        SgShow dbShow = database.showHelper().getShow();
        assertThat(dbShow.posterSmall)
                .isEqualTo(TvdbImageTools.TVDB_LEGACY_CACHE_PREFIX + dbShow.poster);
    }

    @Test
    public void migrationFrom47To48_containsCorrectData() throws IOException {
        int v47 = SgRoomDatabase.VERSION_47_SERIES_POSTER_THUMB;
        SupportSQLiteDatabase db = migrationTestHelper.createDatabase(TEST_DB_NAME, v47);
        RoomDatabaseTestHelper.insertShow(SHOW, db, v47);
        RoomDatabaseTestHelper.insertSeason(SEASON, db);

        TestEpisode testEpisode = getTestEpisode(21);
        RoomDatabaseTestHelper
                .insertEpisode(db, v47, testEpisode, SHOW.getTvdbId(), SEASON.getTvdbId(), SEASON.getNumber(),
                        true);

        testEpisode = getTestEpisode(22);
        RoomDatabaseTestHelper
                .insertEpisode(db, v47, testEpisode, SHOW.getTvdbId(), SEASON.getTvdbId(), SEASON.getNumber(),
                        false);

        MovieDetails testMovieDetails = getTestMovieDetails(12);
        testMovieDetails.setWatched(true);
        RoomDatabaseTestHelper.insertMovie(db, testMovieDetails);

        testMovieDetails = getTestMovieDetails(13);
        testMovieDetails.setWatched(false);
        RoomDatabaseTestHelper.insertMovie(db, testMovieDetails);
        db.close();

        SgRoomDatabase database = getMigratedRoomDatabase();
        assertTestData(database);

        // Watched episode should have 1 play.
        SgEpisode episodeWatched = database.episodeHelper().getEpisode(21);
        assertThat(episodeWatched.plays).isEqualTo(1);

        SgEpisode episodeNotWatched = database.episodeHelper().getEpisode(22);
        assertThat(episodeNotWatched.plays).isEqualTo(0);

        // Watched movie should have 1 play.
        SgMovie movieWatched = database.movieHelper().getMovie(12);
        assertThat(movieWatched.plays).isEqualTo(1);

        SgMovie movieNotWatched = database.movieHelper().getMovie(13);
        assertThat(movieNotWatched.plays).isEqualTo(0);
    }

    private void assertTestData(SgRoomDatabase database) {
        // MigrationTestHelper automatically verifies the schema changes, but not the data validity.
        // Validate that the data was migrated properly.
        SgShow dbShow = database.showHelper().getShow();
        assertThat(dbShow.tvdbId).isEqualTo(SHOW.getTvdbId());
        assertThat(dbShow.title).isEqualTo(SHOW.getTitle());
        assertThat(dbShow.runtime).isEqualTo(String.valueOf(SHOW.getRuntime()));
        assertThat(dbShow.poster).isEqualTo(SHOW.getPoster());

        SgSeason dbSeason = database.seasonHelper().getSeason();
        assertThat(dbSeason.tvdbId).isEqualTo(SEASON.getTvdbId());
        assertThat(dbSeason.showId).isEqualTo(dbShow.id);
        assertThat(dbSeason.number).isEqualTo(SEASON.getNumber());

        SgEpisode dbEpisode = database.episodeHelper().getEpisode();
        assertThat(dbEpisode.tvdbId).isEqualTo(EPISODE.getTvdbId());
        assertThat(dbEpisode.showId).isEqualTo(dbShow.id);
        assertThat(dbEpisode.seasonId).isEqualTo(dbSeason.id);
        assertThat(dbEpisode.title).isEqualTo(EPISODE.getName());
        assertThat(dbEpisode.number).isEqualTo(EPISODE.getNumber());
        assertThat(dbEpisode.season).isEqualTo(SEASON.getNumber());
    }

    private SgRoomDatabase getMigratedRoomDatabase() {
        SgRoomDatabase database = Room.databaseBuilder(ApplicationProvider.getApplicationContext(),
                SgRoomDatabase.class, TEST_DB_NAME)
                .addMigrations(
                        MIGRATION_42_43,
                        MIGRATION_43_44,
                        MIGRATION_44_45,
                        MIGRATION_45_46,
                        MIGRATION_46_47,
                        MIGRATION_47_48,
                        MIGRATION_48_49
                )
                .build();
        // close the database and release any stream resources when the test finishes
        migrationTestHelper.closeWhenFinished(database);
        return database;
    }
}