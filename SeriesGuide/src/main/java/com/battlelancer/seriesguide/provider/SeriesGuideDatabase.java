/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.provider;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.EpisodeSearch;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.EpisodeSearchColumns;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.EpisodesColumns;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemsColumns;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListsColumns;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.MoviesColumns;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SeasonsColumns;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ShowsColumns;
import com.battlelancer.seriesguide.util.TimeTools;
import timber.log.Timber;

public class SeriesGuideDatabase extends SQLiteOpenHelper {

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

    public static final int DBVER_IMDBIDSLASTEDIT = 27;

    public static final int DBVER_LISTS = 28;

    public static final int DBVER_GETGLUE_CHECKIN_FIX = 29;

    public static final int DBVER_ABSOLUTE_NUMBERS = 30;

    public static final int DBVER_31_LAST_WATCHED_ID = 31;

    public static final int DBVER_32_MOVIES = 32;

    public static final int DATABASE_VERSION = DBVER_32_MOVIES;

    /**
     * Qualifies column names by prefixing their {@link Tables} name.
     */
    public interface Qualified {

        String SHOWS_ID = Tables.SHOWS + "." + Shows._ID;
        String SHOWS_NEXTEPISODE = Tables.SHOWS + "." + Shows.NEXTEPISODE;
        String EPISODES_ID = Tables.EPISODES + "." + Episodes._ID;
        String EPISODES_SHOW_ID = Tables.EPISODES + "." + Shows.REF_SHOW_ID;
        String SEASONS_SHOW_ID = Tables.SEASONS + "." + Shows.REF_SHOW_ID;
    }

    public interface Tables {

        String SHOWS = "series";

        String SEASONS = "seasons";

        String EPISODES = "episodes";

        String SHOWS_JOIN_EPISODES = SHOWS + " LEFT OUTER JOIN " + EPISODES
                + " ON " + Qualified.SHOWS_NEXTEPISODE + "=" + Qualified.EPISODES_ID;

        String SEASONS_JOIN_SHOWS = SEASONS + " LEFT OUTER JOIN " + SHOWS
                + " ON " + Qualified.SEASONS_SHOW_ID + "=" + Qualified.SHOWS_ID;

        String EPISODES_JOIN_SHOWS = EPISODES + " LEFT OUTER JOIN " + SHOWS
                + " ON " + Qualified.EPISODES_SHOW_ID + "=" + Qualified.SHOWS_ID;

        String EPISODES_SEARCH = "searchtable";

        String LISTS = "lists";

        String LIST_ITEMS = "listitems";

        String LIST_ITEMS_WITH_DETAILS = "(SELECT "
                + Selections.SHOWS_COLUMNS + " FROM "
                + "((SELECT " + Selections.LIST_ITEMS_COLUMNS_INTERNAL
                + " FROM listitems WHERE item_type=1) AS listitems "
                + "LEFT OUTER JOIN (SELECT " + Selections.SHOWS_COLUMNS_INTERNAL
                + " FROM series) as series ON item_ref_id=series_id) "

                + "UNION SELECT " + Selections.SEASONS_COLUMNS + " FROM "
                + "((SELECT " + Selections.LIST_ITEMS_COLUMNS_INTERNAL
                + " FROM listitems WHERE item_type=2) AS listitems LEFT OUTER JOIN ("
                + SEASONS_JOIN_SHOWS
                + ") AS seasons ON listitems.item_ref_id=seasons._id) "

                + "UNION SELECT " + Selections.EPISODES_COLUMNS + " FROM "
                + "((SELECT " + Selections.LIST_ITEMS_COLUMNS_INTERNAL
                + " FROM listitems WHERE item_type=3) AS listitems LEFT OUTER JOIN ("
                + EPISODES_JOIN_SHOWS + ") AS episodes ON listitems.item_ref_id=episodes._id))";

        String MOVIES = "movies";

    }

    private interface Selections {

        String COMMON_SHOW_COLUMNS =
                Shows.REF_SHOW_ID + ","
                        + Shows.TITLE + ","
                        + Shows.POSTER + ","
                        + Shows.NETWORK + ","
                        + Shows.STATUS + ","
                        + Shows.AIRSDAYOFWEEK + ","
                        + Shows.FAVORITE;
        String COMMON_LIST_ITEMS_COLUMNS
                = "listitem_id AS _id,list_item_id,list_id,item_type,item_ref_id";

        String LIST_ITEMS_COLUMNS_INTERNAL
                = "_id AS listitem_id,list_item_id,list_id,item_type,item_ref_id";
        String SHOWS_COLUMNS_INTERNAL = Shows._ID + " AS " + COMMON_SHOW_COLUMNS + ","
                + "overview,airstime,nexttext,series_nextairdatetext";

        String SHOWS_COLUMNS = COMMON_LIST_ITEMS_COLUMNS + "," + COMMON_SHOW_COLUMNS + ","
                + "overview,airstime,nexttext,series_nextairdatetext";
        String SEASONS_COLUMNS = COMMON_LIST_ITEMS_COLUMNS + "," + COMMON_SHOW_COLUMNS + ","
                + "combinednr AS overview,airstime,nexttext,series_nextairdatetext";
        String EPISODES_COLUMNS = COMMON_LIST_ITEMS_COLUMNS
                + ","
                + COMMON_SHOW_COLUMNS
                + ","
                + "episodetitle AS overview,episode_firstairedms AS airstime,"
                + "season AS nexttext,episodenumber AS series_nextairdatetext";
    }

    interface References {

        String SHOW_ID = "REFERENCES " + Tables.SHOWS + "(" + BaseColumns._ID + ")";

        String SEASON_ID = "REFERENCES " + Tables.SEASONS + "(" + BaseColumns._ID + ")";

        String LIST_ID = "REFERENCES " + Tables.LISTS + "(" + Lists.LIST_ID + ")";
    }

    private static final String CREATE_SHOWS_TABLE = "CREATE TABLE " + Tables.SHOWS
            + " ("

            + BaseColumns._ID + " INTEGER PRIMARY KEY,"

            + ShowsColumns.TITLE + " TEXT NOT NULL,"

            + ShowsColumns.OVERVIEW + " TEXT DEFAULT '',"

            + ShowsColumns.ACTORS + " TEXT DEFAULT '',"

            + ShowsColumns.AIRSDAYOFWEEK + " TEXT DEFAULT '',"

            + ShowsColumns.AIRSTIME + " INTEGER DEFAULT '',"

            + ShowsColumns.FIRSTAIRED + " TEXT DEFAULT '',"

            + ShowsColumns.GENRES + " TEXT DEFAULT '',"

            + ShowsColumns.NETWORK + " TEXT DEFAULT '',"

            + ShowsColumns.RATING + " TEXT DEFAULT '',"

            + ShowsColumns.RUNTIME + " TEXT DEFAULT '',"

            + ShowsColumns.STATUS + " TEXT DEFAULT '',"

            + ShowsColumns.CONTENTRATING + " TEXT DEFAULT '',"

            + ShowsColumns.NEXTEPISODE + " TEXT DEFAULT '',"

            + ShowsColumns.POSTER + " TEXT DEFAULT '',"

            + ShowsColumns.NEXTAIRDATEMS + " INTEGER,"

            + ShowsColumns.NEXTTEXT + " TEXT DEFAULT '',"

            + ShowsColumns.IMDBID + " TEXT DEFAULT '',"

            + ShowsColumns.FAVORITE + " INTEGER DEFAULT 0,"

            + ShowsColumns.NEXTAIRDATETEXT + " TEXT DEFAULT '',"

            + ShowsColumns.SYNCENABLED + " INTEGER DEFAULT 1,"

            + ShowsColumns.RELEASE_COUNTRY + " TEXT DEFAULT '',"

            + ShowsColumns.HIDDEN + " INTEGER DEFAULT 0,"

            + ShowsColumns.LASTUPDATED + " INTEGER DEFAULT 0,"

            + ShowsColumns.LASTEDIT + " INTEGER DEFAULT 0,"

            + ShowsColumns.GETGLUEID + " TEXT DEFAULT '',"

            + ShowsColumns.LASTWATCHEDID + " INTEGER DEFAULT 0"

            + ");";

    private static final String CREATE_SEASONS_TABLE = "CREATE TABLE " + Tables.SEASONS
            + " ("

            + BaseColumns._ID + " INTEGER PRIMARY KEY,"

            + SeasonsColumns.COMBINED + " INTEGER,"

            + ShowsColumns.REF_SHOW_ID + " TEXT " + References.SHOW_ID + ","

            + SeasonsColumns.WATCHCOUNT + " INTEGER DEFAULT 0,"

            + SeasonsColumns.UNAIREDCOUNT + " INTEGER DEFAULT 0,"

            + SeasonsColumns.NOAIRDATECOUNT + " INTEGER DEFAULT 0,"

            + SeasonsColumns.TAGS + " TEXT DEFAULT '',"

            + SeasonsColumns.TOTALCOUNT + " INTEGER DEFAULT 0"

            + ");";

    private static final String CREATE_EPISODES_TABLE = "CREATE TABLE " + Tables.EPISODES
            + " ("

            + BaseColumns._ID + " INTEGER PRIMARY KEY,"

            + EpisodesColumns.TITLE + " TEXT NOT NULL,"

            + EpisodesColumns.OVERVIEW + " TEXT,"

            + EpisodesColumns.NUMBER + " INTEGER DEFAULT 0,"

            + EpisodesColumns.SEASON + " INTEGER DEFAULT 0,"

            + EpisodesColumns.DVDNUMBER + " REAL,"

            + EpisodesColumns.FIRSTAIRED + " TEXT,"

            + SeasonsColumns.REF_SEASON_ID + " TEXT " + References.SEASON_ID + ","

            + ShowsColumns.REF_SHOW_ID + " TEXT " + References.SHOW_ID + ","

            + EpisodesColumns.WATCHED + " INTEGER DEFAULT 0,"

            + EpisodesColumns.DIRECTORS + " TEXT DEFAULT '',"

            + EpisodesColumns.GUESTSTARS + " TEXT DEFAULT '',"

            + EpisodesColumns.WRITERS + " TEXT DEFAULT '',"

            + EpisodesColumns.IMAGE + " TEXT DEFAULT '',"

            + EpisodesColumns.FIRSTAIREDMS + " INTEGER DEFAULT -1,"

            + EpisodesColumns.COLLECTED + " INTEGER DEFAULT 0,"

            + EpisodesColumns.RATING + " TEXT DEFAULT '',"

            + EpisodesColumns.IMDBID + " TEXT DEFAULT '',"

            + EpisodesColumns.LAST_EDITED + " INTEGER DEFAULT 0,"

            + EpisodesColumns.ABSOLUTE_NUMBER + " INTEGER"

            + ");";

    private static final String CREATE_SEARCH_TABLE = "CREATE VIRTUAL TABLE "
            + Tables.EPISODES_SEARCH + " USING FTS3("

            + EpisodeSearchColumns.TITLE + " TEXT,"

            + EpisodeSearchColumns.OVERVIEW + " TEXT"

            + ");";

    private static final String CREATE_LISTS_TABLE = "CREATE TABLE " + Tables.LISTS
            + " ("

            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"

            + ListsColumns.LIST_ID + " TEXT NOT NULL,"

            + ListsColumns.NAME + " TEXT NOT NULL,"

            + "UNIQUE (" + ListsColumns.LIST_ID + ") ON CONFLICT REPLACE"

            + ");";

    private static final String CREATE_LIST_ITEMS_TABLE = "CREATE TABLE " + Tables.LIST_ITEMS
            + " ("

            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"

            + ListItemsColumns.LIST_ITEM_ID + " TEXT NOT NULL,"

            + ListItemsColumns.ITEM_REF_ID + " TEXT NOT NULL,"

            + ListItemsColumns.TYPE + " INTEGER NOT NULL,"

            + ListsColumns.LIST_ID + " TEXT " + References.LIST_ID + ","

            + "UNIQUE (" + ListItemsColumns.LIST_ITEM_ID + ") ON CONFLICT REPLACE"

            + ");";

    private static final String CREATE_MOVIES_TABLE = "CREATE TABLE " + Tables.MOVIES
            + " ("

            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"

            + MoviesColumns.TMDB_ID + " INTEGER NOT NULL,"

            + MoviesColumns.IMDB_ID + " TEXT,"

            + MoviesColumns.TITLE + " TEXT,"

            + MoviesColumns.POSTER + " TEXT,"

            + MoviesColumns.GENRES + " TEXT,"

            + MoviesColumns.OVERVIEW + " TEXT,"

            + MoviesColumns.RELEASED_UTC_MS + " INTEGER,"

            + MoviesColumns.RUNTIME_MIN + " INTEGER DEFAULT 0,"

            + MoviesColumns.TRAILER + " TEXT,"

            + MoviesColumns.CERTIFICATION + " TEXT,"

            + MoviesColumns.IN_COLLECTION + " INTEGER DEFAULT 0,"

            + MoviesColumns.IN_WATCHLIST + " INTEGER DEFAULT 0,"

            + MoviesColumns.PLAYS + " INTEGER DEFAULT 0,"

            + MoviesColumns.WATCHED + " INTEGER DEFAULT 0,"

            + MoviesColumns.RATING_TMDB + " REAL DEFAULT 0,"

            + MoviesColumns.RATING_VOTES_TMDB + " INTEGER DEFAULT 0,"

            + MoviesColumns.RATING_TRAKT + " INTEGER DEFAULT 0,"

            + MoviesColumns.RATING_VOTES_TRAKT + " INTEGER DEFAULT 0,"

            + MoviesColumns.LAST_UPDATED + " INTEGER,"

            + "UNIQUE (" + MoviesColumns.TMDB_ID + ") ON CONFLICT REPLACE"

            + ");";

    public SeriesGuideDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_SHOWS_TABLE);

        db.execSQL(CREATE_SEASONS_TABLE);

        db.execSQL(CREATE_EPISODES_TABLE);

        db.execSQL(CREATE_SEARCH_TABLE);

        db.execSQL(CREATE_LISTS_TABLE);

        db.execSQL(CREATE_LIST_ITEMS_TABLE);

        db.execSQL(CREATE_MOVIES_TABLE);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Timber.d("Can't downgrade from version " + oldVersion + " to " + newVersion);
        onResetDatabase(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Timber.d("Upgrading from " + oldVersion + " to " + newVersion);

        // run necessary upgrades
        int version = oldVersion;
        switch (version) {
            case 16:
                upgradeToSeventeen(db);
            case 17:
                upgradeToEighteen(db);
            case 18:
                upgradeToNineteen(db);
            case 19:
                upgradeToTwenty(db);
            case 20:
                upgradeToTwentyOne(db);
            case 21:
                upgradeToTwentyTwo(db);
            case 22:
                upgradeToTwentyThree(db);
            case 23:
                upgradeToTwentyFour(db);
            case 24:
                upgradeToTwentyFive(db);
            case 25:
                upgradeToTwentySix(db);
            case 26:
                upgradeToTwentySeven(db);
            case 27:
                upgradeToTwentyEight(db);
            case 28:
                upgradeToTwentyNine(db);
            case 29:
                upgradeToThirty(db);
            case 30:
                upgradeToThirtyOne(db);
            case DBVER_31_LAST_WATCHED_ID:
                upgradeToThirtyTwo(db);
                version = DBVER_32_MOVIES;
        }

        // drop all tables if version is not right
        Timber.d("After upgrade at version " + version);
        if (version != DATABASE_VERSION) {
            onResetDatabase(db);
        }
    }

    /**
     * Drops all tables and creates an empty database.
     */
    private void onResetDatabase(SQLiteDatabase db) {
        Timber.w("Resetting database");
        db.execSQL("DROP TABLE IF EXISTS " + Tables.SHOWS);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.SEASONS);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.EPISODES);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.LISTS);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.LIST_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.MOVIES);

        db.execSQL("DROP TABLE IF EXISTS " + Tables.EPISODES_SEARCH);

        onCreate(db);
    }

    // Must be watched and have an airdate
    private static final String LATEST_SELECTION = Episodes.WATCHED + "=1 AND "
            + Episodes.FIRSTAIREDMS + "!=-1 AND " + Shows.REF_SHOW_ID + "=?";

    // Latest aired first (ensures we get specials), if equal sort by season,
    // then number
    private static final String LATEST_ORDER = Episodes.FIRSTAIREDMS + " DESC,"
            + Episodes.SEASON + " DESC,"
            + Episodes.NUMBER + " DESC";

    /**
     * Add movies table.
     */
    private static void upgradeToThirtyTwo(SQLiteDatabase db) {
        db.execSQL(CREATE_MOVIES_TABLE);
    }

    /**
     * Add {@link Shows} column to store the last watched episode id for better prediction of next
     * episode.
     */
    private static void upgradeToThirtyOne(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.LASTWATCHEDID
                + " INTEGER DEFAULT 0;");

        // pre populate with latest watched episode ids
        ContentValues values = new ContentValues();
        final Cursor shows = db.query(Tables.SHOWS, new String[]{
                Shows._ID,
        }, null, null, null, null, null);
        if (shows != null) {

            db.beginTransaction();
            try {

                while (shows.moveToNext()) {
                    final String showId = shows.getString(0);
                    final Cursor highestWatchedEpisode = db.query(Tables.EPISODES, new String[]{
                            Episodes._ID
                    }, LATEST_SELECTION, new String[]{
                            showId
                    }, null, null, LATEST_ORDER);

                    if (highestWatchedEpisode != null) {
                        if (highestWatchedEpisode.moveToFirst()) {
                            values.put(Shows.LASTWATCHEDID, highestWatchedEpisode.getInt(0));
                            db.update(Tables.SHOWS, values, Shows._ID + "=?", new String[]{
                                    showId
                            });
                            values.clear();
                        }

                        highestWatchedEpisode.close();
                    }
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            shows.close();
        }
    }

    /**
     * Add {@link Episodes} column to store absolute episode number.
     */
    private static void upgradeToThirty(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.EPISODES + " ADD COLUMN "
                + EpisodesColumns.ABSOLUTE_NUMBER + " INTEGER;");
    }

    /**
     * Add {@link Shows} column to store a GetGlue object id.
     */
    private static void upgradeToTwentyNine(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.GETGLUEID
                + " TEXT DEFAULT '';");
    }

    /**
     * Add tables to store lists and list items.
     */
    private static void upgradeToTwentyEight(SQLiteDatabase db) {
        db.execSQL(CREATE_LISTS_TABLE);

        db.execSQL(CREATE_LIST_ITEMS_TABLE);
    }

    /**
     * Add {@link Episodes} columns for storing its IMDb id and last time of edit on theTVDB.com.
     * Add {@link Shows} column for storing last time of edit as well.
     */
    private static void upgradeToTwentySeven(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.LASTEDIT
                + " INTEGER DEFAULT 0;");
        db.execSQL("ALTER TABLE " + Tables.EPISODES + " ADD COLUMN " + EpisodesColumns.IMDBID
                + " TEXT DEFAULT '';");
        db.execSQL("ALTER TABLE " + Tables.EPISODES + " ADD COLUMN " + EpisodesColumns.LAST_EDITED
                + " INTEGER DEFAULT 0;");
    }

    /**
     * Add a {@link Episodes} column for storing whether an episode was collected in digital or
     * physical form.
     */
    private static void upgradeToTwentySix(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.EPISODES + " ADD COLUMN " + EpisodesColumns.COLLECTED
                + " INTEGER DEFAULT 0;");
    }

    /**
     * Add a {@link Shows} column for storing the next air date in ms as integer data type rather
     * than as text.
     */
    private static void upgradeToTwentyFive(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.NEXTAIRDATEMS
                + " INTEGER DEFAULT 0;");
    }

    /**
     * Adds a column to the {@link Tables#EPISODES} table to store the airdate and possibly time in
     * milliseconds.
     */
    private static void upgradeToTwentyFour(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.EPISODES + " ADD COLUMN " + EpisodesColumns.FIRSTAIREDMS
                + " INTEGER DEFAULT -1;");

        // populate the new column from existing data
        final Cursor shows = db.query(Tables.SHOWS, new String[]{
                Shows._ID, Shows.AIRSTIME
        }, null, null, null, null, null);

        while (shows.moveToNext()) {
            final String showId = shows.getString(0);
            final long airtime = shows.getLong(1);

            final Cursor episodes = db.query(Tables.EPISODES, new String[]{
                    Episodes._ID, Episodes.FIRSTAIRED
            }, Shows.REF_SHOW_ID + "=?", new String[]{
                    showId
            }, null, null, null);

            db.beginTransaction();
            try {
                ContentValues values = new ContentValues();
                while (episodes.moveToNext()) {
                    String firstAired = episodes.getString(1);
                    long episodeAirtime = TimeTools.parseEpisodeReleaseTime(firstAired, airtime, null);

                    values.put(Episodes.FIRSTAIREDMS, episodeAirtime);
                    db.update(Tables.EPISODES, values, Episodes._ID + "=?", new String[]{
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
     * Adds a column to the {@link Tables#SHOWS} table similar to the favorite boolean, but to allow
     * hiding shows.
     */
    private static void upgradeToTwentyThree(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.HIDDEN
                + " INTEGER DEFAULT 0;");
    }

    /**
     * Add a column to store the last time a show has been updated to allow for more precise control
     * over which shows should get updated. This is in conjunction with a 7 day limit when a show
     * will get updated regardless if it has been marked as updated or not.
     */
    private static void upgradeToTwentyTwo(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.LASTUPDATED
                + " INTEGER DEFAULT 0;");
    }

    private static void upgradeToTwentyOne(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.RELEASE_COUNTRY
                + " TEXT DEFAULT '';");
    }

    private static void upgradeToTwenty(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.SYNCENABLED
                + " INTEGER DEFAULT 1;");
    }

    /**
     * In version 19 the season integer column totalcount was added.
     */
    private static void upgradeToNineteen(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.SEASONS + " ADD COLUMN " + SeasonsColumns.TOTALCOUNT
                + " INTEGER DEFAULT 0;");
    }

    /**
     * In version 18 the series text column nextairdatetext was added.
     */
    private static void upgradeToEighteen(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.NEXTAIRDATETEXT
                + " TEXT DEFAULT '';");

        // convert status text to 0/1 integer
        final Cursor shows = db.query(Tables.SHOWS, new String[]{
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
                db.update(Tables.SHOWS, values, Shows._ID + "=?", new String[]{
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
     */
    private static void upgradeToSeventeen(SQLiteDatabase db) {
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
        return db.rawQuery(query.toString(), new String[]{
                "\"" + searchterm + "*\""
        });
    }

}
