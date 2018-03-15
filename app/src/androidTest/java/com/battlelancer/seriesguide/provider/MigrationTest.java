package com.battlelancer.seriesguide.provider;

import static com.battlelancer.seriesguide.provider.SgRoomDatabase.MIGRATION_42_43;
import static org.junit.Assert.assertEquals;

import android.arch.persistence.db.framework.FrameworkSQLiteOpenHelperFactory;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.testing.MigrationTestHelper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import com.battlelancer.seriesguide.model.Show;
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

    static {
        SHOW.tvdbId = 21;
        SHOW.title = "The No Answers Show";
        SHOW.titleNoArticle = "No Answers Show";
        SHOW.runtime = "45";
        // TODO add more values
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
    public void setUp() throws Exception {
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
        // Create the database with the initial version 42 schema and insert a show
        SqliteDatabaseTestHelper.insertShow(SHOW, sqliteTestDbHelper);

        // Re-open the database with version 43 and
        // provide MIGRATION_42_43 as the migration process.
        migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, 43,
                false /* adding FTS table ourselves */, MIGRATION_42_43);

        // MigrationTestHelper automatically verifies the schema changes, but not the data validity
        // Validate that the data was migrated properly.
        Show dbShow = getMigratedRoomDatabase().showHelper().getShow();
        assertEquals(SHOW.tvdbId, dbShow.tvdbId);
        assertEquals(SHOW.title, dbShow.title);
        assertEquals(SHOW.titleNoArticle, dbShow.titleNoArticle);
        assertEquals(SHOW.runtime, dbShow.runtime);
    }

    private SgRoomDatabase getMigratedRoomDatabase() {
        SgRoomDatabase database = Room.databaseBuilder(InstrumentationRegistry.getTargetContext(),
                SgRoomDatabase.class, TEST_DB_NAME)
                .addMigrations(MIGRATION_42_43)
                .build();
        // close the database and release any stream resources when the test finishes
        migrationTestHelper.closeWhenFinished(database);
        return database;
    }
}