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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

/**
 * DAO class for purchase data.
 */
public class PurchaseDataSource {

    private SQLiteDatabase database;
    private final AmazonBillingSQLiteHelper dbHelper;

    private final String[] allColumns = {
            AmazonBillingSQLiteHelper.COLUMN_RECEIPT_ID,
            AmazonBillingSQLiteHelper.COLUMN_USER_ID,
            AmazonBillingSQLiteHelper.COLUMN_DATE_FROM,
            AmazonBillingSQLiteHelper.COLUMN_DATE_TO,
            AmazonBillingSQLiteHelper.COLUMN_SKU
    };

    public PurchaseDataSource(final Context context) {
        dbHelper = new AmazonBillingSQLiteHelper(context.getApplicationContext());
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    private PurchaseRecord cursorToPurchaseRecord(final Cursor cursor) {
        final PurchaseRecord purchaseRecord = new PurchaseRecord();
        purchaseRecord.setAmazonReceiptId(
                cursor.getString(
                        cursor.getColumnIndex(AmazonBillingSQLiteHelper.COLUMN_RECEIPT_ID)));
        purchaseRecord.setAmazonUserId(
                cursor.getString(cursor.getColumnIndex(AmazonBillingSQLiteHelper.COLUMN_USER_ID)));
        purchaseRecord.setValidFrom(
                cursor.getLong(cursor.getColumnIndex(AmazonBillingSQLiteHelper.COLUMN_DATE_FROM)));
        purchaseRecord.setValidTo(
                cursor.getLong(cursor.getColumnIndex(AmazonBillingSQLiteHelper.COLUMN_DATE_TO)));
        purchaseRecord.setSku(
                cursor.getString(cursor.getColumnIndex(AmazonBillingSQLiteHelper.COLUMN_SKU)));
        return purchaseRecord;
    }

    /**
     * Find entitlement record by specified receipt ID.
     */
    @Nullable
    public PurchaseRecord getEntitlementRecordByReceiptId(final String receiptId) {
        Timber.d("getEntitlementRecordByReceiptId: receiptId (" + receiptId + ")");

        final String where = AmazonBillingSQLiteHelper.COLUMN_RECEIPT_ID + "= ?";
        final Cursor cursor = database.query(AmazonBillingSQLiteHelper.TABLE_PURCHASES,
                allColumns,
                where,
                new String[] { receiptId },
                null,
                null,
                null);
        final PurchaseRecord result;
        cursor.moveToFirst();
        if (cursor.isAfterLast()) {
            result = null;
            Timber.d("getEntitlementRecordByReceiptId: no record found ");
        } else {
            result = cursorToPurchaseRecord(cursor);
            Timber.d("getEntitlementRecordByReceiptId: found ");
        }
        cursor.close();
        return result;
    }

    /**
     * Return the entitlement for given user and sku.
     */
    @Nullable
    public PurchaseRecord getLatestEntitlementRecordBySku(String userId, String sku) {
        Timber.d("getEntitlementRecordBySku: userId (" + userId + "), sku (" + sku + ")");

        final String where = AmazonBillingSQLiteHelper.COLUMN_USER_ID + " = ? and "
                + AmazonBillingSQLiteHelper.COLUMN_SKU + " = ?";
        final Cursor cursor = database.query(AmazonBillingSQLiteHelper.TABLE_PURCHASES, allColumns,
                where, new String[] { userId, sku }, null, null,
                AmazonBillingSQLiteHelper.COLUMN_DATE_FROM + " desc ");
        final PurchaseRecord result;
        cursor.moveToFirst();
        if (cursor.isAfterLast()) {
            result = null;
            Timber.d("getEntitlementRecordBySku: no record found ");
        } else {
            result = cursorToPurchaseRecord(cursor);
            Timber.d("getEntitlementRecordBySku: found ");
        }
        cursor.close();
        return result;
    }

    /**
     * Return all purchase records for the user.
     *
     * @param userId user id used to verify the purchase record.
     */
    public final List<PurchaseRecord> getPurchaseRecords(final String userId) {
        Timber.d("getPurchaseRecords: userId (" + userId + ")");

        final String where = AmazonBillingSQLiteHelper.COLUMN_USER_ID + " = ?";
        final Cursor cursor = database.query(AmazonBillingSQLiteHelper.TABLE_PURCHASES,
                allColumns,
                where,
                new String[] { userId },
                null,
                null,
                null);
        cursor.moveToFirst();
        final List<PurchaseRecord> results = new ArrayList<>();
        while (!cursor.isAfterLast()) {
            final PurchaseRecord subsRecord = cursorToPurchaseRecord(cursor);
            results.add(subsRecord);
            cursor.moveToNext();
        }
        Timber.d("getPurchaseRecords: found " + results.size() + " records");
        cursor.close();
        return results;
    }

    /**
     * Insert or update the subscription record by receiptId
     *
     * @param receiptId The receipt id
     * @param userId Amazon user id
     * @param dateFrom Timestamp for subscription's valid from date
     * @param dateTo Timestamp for subscription's valid to date. less than 1 means cancel date not
     * set, the subscription in active status.
     * @param sku The sku
     */
    public void insertOrUpdateSubscriptionRecord(final String receiptId,
            final String userId,
            final long dateFrom,
            final long dateTo,
            final String sku) {
        Timber.d("insertOrUpdateSubscriptionRecord: receiptId (" + receiptId + "),userId (" + userId
                + ")");
        final String where = AmazonBillingSQLiteHelper.COLUMN_RECEIPT_ID + " = ? and "
                + AmazonBillingSQLiteHelper.COLUMN_DATE_TO
                + " > 0";

        final Cursor cursor = database.query(AmazonBillingSQLiteHelper.TABLE_PURCHASES,
                allColumns,
                where,
                new String[] { receiptId },
                null,
                null,
                null);
        final int count = cursor.getCount();
        cursor.close();
        if (count > 0) {
            // There are record with given receipt id and cancel_date>0 in the
            // table, this record should be final and cannot be overwritten
            // anymore.
            Timber.w("Record already in final state");
        } else {
            // Insert the record into database with CONFLICT_REPLACE flag.
            final ContentValues values = new ContentValues();
            values.put(AmazonBillingSQLiteHelper.COLUMN_RECEIPT_ID, receiptId);
            values.put(AmazonBillingSQLiteHelper.COLUMN_USER_ID, userId);
            values.put(AmazonBillingSQLiteHelper.COLUMN_DATE_FROM, dateFrom);
            values.put(AmazonBillingSQLiteHelper.COLUMN_DATE_TO, dateTo);
            values.put(AmazonBillingSQLiteHelper.COLUMN_SKU, sku);
            database.insertWithOnConflict(AmazonBillingSQLiteHelper.TABLE_PURCHASES,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    /**
     * Cancel the specified Entitlement record by setting the cancel date.
     */
    public boolean cancelEntitlement(final String receiptId, final long cancelDate) {
        Timber.d("cancelEntitlement: receiptId (" + receiptId + "), cancelDate:(" + cancelDate
                + ")");

        final String where = AmazonBillingSQLiteHelper.COLUMN_RECEIPT_ID + " = ?";
        final ContentValues values = new ContentValues();
        values.put(AmazonBillingSQLiteHelper.COLUMN_DATE_TO, cancelDate);
        final int updated = database.update(AmazonBillingSQLiteHelper.TABLE_PURCHASES,
                values,
                where,
                new String[] { receiptId });
        Timber.d("cancelEntitlement: updated " + updated);
        return updated > 0;
    }

    /**
     * Cancel a subscription by set the cancel date for the subscription record
     *
     * @param receiptId The receipt id
     * @param cancelDate Timestamp for the cancel date
     */
    public boolean cancelSubscription(final String receiptId, final long cancelDate) {
        Timber.d("cancelSubscription: receiptId (" + receiptId + "), cancelDate:(" + cancelDate
                + ")");

        final String where = AmazonBillingSQLiteHelper.COLUMN_RECEIPT_ID + " = ?";
        final ContentValues values = new ContentValues();
        values.put(AmazonBillingSQLiteHelper.COLUMN_DATE_TO, cancelDate);
        final int updated = database.update(AmazonBillingSQLiteHelper.TABLE_PURCHASES,
                values,
                where,
                new String[] { receiptId });
        Timber.d("cancelSubscription: updated " + updated);
        return updated > 0;
    }
}
