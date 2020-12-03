package com.battlelancer.seriesguide.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SqliteTestDbOpenHelper extends SQLiteOpenHelper {

    public SqliteTestDbOpenHelper(Context context, String databaseName) {
        super(context, databaseName, null, SeriesGuideDatabase.DBVER_42_JOBS);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_SHOWS_TABLE);
        db.execSQL(CREATE_SEASONS_TABLE);
        db.execSQL(CREATE_EPISODES_TABLE);
        db.execSQL(SeriesGuideDatabase.CREATE_SEARCH_TABLE); /* Invisible to Room */
        db.execSQL(CREATE_LISTS_TABLE);
        db.execSQL(CREATE_LIST_ITEMS_TABLE);
        db.execSQL(CREATE_MOVIES_TABLE);
        db.execSQL("CREATE TABLE " + SeriesGuideDatabase.ACTIVITY_TABLE);
        db.execSQL("CREATE TABLE " + SeriesGuideDatabase.JOBS_TABLE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Not testing migration from older versions created using SQLiteOpenHelper API
    }

    private static final String CREATE_SHOWS_TABLE = "CREATE TABLE series ("
            + "_id INTEGER PRIMARY KEY,"
            + "seriestitle TEXT NOT NULL,"
            + "series_title_noarticle TEXT,"
            + "overview TEXT DEFAULT '',"
            + "airstime INTEGER,"
            + "airsdayofweek INTEGER,"
            + "series_airtime TEXT,"
            + "series_timezone TEXT,"
            + "firstaired TEXT,"
            + "genres TEXT DEFAULT '',"
            + "network TEXT DEFAULT '',"
            + "rating REAL,"
            + "series_rating_votes INTEGER,"
            + "series_rating_user INTEGER,"
            + "runtime TEXT DEFAULT '',"
            + "status TEXT DEFAULT '',"
            + "contentrating TEXT DEFAULT '',"
            + "next TEXT DEFAULT '',"
            + "poster TEXT DEFAULT '',"
            + "series_nextairdate INTEGER,"
            + "nexttext TEXT DEFAULT '',"
            + "imdbid TEXT DEFAULT '',"
            + "series_trakt_id INTEGER DEFAULT 0,"
            + "series_favorite INTEGER DEFAULT 0,"
            + "series_nextairdatetext TEXT DEFAULT '',"
            + "series_syncenabled INTEGER DEFAULT 1,"
            + "series_hidden INTEGER DEFAULT 0,"
            + "series_lastupdate INTEGER DEFAULT 0,"
            + "series_lastedit INTEGER DEFAULT 0,"
            + "series_lastwatchedid INTEGER DEFAULT 0,"
            + "series_lastwatched_ms INTEGER DEFAULT 0,"
            + "series_language TEXT DEFAULT '',"
            + "series_unwatched_count INTEGER DEFAULT -1,"
            + "series_notify INTEGER DEFAULT 1"
            + ");";

    private static final String CREATE_SEASONS_TABLE = "CREATE TABLE seasons ("
            + "_id INTEGER PRIMARY KEY,"
            + "combinednr INTEGER,"
            + "series_id TEXT REFERENCES series(_id),"
            + "watchcount INTEGER DEFAULT 0,"
            + "willaircount INTEGER DEFAULT 0,"
            + "noairdatecount INTEGER DEFAULT 0,"
            + "seasonposter TEXT DEFAULT '',"
            + "season_totalcount INTEGER DEFAULT 0"
            + ");";

    private static final String CREATE_EPISODES_TABLE = "CREATE TABLE episodes ("
            + "_id INTEGER PRIMARY KEY,"
            + "episodetitle TEXT NOT NULL,"
            + "episodedescription TEXT,"
            + "episodenumber INTEGER DEFAULT 0,"
            + "season INTEGER DEFAULT 0,"
            + "dvdnumber REAL,"
            + "season_id TEXT REFERENCES seasons(_id),"
            + "series_id TEXT REFERENCES series(_id),"
            + "watched INTEGER DEFAULT 0,"
            + "directors TEXT DEFAULT '',"
            + "gueststars TEXT DEFAULT '',"
            + "writers TEXT DEFAULT '',"
            + "episodeimage TEXT DEFAULT '',"
            + "episode_firstairedms INTEGER DEFAULT -1,"
            + "episode_collected INTEGER DEFAULT 0,"
            + "rating REAL,"
            + "episode_rating_votes INTEGER,"
            + "episode_rating_user INTEGER,"
            + "episode_imdbid TEXT DEFAULT '',"
            + "episode_lastedit INTEGER DEFAULT 0,"
            + "absolute_number INTEGER,"
            + "episode_lastupdate INTEGER DEFAULT 0"
            + ");";

    private static final String CREATE_LISTS_TABLE = "CREATE TABLE lists ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "list_id TEXT NOT NULL,"
            + "list_name TEXT NOT NULL,"
            + "list_order INTEGER DEFAULT 0,"
            + "UNIQUE (list_id) ON CONFLICT REPLACE"
            + ");";

    private static final String CREATE_LIST_ITEMS_TABLE = "CREATE TABLE listitems ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "list_item_id TEXT NOT NULL,"
            + "item_ref_id TEXT NOT NULL,"
            + "item_type INTEGER NOT NULL,"
            + "list_id TEXT REFERENCES lists(list_id),"
            + "UNIQUE (list_item_id) ON CONFLICT REPLACE"
            + ");";

    private static final String CREATE_MOVIES_TABLE = "CREATE TABLE movies ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "movies_tmdbid INTEGER NOT NULL,"
            + "movies_imdbid TEXT,"
            + "movies_title TEXT,"
            + "movies_title_noarticle TEXT,"
            + "movies_poster TEXT,"
            + "movies_genres TEXT,"
            + "movies_overview TEXT,"
            + "movies_released INTEGER,"
            + "movies_runtime INTEGER DEFAULT 0,"
            + "movies_trailer TEXT,"
            + "movies_certification TEXT,"
            + "movies_incollection INTEGER DEFAULT 0,"
            + "movies_inwatchlist INTEGER DEFAULT 0,"
            + "movies_plays INTEGER DEFAULT 0,"
            + "movies_watched INTEGER DEFAULT 0,"
            + "movies_rating_tmdb REAL DEFAULT 0,"
            + "movies_rating_votes_tmdb INTEGER DEFAULT 0,"
            + "movies_rating_trakt INTEGER DEFAULT 0,"
            + "movies_rating_votes_trakt INTEGER DEFAULT 0,"
            + "movies_rating_user INTEGER,"
            + "movies_last_updated INTEGER,"
            + "UNIQUE (movies_tmdbid) ON CONFLICT REPLACE"
            + ");";
}
