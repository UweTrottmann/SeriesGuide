package com.battlelancer.seriesguide.provider;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import com.battlelancer.seriesguide.model.Show;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;

/**
 * Helper class for working with the SQLiteDatabase using SQLite APIs (before Room).
 */
public class SqliteDatabaseTestHelper {

    public static void insertShow(Show show, SqliteTestDbOpenHelper helper) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Shows._ID, show.tvdbId);
        values.put(Shows.TITLE, show.title);
        values.put(Shows.TITLE_NOARTICLE, show.titleNoArticle);
        values.put(Shows.RUNTIME, show.runtime);

        db.insertWithOnConflict(Tables.SHOWS, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);

        db.close();
    }

}
