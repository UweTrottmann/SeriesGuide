package com.battlelancer.seriesguide.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentProviderOperation;
import android.database.Cursor;
import android.support.test.rule.provider.ProviderTestRule;
import android.support.test.runner.AndroidJUnit4;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.util.DBUtils;
import java.util.ArrayList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ProviderTest {

    @Rule
    public ProviderTestRule providerRule = new ProviderTestRule.Builder(SeriesGuideProvider.class,
            SgApp.CONTENT_AUTHORITY).build();

    @Test
    public void seasonDefaultValues() throws Exception {
        ContentProviderOperation op = DBUtils
                .buildSeasonOp(12, 1234, 42, true);

        // note how this does not cause a foreign key constraint failure
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        batch.add(op);
        providerRule.getResolver().applyBatch(SgApp.CONTENT_AUTHORITY, batch);

        Cursor query = providerRule.getResolver().query(Seasons.CONTENT_URI, null,
                        null, null, null);
        assertNotNull(query);
        assertTrue(query.moveToFirst());

        assertEquals(1234, query.getInt(query.getColumnIndexOrThrow(Seasons._ID)));
        assertEquals(12, query.getInt(query.getColumnIndexOrThrow(Shows.REF_SHOW_ID)));
        assertEquals(42, query.getInt(query.getColumnIndexOrThrow(Seasons.COMBINED)));
        // getInt returns 0 if NULL, so check explicitly
        assertFalse(query.isNull(query.getColumnIndexOrThrow(Seasons.WATCHCOUNT)));
        assertFalse(query.isNull(query.getColumnIndexOrThrow(Seasons.UNAIREDCOUNT)));
        assertFalse(query.isNull(query.getColumnIndexOrThrow(Seasons.NOAIRDATECOUNT)));
        assertFalse(query.isNull(query.getColumnIndexOrThrow(Seasons.TOTALCOUNT)));
        assertEquals(0, query.getInt(query.getColumnIndexOrThrow(Seasons.WATCHCOUNT)));
        assertEquals(0, query.getInt(query.getColumnIndexOrThrow(Seasons.UNAIREDCOUNT)));
        assertEquals(0, query.getInt(query.getColumnIndexOrThrow(Seasons.NOAIRDATECOUNT)));
        assertEquals(0, query.getInt(query.getColumnIndexOrThrow(Seasons.TOTALCOUNT)));

        assertEquals(1, query.getCount());
    }
}
