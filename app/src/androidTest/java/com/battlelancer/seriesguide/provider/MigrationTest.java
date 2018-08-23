package com.battlelancer.seriesguide.provider;

import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_42_43;
import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_43_44;
import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_44_45;
import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_45_46;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.framework.FrameworkSQLiteOpenHelperFactory;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.testing.MigrationTestHelper;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.model.SgEpisode;
import com.battlelancer.seriesguide.model.SgSeason;
import com.battlelancer.seriesguide.model.SgShow;
import com.uwetrottmann.thetvdb.entities.Episode;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MigrationTest {

    private static final String TEST_DB_NAME = "test-db";

    private static final Show SHOW = new Show();
    private static final SgSeason SEASON = new SgSeason();
    private static final Episode EPISODE = new Episode();

    static {
        SHOW.tvdb_id = 21;
        SHOW.title = "The No Answers Show";
        SHOW.runtime = 45;

        SEASON.tvdbId = 21;
        SEASON.showTvdbId = "21";
        SEASON.number = 2;

        EPISODE.id = 21;
        EPISODE.episodeName = "Episode Title";
        EPISODE.airedEpisodeNumber = 1;
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
        InstrumentationRegistry.getTargetContext().deleteDatabase(TEST_DB_NAME);

        // create the database with version 42 using SQLite API
        sqliteTestDbHelper = new SqliteTestDbOpenHelper(InstrumentationRegistry.getTargetContext(),
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
                .insertEpisode(EPISODE, SHOW.tvdb_id, SEASON.tvdbId, SEASON.number, db);
        db.close();
    }

    @Test
    public void migrationFrom43To44_containsCorrectData() throws IOException {
        // First version that uses Room, so can use migration test helper
        SupportSQLiteDatabase db = migrationTestHelper.createDatabase(TEST_DB_NAME, 43);
        RoomDatabaseTestHelper.insertShow(SHOW, db);
        RoomDatabaseTestHelper.insertSeason(SEASON, db);
        RoomDatabaseTestHelper
                .insertEpisode(EPISODE, SHOW.tvdb_id, SEASON.tvdbId, SEASON.number, db);
        db.close();

        assertTestData(getMigratedRoomDatabase());
    }

    @Test
    public void migrationFrom44To45_containsCorrectData() throws IOException {
        SupportSQLiteDatabase db = migrationTestHelper.createDatabase(TEST_DB_NAME, 44);
        RoomDatabaseTestHelper.insertShow(SHOW, db);
        RoomDatabaseTestHelper.insertSeason(SEASON, db);
        RoomDatabaseTestHelper
                .insertEpisode(EPISODE, SHOW.tvdb_id, SEASON.tvdbId, SEASON.number, db);
        db.close();

        assertTestData(getMigratedRoomDatabase());
    }

    @Test
    public void migrationFrom45To46_containsCorrectData() throws IOException {
        SupportSQLiteDatabase db = migrationTestHelper.createDatabase(TEST_DB_NAME, 45);
        RoomDatabaseTestHelper.insertShow(SHOW, db);
        RoomDatabaseTestHelper.insertSeason(SEASON, db);
        RoomDatabaseTestHelper
                .insertEpisode(EPISODE, SHOW.tvdb_id, SEASON.tvdbId, SEASON.number, db);
        db.close();

        SgRoomDatabase database = getMigratedRoomDatabase();
        assertTestData(database);
        SgShow dbShow = database.showHelper().getShow();
        assertNull(dbShow.slug);
    }

    private void assertTestData(SgRoomDatabase database) {
        // MigrationTestHelper automatically verifies the schema changes, but not the data validity
        // Validate that the data was migrated properly.
        SgShow dbShow = database.showHelper().getShow();
        assertEquals(SHOW.tvdb_id, dbShow.tvdbId);
        assertEquals(SHOW.title, dbShow.title);
        assertEquals(String.valueOf(SHOW.runtime), dbShow.runtime);

        SgSeason dbSeason = database.seasonHelper().getSeason();
        assertEquals(SEASON.tvdbId, dbSeason.tvdbId);
        assertEquals(SEASON.showTvdbId, dbSeason.showTvdbId);
        assertEquals(SEASON.number, dbSeason.number);

        SgEpisode dbEpisode = database.episodeHelper().getEpisode();
        assertEquals(EPISODE.id.intValue(), dbEpisode.tvdbId);
        assertEquals(SHOW.tvdb_id, dbEpisode.showTvdbId);
        assertEquals(SEASON.tvdbId.intValue(), dbEpisode.seasonTvdbId);
        assertEquals(EPISODE.episodeName, dbEpisode.title);
        assertEquals(EPISODE.airedEpisodeNumber.intValue(), dbEpisode.number);
        assertEquals(SEASON.number.intValue(), dbEpisode.season);
    }

    private SgRoomDatabase getMigratedRoomDatabase() {
        SgRoomDatabase database = Room.databaseBuilder(InstrumentationRegistry.getTargetContext(),
                SgRoomDatabase.class, TEST_DB_NAME)
                .addMigrations(
                        MIGRATION_42_43,
                        MIGRATION_43_44,
                        MIGRATION_44_45,
                        MIGRATION_45_46
                )
                .build();
        // close the database and release any stream resources when the test finishes
        migrationTestHelper.closeWhenFinished(database);
        return database;
    }
}