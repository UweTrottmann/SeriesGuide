// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.provider;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;
import static com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.BaseColumns;
import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Jobs;
import com.battlelancer.seriesguide.util.SelectionBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import timber.log.Timber;

/**
 * Legacy content provider. New code should access the database through Room APIs.
 */
public class SeriesGuideProvider extends ContentProvider {

    public static final boolean LOGV = false;

    private static UriMatcher sUriMatcher;

    static final int LISTS = 500;

    static final int LISTS_ID = 501;

    static final int LIST_ITEMS = 600;

    static final int LIST_ITEMS_ID = 601;

    static final int LIST_ITEMS_WITH_DETAILS = 602;

    static final int MOVIES = 700;

    static final int MOVIES_ID = 701;

    static final int JOBS = 1100;

    static final int JOBS_ID = 1101;

    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri} variations supported by
     * this {@link ContentProvider}.
     */
    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = SgApp.CONTENT_AUTHORITY;

        // Lists
        matcher.addURI(authority, SeriesGuideContract.PATH_LISTS, LISTS);
        matcher.addURI(authority, SeriesGuideContract.PATH_LISTS + "/*", LISTS_ID);

        // List items
        matcher.addURI(authority, SeriesGuideContract.PATH_LIST_ITEMS, LIST_ITEMS);
        matcher.addURI(authority, SeriesGuideContract.PATH_LIST_ITEMS + "/"
                + SeriesGuideContract.PATH_WITH_DETAILS, LIST_ITEMS_WITH_DETAILS);
        matcher.addURI(authority, SeriesGuideContract.PATH_LIST_ITEMS + "/*", LIST_ITEMS_ID);

        // Movies
        matcher.addURI(authority, SeriesGuideContract.PATH_MOVIES, MOVIES);
        matcher.addURI(authority, SeriesGuideContract.PATH_MOVIES + "/*", MOVIES_ID);

        // Jobs
        matcher.addURI(authority, SeriesGuideContract.PATH_JOBS, JOBS);
        matcher.addURI(authority, SeriesGuideContract.PATH_JOBS + "/*", JOBS_ID);

        return matcher;
    }

    private final ThreadLocal<Boolean> applyingBatch = new ThreadLocal<>();
    protected SupportSQLiteDatabase database;

    @Override
    public void shutdown() {
        SgRoomDatabase.getInstance(getContext()).getOpenHelper().close();
        database = null;
    }

    @Override
    public boolean onCreate() {
        sUriMatcher = buildUriMatcher();
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        if (LOGV) {
            Timber.v("query(uri=%s, proj=%s)", uri, Arrays.toString(projection));
        }

        SupportSQLiteOpenHelper databaseHelper = SgRoomDatabase.getInstance(getContext())
                .getOpenHelper();
        final int match = sUriMatcher.match(uri);

        // always get writable database, might have to be upgraded
        final SupportSQLiteDatabase db = databaseHelper.getWritableDatabase();

        switch (match) {
            default: {
                // Most cases are handled with simple SelectionBuilder
                final SelectionBuilder builder = buildSelection(uri, match);
                Cursor query = null;
                try {
                    query = builder
                            .map(BaseColumns._COUNT, "count(*)") // support count base column
                            .where(selection, selectionArgs)
                            .query(db, projection, sortOrder);
                } catch (SQLiteException e) {
                    Timber.e(e, "Failed to query with uri=%s", uri);
                }
                if (query != null) {
                    //noinspection ConstantConditions
                    query.setNotificationUri(getContext().getContentResolver(), uri);
                }
                return query;
            }
        }
    }

    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case LISTS:
                return Lists.CONTENT_TYPE;
            case LISTS_ID:
                return Lists.CONTENT_ITEM_TYPE;
            case LIST_ITEMS:
            case LIST_ITEMS_WITH_DETAILS:
                return ListItems.CONTENT_TYPE;
            case LIST_ITEMS_ID:
                return ListItems.CONTENT_ITEM_TYPE;
            case MOVIES:
                return Movies.CONTENT_TYPE;
            case MOVIES_ID:
                return Movies.CONTENT_ITEM_TYPE;
            case JOBS:
                return Jobs.CONTENT_TYPE;
            case JOBS_ID:
                return Jobs.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        Uri newItemUri;

        SgRoomDatabase room = SgRoomDatabase.getInstance(getContext());
        if (!applyingBatch()) {
            room.beginTransaction();
            try {
                newItemUri = insertInTransaction(room, uri, values, false);
                room.setTransactionSuccessful();
            } finally {
                room.endTransaction();
            }
        } else {
            newItemUri = insertInTransaction(room, uri, values, false);
        }

        if (newItemUri != null) {
            //noinspection ConstantConditions
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return newItemUri;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        int numValues = values.length;
        boolean notifyChange = false;

        SgRoomDatabase room = SgRoomDatabase.getInstance(getContext());
        room.beginTransaction();
        try {
            for (int i = 0; i < numValues; i++) {
                Uri result = insertInTransaction(room, uri, values[i], true);
                if (result != null) {
                    notifyChange = true;
                }
                // do not yield as a pre-caution to not break Room invalidation tracker
                // db.yieldIfContendedSafely();
            }
            room.setTransactionSuccessful();
        } finally {
            room.endTransaction();
        }

        if (notifyChange) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return numValues;
    }

    /**
     * @param bulkInsert It seems to happen on occasion that TVDB has duplicate episodes, also
     * backup files may contain duplicates. Handle them by making the last insert win (ON CONFLICT
     * REPLACE) for bulk inserts.
     */
    private Uri insertInTransaction(SgRoomDatabase room, Uri uri, ContentValues values,
            boolean bulkInsert) {
        if (LOGV) {
            Timber.v("insert(uri=%s, values=%s)", uri, values.toString());
        }
        Uri notifyUri = null;

        final SupportSQLiteDatabase db = room.getOpenHelper().getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case LISTS: {
                long id = tryInsert(db, Tables.LISTS, values);
                if (id < 0) {
                    break;
                }
                notifyUri = Lists.buildListUri(values.getAsString(Lists.LIST_ID));
                break;
            }
            case LIST_ITEMS: {
                long id;
                id = tryInsert(db, Tables.LIST_ITEMS, values);
                if (id < 0) {
                    break;
                }
                notifyUri = ListItems.buildListItemUri(values.getAsString(ListItems.LIST_ITEM_ID));
                break;
            }
            case MOVIES: {
                long id = tryInsert(db, Tables.MOVIES, values);
                if (id < 0) {
                    break;
                }
                notifyUri = Movies.buildMovieUri(values.getAsInteger(Movies.TMDB_ID));
                break;
            }
            case JOBS: {
                long id = tryInsert(db, Tables.JOBS, values);
                if (id < 0) {
                    break;
                }
                notifyUri = Jobs.buildJobUri(id);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown uri: " + uri);
            }
        }

        return notifyUri;
    }

    /**
     * Tries insert, always returns -1 on failure.
     */
    private long tryInsert(SupportSQLiteDatabase db, String table, ContentValues values) {
        try {
            return db.insert(table, SQLiteDatabase.CONFLICT_REPLACE, values);
        } catch (SQLException e) {
            Timber.e(e, "Error inserting %s", values);
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        if (LOGV) {
            Timber.v("update(uri=%s, values=%s)", uri, values.toString());
        }
        int count;

        SgRoomDatabase room = SgRoomDatabase.getInstance(getContext());
        if (!applyingBatch()) {
            room.beginTransaction();
            try {
                count = buildSelection(uri, sUriMatcher.match(uri))
                        .where(selection, selectionArgs)
                        .update(room.getOpenHelper().getWritableDatabase(), values);
                room.setTransactionSuccessful();
            } finally {
                room.endTransaction();
            }
        } else {
            database = room.getOpenHelper().getWritableDatabase();
            count = buildSelection(uri, sUriMatcher.match(uri))
                    .where(selection, selectionArgs)
                    .update(database, values);
        }

        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        if (LOGV) {
            Timber.v("delete(uri=%s)", uri);
        }
        int count;

        SgRoomDatabase room = SgRoomDatabase.getInstance(getContext());
        if (!applyingBatch()) {
            room.beginTransaction();
            try {
                count = buildSelection(uri, sUriMatcher.match(uri))
                        .where(selection, selectionArgs)
                        .delete(room.getOpenHelper().getWritableDatabase());
                room.setTransactionSuccessful();
            } finally {
                room.endTransaction();
            }
        } else {
            database = room.getOpenHelper().getWritableDatabase();
            count = buildSelection(uri, sUriMatcher.match(uri))
                    .where(selection, selectionArgs)
                    .delete(database);
        }

        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return count;
    }

    /**
     * Apply the given set of {@link ContentProviderOperation}, executing inside a {@link
     * SQLiteDatabase} transaction. All changes will be rolled back if any single one fails.
     */
    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(
            @NonNull ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        final int numOperations = operations.size();
        if (numOperations == 0) {
            return new ContentProviderResult[0];
        }

        SgRoomDatabase room = SgRoomDatabase.getInstance(getContext());
        database = room.getOpenHelper().getWritableDatabase();
        room.beginTransaction();
        try {
            applyingBatch.set(true);
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                final ContentProviderOperation operation = operations.get(i);
                // do not yield as a pre-caution to not break Room invalidation tracker
//                if (i > 0 && operation.isYieldAllowed()) {
//                    database.yieldIfContendedSafely();
//                }
                results[i] = operation.apply(this, results, i);
            }
            room.setTransactionSuccessful();
            return results;
        } finally {
            applyingBatch.set(false);
            room.endTransaction();
        }
    }

    private boolean applyingBatch() {
        return applyingBatch.get() != null && applyingBatch.get();
    }

    /**
     * Builds selection using a {@link SelectionBuilder} to match the requested {@link Uri}.
     */
    private static SelectionBuilder buildSelection(Uri uri, int match) {
        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            case LISTS: {
                return builder.table(Tables.LISTS);
            }
            case LISTS_ID: {
                final String listId = Lists.getId(uri);
                return builder.table(Tables.LISTS).where(Lists.LIST_ID + "=?", listId);
            }
            case LIST_ITEMS: {
                return builder.table(Tables.LIST_ITEMS);
            }
            case LIST_ITEMS_ID: {
                final String list_item_id = ListItems.getId(uri);
                return builder.table(Tables.LIST_ITEMS).where(ListItems.LIST_ITEM_ID + "=?",
                        list_item_id);
            }
            case LIST_ITEMS_WITH_DETAILS: {
                return builder.table(Tables.LIST_ITEMS_WITH_DETAILS);
            }
            case MOVIES: {
                return builder.table(Tables.MOVIES);
            }
            case MOVIES_ID: {
                final String movieId = Movies.getId(uri);
                return builder.table(Tables.MOVIES).where(Movies.TMDB_ID + "=?", movieId);
            }
            case JOBS: {
                return builder.table(Tables.JOBS);
            }
            case JOBS_ID: {
                String jobId = Jobs.getJobId(uri);
                return builder.table(Tables.JOBS).where(Jobs._ID + "=?", jobId);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }
}
