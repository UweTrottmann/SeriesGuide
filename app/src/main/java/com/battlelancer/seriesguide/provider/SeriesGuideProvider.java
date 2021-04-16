package com.battlelancer.seriesguide.provider;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_NONE;
import static android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE;
import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Activity;
import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import static com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;
import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
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
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgSeason2Columns;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns;
import com.battlelancer.seriesguide.util.SelectionBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import timber.log.Timber;

public class SeriesGuideProvider extends ContentProvider {

    public static final boolean LOGV = false;

    private static UriMatcher sUriMatcher;

    static final int SG_SHOW = 10;
    static final int SG_SHOW_ID = 11;
    static final int SG_SEASON = 20;
    static final int SG_SEASON_ID = 21;
    static final int SG_EPISODE = 30;
    static final int SG_EPISODE_ID = 31;

    static final int SHOWS = 100;

    static final int SHOWS_ID = 101;

    static final int SHOWS_FILTERED = 102;

    static final int SHOWS_WITH_LAST_EPISODE = 103;

    static final int SHOWS_WITH_NEXT_EPISODE = 104;

    static final int EPISODES = 200;

    static final int EPISODES_ID = 201;

    static final int EPISODES_OFSHOW = 202;

    static final int EPISODES_OFSEASON = 203;

    static final int EPISODES_OFSEASON_WITHSHOW = 204;

    static final int EPISODES_WITHSHOW = 205;

    static final int EPISODES_ID_WITHSHOW = 206;

    static final int SEASONS = 300;

    static final int SEASONS_ID = 301;

    static final int SEASONS_OFSHOW = 302;

    static final int LISTS = 500;

    static final int LISTS_ID = 501;

    static final int LISTS_WITH_LIST_ITEM_ID = 502;

    static final int LIST_ITEMS = 600;

    static final int LIST_ITEMS_ID = 601;

    static final int LIST_ITEMS_WITH_DETAILS = 602;

    static final int MOVIES = 700;

    static final int MOVIES_ID = 701;

    static final int ACTIVITY = 800;

    static final int JOBS = 1100;

    static final int JOBS_ID = 1101;

    static final int CLOSE = 1200;

    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri} variations supported by
     * this {@link ContentProvider}.
     */
    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = SgApp.CONTENT_AUTHORITY;

        // SgShow2
        matcher.addURI(authority, SeriesGuideContract.PATH_SG_SHOW, SG_SHOW);
        matcher.addURI(authority, SeriesGuideContract.PATH_SG_SHOW + "/*", SG_SHOW_ID);
        // SgSeason2
        matcher.addURI(authority, SeriesGuideContract.PATH_SG_SEASON, SG_SEASON);
        matcher.addURI(authority, SeriesGuideContract.PATH_SG_SEASON + "/*", SG_SEASON_ID);
        // SgEpisode2
        matcher.addURI(authority, SeriesGuideContract.PATH_SG_EPISODE, SG_EPISODE);
        matcher.addURI(authority, SeriesGuideContract.PATH_SG_EPISODE + "/*", SG_EPISODE_ID);

        // Shows
        matcher.addURI(authority, SeriesGuideContract.PATH_SHOWS, SHOWS);
        matcher.addURI(authority,
                SeriesGuideContract.PATH_SHOWS + "/" + SeriesGuideContract.PATH_FILTER
                        + "/*", SHOWS_FILTERED);
        matcher.addURI(authority, SeriesGuideContract.PATH_SHOWS + "/"
                + SeriesGuideContract.PATH_WITH_LAST_EPISODE, SHOWS_WITH_LAST_EPISODE);
        matcher.addURI(authority, SeriesGuideContract.PATH_SHOWS + "/"
                + SeriesGuideContract.PATH_WITH_NEXT_EPISODE, SHOWS_WITH_NEXT_EPISODE);
        matcher.addURI(authority, SeriesGuideContract.PATH_SHOWS + "/*", SHOWS_ID);

        // Episodes
        matcher.addURI(authority, SeriesGuideContract.PATH_EPISODES, EPISODES);
        matcher.addURI(authority,
                SeriesGuideContract.PATH_EPISODES + "/" + SeriesGuideContract.PATH_OFSEASON
                        + "/" + SeriesGuideContract.PATH_WITHSHOW + "/*",
                EPISODES_OFSEASON_WITHSHOW);
        matcher.addURI(authority,
                SeriesGuideContract.PATH_EPISODES + "/" + SeriesGuideContract.PATH_OFSEASON
                        + "/*", EPISODES_OFSEASON);
        matcher.addURI(authority,
                SeriesGuideContract.PATH_EPISODES + "/" + SeriesGuideContract.PATH_OFSHOW
                        + "/*", EPISODES_OFSHOW);
        matcher.addURI(authority,
                SeriesGuideContract.PATH_EPISODES + "/" + SeriesGuideContract.PATH_WITHSHOW,
                EPISODES_WITHSHOW);
        matcher.addURI(authority,
                SeriesGuideContract.PATH_EPISODES + "/" + SeriesGuideContract.PATH_WITHSHOW
                        + "/*", EPISODES_ID_WITHSHOW);
        matcher.addURI(authority, SeriesGuideContract.PATH_EPISODES + "/*", EPISODES_ID);

        // Seasons
        matcher.addURI(authority, SeriesGuideContract.PATH_SEASONS, SEASONS);
        matcher.addURI(authority,
                SeriesGuideContract.PATH_SEASONS + "/" + SeriesGuideContract.PATH_OFSHOW
                        + "/*", SEASONS_OFSHOW);
        matcher.addURI(authority, SeriesGuideContract.PATH_SEASONS + "/*", SEASONS_ID);

        // Lists
        matcher.addURI(authority, SeriesGuideContract.PATH_LISTS, LISTS);
        matcher.addURI(authority, SeriesGuideContract.PATH_LISTS + "/"
                + SeriesGuideContract.PATH_WITH_LIST_ITEM_ID + "/*", LISTS_WITH_LIST_ITEM_ID);
        matcher.addURI(authority, SeriesGuideContract.PATH_LISTS + "/*", LISTS_ID);

        // List items
        matcher.addURI(authority, SeriesGuideContract.PATH_LIST_ITEMS, LIST_ITEMS);
        matcher.addURI(authority, SeriesGuideContract.PATH_LIST_ITEMS + "/"
                + SeriesGuideContract.PATH_WITH_DETAILS, LIST_ITEMS_WITH_DETAILS);
        matcher.addURI(authority, SeriesGuideContract.PATH_LIST_ITEMS + "/*", LIST_ITEMS_ID);

        // Movies
        matcher.addURI(authority, SeriesGuideContract.PATH_MOVIES, MOVIES);
        matcher.addURI(authority, SeriesGuideContract.PATH_MOVIES + "/*", MOVIES_ID);

        // Activity
        matcher.addURI(authority, SeriesGuideContract.PATH_ACTIVITY, ACTIVITY);

        // Jobs
        matcher.addURI(authority, SeriesGuideContract.PATH_JOBS, JOBS);
        matcher.addURI(authority, SeriesGuideContract.PATH_JOBS + "/*", JOBS_ID);

        // Ops
        matcher.addURI(authority, SeriesGuideContract.PATH_CLOSE, CLOSE);

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

        // support close op for legacy database import tool, will reopen on next op
        if (match == CLOSE) {
            databaseHelper.close();
            return null;
        }

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
            case SG_SHOW:
                return SgShow2Columns.CONTENT_TYPE;
            case SG_SHOW_ID:
                return SgShow2Columns.CONTENT_ITEM_TYPE;
            case SG_SEASON:
                return SgSeason2Columns.CONTENT_TYPE;
            case SG_SEASON_ID:
                return SgSeason2Columns.CONTENT_ITEM_TYPE;
            case SG_EPISODE:
                return SgEpisode2Columns.CONTENT_TYPE;
            case SG_EPISODE_ID:
                return SgEpisode2Columns.CONTENT_ITEM_TYPE;
            case SHOWS:
            case SHOWS_FILTERED:
            case SHOWS_WITH_LAST_EPISODE:
            case SHOWS_WITH_NEXT_EPISODE:
                return Shows.CONTENT_TYPE;
            case SHOWS_ID:
                return Shows.CONTENT_ITEM_TYPE;
            case EPISODES:
            case EPISODES_OFSHOW:
            case EPISODES_OFSEASON:
            case EPISODES_OFSEASON_WITHSHOW:
            case EPISODES_WITHSHOW:
                return Episodes.CONTENT_TYPE;
            case EPISODES_ID:
            case EPISODES_ID_WITHSHOW:
                return Episodes.CONTENT_ITEM_TYPE;
            case SEASONS:
            case SEASONS_OFSHOW:
                return Seasons.CONTENT_TYPE;
            case SEASONS_ID:
                return Seasons.CONTENT_ITEM_TYPE;
            case LISTS:
            case LISTS_WITH_LIST_ITEM_ID:
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
            case ACTIVITY:
                return Activity.CONTENT_TYPE;
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
            case SHOWS: {
                long id = tryInsert(db, Tables.SHOWS, CONFLICT_NONE, values);
                if (id < 0) {
                    break;
                }
                notifyUri = Shows.buildShowUri(values.getAsString(Shows._ID));
                break;
            }
            case SEASONS: {
                long id;
                if (bulkInsert) {
                    id = tryInsert(db, Tables.SEASONS, CONFLICT_REPLACE, values);
                } else {
                    id = tryInsert(db, Tables.SEASONS, CONFLICT_NONE, values);
                }
                if (id < 0) {
                    break;
                }
                notifyUri = Seasons.buildSeasonUri(values.getAsString(Seasons._ID));
                break;
            }
            case EPISODES: {
                long id;
                if (bulkInsert) {
                    id = tryInsert(db, Tables.EPISODES, CONFLICT_REPLACE, values);
                } else {
                    id = tryInsert(db, Tables.EPISODES, CONFLICT_NONE, values);
                }
                if (id < 0) {
                    break;
                }
                notifyUri = Episodes.buildEpisodeUri(values.getAsString(Episodes._ID));
                break;
            }
            case LISTS: {
                long id = tryInsert(db, Tables.LISTS, CONFLICT_REPLACE, values);
                if (id < 0) {
                    break;
                }
                notifyUri = Lists.buildListUri(values.getAsString(Lists.LIST_ID));
                break;
            }
            case LIST_ITEMS: {
                long id;
                id = tryInsert(db, Tables.LIST_ITEMS, CONFLICT_REPLACE, values);
                if (id < 0) {
                    break;
                }
                notifyUri = ListItems.buildListItemUri(values.getAsString(ListItems.LIST_ITEM_ID));
                break;
            }
            case MOVIES: {
                long id = tryInsert(db, Tables.MOVIES, CONFLICT_REPLACE, values);
                if (id < 0) {
                    break;
                }
                notifyUri = Movies.buildMovieUri(values.getAsInteger(Movies.TMDB_ID));
                break;
            }
            case ACTIVITY: {
                long id = tryInsert(db, Tables.ACTIVITY, CONFLICT_REPLACE, values);
                if (id < 0) {
                    break;
                }
                notifyUri = Activity.buildActivityUri(values.getAsString(Activity.EPISODE_TVDB_OR_TMDB_ID));
                break;
            }
            case JOBS: {
                long id = tryInsert(db, Tables.JOBS, CONFLICT_REPLACE, values);
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
    private long tryInsert(SupportSQLiteDatabase db,
            String table, int conflictAlgorithm, ContentValues values) {
        try {
            return db.insert(table, conflictAlgorithm, values);
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
            case SG_SHOW: {
                return builder.table(Tables.SG_SHOW);
            }
            case SG_SHOW_ID: {
                long id = SgShow2Columns.getId(uri);
                return builder.table(Tables.SG_SHOW).where(SgShow2Columns._ID + "=?",
                        String.valueOf(id));
            }
            case SG_SEASON: {
                return builder.table(Tables.SG_SEASON);
            }
            case SG_EPISODE: {
                return builder.table(Tables.SG_EPISODE);
            }
            case SHOWS: {
                return builder.table(Tables.SHOWS);
            }
            case SHOWS_ID: {
                final String showId = Shows.getShowId(uri);
                return builder.table(Tables.SHOWS).where(Shows._ID + "=?", showId);
            }
            case SHOWS_FILTERED: {
                final String filter = uri.getLastPathSegment();
                return builder.table(Tables.SHOWS).where(Shows.TITLE + " LIKE ?",
                        "%" + filter + "%");
            }
            case SHOWS_WITH_LAST_EPISODE: {
                return builder.table(Tables.SHOWS_JOIN_EPISODES_ON_LAST_EPISODE)
                        .mapToTable(Shows._ID, Tables.SHOWS)
                        .mapToTable(Shows.RATING_GLOBAL, Tables.SHOWS);
            }
            case SHOWS_WITH_NEXT_EPISODE: {
                return builder.table(Tables.SHOWS_JOIN_EPISODES_ON_NEXT_EPISODE)
                        .mapToTable(Shows._ID, Tables.SHOWS)
                        .mapToTable(Shows.RATING_GLOBAL, Tables.SHOWS);
            }
            case EPISODES: {
                return builder.table(Tables.EPISODES);
            }
            case EPISODES_ID: {
                final String episodeId = Episodes.getEpisodeId(uri);
                return builder.table(Tables.EPISODES).where(Episodes._ID + "=?", episodeId);
            }
            case EPISODES_OFSHOW: {
                final String showId = uri.getPathSegments().get(2);
                return builder.table(Tables.EPISODES).where(Shows.REF_SHOW_ID + "=?", showId);
            }
            case EPISODES_OFSEASON: {
                final String seasonId = uri.getPathSegments().get(2);
                return builder.table(Tables.EPISODES).where(Seasons.REF_SEASON_ID + "=?", seasonId);
            }
            case EPISODES_OFSEASON_WITHSHOW: {
                final String seasonId = uri.getPathSegments().get(3);
                return builder.table(Tables.EPISODES_JOIN_SHOWS)
                        .mapToTable(Episodes._ID, Tables.EPISODES)
                        .mapToTable(Episodes.RATING_GLOBAL, Tables.EPISODES)
                        .where(Seasons.REF_SEASON_ID + "=?", seasonId);
            }
            case EPISODES_WITHSHOW: {
                return builder.table(Tables.EPISODES_JOIN_SHOWS)
                        .mapToTable(Episodes._ID, Tables.EPISODES)
                        .mapToTable(Episodes.RATING_GLOBAL, Tables.EPISODES);
            }
            case EPISODES_ID_WITHSHOW: {
                final String episodeId = Episodes.getEpisodeId(uri);
                return builder.table(Tables.EPISODES_JOIN_SHOWS)
                        .mapToTable(Episodes._ID, Tables.EPISODES)
                        .mapToTable(Episodes.RATING_GLOBAL, Tables.EPISODES)
                        .where(Qualified.EPISODES_EPISODE_ID + "=?", episodeId);
            }
            case SEASONS: {
                return builder.table(Tables.SEASONS);
            }
            case SEASONS_ID: {
                final String seasonId = Seasons.getSeasonId(uri);
                return builder.table(Tables.SEASONS).where(Seasons._ID + "=?", seasonId);
            }
            case SEASONS_OFSHOW: {
                final String showId = uri.getPathSegments().get(2);
                return builder.table(Tables.SEASONS).where(Shows.REF_SHOW_ID + "=?", showId);
            }
            case LISTS: {
                return builder.table(Tables.LISTS);
            }
            case LISTS_ID: {
                final String listId = Lists.getId(uri);
                return builder.table(Tables.LISTS).where(Lists.LIST_ID + "=?", listId);
            }
            case LISTS_WITH_LIST_ITEM_ID: {
                final String itemId = uri.getPathSegments().get(2);
                return builder
                        .table(Tables.LISTS
                                + " LEFT OUTER JOIN (" + SubQuery.LISTS_LIST_ITEM_ID
                                + "'" + itemId
                                + "%') AS " + Tables.LIST_ITEMS + " ON "
                                + Qualified.LISTS_LIST_ID + "=" + Qualified.LIST_ITEMS_LIST_ID)
                        .mapToTable(Lists._ID, Tables.LISTS);
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
            case ACTIVITY: {
                return builder.table(Tables.ACTIVITY);
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

    private interface SubQuery {

        String LISTS_LIST_ITEM_ID = "SELECT * FROM "
                + Tables.LIST_ITEMS + " WHERE "
                + ListItems.LIST_ITEM_ID + " LIKE ";
    }

    /**
     * {@link SeriesGuideContract} fields that are fully qualified with a specific parent {@link
     * Tables}. Used when needed to work around SQL ambiguity.
     */
    private interface Qualified {

        String EPISODES_EPISODE_ID = Tables.EPISODES + "." + Episodes._ID;

        String LISTS_LIST_ID = Tables.LISTS + "." + Lists.LIST_ID;

        String LIST_ITEMS_LIST_ID = Tables.LIST_ITEMS + "." + Lists.LIST_ID;
    }
}
