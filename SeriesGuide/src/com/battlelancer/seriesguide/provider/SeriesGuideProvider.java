
package com.battlelancer.seriesguide.provider;

import com.battlelancer.seriesguide.SeriesDatabase;
import com.battlelancer.seriesguide.SeriesGuidePreferences;
import com.battlelancer.seriesguide.provider.SeriesContract.EpisodeSearch;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.util.SelectionBuilder;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

public class SeriesGuideProvider extends ContentProvider {

    private static final String TAG = "SeriesGuideProvider";

    private static final boolean LOGV = false;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final int SHOWS = 100;

    private static final int SHOWS_ID = 101;

    private static final int EPISODES = 200;

    private static final int EPISODES_ID = 201;

    private static final int EPISODES_OFSHOW = 202;

    private static final int EPISODES_OFSEASON = 203;

    private static final int EPISODES_OFSEASON_WITHSHOW = 204;

    private static final int EPISODES_WITHSHOW = 205;

    private static final int EPISODES_ID_WITHSHOW = 206;

    private static final int SEASONS = 300;

    private static final int SEASONS_ID = 301;

    private static final int SEASONS_OFSHOW = 302;

    private static final int EPISODESEARCH = 400;

    private static final int EPISODESEARCH_ID = 401;

    private static final int SEARCH_SUGGEST = 800;

    private static final int RENEW_FTSTABLE = 900;

    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri}
     * variations supported by this {@link ContentProvider}.
     */
    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = SeriesContract.CONTENT_AUTHORITY;

        // Shows
        matcher.addURI(authority, SeriesContract.PATH_SHOWS, SHOWS);
        matcher.addURI(authority, SeriesContract.PATH_SHOWS + "/*", SHOWS_ID);

        // Episodes
        matcher.addURI(authority, SeriesContract.PATH_EPISODES, EPISODES);
        matcher.addURI(authority, SeriesContract.PATH_EPISODES + "/" + SeriesContract.PATH_OFSEASON
                + "/" + SeriesContract.PATH_WITHSHOW + "/*", EPISODES_OFSEASON_WITHSHOW);
        matcher.addURI(authority, SeriesContract.PATH_EPISODES + "/" + SeriesContract.PATH_OFSEASON
                + "/*", EPISODES_OFSEASON);
        matcher.addURI(authority, SeriesContract.PATH_EPISODES + "/" + SeriesContract.PATH_OFSHOW
                + "/*", EPISODES_OFSHOW);
        matcher.addURI(authority,
                SeriesContract.PATH_EPISODES + "/" + SeriesContract.PATH_WITHSHOW,
                EPISODES_WITHSHOW);
        matcher.addURI(authority, SeriesContract.PATH_EPISODES + "/" + SeriesContract.PATH_WITHSHOW
                + "/*", EPISODES_ID_WITHSHOW);
        matcher.addURI(authority, SeriesContract.PATH_EPISODES + "/*", EPISODES_ID);

        // Seasons
        matcher.addURI(authority, SeriesContract.PATH_SEASONS, SEASONS);
        matcher.addURI(authority, SeriesContract.PATH_SEASONS + "/" + SeriesContract.PATH_OFSHOW
                + "/*", SEASONS_OFSHOW);
        matcher.addURI(authority, SeriesContract.PATH_SEASONS + "/*", SEASONS_ID);

        // Search
        matcher.addURI(authority, SeriesContract.PATH_EPISODESEARCH + "/"
                + SeriesContract.PATH_SEARCH, EPISODESEARCH);
        matcher.addURI(authority, SeriesContract.PATH_EPISODESEARCH + "/*", EPISODESEARCH_ID);

        // Suggestions
        matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);

        // Ops
        matcher.addURI(authority, SeriesContract.PATH_RENEWFTSTABLE, RENEW_FTSTABLE);

        return matcher;
    }

    private SeriesGuideDatabase mOpenHelper;

    private boolean mOnlyUnwatchedShows;

    @Override
    public boolean onCreate() {
        final Context context = getContext();
        mOpenHelper = new SeriesGuideDatabase(context);
        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(mImportListener);
        return true;
    }

    final OnSharedPreferenceChangeListener mImportListener = new OnSharedPreferenceChangeListener() {

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equalsIgnoreCase(SeriesGuidePreferences.KEY_DATABASEIMPORTED)) {
                if (sharedPreferences
                        .getBoolean(SeriesGuidePreferences.KEY_DATABASEIMPORTED, false)) {
                    mOpenHelper.close();
                    sharedPreferences.edit()
                            .putBoolean(SeriesGuidePreferences.KEY_DATABASEIMPORTED, false)
                            .commit();
                }
            } else if (key.equalsIgnoreCase(SeriesGuidePreferences.KEY_ONLY_UNWATCHED_SHOWS)) {
                mOnlyUnwatchedShows = sharedPreferences
                        .getBoolean(SeriesGuidePreferences.KEY_ONLY_UNWATCHED_SHOWS, false);
            }
        }
    };

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case SHOWS:
                return Shows.CONTENT_TYPE;
            case SHOWS_ID:
                return Shows.CONTENT_ITEM_TYPE;
            case EPISODES:
                return Episodes.CONTENT_TYPE;
            case EPISODES_OFSHOW:
                return Episodes.CONTENT_TYPE;
            case EPISODES_OFSEASON:
                return Episodes.CONTENT_TYPE;
            case EPISODES_OFSEASON_WITHSHOW:
                return Episodes.CONTENT_TYPE;
            case EPISODES_ID:
                return Episodes.CONTENT_ITEM_TYPE;
            case EPISODES_WITHSHOW:
                return Episodes.CONTENT_TYPE;
            case EPISODES_ID_WITHSHOW:
                return Episodes.CONTENT_ITEM_TYPE;
            case SEASONS:
                return Seasons.CONTENT_TYPE;
            case SEASONS_OFSHOW:
                return Seasons.CONTENT_TYPE;
            case SEASONS_ID:
                return Seasons.CONTENT_ITEM_TYPE;
            case SEARCH_SUGGEST:
                return SearchManager.SUGGEST_MIME_TYPE;
            case RENEW_FTSTABLE:
                // however there is nothing returned
                return Episodes.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (LOGV)
            Log.v(TAG, "insert(uri=" + uri + ", values=" + values.toString() + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case SHOWS: {
                db.insertOrThrow(Tables.SHOWS, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return Shows.buildShowUri(values.getAsString(Shows._ID));
            }
            case SEASONS: {
                db.insertOrThrow(Tables.SEASONS, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return Seasons.buildSeasonUri(values.getAsString(Seasons._ID));
            }
            case EPISODES: {
                db.insertOrThrow(Tables.EPISODES, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return Episodes.buildEpisodeUri(values.getAsString(Episodes._ID));
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (LOGV) {
            Log.v(TAG, "query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");
        }
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case RENEW_FTSTABLE: {
                SeriesGuideDatabase.onRenewFTSTable(db);
                return null;
            }
            case EPISODESEARCH: {
                if (selectionArgs == null) {
                    throw new IllegalArgumentException(
                            "selectionArgs must be provided for the Uri: " + uri);
                }
                return SeriesGuideDatabase.search(selectionArgs[0], db);
            }
            case SEARCH_SUGGEST: {
                if (selectionArgs == null) {
                    throw new IllegalArgumentException(
                            "selectionArgs must be provided for the Uri: " + uri);
                }
                return SeriesGuideDatabase.getSuggestions(selectionArgs[0], db);
            }
            default: {
                // Most cases are handled with simple SelectionBuilder
                final SelectionBuilder builder = buildExpandedSelection(uri, match);
                Cursor query = builder.where(selection, selectionArgs).query(db, projection,
                        sortOrder);
                query.setNotificationUri(getContext().getContentResolver(), uri);
                return query;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (LOGV)
            Log.v(TAG, "update(uri=" + uri + ", values=" + values.toString() + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).update(db, values);
        getContext().getContentResolver().notifyChange(uri, null);
        return retVal;
    }

    /** {@inheritDoc} */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (LOGV)
            Log.v(TAG, "delete(uri=" + uri + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).delete(db);
        getContext().getContentResolver().notifyChange(uri, null);
        return retVal;
    }

    /**
     * Apply the given set of {@link ContentProviderOperation}, executing inside
     * a {@link SQLiteDatabase} transaction. All changes will be rolled back if
     * any single one fails.
     */
    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                results[i] = operations.get(i).apply(this, results, i);
            }
            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Build a simple {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually enough to support {@link #insert},
     * {@link #update}, and {@link #delete} operations.
     */
    private SelectionBuilder buildSimpleSelection(Uri uri) {
        final SelectionBuilder builder = new SelectionBuilder();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case SHOWS_ID: {
                final String showId = Shows.getShowId(uri);
                return builder.table(Tables.SHOWS).where(Shows._ID + "=?", showId);
            }
            case EPISODES_OFSHOW: {
                final String showId = uri.getPathSegments().get(2);
                return builder.table(Tables.EPISODES).where(Shows.REF_SHOW_ID + "=?", showId);
            }
            case EPISODES_OFSEASON: {
                final String seasonId = uri.getPathSegments().get(2);
                return builder.table(Tables.EPISODES).where(Seasons.REF_SEASON_ID + "=?", seasonId);
            }
            case EPISODES_ID: {
                final String episodeId = Episodes.getEpisodeId(uri);
                return builder.table(Tables.EPISODES).where(Episodes._ID + "=?", episodeId);
            }
            case SEASONS_OFSHOW: {
                final String showId = uri.getPathSegments().get(2);
                return builder.table(Tables.SEASONS).where(Shows.REF_SHOW_ID + "=?", showId);
            }
            case SEASONS_ID: {
                final String seasonId = Seasons.getSeasonId(uri);
                return builder.table(Tables.SEASONS).where(Seasons._ID + "=?", seasonId);
            }
            case EPISODESEARCH_ID: {
                final String rowid = EpisodeSearch.getDocId(uri);
                return builder.table(Tables.EPISODES_SEARCH).where(EpisodeSearch._DOCID + "=?",
                        rowid);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    /**
     * Build an advanced {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually only used by {@link #query}, since it
     * performs table joins useful for {@link Cursor} data.
     */
    private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            case SHOWS: {
                if (mOnlyUnwatchedShows) {
                    return builder.table(Tables.SHOWS)
                                  .where(Shows.NEXTAIRDATE + "!=?", SeriesDatabase.UNKNOWN_NEXT_AIR_DATE)
                                  .where("julianday(" + Shows.NEXTAIRDATE + ") <= julianday('now')");
                }
                return builder.table(Tables.SHOWS);
            }
            case SHOWS_ID: {
                final String showId = Shows.getShowId(uri);
                return builder.table(Tables.SHOWS).where(Shows._ID + "=?", showId);
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
                        .mapToTable(Episodes.RATING, Tables.EPISODES)
                        .where(Seasons.REF_SEASON_ID + "=?", seasonId);
            }
            case EPISODES_ID: {
                final String episodeId = Episodes.getEpisodeId(uri);
                return builder.table(Tables.EPISODES).where(Episodes._ID + "=?", episodeId);
            }
            case EPISODES_WITHSHOW: {
                return builder.table(Tables.EPISODES_JOIN_SHOWS)
                        .mapToTable(Episodes._ID, Tables.EPISODES)
                        .mapToTable(Episodes.RATING, Tables.EPISODES);
            }
            case EPISODES_ID_WITHSHOW: {
                final String episodeId = Episodes.getEpisodeId(uri);
                return builder.table(Tables.EPISODES_JOIN_SHOWS)
                        .mapToTable(Episodes._ID, Tables.EPISODES)
                        .mapToTable(Episodes.RATING, Tables.EPISODES)
                        .where(Tables.EPISODES + "." + Episodes._ID + "=?", episodeId);
            }
            case SEASONS_OFSHOW: {
                final String showId = uri.getPathSegments().get(2);
                return builder.table(Tables.SEASONS).where(Shows.REF_SHOW_ID + "=?", showId);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }
}
