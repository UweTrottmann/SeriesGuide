package com.battlelancer.seriesguide.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SqliteTestDbOpenHelper extends SQLiteOpenHelper {

    public SqliteTestDbOpenHelper(Context context, String databaseName) {
        super(context, databaseName, null, SeriesGuideDatabase.DBVER_42_JOBS);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SeriesGuideDatabase.CREATE_SHOWS_TABLE);
        db.execSQL(SeriesGuideDatabase.CREATE_SEASONS_TABLE);
        db.execSQL(SeriesGuideDatabase.CREATE_EPISODES_TABLE);
        db.execSQL(SeriesGuideDatabase.CREATE_LISTS_TABLE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Not testing migration from older versions created using SQLite API
    }

}
