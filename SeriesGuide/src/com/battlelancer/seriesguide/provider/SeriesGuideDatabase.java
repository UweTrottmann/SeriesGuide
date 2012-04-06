
package com.battlelancer.seriesguide.provider;

import com.battlelancer.seriesguide.provider.SeriesContract.EpisodeSearch;
import com.battlelancer.seriesguide.provider.SeriesContract.EpisodeSearchColumns;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.EpisodesColumns;
import com.battlelancer.seriesguide.provider.SeriesContract.SeasonsColumns;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesContract.ShowsColumns;
import com.battlelancer.seriesguide.util.Utils;

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

    public static final int DBVER_FAVORITES = 17;

    public static final int DBVER_NEXTAIRDATETEXT = 18;

    public static final int DBVER_SETOTALCOUNT = 19;

    public static final int DBVER_SYNC = 20;

    public static final int DBVER_AIRTIMECOLUMN = 21;

    public static final int DBVER_PERSHOWUPDATEDATE = 22;

    public static final int DBVER_HIDDENSHOWS = 23;

    public static final int DBVER_AIRTIMEREFORM = 24;

    public static final int DBVER_NEXTAIRDATEMS = 25;

    public static final int DBVER_COLLECTED = 26;

    public static final int DATABASE_VERSION = DBVER_COLLECTED;

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
            + ShowsColumns.POSTER + " TEXT DEFAULT ''," + ShowsColumns.NEXTAIRDATEMS + " INTEGER,"
            + ShowsColumns.NEXTTEXT + " TEXT DEFAULT ''," + ShowsColumns.IMDBID
            + " TEXT DEFAULT ''," + ShowsColumns.FAVORITE + " INTEGER DEFAULT 0,"
            + ShowsColumns.NEXTAIRDATETEXT + " TEXT DEFAULT ''" + "," + ShowsColumns.SYNCENABLED
            + " INTEGER DEFAULT 1" + "," + ShowsColumns.AIRTIME + " TEXT DEFAULT '',"
            + ShowsColumns.HIDDEN + " INTEGER DEFAULT 0," + ShowsColumns.LASTUPDATED
            + " INTEGER DEFAULT 0" + ");";

    private static final String CREATE_SEASONS_TABLE = "CREATE TABLE " + Tables.SEASONS + " ("
            + BaseColumns._ID + " INTEGER PRIMARY KEY," + SeasonsColumns.COMBINED + " INTEGER,"
            + ShowsColumns.REF_SHOW_ID + " TEXT " + References.SHOW_ID + ","
            + SeasonsColumns.WATCHCOUNT + " INTEGER DEFAULT 0," + SeasonsColumns.UNAIREDCOUNT
            + " INTEGER DEFAULT 0," + SeasonsColumns.NOAIRDATECOUNT + " INTEGER DEFAULT 0,"
            + SeasonsColumns.POSTER + " TEXT DEFAULT ''," + SeasonsColumns.TOTALCOUNT
            + " INTEGER DEFAULT 0);";

    private static final String CREATE_EPISODES_TABLE = "CREATE TABLE " + Tables.EPISODES + " ("
            + BaseColumns._ID + " INTEGER PRIMARY KEY," + EpisodesColumns.TITLE + " TEXT NOT NULL,"
            + EpisodesColumns.OVERVIEW + " TEXT," + EpisodesColumns.NUMBER + " INTEGER default 0,"
            + EpisodesColumns.SEASON + " INTEGER DEFAULT 0," + EpisodesColumns.DVDNUMBER + " REAL,"
            + EpisodesColumns.FIRSTAIRED + " TEXT," + SeasonsColumns.REF_SEASON_ID + " TEXT "
            + References.SEASON_ID + "," + ShowsColumns.REF_SHOW_ID + " TEXT " + References.SHOW_ID
            + "," + EpisodesColumns.WATCHED + " INTEGER DEFAULT 0," + EpisodesColumns.DIRECTORS
            + " TEXT DEFAULT ''," + EpisodesColumns.GUESTSTARS + " TEXT DEFAULT '',"
            + EpisodesColumns.WRITERS + " TEXT DEFAULT ''," + EpisodesColumns.IMAGE
            + " TEXT DEFAULT ''," + EpisodesColumns.FIRSTAIREDMS + " INTEGER DEFAULT -1,"
            + EpisodesColumns.COLLECTED + " INTEGER DEFAULT 0," + EpisodesColumns.RATING
            + " TEXT DEFAULT '');";

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
        if (Utils.isExtStorageAvailable()) {
            File dbFile = new File(db.getPath());
            File exportDir = new File(Environment.getExternalStorageDirectory(),
                    "seriesguidebackup");
            exportDir.mkdirs();
            File file = new File(exportDir, dbFile.getName() + "_b4upgr");

            try {
                file.createNewFile();
                Utils.copyFile(dbFile, file);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }

        // run necessary upgrades
        int version = oldVersion;
        switch (version) {
            case 16:
                upgradeToSeventeen(db);
                version = 17;
            case 17:
                upgradeToEighteen(db);
                version = 18;
            case 18:
                upgradeToNineteen(db);
                version = 19;
            case 19:
                upgradeToTwenty(db);
                version = 20;
            case 20:
                upgradeToTwentyOne(db);
                version = 21;
            case 21:
                upgradeToTwentyTwo(db);
                version = 22;
            case 22:
                upgradeToTwentyThree(db);
                version = 23;
            case 23:
                upgradeToTwentyFour(db);
                version = 24;
            case 24:
                upgradeToTwentyFive(db);
                version = 25;
            case 25:
                upgradeToTwentySix(db);
                version = 26;
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
     * Add a {@link Episodes} column for storing whether an episode was
     * collected in digital or physical form.
     * 
     * @param db
     */
    private void upgradeToTwentySix(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.EPISODES + " ADD COLUMN " + EpisodesColumns.COLLECTED
                + " INTEGER DEFAULT 0;");
    }

    /**
     * Add a {@link Shows} column for storing the next air date in ms as integer
     * data type rather than as text.
     * 
     * @param db
     */
    private void upgradeToTwentyFive(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.NEXTAIRDATEMS
                + " INTEGER DEFAULT 0;");
    }

    /**
     * Adds a column to the {@link Tables.EPISODES} table to store the airdate
     * and possibly time in milliseconds.
     * 
     * @param db
     */
    private void upgradeToTwentyFour(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.EPISODES + " ADD COLUMN " + EpisodesColumns.FIRSTAIREDMS
                + " INTEGER DEFAULT -1;");

        // populate the new column from existing data
        final Cursor shows = db.query(Tables.SHOWS, new String[] {
                Shows._ID, Shows.AIRSTIME
        }, null, null, null, null, null);

        while (shows.moveToNext()) {
            final String showId = shows.getString(0);
            final long airtime = shows.getLong(1);

            final Cursor episodes = db.query(Tables.EPISODES, new String[] {
                    Episodes._ID, Episodes.FIRSTAIRED
            }, Shows.REF_SHOW_ID + "=?", new String[] {
                showId
            }, null, null, null);

            db.beginTransaction();
            try {
                ContentValues values = new ContentValues();
                while (episodes.moveToNext()) {
                    String firstAired = episodes.getString(1);
                    long episodeAirtime = Utils.buildEpisodeAirtime(firstAired, airtime);

                    values.put(Episodes.FIRSTAIREDMS, episodeAirtime);
                    db.update(Tables.EPISODES, values, Episodes._ID + "=?", new String[] {
                        episodes.getString(0)
                    });
                    values.clear();
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            episodes.close();
        }

        shows.close();
    }

    /**
     * Adds a column to the {@link Tables.SHOWS} table similar to the favorite
     * boolean, but to allow hiding shows.
     * 
     * @param db
     */
    private void upgradeToTwentyThree(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.HIDDEN
                + " INTEGER DEFAULT 0;");
    }

    /**
     * Add a column to store the last time a show has been updated to allow for
     * more precise control over which shows should get updated. This is in
     * conjunction with a 7 day limit when a show will get updated regardless if
     * it has been marked as updated or not.
     * 
     * @param db
     */
    private void upgradeToTwentyTwo(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.LASTUPDATED
                + " INTEGER DEFAULT 0;");
    }

    private void upgradeToTwentyOne(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.AIRTIME
                + " TEXT DEFAULT '';");
    }

    private void upgradeToTwenty(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.SYNCENABLED
                + " INTEGER DEFAULT 1;");
    }

    /**
     * In version 19 the season integer column totalcount was added.
     * 
     * @param db
     */
    private void upgradeToNineteen(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.SEASONS + " ADD COLUMN " + SeasonsColumns.TOTALCOUNT
                + " INTEGER DEFAULT 0;");
    }

    /**
     * In version 18 the series text column nextairdatetext was added.
     * 
     * @param db
     */
    private void upgradeToEighteen(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.NEXTAIRDATETEXT
                + " TEXT DEFAULT '';");

        // convert status text to 0/1 integer
        final Cursor shows = db.query(Tables.SHOWS, new String[] {
                Shows._ID, Shows.STATUS
        }, null, null, null, null, null);
        final ContentValues values = new ContentValues();
        String status;

        db.beginTransaction();
        try {
            while (shows.moveToNext()) {
                status = shows.getString(1);
                if (status.length() == 10) {
                    status = "1";
                } else if (status.length() == 5) {
                    status = "0";
                } else {
                    status = "";
                }
                values.put(Shows.STATUS, status);
                db.update(Tables.SHOWS, values, Shows._ID + "=?", new String[] {
                    shows.getString(0)
                });
                values.clear();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        shows.close();
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

    public static Cursor search(String selection, String[] selectionArgs, SQLiteDatabase db) {
        // select
        // _id,episodetitle,episodedescription,number,season,watched,seriestitle
        // from (
        // (select _id as sid,seriestitle from series)
        // join
        // (select
        // _id,episodedescription,series_id,episodetitle,number,season,watched
        // from(select rowid,snippet(searchtable) as episodedescription from
        // searchtable where searchtable match 'QUERY')
        // join (select
        // _id,series_id,episodetitle,number,season,watched from episodes)
        // on _id=rowid)
        // on sid=series_id)

        StringBuilder query = new StringBuilder();
        // select final result columns
        query.append("SELECT ");
        query.append(Episodes._ID).append(",");
        query.append(Episodes.TITLE).append(",");
        query.append(Episodes.OVERVIEW).append(",");
        query.append(Episodes.NUMBER).append(",");
        query.append(Episodes.SEASON).append(",");
        query.append(Episodes.WATCHED).append(",");
        query.append(Shows.TITLE);

        query.append(" FROM ");
        query.append("(");

        // join all shows...
        query.append("(");
        query.append("SELECT ").append(BaseColumns._ID).append(" as sid,").append(Shows.TITLE);
        query.append(" FROM ").append(Tables.SHOWS);
        query.append(")");

        query.append(" JOIN ");

        // ...with matching episodes
        query.append("(");
        query.append("SELECT ");
        query.append(Episodes._ID).append(",");
        query.append(Episodes.TITLE).append(",");
        query.append(Episodes.OVERVIEW).append(",");
        query.append(Episodes.NUMBER).append(",");
        query.append(Episodes.SEASON).append(",");
        query.append(Episodes.WATCHED).append(",");
        query.append(Shows.REF_SHOW_ID);
        query.append(" FROM ");
        // join searchtable results...
        query.append("(");
        query.append("SELECT ");
        query.append(EpisodeSearch._DOCID).append(",");
        query.append("snippet(" + Tables.EPISODES_SEARCH + ",'<b>','</b>','...')").append(" AS ")
                .append(Episodes.OVERVIEW);
        query.append(" FROM ").append(Tables.EPISODES_SEARCH);
        query.append(" WHERE ").append(Tables.EPISODES_SEARCH).append(" MATCH ?");
        query.append(")");
        query.append(" JOIN ");
        // ...with episodes table
        query.append("(");
        query.append("SELECT ");
        query.append(Episodes._ID).append(",");
        query.append(Episodes.TITLE).append(",");
        query.append(Episodes.NUMBER).append(",");
        query.append(Episodes.SEASON).append(",");
        query.append(Episodes.WATCHED).append(",");
        query.append(Shows.REF_SHOW_ID);
        query.append(" FROM ").append(Tables.EPISODES);
        query.append(")");
        query.append(" ON ").append(Episodes._ID).append("=").append(EpisodeSearch._DOCID);

        query.append(")");
        query.append(" ON ").append("sid=").append(Shows.REF_SHOW_ID);
        query.append(")");

        // append given selection
        if (selection != null) {
            query.append(" WHERE ");
            query.append("(").append(selection).append(")");
        }

        // ordering
        query.append(" ORDER BY ");
        query.append(Shows.TITLE).append(" ASC,");
        query.append(Episodes.SEASON).append(" ASC,");
        query.append(Episodes.NUMBER).append(" ASC");

        // search for anything starting with the given search term
        selectionArgs[0] = "\"" + selectionArgs[0] + "*\"";

        return db.rawQuery(query.toString(), selectionArgs);
    }

    public static Cursor getSuggestions(String searchterm, SQLiteDatabase db) {
        StringBuilder query = new StringBuilder("select _id," + Episodes.TITLE + " as "
                + SearchManager.SUGGEST_COLUMN_TEXT_1 + "," + Shows.TITLE + " as "
                + SearchManager.SUGGEST_COLUMN_TEXT_2 + "," + "_id as "
                + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID + " from ((select _id as sid,"
                + Shows.TITLE + " from " + Tables.SHOWS + ")" + " join " + "(select _id,"
                + Episodes.TITLE + "," + Shows.REF_SHOW_ID + " from " + "(select docid" + " from "
                + Tables.EPISODES_SEARCH + " where " + Tables.EPISODES_SEARCH + " match " + "?)"
                + " join " + "(select _id," + Episodes.TITLE + "," + Shows.REF_SHOW_ID
                + " from episodes)" + "on _id=docid)" + "on sid=" + Shows.REF_SHOW_ID + ")");

        // search for anything starting with the given search term
        return db.rawQuery(query.toString(), new String[] {
            "\"" + searchterm + "*\""
        });
    }

}
