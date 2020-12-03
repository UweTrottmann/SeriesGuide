package com.battlelancer.seriesguide.provider;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.ActivityColumns;
import static com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;

import android.app.SearchManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.EpisodeSearch;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.EpisodeSearchColumns;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.EpisodesColumns;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.JobsColumns;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemsColumns;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListsColumns;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.MoviesColumns;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SeasonsColumns;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ShowsColumns;
import com.battlelancer.seriesguide.util.DBUtils;
import timber.log.Timber;

public class SeriesGuideDatabase {

    public static final String DATABASE_NAME = "seriesdatabase";

    public static final int DBVER_17_FAVORITES = 17;
    public static final int DBVER_18_NEXTAIRDATETEXT = 18;
    public static final int DBVER_19_SETOTALCOUNT = 19;
    public static final int DBVER_20_SYNC = 20;
    public static final int DBVER_21_AIRTIMECOLUMN = 21;
    public static final int DBVER_22_PERSHOWUPDATEDATE = 22;
    public static final int DBVER_23_HIDDENSHOWS = 23;
    public static final int DBVER_24_AIRTIMEREFORM = 24;
    public static final int DBVER_25_NEXTAIRDATEMS = 25;
    public static final int DBVER_26_COLLECTED = 26;
    public static final int DBVER_27_IMDBIDSLASTEDIT = 27;
    public static final int DBVER_28_LISTS = 28;
    public static final int DBVER_29_GETGLUE_CHECKIN_FIX = 29;
    public static final int DBVER_30_ABSOLUTE_NUMBERS = 30;
    public static final int DBVER_31_LAST_WATCHED_ID = 31;
    public static final int DBVER_32_MOVIES = 32;
    public static final int DBVER_33_IGNORE_ARTICLE_SORT = 33;

    /**
     * Changes for trakt v2 compatibility, also for storing ratings offline.
     *
     * Shows:
     *
     * <ul>
     *
     * <li>changed release time encoding
     *
     * <li>changed release week day encoding
     *
     * <li>first release date now includes time
     *
     * <li>added time zone
     *
     * <li>added rating votes
     *
     * <li>added user rating
     *
     * </ul>
     *
     * Episodes:
     *
     * <ul>
     *
     * <li>added rating votes
     *
     * <li>added user rating
     *
     * </ul>
     *
     * Movies:
     *
     * <ul>
     *
     * <li>added user rating
     *
     * </ul>
     */
    public static final int DBVER_34_TRAKT_V2 = 34;

    /**
     * Added activity table to store recently watched episodes.
     */
    public static final int DBVER_35_ACTIVITY_TABLE = 35;

    /**
     * Support for re-ordering lists: added new column to lists table.
     */
    public static final int DBVER_36_ORDERABLE_LISTS = 36;

    /**
     * Added language column to shows table.
     */
    public static final int DBVER_37_LANGUAGE_PER_SERIES = 37;

    /**
     * Added trakt id column to shows table.
     */
    static final int DBVER_38_SHOW_TRAKT_ID = 38;

    /**
     * Added last watched time and unwatched counter to shows table.
     */
    static final int DBVER_39_SHOW_LAST_WATCHED = 39;

    /**
     * Add {@link Shows#NOTIFY} flag to shows table.
     */
    static final int DBVER_40_NOTIFY_PER_SHOW = 40;

    /**
     * Add {@link Episodes#LAST_UPDATED} flag to episodes table.
     */
    static final int DBVER_41_EPISODE_LAST_UPDATED = 41;

    /**
     * Added jobs table.
     */
    public static final int DBVER_42_JOBS = 42;

    public static final int DATABASE_VERSION = DBVER_42_JOBS;

    /**
     * Qualifies column names by prefixing their {@link Tables} name.
     */
    public interface Qualified {

        String SHOWS_ID = Tables.SHOWS + "." + Shows._ID;
        String SHOWS_LAST_EPISODE = Tables.SHOWS + "." + Shows.LASTWATCHEDID;
        String SHOWS_NEXT_EPISODE = Tables.SHOWS + "." + Shows.NEXTEPISODE;
        String EPISODES_ID = Tables.EPISODES + "." + Episodes._ID;
        String EPISODES_SHOW_ID = Tables.EPISODES + "." + Shows.REF_SHOW_ID;
        String SEASONS_ID = Tables.SEASONS + "." + Seasons._ID;
        String SEASONS_SHOW_ID = Tables.SEASONS + "." + Shows.REF_SHOW_ID;
        String LIST_ITEMS_REF_ID = Tables.LIST_ITEMS + "." + ListItems.ITEM_REF_ID;
    }

    public interface Tables {

        String SHOWS = "series";

        String SEASONS = "seasons";

        String EPISODES = "episodes";

        String SHOWS_JOIN_EPISODES_ON_LAST_EPISODE = SHOWS + " LEFT OUTER JOIN " + EPISODES
                + " ON " + Qualified.SHOWS_LAST_EPISODE + "=" + Qualified.EPISODES_ID;

        String SHOWS_JOIN_EPISODES_ON_NEXT_EPISODE = SHOWS + " LEFT OUTER JOIN " + EPISODES
                + " ON " + Qualified.SHOWS_NEXT_EPISODE + "=" + Qualified.EPISODES_ID;

        String SEASONS_JOIN_SHOWS = SEASONS + " LEFT OUTER JOIN " + SHOWS
                + " ON " + Qualified.SEASONS_SHOW_ID + "=" + Qualified.SHOWS_ID;

        String EPISODES_JOIN_SHOWS = EPISODES + " LEFT OUTER JOIN " + SHOWS
                + " ON " + Qualified.EPISODES_SHOW_ID + "=" + Qualified.SHOWS_ID;

        String EPISODES_SEARCH = "searchtable";

        String LISTS = "lists";

        String LIST_ITEMS = "listitems";

        String LIST_ITEMS_WITH_DETAILS = "("
                // shows
                + "SELECT " + Selections.SHOWS_COLUMNS + " FROM "
                + "("
                + Selections.LIST_ITEMS_SHOWS
                + " LEFT OUTER JOIN " + Tables.SHOWS
                + " ON " + Qualified.LIST_ITEMS_REF_ID + "=" + Qualified.SHOWS_ID
                + ")"
                // seasons
                + " UNION SELECT " + Selections.SEASONS_COLUMNS + " FROM "
                + "("
                + Selections.LIST_ITEMS_SEASONS
                + " LEFT OUTER JOIN " + "(" + SEASONS_JOIN_SHOWS + ") AS " + Tables.SEASONS
                + " ON " + Qualified.LIST_ITEMS_REF_ID + "=" + Qualified.SEASONS_ID
                + ")"
                // episodes
                + " UNION SELECT " + Selections.EPISODES_COLUMNS + " FROM "
                + "("
                + Selections.LIST_ITEMS_EPISODES
                + " LEFT OUTER JOIN " + "(" + EPISODES_JOIN_SHOWS + ") AS " + Tables.EPISODES
                + " ON " + Qualified.LIST_ITEMS_REF_ID + "=" + Qualified.EPISODES_ID
                + ")"
                //
                + ")";

        String MOVIES = "movies";

        String ACTIVITY = "activity";

        String JOBS = "jobs";
    }

    private interface Selections {

        String LIST_ITEMS_SHOWS = "(SELECT " + Selections.LIST_ITEMS_COLUMNS_INTERNAL
                + " FROM " + Tables.LIST_ITEMS
                + " WHERE " + ListItems.SELECTION_SHOWS + ")"
                + " AS " + Tables.LIST_ITEMS;

        String LIST_ITEMS_SEASONS = "(SELECT " + Selections.LIST_ITEMS_COLUMNS_INTERNAL
                + " FROM " + Tables.LIST_ITEMS
                + " WHERE " + ListItems.SELECTION_SEASONS + ")"
                + " AS " + Tables.LIST_ITEMS;

        String LIST_ITEMS_EPISODES = "(SELECT " + Selections.LIST_ITEMS_COLUMNS_INTERNAL
                + " FROM " + Tables.LIST_ITEMS
                + " WHERE " + ListItems.SELECTION_EPISODES + ")"
                + " AS " + Tables.LIST_ITEMS;

        String LIST_ITEMS_COLUMNS_INTERNAL =
                ListItems._ID + " as listitem_id,"
                        + ListItems.LIST_ITEM_ID + ","
                        + Lists.LIST_ID + ","
                        + ListItems.TYPE + ","
                        + ListItems.ITEM_REF_ID;

        String COMMON_LIST_ITEMS_COLUMNS =
                // from list items table
                "listitem_id as " + ListItems._ID + ","
                        + ListItems.LIST_ITEM_ID + ","
                        + Lists.LIST_ID + ","
                        + ListItems.TYPE + ","
                        + ListItems.ITEM_REF_ID + ","
                        // from shows table
                        + Shows.TITLE + ","
                        + Shows.TITLE_NOARTICLE + ","
                        + Shows.POSTER_SMALL + ","
                        + Shows.NETWORK + ","
                        + Shows.STATUS + ","
                        + Shows.FAVORITE + ","
                        + Shows.RELEASE_WEEKDAY + ","
                        + Shows.RELEASE_TIMEZONE + ","
                        + Shows.RELEASE_COUNTRY + ","
                        + Shows.LASTWATCHED_MS + ","
                        + Shows.UNWATCHED_COUNT;

        String SHOWS_COLUMNS = COMMON_LIST_ITEMS_COLUMNS + ","
                + Qualified.SHOWS_ID + " as " + Shows.REF_SHOW_ID + ","
                + Shows.OVERVIEW + ","
                + Shows.RELEASE_TIME + ","
                + Shows.NEXTTEXT + ","
                + Shows.NEXTEPISODE + ","
                + Shows.NEXTAIRDATEMS;

        String SEASONS_COLUMNS = COMMON_LIST_ITEMS_COLUMNS + ","
                + Shows.REF_SHOW_ID + ","
                + Seasons.COMBINED + " as " + Shows.OVERVIEW + ","
                + Shows.RELEASE_TIME + ","
                + Shows.NEXTTEXT + ","
                + Shows.NEXTEPISODE + ","
                + Shows.NEXTAIRDATEMS;

        String EPISODES_COLUMNS = COMMON_LIST_ITEMS_COLUMNS + ","
                + Shows.REF_SHOW_ID + ","
                + Episodes.TITLE + " as " + Shows.OVERVIEW + ","
                + Episodes.FIRSTAIREDMS + " as " + Shows.RELEASE_TIME + ","
                + Episodes.SEASON + " as " + Shows.NEXTTEXT + ","
                + Episodes.NUMBER + " as " + Shows.NEXTEPISODE + ","
                + Episodes.FIRSTAIREDMS + " as " + Shows.NEXTAIRDATEMS;
    }

    static final String CREATE_SEARCH_TABLE = "CREATE VIRTUAL TABLE "
            + Tables.EPISODES_SEARCH + " USING fts4("

            // set episodes table as external content table
            + "content='" + Tables.EPISODES + "',"

            + EpisodeSearchColumns.TITLE + ","

            + EpisodeSearchColumns.OVERVIEW

            + ");";

    @VisibleForTesting
    public static final String ACTIVITY_TABLE = "activity ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "activity_episode TEXT NOT NULL,"
            + "activity_show TEXT NOT NULL,"
            + "activity_time INTEGER NOT NULL,"
            + "UNIQUE (activity_episode) ON CONFLICT REPLACE"
            + ");";

    @VisibleForTesting
    public static final String JOBS_TABLE = "jobs ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "job_created_at INTEGER,"
            + "job_type INTEGER,"
            + "job_extras BLOB,"
            + "UNIQUE (job_created_at) ON CONFLICT REPLACE"
            + ");";

    /**
     * See {@link #DBVER_42_JOBS}.
     */
    static void upgradeToFortyTwo(@NonNull SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + JOBS_TABLE);
    }

    /**
     * See {@link #DBVER_41_EPISODE_LAST_UPDATED}.
     */
    static void upgradeToFortyOne(@NonNull SupportSQLiteDatabase db) {
        if (isTableColumnMissing(db, Tables.EPISODES, Episodes.LAST_UPDATED)) {
            db.execSQL("ALTER TABLE " + Tables.EPISODES + " ADD COLUMN "
                    + Episodes.LAST_UPDATED + " INTEGER DEFAULT 0;");
        }
    }

    /**
     * See {@link #DBVER_40_NOTIFY_PER_SHOW}.
     */
    static void upgradeToForty(@NonNull SupportSQLiteDatabase db) {
        if (isTableColumnMissing(db, Tables.SHOWS, Shows.NOTIFY)) {
            db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN "
                    + Shows.NOTIFY + " INTEGER DEFAULT 1;");

//            // check if notifications should be enabled only for favorite shows
//            // noinspection deprecation
//            boolean favoritesOnly = NotificationSettings.isNotifyAboutFavoritesOnly(context);
//            if (favoritesOnly) {
//                // disable notifications for all but favorite shows
//                ContentValues values = new ContentValues();
//                values.put(Shows.NOTIFY, 0);
//                db.update(Tables.SHOWS, SQLiteDatabase.CONFLICT_NONE, values,
//                        Shows.SELECTION_NOT_FAVORITES, null);
//            }
        }
    }

    /**
     * See {@link #DBVER_39_SHOW_LAST_WATCHED}.
     */
    static void upgradeToThirtyNine(@NonNull SupportSQLiteDatabase db) {
        if (isTableColumnMissing(db, Tables.SHOWS, Shows.LASTWATCHED_MS)) {
            db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN "
                    + Shows.LASTWATCHED_MS + " INTEGER DEFAULT 0;");
        }
        if (isTableColumnMissing(db, Tables.SHOWS, Shows.UNWATCHED_COUNT)) {
            db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN "
                    + Shows.UNWATCHED_COUNT + " INTEGER DEFAULT " + DBUtils.UNKNOWN_UNWATCHED_COUNT
                    + ";");
        }
    }

    /**
     * See {@link #DBVER_38_SHOW_TRAKT_ID}.
     */
    static void upgradeToThirtyEight(@NonNull SupportSQLiteDatabase db) {
        if (isTableColumnMissing(db, Tables.SHOWS, Shows.TRAKT_ID)) {
            db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN "
                    + Shows.TRAKT_ID + " INTEGER DEFAULT 0;");
        }
    }

    /**
     * See {@link #DBVER_37_LANGUAGE_PER_SERIES}.
     */
    static void upgradeToThirtySeven(@NonNull SupportSQLiteDatabase db) {
        if (isTableColumnMissing(db, Tables.SHOWS, Shows.LANGUAGE)) {
            db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN "
                    + Shows.LANGUAGE + " TEXT DEFAULT '';");
        }
    }

    /**
     * See {@link #DBVER_36_ORDERABLE_LISTS}.
     */
    static void upgradeToThirtySix(@NonNull SupportSQLiteDatabase db) {
        if (isTableColumnMissing(db, Tables.LISTS, Lists.ORDER)) {
            db.execSQL("ALTER TABLE " + Tables.LISTS + " ADD COLUMN "
                    + Lists.ORDER + " INTEGER DEFAULT 0;");
        }
    }

    /**
     * See {@link #DBVER_35_ACTIVITY_TABLE}.
     */
    static void upgradeToThirtyFive(@NonNull SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + ACTIVITY_TABLE);
    }

    // Upgrading from versions older than 34 is no longer supported. Keeping upgrade code for reference.

//    /**
//     * See {@link #DBVER_34_TRAKT_V2}.
//     */
//    private static void upgradeToThirtyFour(SQLiteDatabase db) {
//        // add new columns
//        db.beginTransaction();
//        try {
//            // shows
//            if (isTableColumnMissing(db, Tables.SHOWS, Shows.RELEASE_TIMEZONE)) {
//                db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN "
//                        + Shows.RELEASE_TIMEZONE + " TEXT;");
//            }
//            if (isTableColumnMissing(db, Tables.SHOWS, Shows.RATING_VOTES)) {
//                db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN "
//                        + Shows.RATING_VOTES + " INTEGER;");
//            }
//            if (isTableColumnMissing(db, Tables.SHOWS, Shows.RATING_USER)) {
//                db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN "
//                        + Shows.RATING_USER + " INTEGER;");
//            }
//
//            // episodes
//            if (isTableColumnMissing(db, Tables.EPISODES, Episodes.RATING_VOTES)) {
//                db.execSQL("ALTER TABLE " + Tables.EPISODES + " ADD COLUMN "
//                        + Episodes.RATING_VOTES + " INTEGER;");
//            }
//            if (isTableColumnMissing(db, Tables.EPISODES, Episodes.RATING_USER)) {
//                db.execSQL("ALTER TABLE " + Tables.EPISODES + " ADD COLUMN "
//                        + Episodes.RATING_USER + " INTEGER;");
//            }
//
//            // movies
//            if (isTableColumnMissing(db, Tables.MOVIES, Movies.RATING_USER)) {
//                db.execSQL("ALTER TABLE " + Tables.MOVIES + " ADD COLUMN "
//                        + Movies.RATING_USER + " INTEGER;");
//            }
//
//            db.setTransactionSuccessful();
//        } finally {
//            db.endTransaction();
//        }
//
//        // migrate existing data to new formats
//        Cursor query = db.query(Tables.SHOWS,
//                new String[]{Shows._ID, Shows.RELEASE_TIME, Shows.RELEASE_WEEKDAY}, null, null,
//                null, null, null);
//
//        // create calendar, set to custom time zone
//        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT-08:00"));
//        ContentValues values = new ContentValues();
//
//        db.beginTransaction();
//        try {
//            while (query.moveToNext()) {
//                // time changed from ms to encoded local time
//                long timeOld = query.getLong(1);
//                int timeNew;
//                if (timeOld != -1) {
//                    calendar.setTimeInMillis(timeOld);
//                    int hour = calendar.get(Calendar.HOUR_OF_DAY);
//                    int minute = calendar.get(Calendar.MINUTE);
//                    timeNew = hour * 100 + minute;
//                } else {
//                    timeNew = -1;
//                }
//                values.put(Shows.RELEASE_TIME, timeNew);
//
//                // week day changed from string to int
//                String weekDayOld = query.getString(2);
//                int weekDayNew = TimeTools.parseShowReleaseWeekDay(weekDayOld);
//                values.put(Shows.RELEASE_WEEKDAY, weekDayNew);
//
//                db.update(Tables.SHOWS, values, Shows._ID + "=" + query.getInt(0), null);
//            }
//
//            db.setTransactionSuccessful();
//        } finally {
//            db.endTransaction();
//            query.close();
//        }
//    }
//
//    /**
//     * Add shows and movies title column without articles.
//     */
//    private static void upgradeToThirtyThree(SQLiteDatabase db) {
//        /*
//        Add new columns. Added existence checks as 14.0.3 update botched upgrade process.
//         */
//        if (isTableColumnMissing(db, Tables.SHOWS, Shows.TITLE_NOARTICLE)) {
//            db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + Shows.TITLE_NOARTICLE
//                    + " TEXT;");
//        }
//        if (isTableColumnMissing(db, Tables.MOVIES, Movies.TITLE_NOARTICLE)) {
//            db.execSQL("ALTER TABLE " + Tables.MOVIES + " ADD COLUMN " + Movies.TITLE_NOARTICLE
//                    + " TEXT;");
//        }
//
//        // shows
//        Cursor shows = db.query(Tables.SHOWS, new String[]{Shows._ID, Shows.TITLE}, null, null,
//                null, null, null);
//        ContentValues newTitleValues = new ContentValues();
//        if (shows != null) {
//            db.beginTransaction();
//            try {
//                while (shows.moveToNext()) {
//                    // put overwrites previous value
//                    newTitleValues.put(Shows.TITLE_NOARTICLE,
//                            DBUtils.trimLeadingArticle(shows.getString(1)));
//                    db.update(Tables.SHOWS, newTitleValues, Shows._ID + "=" + shows.getInt(0),
//                            null);
//                }
//
//                db.setTransactionSuccessful();
//            } finally {
//                db.endTransaction();
//            }
//            shows.close();
//        }
//
//        newTitleValues.clear();
//
//        // movies
//        Cursor movies = db.query(Tables.MOVIES, new String[]{Movies._ID, Movies.TITLE}, null,
//                null, null, null, null);
//        if (movies != null) {
//            db.beginTransaction();
//            try {
//                while (movies.moveToNext()) {
//                    // put overwrites previous value
//                    newTitleValues.put(Movies.TITLE_NOARTICLE,
//                            DBUtils.trimLeadingArticle(movies.getString(1)));
//                    db.update(Tables.MOVIES, newTitleValues, Movies._ID + "=" + movies.getInt(0),
//                            null);
//                }
//
//                db.setTransactionSuccessful();
//            } finally {
//                db.endTransaction();
//            }
//            movies.close();
//        }
//    }
//
//    /**
//     * Add movies table.
//     */
//    private static void upgradeToThirtyTwo(SQLiteDatabase db) {
//        if (!isTableExisting(db, Tables.MOVIES)) {
//            db.execSQL(CREATE_MOVIES_TABLE);
//        }
//    }
//
//    // Must be watched and have an airdate
//    private static final String LATEST_SELECTION = Episodes.WATCHED + "=1 AND "
//            + Episodes.FIRSTAIREDMS + "!=-1 AND " + Shows.REF_SHOW_ID + "=?";
//
//    // Latest aired first (ensures we get specials), if equal sort by season,
//    // then number
//    private static final String LATEST_ORDER = Episodes.FIRSTAIREDMS + " DESC,"
//            + Episodes.SEASON + " DESC,"
//            + Episodes.NUMBER + " DESC";
//
//    /**
//     * Add {@link Shows} column to store the last watched episode id for better prediction of next
//     * episode.
//     */
//    private static void upgradeToThirtyOne(SQLiteDatabase db) {
//        if (isTableColumnMissing(db, Tables.SHOWS, Shows.LASTWATCHEDID)) {
//            db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + Shows.LASTWATCHEDID
//                    + " INTEGER DEFAULT 0;");
//        }
//
//        // pre populate with latest watched episode ids
//        ContentValues values = new ContentValues();
//        final Cursor shows = db.query(Tables.SHOWS, new String[]{
//                Shows._ID,
//        }, null, null, null, null, null);
//        if (shows != null) {
//            db.beginTransaction();
//            try {
//                while (shows.moveToNext()) {
//                    final String showId = shows.getString(0);
//                    final Cursor highestWatchedEpisode = db.query(Tables.EPISODES, new String[]{
//                            Episodes._ID
//                    }, LATEST_SELECTION, new String[]{
//                            showId
//                    }, null, null, LATEST_ORDER);
//
//                    if (highestWatchedEpisode != null) {
//                        if (highestWatchedEpisode.moveToFirst()) {
//                            values.put(Shows.LASTWATCHEDID, highestWatchedEpisode.getInt(0));
//                            db.update(Tables.SHOWS, values, Shows._ID + "=?", new String[]{
//                                    showId
//                            });
//                            values.clear();
//                        }
//
//                        highestWatchedEpisode.close();
//                    }
//                }
//
//                db.setTransactionSuccessful();
//            } finally {
//                db.endTransaction();
//            }
//
//            shows.close();
//        }
//    }
//
//    /**
//     * Add {@link Episodes} column to store absolute episode number.
//     */
//    private static void upgradeToThirty(SQLiteDatabase db) {
//        if (isTableColumnMissing(db, Tables.EPISODES, Episodes.ABSOLUTE_NUMBER)) {
//            db.execSQL("ALTER TABLE " + Tables.EPISODES + " ADD COLUMN "
//                    + Episodes.ABSOLUTE_NUMBER + " INTEGER;");
//        }
//    }
//
//    /**
//     * Add tables to store lists and list items.
//     */
//    private static void upgradeToTwentyEight(SQLiteDatabase db) {
//        db.execSQL(CREATE_LISTS_TABLE);
//
//        db.execSQL(CREATE_LIST_ITEMS_TABLE);
//    }
//
//    /**
//     * Add {@link Episodes} columns for storing its IMDb id and last time of edit on theTVDB.com.
//     * Add {@link Shows} column for storing last time of edit as well.
//     */
//    private static void upgradeToTwentySeven(SQLiteDatabase db) {
//        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.LASTEDIT
//                + " INTEGER DEFAULT 0;");
//        db.execSQL("ALTER TABLE " + Tables.EPISODES + " ADD COLUMN " + EpisodesColumns.IMDBID
//                + " TEXT DEFAULT '';");
//        db.execSQL("ALTER TABLE " + Tables.EPISODES + " ADD COLUMN " + EpisodesColumns.LAST_EDITED
//                + " INTEGER DEFAULT 0;");
//    }
//
//    /**
//     * Add a {@link Episodes} column for storing whether an episode was collected in digital or
//     * physical form.
//     */
//    private static void upgradeToTwentySix(SQLiteDatabase db) {
//        db.execSQL("ALTER TABLE " + Tables.EPISODES + " ADD COLUMN " + EpisodesColumns.COLLECTED
//                + " INTEGER DEFAULT 0;");
//    }
//
//    /**
//     * Add a {@link Shows} column for storing the next air date in ms as integer data type rather
//     * than as text.
//     */
//    private static void upgradeToTwentyFive(SQLiteDatabase db) {
//        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.NEXTAIRDATEMS
//                + " INTEGER DEFAULT 0;");
//    }
//
//    /**
//     * Adds a column to the {@link Tables#EPISODES} table to store the airdate and possibly time in
//     * milliseconds.
//     */
//    private static void upgradeToTwentyFour(SQLiteDatabase db) {
//        db.execSQL("ALTER TABLE " + Tables.EPISODES + " ADD COLUMN " + EpisodesColumns.FIRSTAIREDMS
//                + " INTEGER DEFAULT -1;");
//
//        // populate the new column from existing data
//        final Cursor shows = db.query(Tables.SHOWS, new String[]{
//                Shows._ID
//        }, null, null, null, null, null);
//
//        while (shows.moveToNext()) {
//            final String showId = shows.getString(0);
//
//            //noinspection deprecation
//            final Cursor episodes = db.query(Tables.EPISODES, new String[]{
//                    Episodes._ID, Episodes.FIRSTAIRED
//            }, Shows.REF_SHOW_ID + "=?", new String[]{
//                    showId
//            }, null, null, null);
//
//            db.beginTransaction();
//            try {
//                ContentValues values = new ContentValues();
//                ZoneId defaultShowTimeZone = TimeTools.getDateTimeZone(null);
//                LocalTime defaultShowReleaseTime = TimeTools.getShowReleaseTime(-1);
//                String deviceTimeZone = TimeZone.getDefault().getID();
//                while (episodes.moveToNext()) {
//                    String firstAired = episodes.getString(1);
//                    long episodeAirtime = TimeTools.parseEpisodeReleaseDate(null,
//                            defaultShowTimeZone, firstAired, defaultShowReleaseTime, null, null,
//                            deviceTimeZone);
//
//                    values.put(Episodes.FIRSTAIREDMS, episodeAirtime);
//                    db.update(Tables.EPISODES, values, Episodes._ID + "=?", new String[]{
//                            episodes.getString(0)
//                    });
//                    values.clear();
//                }
//                db.setTransactionSuccessful();
//            } finally {
//                db.endTransaction();
//            }
//
//            episodes.close();
//        }
//
//        shows.close();
//    }
//
//    /**
//     * Adds a column to the {@link Tables#SHOWS} table similar to the favorite boolean, but to allow
//     * hiding shows.
//     */
//    private static void upgradeToTwentyThree(SQLiteDatabase db) {
//        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.HIDDEN
//                + " INTEGER DEFAULT 0;");
//    }
//
//    /**
//     * Add a column to store the last time a show has been updated to allow for more precise control
//     * over which shows should get updated. This is in conjunction with a 7 day limit when a show
//     * will get updated regardless if it has been marked as updated or not.
//     */
//    private static void upgradeToTwentyTwo(SQLiteDatabase db) {
//        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.LASTUPDATED
//                + " INTEGER DEFAULT 0;");
//    }
//
//    private static void upgradeToTwentyOne(SQLiteDatabase db) {
//        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.RELEASE_COUNTRY
//                + " TEXT DEFAULT '';");
//    }
//
//    private static void upgradeToTwenty(SQLiteDatabase db) {
//        db.execSQL(
//                "ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.HEXAGON_MERGE_COMPLETE
//                        + " INTEGER DEFAULT 1;");
//    }
//
//    /**
//     * In version 19 the season integer column totalcount was added.
//     */
//    private static void upgradeToNineteen(SQLiteDatabase db) {
//        db.execSQL("ALTER TABLE " + Tables.SEASONS + " ADD COLUMN " + SeasonsColumns.TOTALCOUNT
//                + " INTEGER DEFAULT 0;");
//    }
//
//    /**
//     * In version 18 the series text column nextairdatetext was added.
//     */
//    private static void upgradeToEighteen(SQLiteDatabase db) {
//        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.NEXTAIRDATETEXT
//                + " TEXT DEFAULT '';");
//
//        // convert status text to 0/1 integer
//        final Cursor shows = db.query(Tables.SHOWS, new String[]{
//                Shows._ID, Shows.STATUS
//        }, null, null, null, null, null);
//        final ContentValues values = new ContentValues();
//        String status;
//
//        db.beginTransaction();
//        try {
//            while (shows.moveToNext()) {
//                status = shows.getString(1);
//                if (status.length() == 10) {
//                    status = "1";
//                } else if (status.length() == 5) {
//                    status = "0";
//                } else {
//                    status = "";
//                }
//                values.put(Shows.STATUS, status);
//                db.update(Tables.SHOWS, values, Shows._ID + "=?", new String[]{
//                        shows.getString(0)
//                });
//                values.clear();
//            }
//            db.setTransactionSuccessful();
//        } finally {
//            db.endTransaction();
//        }
//
//        shows.close();
//    }
//
//    /**
//     * In version 17 the series boolean column favorite was added.
//     */
//    private static void upgradeToSeventeen(@NonNull SupportSQLiteDatabase db) {
//        db.execSQL("ALTER TABLE " + Tables.SHOWS + " ADD COLUMN " + ShowsColumns.FAVORITE
//                + " INTEGER DEFAULT 0;");
//    }

    /**
     * Drops the current {@link Tables#EPISODES_SEARCH} table and re-creates it with current data
     * from {@link Tables#EPISODES}.
     */
    public static void rebuildFtsTable(SupportSQLiteDatabase db) {
        if (!recreateFtsTable(db)) {
            return;
        }

        rebuildFtsTableJellyBean(db);
    }

    /**
     * Works with FTS4 search table.
     */
    private static void rebuildFtsTableJellyBean(SupportSQLiteDatabase db) {
        try {
            db.beginTransaction();
            try {
                db.execSQL("INSERT OR IGNORE INTO " + Tables.EPISODES_SEARCH
                        + "(" + Tables.EPISODES_SEARCH + ") VALUES('rebuild')");
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (SQLiteException e) {
            Timber.e(e, "rebuildFtsTableJellyBean: failed to populate table.");
            DBUtils.postDatabaseError(e);
        }
    }

    private static boolean recreateFtsTable(SupportSQLiteDatabase db) {
        try {
            db.beginTransaction();
            try {
                db.execSQL("drop table if exists " + Tables.EPISODES_SEARCH);
                db.execSQL(CREATE_SEARCH_TABLE);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            return true;
        } catch (SQLiteException e) {
            Timber.e(e, "recreateFtsTable: failed.");
            DBUtils.postDatabaseError(e);
            return false;
        }
    }

    // This should match QUERY_SEARCH_EPISODES//
    public interface EpisodeSearchQuery {
        String[] PROJECTION = new String[]{
                Episodes._ID,
                Episodes.TITLE,
                Episodes.NUMBER,
                Episodes.SEASON,
                Episodes.WATCHED,
                Episodes.OVERVIEW,
                Shows.TITLE,
                Shows.POSTER_SMALL
        };

        int _ID = 0;
        int TITLE = 1;
        int NUMBER = 2;
        int SEASON = 3;
        int WATCHED = 4;
        int OVERVIEW = 5;
        int SHOW_TITLE = 6;
        int SHOW_POSTER_SMALL = 7;
    }

    private final static String EPISODE_COLUMNS = Episodes._ID + ","
            + Episodes.TITLE + ","
            + Episodes.NUMBER + ","
            + Episodes.SEASON + ","
            + Episodes.WATCHED;

    private final static String SELECT_SHOWS = "SELECT "
            + BaseColumns._ID + " as sid,"
            + Shows.TITLE + ","
            + Shows.POSTER_SMALL
            + " FROM " + Tables.SHOWS;

    private final static String SELECT_MATCH = "SELECT "
            + EpisodeSearch._DOCID + ","
            + "snippet(" + Tables.EPISODES_SEARCH + ",'<b>','</b>','...') AS " + Episodes.OVERVIEW
            + " FROM " + Tables.EPISODES_SEARCH
            + " WHERE " + Tables.EPISODES_SEARCH + " MATCH ?";

    private final static String SELECT_EPISODES = "SELECT "
            + EPISODE_COLUMNS + "," + Shows.REF_SHOW_ID
            + " FROM " + Tables.EPISODES;

    private final static String JOIN_MATCHES_EPISODES = "SELECT "
            + EPISODE_COLUMNS + "," + Episodes.OVERVIEW + "," + Shows.REF_SHOW_ID
            + " FROM (" + SELECT_MATCH + ")"
            + " JOIN (" + SELECT_EPISODES + ")"
            + " ON " + EpisodeSearch._DOCID + "=" + Episodes._ID;

    private final static String QUERY_SEARCH_EPISODES = "SELECT "
            + EPISODE_COLUMNS + "," + Episodes.OVERVIEW + "," + Shows.TITLE + "," + Shows.POSTER_SMALL
            + " FROM "
            + "("
            + "(" + SELECT_SHOWS + ") JOIN (" + JOIN_MATCHES_EPISODES + ") ON sid="
            + Shows.REF_SHOW_ID
            + ")";

    private final static String ORDER_SEARCH_EPISODES = " ORDER BY "
            + Shows.SORT_TITLE + ","
            + Episodes.SEASON + " ASC,"
            + Episodes.NUMBER + " ASC";

    @Nullable
    public static Cursor search(SupportSQLiteDatabase db, String selection,
            String[] selectionArgs) {
        StringBuilder query = new StringBuilder(QUERY_SEARCH_EPISODES);
        if (selection != null) {
            query.append(" WHERE (").append(selection).append(")");
        }
        query.append(ORDER_SEARCH_EPISODES);

        // ensure to strip double quotation marks (would break the MATCH query)
        String searchTerm = selectionArgs[0];
        if (searchTerm != null) {
            searchTerm = searchTerm.replace("\"", "");
        }
        // search for anything starting with the given search term
        selectionArgs[0] = "\"" + searchTerm + "*\"";

        try {
            return db.query(query.toString(), selectionArgs);
        } catch (SQLiteException e) {
            Timber.e(e, "search: failed, database error.");
            return null;
        }
    }

    private final static String QUERY_SEARCH_SHOWS = "select _id,"
            + Episodes.TITLE + " as " + SearchManager.SUGGEST_COLUMN_TEXT_1 + ","
            + Shows.TITLE + " as " + SearchManager.SUGGEST_COLUMN_TEXT_2 + ","
            + "_id as " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID
            + " from ((select _id as sid," + Shows.TITLE + " from " + Tables.SHOWS + ")"
            + " join "
            + "(select _id," + Episodes.TITLE + "," + Shows.REF_SHOW_ID
            + " from " + "(select docid" + " from " + Tables.EPISODES_SEARCH
            + " where " + Tables.EPISODES_SEARCH + " match " + "?)"
            + " join "
            + "(select _id," + Episodes.TITLE + "," + Shows.REF_SHOW_ID + " from episodes)"
            + "on _id=docid)"
            + "on sid=" + Shows.REF_SHOW_ID + ")";

    @Nullable
    public static Cursor getSuggestions(SupportSQLiteDatabase db, String searchTerm) {
        // ensure to strip double quotation marks (would break the MATCH query)
        if (searchTerm != null) {
            searchTerm = searchTerm.replace("\"", "");
        }

        try {
            // search for anything starting with the given search term
            return db.query(QUERY_SEARCH_SHOWS, new String[]{
                    "\"" + searchTerm + "*\""
            });
        } catch (SQLiteException e) {
            Timber.e(e, "getSuggestions: failed, database error.");
            return null;
        }
    }

//    /**
//     * Checks whether a table exists in the given database.
//     */
//    private static boolean isTableExisting(SQLiteDatabase db, String table) {
//        Cursor cursor = db.query("sqlite_master", new String[]{"name"},
//                "type='table' AND name=?", new String[]{table}, null, null, null, "1");
//        if (cursor == null) {
//            return false;
//        }
//        boolean isTableExisting = cursor.getCount() > 0;
//        cursor.close();
//        return isTableExisting;
//    }

    /**
     * Checks whether the given column exists in the given table of the given database.
     */
    static boolean isTableColumnMissing(@NonNull SupportSQLiteDatabase db, String table,
            String column) {
        Cursor cursor = db.query(SQLiteQueryBuilder
                .buildQueryString(false, table, null, null,
                        null, null, null, "1"));
        if (cursor == null) {
            return true;
        }
        boolean isColumnExisting = cursor.getColumnIndex(column) != -1;
        cursor.close();
        return !isColumnExisting;
    }
}
