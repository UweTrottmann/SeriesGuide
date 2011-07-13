
package com.battlelancer.seriesguide.provider;

import com.battlelancer.seriesguide.FileUtil;
import com.battlelancer.seriesguide.SeriesDatabase;
import com.battlelancer.seriesguide.SeriesGuideData;
import com.battlelancer.seriesguide.provider.SeriesContract.EpisodeSearchColumns;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.EpisodesColumns;
import com.battlelancer.seriesguide.provider.SeriesContract.SeasonsColumns;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesContract.ShowsColumns;
import com.battlelancer.seriesguide.util.UIUtils;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.provider.BaseColumns;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class SeriesGuideDatabase extends SQLiteOpenHelper {

    private static final String TAG = "SeriesGuideDatabase";

    public static final String DATABASE_NAME = "seriesdatabase";

    public static final int DATABASE_VERSION = 17;

    public interface Tables {
        String SHOWS = "series";

        String SEASONS = "seasons";

        String EPISODES = "episodes";

        String EPISODES_JOIN_SHOWS = "episodes "
                + "LEFT OUTER JOIN series ON episodes.series_id=series._id";

        String EPISODES_SEARCH = "searchtable";
    }

    interface References {
        String SHOW_ID = "REFERENCES " + Tables.SHOWS + "(" + BaseColumns._ID + ")";

        String SEASON_ID = "REFERENCES " + Tables.SEASONS + "(" + BaseColumns._ID + ")";
    }

    private static final String CREATE_SHOWS_TABLE = "CREATE TABLE " + Tables.SHOWS + " ("
            + BaseColumns._ID + " INTEGER PRIMARY KEY," + ShowsColumns.TITLE + " TEXT NOT NULL,"
            + ShowsColumns.OVERVIEW + " TEXT DEFAULT ''," + ShowsColumns.ACTORS
            + " TEXT DEFAULT ''," + ShowsColumns.AIRSDAYOFWEEK + " TEXT DEFAULT '',"
            + ShowsColumns.AIRSTIME + " INTEGER DEFAULT ''," + ShowsColumns.FIRSTAIRED
            + " TEXT DEFAULT ''," + ShowsColumns.GENRES + " TEXT DEFAULT '',"
            + ShowsColumns.NETWORK + " TEXT DEFAULT ''," + ShowsColumns.RATING
            + " TEXT DEFAULT ''," + ShowsColumns.RUNTIME + " TEXT DEFAULT '',"
            + ShowsColumns.STATUS + " TEXT DEFAULT ''," + ShowsColumns.CONTENTRATING
            + " TEXT DEFAULT ''," + ShowsColumns.NEXTEPISODE + " TEXT DEFAULT '',"
            + ShowsColumns.POSTER + " TEXT DEFAULT ''," + ShowsColumns.NEXTAIRDATE
            + " TEXT DEFAULT '0'," + ShowsColumns.NEXTTEXT + " TEXT DEFAULT '',"
            + ShowsColumns.IMDBID + " TEXT DEFAULT ''," + ShowsColumns.FAVORITE
            + " INTEGER DEFAULT 0" + ");";

    private static final String CREATE_SEASONS_TABLE = "CREATE TABLE " + Tables.SEASONS + " ("
            + BaseColumns._ID + " INTEGER PRIMARY KEY," + SeasonsColumns.COMBINED + " INTEGER,"
            + ShowsColumns.REF_SHOW_ID + " TEXT " + References.SHOW_ID + ","
            + SeasonsColumns.WATCHCOUNT + " INTEGER DEFAULT 0," + SeasonsColumns.UNAIREDCOUNT
            + " INTEGER DEFAULT 0," + SeasonsColumns.NOAIRDATECOUNT + " INTEGER DEFAULT 0,"
            + SeasonsColumns.POSTER + " TEXT DEFAULT '');";

    private static final String CREATE_EPISODES_TABLE = "CREATE TABLE " + Tables.EPISODES + " ("
            + BaseColumns._ID + " INTEGER PRIMARY KEY," + EpisodesColumns.TITLE + " TEXT NOT NULL,"
            + EpisodesColumns.OVERVIEW + " TEXT," + EpisodesColumns.NUMBER + " INTEGER default 0,"
            + EpisodesColumns.SEASON + " INTEGER default 0," + EpisodesColumns.DVDNUMBER + " REAL,"
            + EpisodesColumns.FIRSTAIRED + " TEXT," + SeasonsColumns.REF_SEASON_ID + " TEXT "
            + References.SEASON_ID + "," + ShowsColumns.REF_SHOW_ID + " TEXT " + References.SHOW_ID
            + "," + EpisodesColumns.WATCHED + " INTEGER DEFAULT 0," + EpisodesColumns.DIRECTORS
            + " TEXT DEFAULT ''," + EpisodesColumns.GUESTSTARS + " TEXT DEFAULT '',"
            + EpisodesColumns.WRITERS + " TEXT DEFAULT ''," + EpisodesColumns.IMAGE
            + " TEXT DEFAULT ''," + EpisodesColumns.RATING + " TEXT DEFAULT '');";

    private static final String CREATE_SEARCH_TABLE = "CREATE VIRTUAL TABLE "
            + Tables.EPISODES_SEARCH + " USING FTS3(" + EpisodeSearchColumns.TITLE + " TEXT,"
            + EpisodeSearchColumns.OVERVIEW + " TEXT" + ");";

    public SeriesGuideDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_SHOWS_TABLE);

        db.execSQL(CREATE_SEASONS_TABLE);

        db.execSQL(CREATE_EPISODES_TABLE);

        db.execSQL(CREATE_SEARCH_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade() from " + oldVersion + " to " + newVersion);

        // make a backup of the database file
        if (UIUtils.isExtStorageAvailable()) {
            File dbFile = new File(db.getPath());
            File exportDir = new File(Environment.getExternalStorageDirectory(),
                    "seriesguidebackup");
            exportDir.mkdirs();
            File file = new File(exportDir, dbFile.getName() + "_b4upgr");

            try {
                file.createNewFile();
                FileUtil.copyFile(dbFile, file);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }

        // run necessary upgrades
        int version = oldVersion;
        switch (version) {
            case 12:
                upgradeToThirteen(db);
                version = 13;
            case 13:
                upgradeToFourteen(db);
                version = 14;
            case 14:
                upgradeToFifteen(db);
                version = 15;
            case 15:
                upgradeToSixteen(db);
                version = 16;
            case 16:
                upgradeToSeventeen(db);
                version = 17;
        }

        // drop all tables if version is not right
        Log.d(TAG, "after upgrade logic, at version " + version);
        if (version != DATABASE_VERSION) {
            Log.w(TAG, "Database has incompatible version, starting from scratch");
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SHOWS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SEASONS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.EPISODES);

            db.execSQL("DROP TABLE IF EXISTS " + Tables.EPISODES_SEARCH);

            onCreate(db);
        }
    }

    /**
     * In version 17 the series boolean column favorite was added.
     * 
     * @param db
     */
    private void upgradeToSeventeen(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.FAVORITE
                + " INTEGER DEFAULT 0;");
    }

    /**
     * In version 13 the next episode field is just storing the id of the next
     * episode. Clears out previous values to "".
     * 
     * @param db
     */
    private void upgradeToThirteen(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(SeriesDatabase.SERIES_NEXTEPISODE, "");
        db.update(SeriesDatabase.SERIES_TABLE, values, null, null);
        db.execSQL("ALTER TABLE " + SeriesDatabase.SERIES_TABLE + " ADD COLUMN "
                + SeriesDatabase.SERIES_NEXTAIRDATE + " TEXT DEFAULT '0';");
        db.execSQL("ALTER TABLE " + SeriesDatabase.SERIES_TABLE + " ADD COLUMN "
                + SeriesDatabase.SERIES_NEXTTEXT + " TEXT DEFAULT '';");
    }

    /**
     * In version 14 the imdb id for shows gets now parsed, too. A season now
     * knows the number of all its episodes.
     * 
     * @param db
     */
    private void upgradeToFourteen(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + SeriesDatabase.SERIES_TABLE + " ADD COLUMN "
                + SeriesDatabase.SERIES_IMDBID + " text default '';");
        db.execSQL("ALTER TABLE " + SeriesDatabase.SEASONS_TABLE + " ADD COLUMN "
                + SeriesDatabase.SEASON_NOAIRDATECOUNT + " integer default 0;");
    }

    /**
     * In version 15 the DVD episode number is stored in the episodes table.
     * 
     * @param db
     */
    private void upgradeToFifteen(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + SeriesDatabase.EPISODE_TABLE + " ADD COLUMN "
                + SeriesDatabase.EPISODE_DVDNUMBER + " real;");
    }

    /**
     * In version 16 the shows airtime was converted to integer values.
     * 
     * @param db
     */
    private void upgradeToSixteen(SQLiteDatabase db) {
        // convert airtime strings to ms-integers
        Cursor shows = db.query(SeriesDatabase.SERIES_TABLE, new String[] {
                SeriesDatabase.SERIES_ID, SeriesDatabase.SERIES_AIRSTIME
        }, null, null, null, null, null);

        String id;
        long airtime;
        String airtimeText;
        ContentValues values = new ContentValues();

        db.beginTransaction();
        try {
            while (shows.moveToNext()) {
                id = shows.getString(shows.getColumnIndexOrThrow(SeriesDatabase.SERIES_ID));
                airtimeText = shows.getString(shows
                        .getColumnIndexOrThrow(SeriesDatabase.SERIES_AIRSTIME));
                airtime = SeriesGuideData.parseTimeToMilliseconds(airtimeText);
                values.put(SeriesDatabase.SERIES_AIRSTIME, airtime);
                db.update(SeriesDatabase.SERIES_TABLE, values, SeriesDatabase.SERIES_ID + "=?",
                        new String[] {
                            id
                        });
                values.clear();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        shows.close();

        // rename old table, create new one with different schema, copy data,
        // delete old one
        db.execSQL("ALTER TABLE " + SeriesDatabase.SERIES_TABLE + " RENAME TO series_old;");
        db.execSQL(CREATE_SHOWS_TABLE);
        db.execSQL("INSERT INTO " + SeriesDatabase.SERIES_TABLE + "(" + SeriesDatabase.SERIES_ID
                + "," + SeriesDatabase.SERIES_NAME + "," + SeriesDatabase.SERIES_OVERVIEW + ","
                + SeriesDatabase.SERIES_ACTORS + "," + SeriesDatabase.SERIES_AIRSDAYOFWEEK + ","
                + SeriesDatabase.SERIES_AIRSTIME + "," + SeriesDatabase.SERIES_FIRSTAIRED + ","
                + SeriesDatabase.SERIES_GENRES + "," + SeriesDatabase.SERIES_NETWORK + ","
                + SeriesDatabase.SERIES_RATING + "," + SeriesDatabase.SERIES_RUNTIME + ","
                + SeriesDatabase.SERIES_STATUS + "," + SeriesDatabase.SERIES_CONTENTRATING + ","
                + SeriesDatabase.SERIES_NEXTEPISODE + "," + SeriesDatabase.SERIES_POSTER + ","
                + SeriesDatabase.SERIES_NEXTAIRDATE + "," + SeriesDatabase.SERIES_NEXTTEXT + ","
                + SeriesDatabase.SERIES_IMDBID + ")" + " SELECT " + SeriesDatabase.SERIES_ID + ","
                + SeriesDatabase.SERIES_NAME + "," + SeriesDatabase.SERIES_OVERVIEW + ","
                + SeriesDatabase.SERIES_ACTORS + "," + SeriesDatabase.SERIES_AIRSDAYOFWEEK + ","
                + SeriesDatabase.SERIES_AIRSTIME + "," + SeriesDatabase.SERIES_FIRSTAIRED + ","
                + SeriesDatabase.SERIES_GENRES + "," + SeriesDatabase.SERIES_NETWORK + ","
                + SeriesDatabase.SERIES_RATING + "," + SeriesDatabase.SERIES_RUNTIME + ","
                + SeriesDatabase.SERIES_STATUS + "," + SeriesDatabase.SERIES_CONTENTRATING + ","
                + SeriesDatabase.SERIES_NEXTEPISODE + "," + SeriesDatabase.SERIES_POSTER + ","
                + SeriesDatabase.SERIES_NEXTAIRDATE + "," + SeriesDatabase.SERIES_NEXTTEXT + ","
                + SeriesDatabase.SERIES_IMDBID + " FROM series_old;");
        db.execSQL("DROP TABLE series_old;");
    }

    public static void onRenewFTSTable(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            db.execSQL("drop table if exists " + Tables.EPISODES_SEARCH);
            db.execSQL(CREATE_SEARCH_TABLE);
            db.execSQL("INSERT INTO " + Tables.EPISODES_SEARCH + "(docid," + Episodes.TITLE + ","
                    + Episodes.OVERVIEW + ")" + " select " + Episodes._ID + "," + Episodes.TITLE
                    + "," + Episodes.OVERVIEW + " from " + Tables.EPISODES + ";");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public static Cursor search(String query, SQLiteDatabase db) {
        /*
         * select
         * _id,episodetitle,episodedescription,number,season,watched,seriestitle
         * from ((select _id as sid,seriestitle from series) join (select
         * _id,episodedescription,series_id,episodetitle,number,season,watched
         * from (select rowid,snippet(searchtable) as episodedescription from
         * searchtable where searchtable match 'pilot') join (select
         * _id,series_id,episodetitle,number,season,watched from episodes) on
         * _id=rowid) on sid=series_id)
         */
        return db.rawQuery("select _id," + Episodes.TITLE + "," + Episodes.OVERVIEW + ","
                + Episodes.NUMBER + "," + Episodes.SEASON + "," + Episodes.WATCHED + ","
                + Shows.TITLE + " from ((select _id as sid," + Shows.TITLE + " from "
                + Tables.SHOWS + ")" + " join " + "(select _id," + Episodes.TITLE + ","
                + Episodes.OVERVIEW + "," + Episodes.NUMBER + "," + Episodes.SEASON + ","
                + Episodes.WATCHED + "," + Shows.REF_SHOW_ID + " from " + "(select docid,snippet("
                + Tables.EPISODES_SEARCH + ",'<b>','</b>','...') as " + Episodes.OVERVIEW
                + " from " + Tables.EPISODES_SEARCH + " where " + Tables.EPISODES_SEARCH
                + " match " + "?)" + " join " + "(select _id," + Episodes.TITLE + ","
                + Episodes.NUMBER + "," + Episodes.SEASON + "," + Episodes.WATCHED + ","
                + Shows.REF_SHOW_ID + " from episodes)" + "on _id=docid)" + "on sid="
                + Shows.REF_SHOW_ID + ")", new String[] {
            "\"" + query + "*\""
        });
    }

    public static Cursor getSuggestions(String query, SQLiteDatabase db) {
        return db.rawQuery("select _id," + Episodes.TITLE + " as "
                + SearchManager.SUGGEST_COLUMN_TEXT_1 + "," + Shows.TITLE + " as "
                + SearchManager.SUGGEST_COLUMN_TEXT_2 + "," + "_id as "
                + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID + " from ((select _id as sid,"
                + Shows.TITLE + " from " + Tables.SHOWS + ")" + " join " + "(select _id,"
                + Episodes.TITLE + "," + Shows.REF_SHOW_ID + " from " + "(select docid" + " from "
                + Tables.EPISODES_SEARCH + " where " + Tables.EPISODES_SEARCH + " match " + "?)"
                + " join " + "(select _id," + Episodes.TITLE + "," + Shows.REF_SHOW_ID
                + " from episodes)" + "on _id=docid)" + "on sid=" + Shows.REF_SHOW_ID + ")",
                new String[] {
                    "\"" + query + "*\""
                });
    }
}
