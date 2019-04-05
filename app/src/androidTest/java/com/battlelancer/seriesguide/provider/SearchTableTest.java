package com.battlelancer.seriesguide.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.database.Cursor;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SearchTableTest {

    private SgRoomDatabase db;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, SgRoomDatabase.class)
                .addCallback(SgRoomDatabase.CALLBACK)
                .build();
    }

    @After
    public void closeDb() throws IOException {
        db.close();
    }

    @Test
    public void searchTableExists() {
        String query = "SELECT name FROM sqlite_master "
                + "WHERE type='table' AND name=? "
                + "LIMIT 1";
        String[] args = {SeriesGuideDatabase.Tables.EPISODES_SEARCH};
        Cursor result = db.query(query, args);
        assertNotNull(result);
        assertEquals(1, result.getCount());
    }

}
