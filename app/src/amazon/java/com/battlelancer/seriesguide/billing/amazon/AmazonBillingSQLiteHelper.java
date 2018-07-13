package com.battlelancer.seriesguide.billing.amazon;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import timber.log.Timber;

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
        Timber.w("Upgrading database from version " + oldVersion + " to " + newVersion);
        // do nothing
    }
}