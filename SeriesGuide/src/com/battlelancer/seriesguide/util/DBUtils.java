/*
 * Copyright 2011 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.util;

import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

import com.battlelancer.seriesguide.SeriesGuideApplication;
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.ShowStatusExport;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.items.Series;
import com.battlelancer.seriesguide.provider.SeriesContract.EpisodeSearch;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.ui.UpcomingFragment.UpcomingQuery;
import com.battlelancer.seriesguide.util.FlagTask.OnFlagListener;
import com.battlelancer.thetvdbapi.TheTVDB.ShowStatus;
import com.uwetrottmann.seriesguide.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class DBUtils {

    static final String TAG = "SeriesDatabase";

    /**
     * Use 9223372036854775807 (Long.MAX_VALUE) for unknown airtime/no next
     * episode so they will get sorted last.
     */
    public static final String UNKNOWN_NEXT_AIR_DATE = "9223372036854775807";

    interface UnwatchedQuery {
        static final String[] PROJECTION = new String[] {
                Episodes._ID
        };

        static final String NOAIRDATE_SELECTION = Episodes.WATCHED + "=? AND "
                + Episodes.FIRSTAIREDMS + "=?";

        static final String FUTURE_SELECTION = Episodes.WATCHED + "=? AND " + Episodes.FIRSTAIREDMS
                + ">?";

        static final String AIRED_SELECTION = Episodes.WATCHED + "=? AND " + Episodes.FIRSTAIREDMS
                + " !=? AND " + Episodes.FIRSTAIREDMS + "<=?";
    }

    /**
     * Looks up the episodes of a given season and stores the count of already
     * aired, but not watched ones in the seasons watchcount.
     * 
     * @param context
     * @param seasonid
     * @param prefs
     */
    public static void updateUnwatchedCount(Context context, String seasonid,
            SharedPreferences prefs) {
        final ContentResolver resolver = context.getContentResolver();
        final String fakenow = String.valueOf(Utils.getFakeCurrentTime(prefs));
        final Uri episodesOfSeasonUri = Episodes.buildEpisodesOfSeasonUri(seasonid);

        // all a seasons episodes
        final Cursor total = resolver.query(episodesOfSeasonUri, new String[] {
                Episodes._ID
        }, null, null, null);
        final int totalcount = total.getCount();
        total.close();

        // unwatched, aired episodes
        final Cursor unwatched = resolver.query(episodesOfSeasonUri, UnwatchedQuery.PROJECTION,
                UnwatchedQuery.AIRED_SELECTION, new String[] {
                        "0", "-1", fakenow
                }, null);
        final int count = unwatched.getCount();
        unwatched.close();

        // unwatched, aired in the future episodes
        final Cursor unaired = resolver.query(episodesOfSeasonUri, UnwatchedQuery.PROJECTION,
                UnwatchedQuery.FUTURE_SELECTION, new String[] {
                        "0", fakenow
                }, null);
        final int unaired_count = unaired.getCount();
        unaired.close();

        // unwatched, no airdate
        final Cursor noairdate = resolver.query(episodesOfSeasonUri, UnwatchedQuery.PROJECTION,
                UnwatchedQuery.NOAIRDATE_SELECTION, new String[] {
                        "0", "-1"
                }, null);
        final int noairdate_count = noairdate.getCount();
        noairdate.close();

        final ContentValues update = new ContentValues();
        update.put(Seasons.WATCHCOUNT, count);
        update.put(Seasons.UNAIREDCOUNT, unaired_count);
        update.put(Seasons.NOAIRDATECOUNT, noairdate_count);
        update.put(Seasons.TOTALCOUNT, totalcount);
        resolver.update(Seasons.buildSeasonUri(seasonid), update, null, null);
    }

    /**
     * Returns a string of how many episodes of a show are left to watch (only
     * aired and not watched, exclusive episodes with no air date).
     * 
     * @param context
     * @param showId
     * @param prefs
     */
    public static String getUnwatchedEpisodesOfShow(Context context, String showId,
            SharedPreferences prefs) {
        if (context == null) {
            return "";
        }
        final ContentResolver resolver = context.getContentResolver();
        final String fakenow = String.valueOf(Utils.getFakeCurrentTime(prefs));
        final Uri episodesOfShowUri = Episodes.buildEpisodesOfShowUri(showId);

        // unwatched, aired episodes
        final Cursor unwatched = resolver.query(episodesOfShowUri, UnwatchedQuery.PROJECTION,
                UnwatchedQuery.AIRED_SELECTION, new String[] {
                        "0", "-1", fakenow
                }, null);
        final int count = unwatched.getCount();
        unwatched.close();

        return context.getString(R.string.remaining, count);
    }

    /**
     * Calls {@code getUpcomingEpisodes(false, context)}.
     * 
     * @param context
     * @return
     */
    public static Cursor getUpcomingEpisodes(Context context) {
        return getUpcomingEpisodes(false, context);
    }

    /**
     * Returns all episodes that air today or later. Using Pacific Time to
     * determine today. Excludes shows that are hidden.
     * 
     * @return Cursor including episodes with show title, network, airtime and
     *         posterpath.
     */
    public static Cursor getUpcomingEpisodes(boolean isOnlyUnwatched, Context context) {
        String[][] args = buildActivityQuery(UpcomingQuery.QUERY_UPCOMING, isOnlyUnwatched, context);

        return context.getContentResolver().query(Episodes.CONTENT_URI_WITHSHOW,
                UpcomingQuery.PROJECTION, args[0][0], args[1], UpcomingQuery.SORTING_UPCOMING);
    }

    /**
     * Calls {@code getRecentEpisodes(false, context)}.
     * 
     * @param context
     * @return
     */
    public static Cursor getRecentEpisodes(Context context) {
        return getRecentEpisodes(false, context);
    }

    /**
     * Return all episodes that aired the day before and earlier. Using Pacific
     * Time to determine today. Excludes shows that are hidden.
     * 
     * @param context
     * @return Cursor including episodes with show title, network, airtime and
     *         posterpath.
     */
    public static Cursor getRecentEpisodes(boolean isOnlyUnwatched, Context context) {
        String[][] args = buildActivityQuery(UpcomingQuery.QUERY_RECENT, isOnlyUnwatched, context);

        return context.getContentResolver().query(Episodes.CONTENT_URI_WITHSHOW,
                UpcomingQuery.PROJECTION, args[0][0], args[1], UpcomingQuery.SORTING_RECENT);
    }

    /**
     * Returns an array of size 2. The built query is stored in {@code [0][0]},
     * the built selection args in {@code [1]}.
     * 
     * @param query
     * @param isOnlyUnwatched
     * @param context
     * @return
     */
    private static String[][] buildActivityQuery(String query, boolean isOnlyUnwatched,
            Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // calc time threshold
        long fakeNow = Utils.getFakeCurrentTime(prefs);
        // go an hour back in time, so episodes move to recent one hour late
        fakeNow -= DateUtils.HOUR_IN_MILLIS;
        final String recentThreshold = String.valueOf(fakeNow);

        // build selection args
        String[] selectionArgs = new String[] {
                recentThreshold, "0"
        };

        // append only favorites selection if necessary
        boolean isOnlyFavorites = prefs.getBoolean(SeriesGuidePreferences.KEY_ONLYFAVORITES, false);
        if (isOnlyFavorites) {
            query += Shows.SELECTION_FAVORITES;
        }

        // append no specials selection if necessary
        boolean isNoSpecials = prefs.getBoolean(SeriesGuidePreferences.KEY_ONLY_SEASON_EPISODES,
                false);
        if (isNoSpecials) {
            query += Episodes.SELECTION_NOSPECIALS;
        }

        // append unwatched selection if necessary
        if (isOnlyUnwatched) {
            query += Episodes.SELECTION_NOWATCHED;
        }

        // build result array
        String[][] results = new String[2][];
        results[0] = new String[] {
                query
        };
        results[1] = selectionArgs;
        return results;
    }

    /**
     * Marks the next episode (if there is one) of the given show as watched.
     * Submits it to trakt if possible.
     * 
     * @param showId
     */
    public static void markNextEpisode(Context context, OnFlagListener listener, int showId,
            int episodeId) {
        if (episodeId > 0) {
            Cursor episode = context.getContentResolver().query(
                    Episodes.buildEpisodeUri(String.valueOf(episodeId)), new String[] {
                            Episodes.SEASON, Episodes.NUMBER
                    }, null, null, null);
            if (episode != null) {
                if (episode.moveToFirst()) {
                    new FlagTask(context, (int) showId, listener)
                            .episodeWatched(episode.getInt(0), episode.getInt(1))
                            .setItemId(episodeId).setFlag(true).execute();
                }
                episode.close();
            }
        }
    }

    private static final String[] SHOW_PROJECTION = new String[] {
            Shows._ID, Shows.ACTORS, Shows.AIRSDAYOFWEEK, Shows.AIRSTIME, Shows.CONTENTRATING,
            Shows.FIRSTAIRED, Shows.GENRES, Shows.NETWORK, Shows.OVERVIEW, Shows.POSTER,
            Shows.RATING, Shows.RUNTIME, Shows.TITLE, Shows.STATUS, Shows.IMDBID,
            Shows.NEXTEPISODE, Shows.LASTEDIT
    };

    /**
     * Returns a {@link Series} object populated with all needed fields, but not
     * all of them! Might Return {@code null} if there is no show with that id.
     */
    public static Series getShow(Context context, String showId) {
        Cursor details = context.getContentResolver().query(Shows.buildShowUri(showId),
                SHOW_PROJECTION, null,
                null, null);

        if (details != null) {
            if (details.moveToFirst()) {
                Series show = new Series();

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

                return show;
            }
            details.close();
        }

        return null;
    }

    public static boolean isShowExists(String showId, Context context) {
        Cursor testsearch = context.getContentResolver().query(Shows.buildShowUri(showId),
                new String[] {
                    Shows._ID
                }, null, null, null);
        boolean isShowExists = testsearch.getCount() != 0 ? true : false;
        testsearch.close();
        return isShowExists;
    }

    /**
     * Builds a {@link ContentProviderOperation} for inserting or updating a
     * show (depending on {@code isNew}.
     * 
     * @param show
     * @param context
     * @param isNew
     * @return
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
     * Transforms a {@link Show} objects attributes into {@link ContentValues}
     * using the correct {@link Shows} columns.
     */
    private static ContentValues putCommonShowValues(Show show, ContentValues values) {
        values.put(Shows.TITLE, show.title);
        values.put(Shows.OVERVIEW, show.overview);
        values.put(Shows.ACTORS, show.actors);
        values.put(Shows.AIRSDAYOFWEEK, show.airday);
        values.put(Shows.AIRSTIME, show.airtime);
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
     * Delete a show and manually delete its seasons and episodes. Also cleans
     * up the poster and images.
     * 
     * @param context
     * @param showId
     * @param progress
     */
    public static void deleteShow(Context context, String showId, ProgressDialog progress) {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
        final ImageProvider imageProvider = ImageProvider.getInstance(context);

        // get poster path of show
        final Cursor poster = context.getContentResolver().query(Shows.buildShowUri(showId),
                new String[] {
                    Shows.POSTER
                }, null, null, null);
        String posterPath = null;
        if (poster != null) {
            if (poster.moveToFirst()) {
                posterPath = poster.getString(0);
            }
            poster.close();
        }

        batch.add(ContentProviderOperation.newDelete(Shows.buildShowUri(showId)).build());

        // remove show entry already so we can hide the progress dialog
        try {
            context.getContentResolver()
                    .applyBatch(SeriesGuideApplication.CONTENT_AUTHORITY, batch);
        } catch (RemoteException e) {
            // Failed binder transactions aren't recoverable
            throw new RuntimeException("Problem applying batch operation", e);
        } catch (OperationApplicationException e) {
            // Failures like constraint violation aren't recoverable
            throw new RuntimeException("Problem applying batch operation", e);
        }

        batch.clear();

        // delete show poster
        if (posterPath != null) {
            imageProvider.removeImage(posterPath);
        }

        // delete episode images
        final Cursor episodes = context.getContentResolver().query(
                Episodes.buildEpisodesOfShowUri(showId), new String[] {
                        Episodes._ID, Episodes.IMAGE
                }, null, null, null);
        if (episodes != null) {
            final String[] episodeIDs = new String[episodes.getCount()];
            int counter = 0;

            episodes.moveToFirst();
            while (!episodes.isAfterLast()) {
                episodeIDs[counter++] = episodes.getString(0);
                imageProvider.removeImage(episodes.getString(1));
                episodes.moveToNext();
            }
            episodes.close();

            // delete search database entries
            for (String episodeID : episodeIDs) {
                batch.add(ContentProviderOperation.newDelete(
                        EpisodeSearch.buildDocIdUri(String.valueOf(episodeID))).build());
            }
        }

        batch.add(ContentProviderOperation.newDelete(Seasons.buildSeasonsOfShowUri(showId)).build());
        batch.add(ContentProviderOperation.newDelete(Episodes.buildEpisodesOfShowUri(showId))
                .build());

        try {
            context.getContentResolver()
                    .applyBatch(SeriesGuideApplication.CONTENT_AUTHORITY, batch);
        } catch (RemoteException e) {
            // Failed binder transactions aren't recoverable
            throw new RuntimeException("Problem applying batch operation", e);
        } catch (OperationApplicationException e) {
            // Failures like constraint violation aren't recoverable
            throw new RuntimeException("Problem applying batch operation", e);
        }

        // hide progress dialog now
        if (progress.isShowing()) {
            progress.dismiss();
        }
    }

    /**
     * Returns the episode IDs and their last edit time for a given show as a
     * efficiently searchable HashMap.
     * 
     * @param showId
     * @return HashMap containing the shows existing episodes
     */
    public static HashMap<Long, Long> getEpisodeMapForShow(String showId, Context context) {
        Cursor eptest = context.getContentResolver().query(
                Episodes.buildEpisodesOfShowUri(showId), new String[] {
                        Episodes._ID, Episodes.LAST_EDITED
                }, null, null, null);
        HashMap<Long, Long> episodeMap = new HashMap<Long, Long>();
        if (eptest != null) {
            eptest.moveToFirst();
            while (!eptest.isAfterLast()) {
                episodeMap.put(eptest.getLong(0), eptest.getLong(1));
                eptest.moveToNext();
            }
            eptest.close();
        }
        return episodeMap;
    }

    /**
     * Returns the season IDs for a given show as a efficiently searchable
     * HashMap.
     * 
     * @param seriesid
     * @return HashMap containing the shows existing seasons
     */
    public static HashSet<Long> getSeasonIDsForShow(String seriesid, Context context) {
        Cursor setest = context.getContentResolver().query(Seasons.buildSeasonsOfShowUri(seriesid),
                new String[] {
                    Seasons._ID
                }, null, null, null);
        HashSet<Long> seasonIDs = new HashSet<Long>();
        while (setest.moveToNext()) {
            seasonIDs.add(setest.getLong(0));
        }
        setest.close();
        return seasonIDs;
    }

    /**
     * Creates a {@link ContentProviderOperation} for insert if isNew, or update
     * instead for with the given episode values.
     * 
     * @param values
     * @param isNew
     * @return
     */
    public static ContentProviderOperation buildEpisodeOp(ContentValues values, boolean isNew) {
        ContentProviderOperation op;
        if (isNew) {
            op = ContentProviderOperation.newInsert(Episodes.CONTENT_URI).withValues(values)
                    .build();
        } else {
            final String episodeId = values.getAsString(Episodes._ID);
            op = ContentProviderOperation.newUpdate(Episodes.buildEpisodeUri(episodeId))
                    .withValues(values).build();
        }
        return op;
    }

    /**
     * Creates a {@link ContentProviderOperation} for insert if isNew, or update
     * instead for with the given season values.
     * 
     * @param values
     * @param isNew
     * @return
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
     * Convenience method for calling {@code updateLatestEpisode} once. If it is
     * going to be called multiple times, use the version which passes more
     * data.
     * 
     * @param context
     * @param showId
     */
    public static long updateLatestEpisode(Context context, String showId) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean isOnlyFutureEpisodes = prefs.getBoolean(
                SeriesGuidePreferences.KEY_ONLY_FUTURE_EPISODES, false);
        final boolean isNoSpecials = prefs.getBoolean(
                SeriesGuidePreferences.KEY_ONLY_SEASON_EPISODES, false);
        return updateLatestEpisode(context, showId, isOnlyFutureEpisodes, isNoSpecials, prefs);
    }

    /**
     * Update the latest episode fields of the show where {@link Shows._ID}
     * equals the given {@code id}.
     * 
     * @return The id of the calculated next episode.
     */
    public static long updateLatestEpisode(Context context, String showId,
            boolean isOnlyFutureEpisodes, boolean isNoSpecials, SharedPreferences prefs) {
        final Uri episodesWithShow = Episodes.buildEpisodesOfShowUri(showId);
        final StringBuilder selectQuery = new StringBuilder();

        // STEP 1: get last watched episode
        final Cursor show = context.getContentResolver().query(Shows.buildShowUri(showId),
                new String[] {
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
            airtime = lastEpisode.getString(NextEpisodeQuery.FIRSTAIREDMS);
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
        if (isOnlyFutureEpisodes) {
            // restrict to episodes with future air date
            selectQuery.append(NextEpisodeQuery.SELECT_ONLYFUTURE);
            final String now = String.valueOf(Utils.getFakeCurrentTime(prefs));
            selectionArgs = new String[] {
                    airtime, number, season, airtime, now
            };
        } else {
            // restrict to episodes with any valid air date
            selectQuery.append(NextEpisodeQuery.SELECT_WITHAIRDATE);
            selectionArgs = new String[] {
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
            final String nextEpisodeString = Utils.getNextEpisodeString(prefs,
                    next.getInt(NextEpisodeQuery.SEASON), next.getInt(NextEpisodeQuery.NUMBER),
                    next.getString(NextEpisodeQuery.TITLE));

            // next air date text, e.g. 'Apr 2 (Mon)'
            final long airTime = next.getLong(NextEpisodeQuery.FIRSTAIREDMS);
            final String[] dayAndTimes = Utils.formatToTimeAndDay(airTime, context);
            final String nextAirdateString = dayAndTimes[2] + " (" + dayAndTimes[1] + ")";

            episodeId = next.getLong(NextEpisodeQuery._ID);
            update.put(Shows.NEXTEPISODE, episodeId);
            update.put(Shows.NEXTAIRDATEMS, airTime);
            update.put(Shows.NEXTTEXT, nextEpisodeString);
            update.put(Shows.NEXTAIRDATETEXT, nextAirdateString);
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
        context.getContentResolver().update(Shows.buildShowUri(showId), update, null, null);

        return episodeId;
    }

    private interface NextEpisodeQuery {
        /**
         * Unwatched, airing later or has a different number or season if airing
         * the same time.
         */
        String SELECT_NEXT = Episodes.WATCHED + "=0 AND ("
                + "(" + Episodes.FIRSTAIREDMS + "=? AND "
                + "(" + Episodes.NUMBER + "!=? OR " + Episodes.SEASON + "!=?)) "
                + "OR " + Episodes.FIRSTAIREDMS + ">?)";

        String SELECT_WITHAIRDATE = " AND " + Episodes.FIRSTAIREDMS + "!=-1";

        String SELECT_ONLYFUTURE = " AND " + Episodes.FIRSTAIREDMS + ">=?";

        String[] PROJECTION_WATCHED = new String[] {
                Episodes._ID, Episodes.SEASON, Episodes.NUMBER, Episodes.FIRSTAIREDMS
        };

        String[] PROJECTION_NEXT = new String[] {
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

        int FIRSTAIREDMS = 3;

        int TITLE = 4;
    }
}
