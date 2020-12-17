package com.battlelancer.seriesguide.util;

import static com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Qualified;

import android.annotation.SuppressLint;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
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
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.enums.SeasonTags;
import com.battlelancer.seriesguide.model.SgEpisodeUpdateInfo;
import com.battlelancer.seriesguide.model.SgSeasonUpdateInfo;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.greenrobot.eventbus.EventBus;
import timber.log.Timber;

public class DBUtils {

    /**
     * Use for unknown release time/no next episode so they will get sorted last in show list (value
     * is {@link Long#MAX_VALUE}). See {@link Shows#NEXTAIRDATEMS}.
     */
    public static final String UNKNOWN_NEXT_RELEASE_DATE = String.valueOf(Long.MAX_VALUE);

    /**
     * Used if the number of remaining episodes to watch for a show is not (yet) known.
     *
     * @see Shows#UNWATCHED_COUNT
     */
    public static final int UNKNOWN_UNWATCHED_COUNT = -1;
    public static final int UNKNOWN_COLLECTED_COUNT = -1;

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

    /**
     * Maps an integer value stored in the database to a boolean.
     */
    public static boolean restoreBooleanFromInt(int value) {
        return value == 1;
    }

    /**
     * Triggers the rebuilding of the episode search table.
     */
    @SuppressLint("Recycle") // Cursor is null
    public static void rebuildFtsTable(Context context) {
        Timber.d("Query to renew FTS table");
        context.getContentResolver()
                .query(SeriesGuideContract.EpisodeSearch.CONTENT_URI_RENEWFTSTABLE, null, null,
                        null, null);
    }

    interface UnwatchedQuery {
        String AIRED_SELECTION = Episodes.WATCHED + "=0 AND " + Episodes.FIRSTAIREDMS
                + " !=-1 AND " + Episodes.FIRSTAIREDMS + "<=?";

        String AIRED_SELECTION_NO_SPECIALS = AIRED_SELECTION
                + " AND " + Episodes.SELECTION_NO_SPECIALS;

        String FUTURE_SELECTION = Episodes.WATCHED + "=0 AND " + Episodes.FIRSTAIREDMS
                + ">?";

        String NOAIRDATE_SELECTION = Episodes.WATCHED + "=0 AND "
                + Episodes.FIRSTAIREDMS + "=-1";

        String SKIPPED_SELECTION = Episodes.WATCHED + "=" + EpisodeFlags.SKIPPED;
    }

    /**
     * Looks up the episodes of a given season and stores the count of all, unwatched and skipped
     * ones in the seasons watch counters.
     */
    public static void updateUnwatchedCount(Context context, int seasonTvdbId) {
        final ContentResolver resolver = context.getContentResolver();
        final Uri uri = Episodes.buildEpisodesOfSeasonUri(seasonTvdbId);

        // all a seasons episodes
        final int totalCount = getCountOf(resolver, uri, null, null, -1);
        if (totalCount == -1) {
            return;
        }

        // unwatched, aired episodes
        String[] customCurrentTimeArgs = {String.valueOf(TimeTools.getCurrentTime(context))};
        final int count = getCountOf(resolver, uri, UnwatchedQuery.AIRED_SELECTION,
                customCurrentTimeArgs, -1);
        if (count == -1) {
            return;
        }

        // unwatched, aired in the future episodes
        final int unairedCount = getCountOf(resolver, uri, UnwatchedQuery.FUTURE_SELECTION,
                customCurrentTimeArgs, -1);
        if (unairedCount == -1) {
            return;
        }

        // unwatched, no airdate
        final int noAirDateCount = getCountOf(resolver, uri,
                UnwatchedQuery.NOAIRDATE_SELECTION, null, -1);
        if (noAirDateCount == -1) {
            return;
        }

        // any skipped episodes
        int skippedCount = getCountOf(resolver, uri, UnwatchedQuery.SKIPPED_SELECTION, null,
                -1);
        if (skippedCount == -1) {
            return;
        }

        final ContentValues update = new ContentValues();
        update.put(Seasons.WATCHCOUNT, count);
        update.put(Seasons.UNAIREDCOUNT, unairedCount);
        update.put(Seasons.NOAIRDATECOUNT, noAirDateCount);
        update.put(Seasons.TAGS, skippedCount > 0 ? SeasonTags.SKIPPED : SeasonTags.NONE);
        update.put(Seasons.TOTALCOUNT, totalCount);
        resolver.update(Seasons.buildSeasonUri(seasonTvdbId), update, null, null);
    }

    /**
     * Returns how many episodes of a season are left to watch (only aired and not watched, exclusive
     * episodes with no air date, includes specials).
     *
     * @return {@link #UNKNOWN_UNWATCHED_COUNT} if the number is unknown or failed to be determined.
     *
     * This should match the results of {@link #updateUnwatchedCount}.
     */
    public static int getUnwatchedEpisodesOfSeason(@NonNull Context context, int seasonTvdbId) {
        return getCountOf(context.getContentResolver(),
                Episodes.buildEpisodesOfSeasonUri(seasonTvdbId),
                UnwatchedQuery.AIRED_SELECTION,
                new String[]{String.valueOf(TimeTools.getCurrentTime(context))},
                UNKNOWN_UNWATCHED_COUNT);
    }

    /**
     * Returns how many episodes of a season are left to collect.
     *
     * @return {@link #UNKNOWN_COLLECTED_COUNT} if the number is unknown or failed to be determined.
     */
    public static int getUncollectedEpisodesOfSeason(@NonNull Context context, int seasonTvdbId) {
        return getCountOf(context.getContentResolver(),
                Episodes.buildEpisodesOfSeasonUri(seasonTvdbId),
                Episodes.SELECTION_NOT_COLLECTED,
                null,
                UNKNOWN_COLLECTED_COUNT);
    }

    /**
     * Returns how many episodes of a show are left to watch (only aired and not watched, exclusive
     * episodes with no air date and without specials).
     *
     * @return {@link #UNKNOWN_UNWATCHED_COUNT} if the number is unknown or failed to be determined.
     */
    public static int getUnwatchedEpisodesOfShow(Context context, String showId) {
        if (context == null) {
            return UNKNOWN_UNWATCHED_COUNT;
        }

        // unwatched, aired episodes
        return getCountOf(context.getContentResolver(),
                Episodes.buildEpisodesOfShowUri(showId),
                UnwatchedQuery.AIRED_SELECTION_NO_SPECIALS,
                new String[]{
                        String.valueOf(TimeTools.getCurrentTime(context))
                }, UNKNOWN_UNWATCHED_COUNT);
    }

    /**
     * Returns how many episodes of a show are left to collect. Only considers regular, released
     * episodes (no specials, must have a release date in the past).
     */
    public static int getUncollectedEpisodesOfShow(Context context, String showId) {
        if (context == null) {
            return UNKNOWN_COLLECTED_COUNT;
        }

        // not collected, no special, previously released episodes
        return getCountOf(context.getContentResolver(), Episodes.buildEpisodesOfShowUri(showId),
                Episodes.SELECTION_NOT_COLLECTED
                        + " AND " + Episodes.SELECTION_NO_SPECIALS
                        + " AND " + Episodes.SELECTION_HAS_RELEASE_DATE
                        + " AND " + Episodes.SELECTION_RELEASED_BEFORE_X,
                new String[]{
                        String.valueOf(TimeTools.getCurrentTime(context))
                },
                UNKNOWN_COLLECTED_COUNT);
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
     * Marks the next episode (if there is one) of the given show as watched. Submits it to trakt if
     * possible.
     */
    public static void markNextEpisode(Context context, int showId, int episodeId) {
        if (episodeId > 0) {
            Cursor episode = context.getContentResolver().query(
                    Episodes.buildEpisodeUri(String.valueOf(episodeId)), new String[]{
                            Episodes.SEASON, Episodes.NUMBER
                    }, null, null, null
            );
            if (episode != null) {
                if (episode.moveToFirst()) {
                    EpisodeTools.episodeWatched(context, showId, episodeId, episode.getInt(0),
                            episode.getInt(1), EpisodeFlags.WATCHED);
                }
                episode.close();
            }
        }
    }

    /**
     * Queries the show table for the given TVDb id and returns whether there is an entry,
     * e.g. the show is already in the database.
     */
    public static boolean isShowExists(Context context, int showTvdbId) {
        return SgRoomDatabase.getInstance(context).showHelper().getShowId(showTvdbId) != 0;
    }

    /**
     * Builds a {@link ContentProviderOperation} for inserting or updating a show (depending on
     * {@code isNew}).
     *
     * <p> If the show is new, sets some default values and the (TheTVDB) id.
     */
    public static ContentValues buildShowContentValues(Context context, Show show, boolean isNew) {
        // last updated now
        show.last_updated = System.currentTimeMillis();
        return show.toContentValues(context, isNew);
    }

    /**
     * Returns the episode IDs and their last updated time for a given show as a efficiently
     * searchable HashMap. Using instead of last edited time, which might be wrong when for example
     * restoring from a backup.
     *
     * @return HashMap containing the shows existing episodes
     */
    public static HashMap<Integer, SgEpisodeUpdateInfo> getEpisodesByTvdbId(Context context,
            long showId) {
        List<SgEpisodeUpdateInfo> episodes = SgRoomDatabase.getInstance(context)
                .episodeHelper()
                .getLastUpdatedInfoForShow(showId);
        @SuppressLint("UseSparseArrays") HashMap<Integer, SgEpisodeUpdateInfo> map = new HashMap<>();
        for (SgEpisodeUpdateInfo episode : episodes) {
            map.put(episode.getTvdbId(), episode);
        }
        return map;
    }

    /**
     * Returns the season TVDB IDs for a given show as a efficiently searchable HashMap.
     *
     * @return HashMap containing the shows existing seasons
     */
    public static HashMap<Integer, SgSeasonUpdateInfo> getSeasonsByTvdbId(Context context,
            long showId) {
        List<SgSeasonUpdateInfo> seasons = SgRoomDatabase.getInstance(context)
                .seasonHelper()
                .getSeasonTvdbIds(showId);
        HashMap<Integer, SgSeasonUpdateInfo> map = new HashMap<>();
        for (SgSeasonUpdateInfo season : seasons) {
            map.put(season.getTvdbId(), season);
        }
        return map;
    }

    private interface LastWatchedEpisodeQuery {
        String[] PROJECTION = new String[]{
                Qualified.SHOWS_ID,
                Shows.LASTWATCHEDID,
                Episodes.SEASON,
                Episodes.NUMBER,
                Episodes.FIRSTAIREDMS
        };

        int SHOW_TVDB_ID = 0;
        int LAST_EPISODE_TVDB_ID = 1;
        int LAST_EPISODE_SEASON = 2;
        int LAST_EPISODE_NUMBER = 3;
        int LAST_EPISODE_FIRST_RELEASE_MS = 4;
    }

    private interface NextEpisodesQuery {
        String[] PROJECTION = new String[]{
                Episodes._ID,
                Episodes.SEASON,
                Episodes.NUMBER,
                Episodes.FIRSTAIREDMS,
                Episodes.TITLE
        };

        /**
         * Unwatched, airing later or has a different number or season if airing the same time.
         */
        String SELECT_NEXT = Episodes.WATCHED + "=0 AND ("
                + "(" + Episodes.FIRSTAIREDMS + "=? AND "
                + "(" + Episodes.NUMBER + "!=? OR " + Episodes.SEASON + "!=?)) "
                + "OR " + Episodes.FIRSTAIREDMS + ">?)";

        String SELECT_WITHAIRDATE = " AND " + Episodes.FIRSTAIREDMS + "!=-1";

        String SELECT_ONLYFUTURE = " AND " + Episodes.FIRSTAIREDMS + ">=?";

        /**
         * Air time, then lowest season, or if identical lowest episode number.
         */
        String SORTORDER = Episodes.FIRSTAIREDMS + " ASC," + Episodes.SEASON + " ASC,"
                + Episodes.NUMBER + " ASC";

        int ID = 0;
        int SEASON = 1;
        int NUMBER = 2;
        int FIRST_RELEASE_MS = 3;
        int TITLE = 4;
    }

    /**
     * Update next episode field and unwatched episode count for the given show. If no show id is
     * passed, will update next episodes for all shows.
     *
     * @return If only one show was passed, the TVDb id of the new next episode. Otherwise -1.
     */
    public static long updateLatestEpisode(Context context, Integer showTvdbIdToUpdate) {
        // get a list of shows and their last watched episodes
        Cursor shows;
        try {
            shows = context.getContentResolver().query(Shows.CONTENT_URI_WITH_LAST_EPISODE,
                    LastWatchedEpisodeQuery.PROJECTION,
                    showTvdbIdToUpdate != null ?
                            Qualified.SHOWS_ID + "=" + showTvdbIdToUpdate : null,
                    null, null
            );
        } catch (SQLiteException e) {
            shows = null;
            Timber.e(e, "updateLatestEpisode: show query failed.");
            postDatabaseError(e);
        }
        if (shows == null) {
            // abort completely on query failure
            Timber.e("Failed to update next episode values");
            return -1;
        }
        final List<String[]> showsLastEpisodes = new ArrayList<>();
        while (shows.moveToNext()) {
            showsLastEpisodes.add(
                    new String[]{
                            shows.getString(LastWatchedEpisodeQuery.SHOW_TVDB_ID), // 0
                            shows.getString(LastWatchedEpisodeQuery.LAST_EPISODE_TVDB_ID), // 1
                            shows.getString(LastWatchedEpisodeQuery.LAST_EPISODE_SEASON), // 2
                            shows.getString(LastWatchedEpisodeQuery.LAST_EPISODE_NUMBER), // 3
                            shows.getString(LastWatchedEpisodeQuery.LAST_EPISODE_FIRST_RELEASE_MS)
                            // 4
                    }
            );
        }
        shows.close();

        // pre-build next episode selection
        final boolean isNoReleasedEpisodes = DisplaySettings.isNoReleasedEpisodes(context);
        final String nextEpisodeSelection = buildNextEpisodeSelection(
                DisplaySettings.isHidingSpecials(context), isNoReleasedEpisodes);

        // build updated next episode values for each show
        int nextEpisodeTvdbId = -1;
        final ContentValues newShowValues = new ContentValues();
        final ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        final String currentTime = String.valueOf(TimeTools.getCurrentTime(context));
        boolean preventSpoilers = DisplaySettings.preventSpoilers(context);
        for (String[] show : showsLastEpisodes) {
            // STEP 1: get last watched episode details
            final String showTvdbId = show[0];
            final String lastEpisodeTvdbId = show[1];
            String season = show[2];
            String number = show[3];
            String releaseTime = show[4];
            if (TextUtils.isEmpty(lastEpisodeTvdbId)
                    || season == null || number == null || releaseTime == null) {
                // by default: no watched episodes, include all starting with special 0
                season = "-1";
                number = "-1";
                releaseTime = String.valueOf(Long.MIN_VALUE);
            }

            // STEP 2: get episode released closest afterwards; or at the same time,
            // but with a higher number
            final String[] selectionArgs;
            if (isNoReleasedEpisodes) {
                // restrict to episodes with future release date
                selectionArgs = new String[]{
                        releaseTime, number, season, releaseTime, currentTime
                };
            } else {
                // restrict to episodes with any valid air date
                selectionArgs = new String[]{
                        releaseTime, number, season, releaseTime
                };
            }
            Cursor next;
            try {
                next = context.getContentResolver()
                        .query(Episodes.buildEpisodesOfShowUri(showTvdbId),
                                NextEpisodesQuery.PROJECTION, nextEpisodeSelection, selectionArgs,
                                NextEpisodesQuery.SORTORDER);
            } catch (SQLiteException e) {
                next = null;
                Timber.e(e, "updateLatestEpisode: next episode query failed.");
                postDatabaseError(e);
            }
            if (next == null) {
                // abort completely on query failure
                Timber.e("Failed to update next episode values");
                return -1;
            }

            // STEP 3: build updated next episode values
            if (next.moveToFirst()) {
                final String nextEpisodeString;
                int seasonNumber = next.getInt(NextEpisodesQuery.SEASON);
                int episodeNumber = next.getInt(NextEpisodesQuery.NUMBER);
                nextEpisodeString = TextTools.getNextEpisodeString(context,
                        seasonNumber,
                        episodeNumber,
                        preventSpoilers
                                // just the number, like '0x12 Episode 12'
                                ? null
                                // next episode text, like '0x12 Episode Name'
                                : next.getString(NextEpisodesQuery.TITLE)
                );
                // next release date text, e.g. "in 15 mins (Fri)"
                long releaseTimeNext = next.getLong(NextEpisodesQuery.FIRST_RELEASE_MS);

                nextEpisodeTvdbId = next.getInt(NextEpisodesQuery.ID);
                newShowValues.put(Shows.NEXTEPISODE, nextEpisodeTvdbId);
                newShowValues.put(Shows.NEXTAIRDATEMS, releaseTimeNext);
                newShowValues.put(Shows.NEXTTEXT, nextEpisodeString);
            } else {
                // no next episode, set empty values
                nextEpisodeTvdbId = 0;
                newShowValues.put(Shows.NEXTEPISODE, "");
                newShowValues.put(Shows.NEXTAIRDATEMS, UNKNOWN_NEXT_RELEASE_DATE);
                newShowValues.put(Shows.NEXTTEXT, "");
            }
            next.close();

            // STEP 4: get remaining episodes count
            int unwatchedEpisodesCount = getUnwatchedEpisodesOfShow(context, showTvdbId);
            newShowValues.put(Shows.UNWATCHED_COUNT, unwatchedEpisodesCount);

            // update the show with the new next episode values
            batch.add(ContentProviderOperation.newUpdate(Shows.buildShowUri(showTvdbId))
                    .withValues(newShowValues)
                    .build());
            newShowValues.clear();
        }

        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "Failed to update next episode values");
            return -1;
        }

        return nextEpisodeTvdbId;
    }

    private static String buildNextEpisodeSelection(boolean isHidingSpecials,
            boolean isNoReleasedEpisodes) {
        StringBuilder nextEpisodeSelectionBuilder = new StringBuilder(
                NextEpisodesQuery.SELECT_NEXT);
        if (isHidingSpecials) {
            // do not take specials into account
            nextEpisodeSelectionBuilder.append(" AND ").append(Episodes.SELECTION_NO_SPECIALS);
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
