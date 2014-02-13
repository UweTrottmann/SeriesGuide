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

package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.SeriesGuideApplication;
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.ShowStatusExport;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.enums.SeasonTags;
import com.battlelancer.seriesguide.items.Series;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.ActivitySettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.ActivityFragment;
import com.battlelancer.seriesguide.ui.ActivityFragment.ActivityType;
import com.battlelancer.thetvdbapi.TheTVDB.ShowStatus;
import com.battlelancer.seriesguide.R;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.text.format.DateUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

public class DBUtils {

    static final String TAG = "SeriesDatabase";

    /**
     * Use 9223372036854775807 (Long.MAX_VALUE) for unknown airtime/no next episode so they will get
     * sorted last.
     */
    public static final String UNKNOWN_NEXT_AIR_DATE = "9223372036854775807";

    public static final int SMALL_BATCH_SIZE = 50;

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

    interface UnwatchedQuery {

        static final String[] PROJECTION = new String[]{
                Episodes._ID
        };

        static final String AIRED_SELECTION = Episodes.WATCHED + "=0 AND " + Episodes.FIRSTAIREDMS
                + " !=-1 AND " + Episodes.FIRSTAIREDMS + "<=?";

        static final String FUTURE_SELECTION = Episodes.WATCHED + "=0 AND " + Episodes.FIRSTAIREDMS
                + ">?";

        static final String NOAIRDATE_SELECTION = Episodes.WATCHED + "=0 AND "
                + Episodes.FIRSTAIREDMS + "=-1";

        static final String SKIPPED_SELECTION = Episodes.WATCHED + "=" + EpisodeFlags.SKIPPED;
    }

    /**
     * Looks up the episodes of a given season and stores the count of already aired, but not
     * watched ones in the seasons watchcount.
     */
    public static void updateUnwatchedCount(Context context, String seasonid) {
        final ContentResolver resolver = context.getContentResolver();
        final String customCurrentTime = String.valueOf(TimeTools.getCurrentTime(context));
        final Uri episodesOfSeasonUri = Episodes.buildEpisodesOfSeasonUri(seasonid);

        // all a seasons episodes
        final Cursor total = resolver.query(episodesOfSeasonUri, new String[]{
                Episodes._ID
        }, null, null, null);
        if (total == null) {
            return;
        }
        final int totalCount = total.getCount();
        total.close();

        // unwatched, aired episodes
        final Cursor unwatched = resolver.query(episodesOfSeasonUri, UnwatchedQuery.PROJECTION,
                UnwatchedQuery.AIRED_SELECTION, new String[]{
                customCurrentTime
        }, null);
        if (unwatched == null) {
            return;
        }
        final int count = unwatched.getCount();
        unwatched.close();

        // unwatched, aired in the future episodes
        final Cursor unAired = resolver.query(episodesOfSeasonUri, UnwatchedQuery.PROJECTION,
                UnwatchedQuery.FUTURE_SELECTION, new String[]{
                customCurrentTime
        }, null);
        if (unAired == null) {
            return;
        }
        final int unairedCount = unAired.getCount();
        unAired.close();

        // unwatched, no airdate
        final Cursor noAirDate = resolver.query(episodesOfSeasonUri, UnwatchedQuery.PROJECTION,
                UnwatchedQuery.NOAIRDATE_SELECTION, null, null);
        if (noAirDate == null) {
            return;
        }
        final int noAirDateCount = noAirDate.getCount();
        noAirDate.close();

        // any skipped episodes
        final Cursor skipped = resolver.query(episodesOfSeasonUri, UnwatchedQuery.PROJECTION,
                UnwatchedQuery.SKIPPED_SELECTION, null, null);
        if (skipped == null) {
            return;
        }
        boolean hasSkippedEpisodes = skipped.getCount() > 0;
        skipped.close();

        final ContentValues update = new ContentValues();
        update.put(Seasons.WATCHCOUNT, count);
        update.put(Seasons.UNAIREDCOUNT, unairedCount);
        update.put(Seasons.NOAIRDATECOUNT, noAirDateCount);
        update.put(Seasons.TAGS, hasSkippedEpisodes ? SeasonTags.SKIPPED : SeasonTags.NONE);
        update.put(Seasons.TOTALCOUNT, totalCount);
        resolver.update(Seasons.buildSeasonUri(seasonid), update, null, null);
    }

    /**
     * Returns how many episodes of a show are left to watch (only aired and not watched, exclusive
     * episodes with no air date and without specials).
     */
    public static int getUnwatchedEpisodesOfShow(Context context, String showId) {
        if (context == null) {
            return -1;
        }

        // unwatched, aired episodes
        final Cursor unwatched = context.getContentResolver()
                .query(Episodes.buildEpisodesOfShowUri(showId), UnwatchedQuery.PROJECTION,
                        UnwatchedQuery.AIRED_SELECTION + Episodes.SELECTION_NOSPECIALS,
                        new String[]{
                                String.valueOf(TimeTools.getCurrentTime(context))
                        }, null);
        if (unwatched == null) {
            return -1;
        }

        final int count = unwatched.getCount();
        unwatched.close();

        return count;
    }

    /**
     * Returns how many episodes of a show are left to collect.
     */
    public static int getUncollectedEpisodesOfShow(Context context, String showId) {
        if (context == null) {
            return -1;
        }
        final ContentResolver resolver = context.getContentResolver();
        final Uri episodesOfShowUri = Episodes.buildEpisodesOfShowUri(showId);

        // unwatched, aired episodes
        final Cursor uncollected = resolver.query(episodesOfShowUri, new String[]{
                Episodes._ID, Episodes.COLLECTED
        },
                Episodes.COLLECTED + "=0", null, null);
        if (uncollected == null) {
            return -1;
        }
        final int count = uncollected.getCount();
        uncollected.close();

        return count;
    }

    /**
     * Calls {@code getUpcomingEpisodes(false, context)}.
     */
    public static Cursor getUpcomingEpisodes(Context context) {
        return getUpcomingEpisodes(false, context);
    }

    /**
     * Returns all episodes that air today or later. Using Pacific Time to determine today. Excludes
     * shows that are hidden.
     *
     * @return Cursor using the projection of {@link com.battlelancer.seriesguide.ui.ActivityFragment.ActivityQuery}.
     */
    public static Cursor getUpcomingEpisodes(boolean isOnlyUnwatched, Context context) {
        String[][] args = buildActivityQuery(context, ActivityType.UPCOMING, isOnlyUnwatched, -1);

        return context.getContentResolver().query(Episodes.CONTENT_URI_WITHSHOW,
                ActivityFragment.ActivityQuery.PROJECTION, args[0][0], args[1], args[2][0]);
    }

    /**
     * Return all episodes that aired the day before and earlier. Using Pacific Time to determine
     * today. Excludes shows that are hidden.
     *
     * @return Cursor using the projection of {@link com.battlelancer.seriesguide.ui.ActivityFragment.ActivityQuery}.
     */
    public static Cursor getRecentEpisodes(boolean isOnlyUnwatched, Context context) {
        String[][] args = buildActivityQuery(context, ActivityType.RECENT, isOnlyUnwatched, -1);

        return context.getContentResolver().query(Episodes.CONTENT_URI_WITHSHOW,
                ActivityFragment.ActivityQuery.PROJECTION, args[0][0], args[1], args[2][0]);
    }

    /**
     * Returns an array of size 3. The built query is stored in {@code [0][0]}, the built selection
     * args in {@code [1]} and the sort order in {@code [2][0]}.
     *
     * @param type                  A {@link ActivityType}, defaults to UPCOMING.
     * @param numberOfDaysToInclude Limits the time range of returned episodes to a number of days
     *                              from today. If lower then 1 defaults to infinity.
     */
    public static String[][] buildActivityQuery(Context context, String type,
            int numberOfDaysToInclude) {
        boolean isNoWatched = DisplaySettings.isNoWatchedEpisodes(context);

        return buildActivityQuery(context, type, isNoWatched, numberOfDaysToInclude);
    }

    private static String[][] buildActivityQuery(Context context, String type,
            boolean isOnlyUnwatched, int numberOfDaysToInclude) {
        // go an hour back in time, so episodes move to recent one hour late
        long recentThreshold = TimeTools.getCurrentTime(context) - DateUtils.HOUR_IN_MILLIS;

        String query;
        String[] selectionArgs;
        String sortOrder;
        long timeThreshold;

        if (ActivityType.RECENT.equals(type)) {
            query = ActivityFragment.ActivityQuery.QUERY_RECENT;
            sortOrder = ActivityFragment.ActivityQuery.SORTING_RECENT;
            if (numberOfDaysToInclude < 1) {
                // at least has an air date
                timeThreshold = 0;
            } else {
                // last x days
                timeThreshold = System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS
                        * numberOfDaysToInclude;
            }
        } else {
            query = ActivityFragment.ActivityQuery.QUERY_UPCOMING;
            sortOrder = ActivityFragment.ActivityQuery.SORTING_UPCOMING;
            if (numberOfDaysToInclude < 1) {
                // to infinity!
                timeThreshold = Long.MAX_VALUE;
            } else {
                timeThreshold = System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS
                        * numberOfDaysToInclude;
            }
        }

        selectionArgs = new String[]{
                String.valueOf(recentThreshold), String.valueOf(timeThreshold)
        };

        // append only favorites selection if necessary
        boolean isOnlyFavorites = ActivitySettings.isOnlyFavorites(context);
        if (isOnlyFavorites) {
            query += Shows.SELECTION_FAVORITES;
        }

        // append no specials selection if necessary
        boolean isNoSpecials = DisplaySettings.isHidingSpecials(context);
        if (isNoSpecials) {
            query += Episodes.SELECTION_NOSPECIALS;
        }

        // append unwatched selection if necessary
        if (isOnlyUnwatched) {
            query += Episodes.SELECTION_NOWATCHED;
        }

        // build result array
        String[][] results = new String[3][];
        results[0] = new String[]{
                query
        };
        results[1] = selectionArgs;
        results[2] = new String[]{
                sortOrder
        };
        return results;
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
            }, null, null, null);
            if (episode != null) {
                if (episode.moveToFirst()) {
                    new FlagTask(context, showId)
                            .episodeWatched(episodeId, episode.getInt(0), episode.getInt(1),
                                    EpisodeFlags.WATCHED)
                            .execute();
                }
                episode.close();
            }
        }
    }

    private static final String[] SHOW_PROJECTION = new String[]{
            Shows._ID, Shows.ACTORS, Shows.AIRSDAYOFWEEK, Shows.AIRSTIME, Shows.CONTENTRATING,
            Shows.FIRSTAIRED, Shows.GENRES, Shows.NETWORK, Shows.OVERVIEW, Shows.POSTER,
            Shows.RATING, Shows.RUNTIME, Shows.TITLE, Shows.STATUS, Shows.IMDBID,
            Shows.NEXTEPISODE, Shows.LASTEDIT, Shows.RELEASE_COUNTRY
    };

    /**
     * Returns a {@link Series} object. Might return {@code null} if there is no show with that TVDb
     * id.
     */
    public static Series getShow(Context context, int showTvdbId) {
        Cursor details = context.getContentResolver().query(Shows.buildShowUri(showTvdbId),
                SHOW_PROJECTION, null,
                null, null);

        Series show = null;
        if (details != null) {
            if (details.moveToFirst()) {
                show = new Series();

                show.setId(details.getString(0));
                show.setActors(details.getString(1));
                show.setAirsDayOfWeek(details.getString(2));
                show.setAirsTime(details.getLong(3));
                show.setContentRating(details.getString(4));
                show.setFirstAired(details.getString(5));
                show.setGenres(details.getString(6));
                show.setNetwork(details.getString(7));
                show.setOverview(details.getString(8));
                show.setPoster(details.getString(9));
                show.setRating(details.getString(10));
                show.setRuntime(details.getString(11));
                show.setTitle(details.getString(12));
                show.setStatus(details.getInt(13));
                show.setImdbId(details.getString(14));
                show.setNextEpisode(details.getLong(15));
                show.setLastEdit(details.getLong(16));
                show.setCountry(details.getString(17));
            }
            details.close();
        }

        return show;
    }

    public static boolean isShowExists(int showTvdbId, Context context) {
        Cursor testsearch = context.getContentResolver().query(Shows.buildShowUri(showTvdbId),
                new String[]{
                        Shows._ID
                }, null, null, null);
        if (testsearch == null) {
            return false;
        }
        boolean isShowExists = testsearch.getCount() != 0;
        testsearch.close();
        return isShowExists;
    }

    /**
     * Builds a {@link ContentProviderOperation} for inserting or updating a show (depending on
     * {@code isNew}.
     */
    public static ContentProviderOperation buildShowOp(Show show, Context context, boolean isNew) {
        ContentValues values = new ContentValues();
        values = putCommonShowValues(show, values);

        if (isNew) {
            values.put(Shows._ID, show.tvdbId);
            return ContentProviderOperation.newInsert(Shows.CONTENT_URI).withValues(values).build();
        } else {
            return ContentProviderOperation
                    .newUpdate(Shows.buildShowUri(String.valueOf(show.tvdbId)))
                    .withValues(values).build();
        }
    }

    /**
     * Transforms a {@link Show} objects attributes into {@link ContentValues} using the correct
     * {@link Shows} columns.
     */
    private static ContentValues putCommonShowValues(Show show, ContentValues values) {
        values.put(Shows.TITLE, show.title);
        values.put(Shows.OVERVIEW, show.overview);
        values.put(Shows.ACTORS, show.actors);
        values.put(Shows.AIRSDAYOFWEEK, show.airday);
        values.put(Shows.AIRSTIME, show.airtime);
        values.put(Shows.RELEASE_COUNTRY, show.country);
        values.put(Shows.FIRSTAIRED, show.firstAired);
        values.put(Shows.GENRES, show.genres);
        values.put(Shows.NETWORK, show.network);
        values.put(Shows.RATING, show.rating);
        values.put(Shows.RUNTIME, show.runtime);
        values.put(Shows.CONTENTRATING, show.contentRating);
        values.put(Shows.POSTER, show.poster);
        values.put(Shows.IMDBID, show.imdbId);
        values.put(Shows.LASTEDIT, show.lastEdited);
        values.put(Shows.LASTUPDATED, System.currentTimeMillis());
        int status;
        if (ShowStatusExport.CONTINUING.equals(show.status)) {
            status = ShowStatus.CONTINUING;
        } else if (ShowStatusExport.ENDED.equals(show.status)) {
            status = ShowStatus.ENDED;
        } else {
            status = ShowStatus.UNKNOWN;
        }
        values.put(Shows.STATUS, status);
        return values;
    }

    /**
     * Returns the episode IDs and their last edit time for a given show as a efficiently searchable
     * HashMap.
     *
     * @return HashMap containing the shows existing episodes
     */
    public static HashMap<Integer, Long> getEpisodeMapForShow(Context context, int showTvdbId) {
        Cursor episodes = context.getContentResolver().query(
                Episodes.buildEpisodesOfShowUri(showTvdbId), new String[]{
                Episodes._ID, Episodes.LAST_EDITED
        }, null, null, null);
        HashMap<Integer, Long> episodeMap = new HashMap<>();
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
                }, null, null, null);
        HashSet<Integer> seasonIds = new HashSet<>();
        if (seasons != null) {
            while (seasons.moveToNext()) {
                seasonIds.add(seasons.getInt(0));
            }
            seasons.close();
        }
        return seasonIds;
    }

    /**
     * Creates an update {@link ContentProviderOperation} for the given episode values.
     */
    public static ContentProviderOperation buildEpisodeUpdateOp(ContentValues values) {
        final String episodeId = values.getAsString(Episodes._ID);
        ContentProviderOperation op = ContentProviderOperation
                .newUpdate(Episodes.buildEpisodeUri(episodeId))
                .withValues(values).build();
        return op;
    }

    /**
     * Creates a {@link ContentProviderOperation} for insert if isNew, or update instead for with
     * the given season values.
     */
    public static ContentProviderOperation buildSeasonOp(ContentValues values, boolean isNew) {
        ContentProviderOperation op;
        final String seasonId = values.getAsString(Seasons.REF_SEASON_ID);
        final ContentValues seasonValues = new ContentValues();
        seasonValues.put(Seasons.COMBINED, values.getAsString(Episodes.SEASON));

        if (isNew) {
            seasonValues.put(Seasons._ID, seasonId);
            seasonValues.put(Shows.REF_SHOW_ID, values.getAsString(Shows.REF_SHOW_ID));
            op = ContentProviderOperation.newInsert(Seasons.CONTENT_URI).withValues(seasonValues)
                    .build();
        } else {
            op = ContentProviderOperation.newUpdate(Seasons.buildSeasonUri(seasonId))
                    .withValues(seasonValues).build();
        }
        return op;
    }

    /**
     * Convenience method for calling {@code updateLatestEpisode} once. If it is going to be called
     * multiple times, use the version which passes more data.
     */
    public static long updateLatestEpisode(Context context, int showTvdbId) {
        final boolean isNoReleasedEpisodes = DisplaySettings.isNoReleasedEpisodes(context);
        final boolean isNoSpecials = DisplaySettings.isHidingSpecials(context);
        return updateLatestEpisode(context, showTvdbId, isNoReleasedEpisodes, isNoSpecials);
    }

    /**
     * Update the latest episode fields of the given show.
     *
     * @return The TVDb id of the calculated next episode.
     */
    public static long updateLatestEpisode(Context context, int showTvdbId,
            boolean isNoReleasedEpisodes, boolean isNoSpecials) {
        final Uri episodesWithShow = Episodes.buildEpisodesOfShowUri(showTvdbId);
        final StringBuilder selectQuery = new StringBuilder();

        // STEP 1: get last watched episode
        final Cursor show = context.getContentResolver().query(Shows.buildShowUri(showTvdbId),
                new String[]{
                        Shows._ID, Shows.LASTWATCHEDID
                }, null, null, null);
        if (show == null || !show.moveToFirst()) {
            if (show != null) {
                show.close();
            }
            return 0;
        }
        final String lastEpisodeId = show.getString(1);
        show.close();

        final Cursor lastEpisode = context.getContentResolver().query(
                Episodes.buildEpisodeUri(lastEpisodeId), NextEpisodeQuery.PROJECTION_WATCHED, null,
                null, null);
        final String season;
        final String number;
        final String airtime;
        if (lastEpisode != null && lastEpisode.moveToFirst()) {
            season = lastEpisode.getString(NextEpisodeQuery.SEASON);
            number = lastEpisode.getString(NextEpisodeQuery.NUMBER);
            airtime = lastEpisode.getString(NextEpisodeQuery.FIRST_RELEASE_MS);
        } else {
            // no watched episodes, include all starting with
            // special 0
            season = "-1";
            number = "-1";
            airtime = String.valueOf(Long.MIN_VALUE);
        }
        if (lastEpisode != null) {
            lastEpisode.close();
        }

        // STEP 2: get episode airing closest afterwards or at the same time,
        // but with a higher number
        final String[] selectionArgs;
        selectQuery.delete(0, selectQuery.length());
        selectQuery.append(NextEpisodeQuery.SELECT_NEXT);
        if (isNoSpecials) {
            // do not take specials into account
            selectQuery.append(Episodes.SELECTION_NOSPECIALS);
        }
        if (isNoReleasedEpisodes) {
            // restrict to episodes with future release date
            selectQuery.append(NextEpisodeQuery.SELECT_ONLYFUTURE);
            selectionArgs = new String[]{
                    airtime, number, season, airtime,
                    String.valueOf(TimeTools.getCurrentTime(context))
            };
        } else {
            // restrict to episodes with any valid air date
            selectQuery.append(NextEpisodeQuery.SELECT_WITHAIRDATE);
            selectionArgs = new String[]{
                    airtime, number, season, airtime
            };
        }

        final Cursor next = context.getContentResolver().query(episodesWithShow,
                NextEpisodeQuery.PROJECTION_NEXT, selectQuery.toString(), selectionArgs,
                NextEpisodeQuery.SORTING_NEXT);

        // STEP 3: build and execute database update for show
        final long episodeId;
        final ContentValues update = new ContentValues();
        if (next != null && next.moveToFirst()) {
            // next episode text, e.g. '0x12 Episode Name'
            final String nextEpisodeString = Utils.getNextEpisodeString(context,
                    next.getInt(NextEpisodeQuery.SEASON), next.getInt(NextEpisodeQuery.NUMBER),
                    next.getString(NextEpisodeQuery.TITLE));

            // next release date text, e.g. "in 15 mins (Fri)"
            long releaseTime = next.getLong(NextEpisodeQuery.FIRST_RELEASE_MS);
            Date actualRelease = TimeTools.getEpisodeReleaseTime(context, releaseTime);
            final String nextReleaseDateString = context.getString(R.string.release_date_and_day,
                    TimeTools.formatToRelativeLocalReleaseTime(actualRelease),
                    TimeTools.formatToLocalReleaseDay(actualRelease));

            episodeId = next.getLong(NextEpisodeQuery._ID);
            update.put(Shows.NEXTEPISODE, episodeId);
            update.put(Shows.NEXTAIRDATEMS, releaseTime);
            update.put(Shows.NEXTTEXT, nextEpisodeString);
            update.put(Shows.NEXTAIRDATETEXT, nextReleaseDateString);
        } else {
            episodeId = 0;
            update.put(Shows.NEXTEPISODE, "");
            update.put(Shows.NEXTAIRDATEMS, UNKNOWN_NEXT_AIR_DATE);
            update.put(Shows.NEXTTEXT, "");
            update.put(Shows.NEXTAIRDATETEXT, "");
        }
        if (next != null) {
            next.close();
        }

        // update the show with the new next episode values
        context.getContentResolver().update(Shows.buildShowUri(showTvdbId), update, null, null);

        return episodeId;
    }

    /**
     * Applies a large {@link ContentProviderOperation} batch in smaller batches as not to overload
     * the transaction cache.
     */
    public static void applyInSmallBatches(Context context,
            ArrayList<ContentProviderOperation> batch) {
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

    private static void applyBatch(Context context, ArrayList<ContentProviderOperation> batch) {
        try {
            context.getContentResolver()
                    .applyBatch(SeriesGuideApplication.CONTENT_AUTHORITY, batch);
        } catch (RemoteException | OperationApplicationException e) {
            // RemoteException: Failed binder transactions aren't recoverable
            // OperationApplicationException: Failures like constraint violation aren't
            // recoverable
            throw new RuntimeException("Problem applying batch operation", e);
        }
    }

    private interface NextEpisodeQuery {

        /**
         * Unwatched, airing later or has a different number or season if airing the same time.
         */
        String SELECT_NEXT = Episodes.WATCHED + "=0 AND ("
                + "(" + Episodes.FIRSTAIREDMS + "=? AND "
                + "(" + Episodes.NUMBER + "!=? OR " + Episodes.SEASON + "!=?)) "
                + "OR " + Episodes.FIRSTAIREDMS + ">?)";

        String SELECT_WITHAIRDATE = " AND " + Episodes.FIRSTAIREDMS + "!=-1";

        String SELECT_ONLYFUTURE = " AND " + Episodes.FIRSTAIREDMS + ">=?";

        String[] PROJECTION_WATCHED = new String[]{
                Episodes._ID, Episodes.SEASON, Episodes.NUMBER, Episodes.FIRSTAIREDMS
        };

        String[] PROJECTION_NEXT = new String[]{
                Episodes._ID, Episodes.SEASON, Episodes.NUMBER, Episodes.FIRSTAIREDMS,
                Episodes.TITLE
        };

        /**
         * Air time, then lowest season, or if identical lowest episode number.
         */
        String SORTING_NEXT = Episodes.FIRSTAIREDMS + " ASC," + Episodes.SEASON + " ASC,"
                + Episodes.NUMBER + " ASC";

        int _ID = 0;

        int SEASON = 1;

        int NUMBER = 2;

        int FIRST_RELEASE_MS = 3;

        int TITLE = 4;
    }
}
