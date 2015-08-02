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

package com.battlelancer.seriesguide.billing.amazon;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * SQLiteHelper holding purchase records.
 *
 * <p><b>Note:</b> Did previously only hold subscriptions, now also entitlements (one-time
 * purchases).
 */
public class AmazonBillingSQLiteHelper extends SQLiteOpenHelper {
    // table name
    public static final String TABLE_PURCHASES = "subscriptions";
    // receipt id
    public static final String COLUMN_RECEIPT_ID = "receipt_id";
    // amazon user id
    public static final String COLUMN_USER_ID = "user_id";
    // purchase valid from date
    public static final String COLUMN_DATE_FROM = "date_from";
    // purchase valid to date
    public static final String COLUMN_DATE_TO = "date_to";
    // sku
    public static final String COLUMN_SKU = "sku";

    private static final String DATABASE_NAME = "subscriptions.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table " + TABLE_PURCHASES
            + "("
            + COLUMN_RECEIPT_ID
            + " text primary key not null, "
            + COLUMN_USER_ID
            + " text not null, "
            + COLUMN_DATE_FROM
            + " integer not null, "
            + COLUMN_DATE_TO
            + " integer, "
            + COLUMN_SKU
            + " text not null"
            + ");";

    public AmazonBillingSQLiteHelper(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(final SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        Log.w(AmazonBillingSQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to " + newVersion);
        // do nothing
    }
}