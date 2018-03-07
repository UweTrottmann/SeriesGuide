package com.battlelancer.seriesguide.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.dataliberation.model.Season;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.thetvdbapi.TvdbEpisodeTools;
import com.battlelancer.seriesguide.util.DBUtils;
import com.uwetrottmann.thetvdb.entities.Episode;
import java.util.ArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ProviderTest {

    private static final Show SHOW;
    private static final Season SEASON;
    private static final Episode EPISODE;
    private static final com.battlelancer.seriesguide.dataliberation.model.Episode EPISODE_I;

    static {
        SHOW = new Show();
        SHOW.tvdb_id = 12;

        SEASON = new Season();
        SEASON.tvdbId = 1234;
        SEASON.season = 42;

        EPISODE = new Episode();
        EPISODE.id = 123456;

        EPISODE_I = new com.battlelancer.seriesguide.dataliberation.model.Episode();
        EPISODE_I.tvdbId = EPISODE.id;
    }

    private ContentResolver resolver;

    @Before
    public void switchToInMemoryDb() {
        // ProviderTestRule does not work with Room
        // so instead blatantly replace the instance with one that uses an in-memory database
        // and use the real ContentResolver
        Context context = InstrumentationRegistry.getTargetContext();
        SgRoomDatabase.switchToInMemory(context);
        resolver = context.getContentResolver();
    }

    @After
    public void closeDb() {
        SgRoomDatabase.getInstance(InstrumentationRegistry.getTargetContext()).close();
    }

    @Test
    public void showDefaultValues() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();

        ContentValues values = SHOW.toContentValues(context, true);
        ContentProviderOperation op = ContentProviderOperation.newInsert(Shows.CONTENT_URI)
                .withValues(values).build();

        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        batch.add(op);
        resolver.applyBatch(SgApp.CONTENT_AUTHORITY, batch);

        Cursor query = resolver.query(Shows.CONTENT_URI, null,
                null, null, null);
        assertNotNull(query);
        assertTrue(query.moveToFirst());

        assertEquals(SHOW.tvdb_id, query.getInt(query.getColumnIndexOrThrow(Shows._ID)));
        assertNotNullValue(query, Shows.TITLE);
        assertNotNullValue(query, Shows.TITLE);
        assertNotNullValue(query, Shows.OVERVIEW);
        assertNotNullValue(query, Shows.GENRES);
        assertNotNullValue(query, Shows.NETWORK);
        assertNotNullValue(query, Shows.RUNTIME);
        assertNotNullValue(query, Shows.STATUS);
        assertNotNullValue(query, Shows.CONTENTRATING);
        assertNotNullValue(query, Shows.NEXTEPISODE);
        assertNotNullValue(query, Shows.POSTER);
        assertNotNullValue(query, Shows.NEXTTEXT);
        assertNotNullValue(query, Shows.IMDBID);
        // getInt returns 0 if NULL, so check explicitly
        assertDefaultValue(query, Shows.TRAKT_ID, 0);
        assertDefaultValue(query, Shows.FAVORITE, 0);
        assertDefaultValue(query, Shows.HEXAGON_MERGE_COMPLETE, 1);
        assertDefaultValue(query, Shows.HIDDEN, 0);
        assertDefaultValue(query, Shows.LASTUPDATED, 0);
        assertDefaultValue(query, Shows.LASTEDIT, 0);
        assertDefaultValue(query, Shows.LASTWATCHEDID, 0);
        assertDefaultValue(query, Shows.LASTWATCHED_MS, 0);
        assertNotNullValue(query, Shows.LANGUAGE);
        assertDefaultValue(query, Shows.UNWATCHED_COUNT, DBUtils.UNKNOWN_UNWATCHED_COUNT);
        assertDefaultValue(query, Shows.NOTIFY, 1);

        assertEquals(1, query.getCount());

        query.close();
    }

    @Test
    public void seasonDefaultValues() throws Exception {
        ContentProviderOperation op = DBUtils
                .buildSeasonOp(SHOW.tvdb_id, SEASON.tvdbId, SEASON.season, true);
        insertAndAssertSeason(op);
    }

    @Test
    public void seasonDefaultValuesImport() throws Exception {
        ContentValues values = SEASON.toContentValues(SHOW.tvdb_id);

        ContentProviderOperation op = ContentProviderOperation.newInsert(Seasons.CONTENT_URI)
                .withValues(values).build();

        insertAndAssertSeason(op);
    }

    private void insertAndAssertSeason(ContentProviderOperation seasonOp)
            throws RemoteException, OperationApplicationException {
        // with Room insert actually checks constraints, so add a matching show first
        Context context = InstrumentationRegistry.getTargetContext();

        ContentValues values = SHOW.toContentValues(context, true);
        ContentProviderOperation showOp = ContentProviderOperation.newInsert(Shows.CONTENT_URI)
                .withValues(values).build();

        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        batch.add(showOp);
        batch.add(seasonOp);
        resolver.applyBatch(SgApp.CONTENT_AUTHORITY, batch);

        Cursor query = resolver.query(Seasons.CONTENT_URI, null,
                null, null, null);
        assertNotNull(query);
        assertTrue(query.moveToFirst());

        assertEquals(SEASON.tvdbId, query.getInt(query.getColumnIndexOrThrow(Seasons._ID)));
        assertEquals(SHOW.tvdb_id, query.getInt(query.getColumnIndexOrThrow(Shows.REF_SHOW_ID)));
        assertEquals(SEASON.season, query.getInt(query.getColumnIndexOrThrow(Seasons.COMBINED)));
        // getInt returns 0 if NULL, so check explicitly
        assertDefaultValue(query, Seasons.WATCHCOUNT, 0);
        assertDefaultValue(query, Seasons.UNAIREDCOUNT, 0);
        assertDefaultValue(query, Seasons.NOAIRDATECOUNT, 0);
        assertDefaultValue(query, Seasons.TOTALCOUNT, 0);

        assertEquals(1, query.getCount());

        query.close();
    }

    @Test
    public void episodeDefaultValues() throws Exception {
        ContentValues values = new ContentValues();
        TvdbEpisodeTools.toContentValues(EPISODE, values,
                EPISODE.id, 1234, SHOW.tvdb_id, 0,
                Constants.EPISODE_UNKNOWN_RELEASE);

        insertAndAssertEpisode(values);
    }

    @Test
    public void episodeDefaultValuesImport() throws Exception {
        ContentValues values = EPISODE_I.toContentValues(SHOW.tvdb_id, 1234, 0);

        insertAndAssertEpisode(values);
    }

    private void insertAndAssertEpisode(ContentValues episodeValues) throws Exception {
        // with Room insert actually checks constraints, so add a matching show and season first
        Context context = InstrumentationRegistry.getTargetContext();

        ContentValues showValues = SHOW.toContentValues(context, true);
        ContentProviderOperation showOp = ContentProviderOperation.newInsert(Shows.CONTENT_URI)
                .withValues(showValues).build();

        ContentProviderOperation seasonOp = DBUtils
                .buildSeasonOp(SHOW.tvdb_id, SEASON.tvdbId, SEASON.season, true);

        ContentProviderOperation episodeOp = ContentProviderOperation
                .newInsert(Episodes.CONTENT_URI).withValues(episodeValues).build();

        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        batch.add(showOp);
        batch.add(seasonOp);
        batch.add(episodeOp);
        resolver.applyBatch(SgApp.CONTENT_AUTHORITY, batch);

        Cursor query = resolver.query(Episodes.CONTENT_URI, null,
                null, null, null);
        assertNotNull(query);
        assertTrue(query.moveToFirst());

        assertNotNullValue(query, Episodes.TITLE);
        assertDefaultValue(query, Episodes.NUMBER, 0);
        assertDefaultValue(query, Episodes.WATCHED, 0);
        assertNotNullValue(query, Episodes.DIRECTORS);
        assertNotNullValue(query, Episodes.GUESTSTARS);
        assertNotNullValue(query, Episodes.WRITERS);
        assertNotNullValue(query, Episodes.IMAGE);
        assertDefaultValue(query, Episodes.COLLECTED, 0);
        assertNotNullValue(query, Episodes.IMDBID);
        assertDefaultValue(query, Episodes.LAST_EDITED, 0);
        assertDefaultValue(query, Episodes.LAST_UPDATED, 0);

        query.close();
    }

    private void assertNotNullValue(Cursor query, String column) {
        assertFalse(query.isNull(query.getColumnIndexOrThrow(column)));
    }

    private void assertDefaultValue(Cursor query, String column, int defaultValue) {
        assertNotNullValue(query, column);
        assertEquals(defaultValue, query.getInt(query.getColumnIndexOrThrow(column)));
    }
}
