package com.battlelancer.seriesguide.util;

import android.annotation.SuppressLint;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.sqlite.db.SimpleSQLiteQuery;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.provider.SgEpisode2Helper;
import com.battlelancer.seriesguide.provider.SgEpisode2Info;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.provider.SgShow2Helper;
import com.battlelancer.seriesguide.provider.SgShow2LastWatchedEpisode;
import com.battlelancer.seriesguide.provider.SgShow2NextEpisodeUpdate;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.greenrobot.eventbus.EventBus;
import timber.log.Timber;

public class DBUtils {

    /**
     * Use for unknown release time/no next episode so they will get sorted last in show list (value
     * is {@link Long#MAX_VALUE}). See {@link Shows#NEXTAIRDATEMS}.
     */
    public static final long UNKNOWN_NEXT_RELEASE_DATE = Long.MAX_VALUE;

    /**
     * Used if the number of remaining episodes to watch for a show is not (yet) known.
     *
     * @see Shows#UNWATCHED_COUNT
     */
    public static final int UNKNOWN_UNWATCHED_COUNT = -1;

    private static final int SMALL_BATCH_SIZE = 50;

    private static final String[] PROJECTION_COUNT = new String[]{
            BaseColumns._COUNT
    };

    public static class DatabaseErrorEvent {

        private final String message;
        private final boolean isCorrupted;

        DatabaseErrorEvent(String message, boolean isCorrupted) {
            this.message = message;
            this.isCorrupted = isCorrupted;
        }

        public void handle(Context context) {
            StringBuilder errorText = new StringBuilder(context.getString(R.string.database_error));
            if (isCorrupted) {
                errorText.append(" ").append(context.getString(R.string.reinstall_info));
            }
            errorText.append(" (").append(message).append(")");
            Toast.makeText(context, errorText, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Post an event to simply show a toast with the error message.
     */
    public static void postDatabaseError(SQLiteException e) {
        EventBus.getDefault()
                .post(new DatabaseErrorEvent(e.getMessage(),
                        e instanceof SQLiteDatabaseCorruptException));
    }

    /**
     * Maps a {@link java.lang.Boolean} object to an int value to store in the database.
     */
    public static int convertBooleanToInt(Boolean value) {
        if (value == null) {
            return 0;
        }
        return value ? 1 : 0;
    }

    public static int getCountOf(@NonNull ContentResolver resolver, @NonNull Uri uri,
            @Nullable String selection, @Nullable String[] selectionArgs, int defaultValue) {
        Cursor cursor = resolver.query(uri, PROJECTION_COUNT, selection, selectionArgs, null);
        if (cursor == null) {
            return defaultValue;
        }
        if (!cursor.moveToFirst()) {
            cursor.close();
            return defaultValue;
        }
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    /**
     * Queries the show table for the given TVDb id and returns whether there is an entry,
     * e.g. the show is already in the database.
     */
    public static boolean isShowExists(Context context, int showTvdbId) {
        return SgRoomDatabase.getInstance(context).sgShow2Helper().getShowIdByTvdbId(showTvdbId) != 0;
    }

    /**
     * Returns the episode IDs and their last updated time for a given show as a efficiently
     * searchable HashMap. Using instead of last edited time, which might be wrong when for example
     * restoring from a backup.
     *
     * @return HashMap containing the shows existing episodes
     */
    public static HashMap<Integer, Long> getLastUpdatedByEpisodeId(Context context,
            int showTvdbId) {
        Cursor episodes = context.getContentResolver().query(
                Episodes.buildEpisodesOfShowUri(showTvdbId), new String[]{
                        Episodes._ID, Episodes.LAST_UPDATED
                }, null, null, null
        );
        @SuppressLint("UseSparseArrays") HashMap<Integer, Long> episodeMap = new HashMap<>();
        if (episodes != null) {
            while (episodes.moveToNext()) {
                episodeMap.put(episodes.getInt(0), episodes.getLong(1));
            }
            episodes.close();
        }
        return episodeMap;
    }

    /**
     * Returns the season IDs for a given show as a efficiently searchable HashMap.
     *
     * @return HashMap containing the shows existing seasons
     */
    public static HashSet<Integer> getSeasonIdsOfShow(Context context, int showTvdbId) {
        Cursor seasons = context.getContentResolver().query(
                Seasons.buildSeasonsOfShowUri(showTvdbId),
                new String[]{
                        Seasons._ID
                }, null, null, null
        );
        HashSet<Integer> seasonIds = new HashSet<>();
        if (seasons != null) {
            while (seasons.moveToNext()) {
                seasonIds.add(seasons.getInt(0));
            }
            seasons.close();
        }
        return seasonIds;
    }

    private interface NextEpisodesQuery {
        /**
         * Unwatched, airing later or has a different number or season if airing the same time.
         */
        String SELECT_NEXT = SgEpisode2Columns.WATCHED + "=0 AND ("
                + "(" + SgEpisode2Columns.FIRSTAIREDMS + "=? AND "
                + "(" + SgEpisode2Columns.NUMBER + "!=? OR " + SgEpisode2Columns.SEASON + "!=?)) "
                + "OR " + SgEpisode2Columns.FIRSTAIREDMS + ">?)";

        String SELECT_WITHAIRDATE = " AND " + SgEpisode2Columns.FIRSTAIREDMS + "!=-1";

        String SELECT_ONLYFUTURE = " AND " + SgEpisode2Columns.FIRSTAIREDMS + ">=?";

        /**
         * Air time, then lowest season, or if identical lowest episode number.
         */
        String SORTORDER = SgEpisode2Columns.FIRSTAIREDMS + " ASC,"
                + SgEpisode2Columns.SEASON + " ASC,"
                + SgEpisode2Columns.NUMBER + " ASC";
    }

    /**
     * Update next episode field and unwatched episode count for the given show. If no show id is
     * passed, will update next episodes for all shows.
     *
     * @return If only one show was passed, the row id of the new next episode. Otherwise -1.
     */
    public static long updateLatestEpisode(@NonNull Context context, @Nullable Long showIdOrNull) {
        // Get a list of shows and their last watched episodes.
        final List<SgShow2LastWatchedEpisode> shows;
        SgRoomDatabase database = SgRoomDatabase.getInstance(context);
        SgShow2Helper showHelper = database.sgShow2Helper();
        if (showIdOrNull != null) {
            SgShow2LastWatchedEpisode show = showHelper.getShowWithLastWatchedEpisode(showIdOrNull);
            if (show == null) {
                Timber.e("Failed to update next episode, show does not exist: %s", showIdOrNull);
                return -1; // Fail, show does not (longer) exist.
            }
            shows = new ArrayList<>();
            shows.add(show);
        } else {
            shows = showHelper.getShowsWithLastWatchedEpisode();
        }

        // pre-build next episode selection
        final boolean isNoReleasedEpisodes = DisplaySettings.isNoReleasedEpisodes(context);
        final String nextEpisodeSelection = buildNextEpisodeSelection(
                DisplaySettings.isHidingSpecials(context), isNoReleasedEpisodes);

        // build updated next episode values for each show
        final List<SgShow2NextEpisodeUpdate> batch = new ArrayList<>();
        long nextEpisodeIdResult = -1;
        SgEpisode2Helper episodeHelper = database.sgEpisode2Helper();
        final long currentTime = TimeTools.getCurrentTime(context);
        boolean preventSpoilers = DisplaySettings.preventSpoilers(context);
        for (SgShow2LastWatchedEpisode show : shows) {
            // STEP 1: get last watched episode details
            Integer season = show.getSeasonNumber();
            Integer number = show.getEpisodeNumber();
            Long releaseTime = show.getEpisodeReleaseDateMs();
            // Note: Due to LEFT JOIN query, episode values are null if no matching episode found.
            if (show.getLastWatchedEpisodeId() == 0
                    || season == null || number == null || releaseTime == null) {
                // by default: no watched episodes, include all starting with special 0
                season = -1;
                number = -1;
                releaseTime = Long.MIN_VALUE;
            }

            // STEP 2: get episode released closest afterwards; or at the same time,
            // but with a higher number
            final Object[] selectionArgs;
            if (isNoReleasedEpisodes) {
                // restrict to episodes with future release date
                selectionArgs = new Object[]{
                        releaseTime, number, season, releaseTime, currentTime
                };
            } else {
                // restrict to episodes with any valid air date
                selectionArgs = new Object[]{
                        releaseTime, number, season, releaseTime
                };
            }
            SgEpisode2Info episodeOrNull = episodeHelper
                    .getEpisodeInfo(new SimpleSQLiteQuery(
                            "SELECT * FROM " + Tables.SG_EPISODE
                                    + " WHERE " + SgShow2Columns.REF_SHOW_ID + " = " + show.getId()
                                    + " AND " + nextEpisodeSelection
                                    + " ORDER BY " + NextEpisodesQuery.SORTORDER
                                    + " LIMIT 1",
                            selectionArgs
                    ));

            // STEP 3: get remaining episodes count
            int unwatchedEpisodesCount = episodeHelper
                    .countNotWatchedEpisodesOfShow(show.getId(), currentTime);

            // STEP 4: build updated next episode values
            SgShow2NextEpisodeUpdate update;
            if (episodeOrNull != null) {
                final String nextEpisodeString;
                nextEpisodeString = TextTools.getNextEpisodeString(context,
                        episodeOrNull.getSeason(),
                        episodeOrNull.getEpisodenumber(),
                        preventSpoilers
                                // just the number, like '0x12 Episode 12'
                                ? null
                                // next episode text, like '0x12 Episode Name'
                                : episodeOrNull.getTitle()
                );
                // next release date text, e.g. "in 15 mins (Fri)"
                long releaseTimeNext = episodeOrNull.getFirstReleasedMs();

                nextEpisodeIdResult = episodeOrNull.getId();
                update = new SgShow2NextEpisodeUpdate(
                        show.getId(),
                        String.valueOf(nextEpisodeIdResult),
                        releaseTimeNext,
                        nextEpisodeString,
                        unwatchedEpisodesCount
                );
            } else {
                // no next episode, set empty values
                nextEpisodeIdResult = 0;
                update = new SgShow2NextEpisodeUpdate(
                        show.getId(),
                        "",
                        UNKNOWN_NEXT_RELEASE_DATE,
                        "",
                        unwatchedEpisodesCount
                );
            }
            batch.add(update);
        }

        // Update shows in database with new next episode values.
        int rowsUpdated = showHelper.updateShowNextEpisode(batch);
        if (rowsUpdated < 0) {
            Timber.e("Failed to apply show next episode db update.");
            return -1;
        }

        return nextEpisodeIdResult;
    }

    private static String buildNextEpisodeSelection(boolean isHidingSpecials,
            boolean isNoReleasedEpisodes) {
        StringBuilder nextEpisodeSelectionBuilder = new StringBuilder(
                NextEpisodesQuery.SELECT_NEXT);
        if (isHidingSpecials) {
            // do not take specials into account
            nextEpisodeSelectionBuilder.append(" AND ")
                    .append(SgEpisode2Columns.SELECTION_NO_SPECIALS);
        }
        if (isNoReleasedEpisodes) {
            // restrict to episodes with future release date
            nextEpisodeSelectionBuilder.append(NextEpisodesQuery.SELECT_ONLYFUTURE);
        } else {
            // restrict to episodes with any valid air date
            nextEpisodeSelectionBuilder.append(NextEpisodesQuery.SELECT_WITHAIRDATE);
        }
        return nextEpisodeSelectionBuilder.toString();
    }

    /**
     * Applies a large {@link ContentProviderOperation} batch in smaller batches as not to overload
     * the transaction cache.
     */
    public static void applyInSmallBatches(Context context,
            ArrayList<ContentProviderOperation> batch) throws OperationApplicationException {
        // split into smaller batches to not overload transaction cache
        // see http://developer.android.com/reference/android/os/TransactionTooLargeException.html

        ArrayList<ContentProviderOperation> smallBatch = new ArrayList<>();

        while (!batch.isEmpty()) {
            if (batch.size() <= SMALL_BATCH_SIZE) {
                // small enough already? apply right away
                applyBatch(context, batch);
                return;
            }

            // take up to 50 elements out of batch
            for (int count = 0; count < SMALL_BATCH_SIZE; count++) {
                if (batch.isEmpty()) {
                    break;
                }
                smallBatch.add(batch.remove(0));
            }

            // apply small batch
            applyBatch(context, smallBatch);

            // prepare for next small batch
            smallBatch.clear();
        }
    }

    private static void applyBatch(Context context, ArrayList<ContentProviderOperation> batch)
            throws OperationApplicationException {
        try {
            context.getContentResolver()
                    .applyBatch(SgApp.CONTENT_AUTHORITY, batch);
        } catch (RemoteException e) {
            // not using a remote provider, so this should never happen. crash if it does.
            throw new RuntimeException("Problem applying batch operation", e);
        } catch (SQLiteException e) {
            Timber.e(e, "applyBatch: failed, database error.");
            postDatabaseError(e);
        }
    }

    /**
     * Removes a leading article from the given string (including the first whitespace that
     * follows). <p> <em>Currently only supports English articles (the, a and an).</em>
     */
    @Nullable
    public static String trimLeadingArticle(String title) {
        if (TextUtils.isEmpty(title)) {
            return title;
        }

        if (title.length() > 4 &&
                (title.startsWith("The ") || title.startsWith("the "))) {
            return title.substring(4);
        }
        if (title.length() > 2 &&
                (title.startsWith("A ") || title.startsWith("a "))) {
            return title.substring(2);
        }
        if (title.length() > 3 &&
                (title.startsWith("An ") || title.startsWith("an "))) {
            return title.substring(3);
        }

        return title;
    }
}
