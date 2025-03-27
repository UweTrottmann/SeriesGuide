// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2025 Uwe Trottmann

package com.battlelancer.seriesguide.provider

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SqliteTestDbOpenHelper(
    context: Context, databaseName: String
) : SQLiteOpenHelper(context, databaseName, null, SeriesGuideDatabase.DBVER_42_JOBS) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SeriesGuideDatabase.CREATE_SHOWS_TABLE)
        db.execSQL(SeriesGuideDatabase.CREATE_SEASONS_TABLE)
        db.execSQL(SeriesGuideDatabase.CREATE_EPISODES_TABLE)
        db.execSQL(SeriesGuideDatabase.CREATE_SEARCH_TABLE) /* Invisible to Room */
        db.execSQL(SeriesGuideDatabase.CREATE_LISTS_TABLE)
        db.execSQL(SeriesGuideDatabase.CREATE_LIST_ITEMS_TABLE)
        db.execSQL(SeriesGuideDatabase.CREATE_MOVIES_TABLE)
        db.execSQL(SeriesGuideDatabase.CREATE_ACTIVITY_TABLE)
        db.execSQL(SeriesGuideDatabase.CREATE_JOBS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Not testing migration from older versions created using SQLiteOpenHelper API
    }
}
