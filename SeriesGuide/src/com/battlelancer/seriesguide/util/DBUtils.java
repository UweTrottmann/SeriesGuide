
package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.items.Series;
import com.battlelancer.seriesguide.provider.SeriesContract;
import com.battlelancer.seriesguide.provider.SeriesContract.EpisodeSearch;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.ui.UpcomingFragment.UpcomingQuery;
import com.battlelancer.thetvdbapi.ImageCache;

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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

public class DBUtils {

    static final String TAG = "SeriesDatabase";

    /**
     * Use 9999 for unkown airdates/no next episodes, sorting then assumes year
     * 9999 and sorts these last.
     */
    public static final String UNKNOWN_NEXT_AIR_DATE = "9999";

    /**
     * Looks up the episodes of a given season and stores the count of already
     * aired, but not watched ones in the seasons watchcount.
     * 
     * @param seasonid
     */
    public static void updateUnwatchedCount(Context context, String seasonid) {
        ContentResolver resolver = context.getContentResolver();
        Date date = new Date();
        String today = Constants.theTVDBDateFormat.format(date);
        Uri episodesOfSeasonUri = Episodes.buildEpisodesOfSeasonUri(seasonid);

        // all a seasons episodes
        Cursor total = resolver.query(episodesOfSeasonUri, new String[] {
            Episodes._ID
        }, null, null, null);
        final int totalcount = total.getCount();
        total.close();

        // unwatched, aired episodes
        String selection = Episodes.WATCHED + "=? AND " + Episodes.FIRSTAIRED + " like '%-%'"
                + " AND " + Episodes.FIRSTAIRED + "<=?";
        Cursor unwatched = resolver.query(episodesOfSeasonUri, new String[] {
            Episodes._ID
        }, selection, new String[] {
                "0", today
        }, null);
        final int count = unwatched.getCount();
        unwatched.close();

        // unwatched, aired in the future episodes
        selection = Episodes.WATCHED + "=? AND " + Episodes.FIRSTAIRED + ">?";
        Cursor unaired = resolver.query(episodesOfSeasonUri, new String[] {
            Episodes._ID
        }, selection, new String[] {
                "0", today
        }, null);
        final int unaired_count = unaired.getCount();
        unaired.close();

        // unwatched, no airdate
        selection = Episodes.WATCHED + "=? AND " + Episodes.FIRSTAIRED + "=?";
        Cursor noairdate = resolver.query(episodesOfSeasonUri, new String[] {
            Episodes._ID
        }, selection, new String[] {
                "0", ""
        }, null);

        final int noairdate_count = noairdate.getCount();
        noairdate.close();

        ContentValues update = new ContentValues();
        update.put(Seasons.WATCHCOUNT, count);
        update.put(Seasons.UNAIREDCOUNT, unaired_count);
        update.put(Seasons.NOAIRDATECOUNT, noairdate_count);
        update.put(Seasons.TOTALCOUNT, totalcount);
        resolver.update(Seasons.buildSeasonUri(seasonid), update, null, null);
    }

    /**
     * Returns all episodes that air today or later. Using Pacific Time to
     * determine today. Excludes shows that are hidden.
     * 
     * @return Cursor including episodes with show title, network, airtime and
     *         posterpath.
     */
    public static Cursor getUpcomingEpisodes(Context context) {
        Calendar cal = Calendar.getInstance();
        // go an hour back in time, so episodes move to recent one hour late
        cal.add(Calendar.HOUR_OF_DAY, -1);
        final String recentThreshold = String.valueOf(cal.getTimeInMillis());
        String query = UpcomingQuery.QUERY_UPCOMING;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isOnlyFavorites = prefs.getBoolean(SeriesGuidePreferences.KEY_ONLYFAVORITES, false);
        String[] selectionArgs;
        if (isOnlyFavorites) {
            query += UpcomingQuery.SELECTION_ONLYFAVORITES;
            selectionArgs = new String[] {
                    recentThreshold, "0", "1"
            };
        } else {
            selectionArgs = new String[] {
                    recentThreshold, "0"
            };
        }

        return context.getContentResolver().query(Episodes.CONTENT_URI_WITHSHOW,
                UpcomingQuery.PROJECTION, query, selectionArgs, UpcomingQuery.SORTING_UPCOMING);
    }

    /**
     * Return all episodes that aired the day before and earlier. Using Pacific
     * Time to determine today. Excludes shows that are hidden.
     * 
     * @param context
     * @return Cursor including episodes with show title, network, airtime and
     *         posterpath.
     */
    public static Cursor getRecentEpisodes(Context context) {
        Calendar cal = Calendar.getInstance();
        // go an hour back in time, so episodes move to recent one hour late
        cal.add(Calendar.HOUR_OF_DAY, -1);
        final String recentThreshold = String.valueOf(cal.getTimeInMillis());
        String query = UpcomingQuery.QUERY_RECENT;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isOnlyFavorites = prefs.getBoolean(SeriesGuidePreferences.KEY_ONLYFAVORITES, false);
        String[] selectionArgs;
        if (isOnlyFavorites) {
            query += UpcomingQuery.SELECTION_ONLYFAVORITES;
            selectionArgs = new String[] {
                    recentThreshold, "0", "1"
            };
        } else {
            selectionArgs = new String[] {
                    recentThreshold, "0"
            };
        }

        return context.getContentResolver().query(Episodes.CONTENT_URI_WITHSHOW,
                UpcomingQuery.PROJECTION, query, selectionArgs, UpcomingQuery.SORTING_RECENT);
    }

    /**
     * Updates the given boolean to the EPISODE_WATCHED column of the given
     * episode row. Be aware that the database stores the value as an integer.
     * 
     * @param rowid
     * @param state
     */
    public static void markEpisode(Context context, String episodeId, boolean state) {
        ContentValues values = new ContentValues();
        values.put(Episodes.WATCHED, state);

        context.getContentResolver()
                .update(Episodes.buildEpisodeUri(episodeId), values, null, null);
        context.getContentResolver().notifyChange(Episodes.CONTENT_URI, null);
    }

    /**
     * Marks the next episode (if there is one) of this show as watched.
     * 
     * @param seriesid
     */
    public static void markNextEpisode(Context context, long seriesid) {
        Cursor show = context.getContentResolver().query(
                Shows.buildShowUri(String.valueOf(seriesid)), new String[] {
                    Shows.NEXTEPISODE
                }, null, null, null);
        show.moveToFirst();
        ContentValues values = new ContentValues();
        values.put(Episodes.WATCHED, true);
        String episodeid = show.getString(show.getColumnIndexOrThrow(Shows.NEXTEPISODE));
        if (episodeid.length() != 0) {
            context.getContentResolver().update(Episodes.buildEpisodeUri(episodeid), values, null,
                    null);
        }
        show.close();
    }

    /**
     * Updates all EPISODE_WATCHED columns of the episodes of the given season
     * with the given boolean. Be aware that the database stores the value as an
     * integer.
     * 
     * @param seasonid
     * @param state
     */
    public static void markSeasonEpisodes(Context context, String seasonId, boolean state) {
        ContentValues values = new ContentValues();
        values.put(Episodes.WATCHED, state);
        context.getContentResolver().update(Episodes.buildEpisodesOfSeasonUri(seasonId), values,
                null, null);
    }

    /**
     * Marks all episodes of a show that have aired before the air date of the
     * given episode.
     * 
     * @param context
     * @param episodeId
     */
    public static void markUntilHere(Context context, String episodeId) {
        Cursor episode = context.getContentResolver().query(Episodes.buildEpisodeUri(episodeId),
                new String[] {
                        Episodes.FIRSTAIRED, Shows.REF_SHOW_ID
                }, null, null, null);
        episode.moveToFirst();
        final String untilDate = episode.getString(0);
        final String showId = episode.getString(1);
        episode.close();

        if (untilDate.length() != 0) {
            ContentValues values = new ContentValues();
            values.put(Episodes.WATCHED, true);

            context.getContentResolver().update(Episodes.buildEpisodesOfShowUri(showId), values,
                    Episodes.FIRSTAIRED + "<? AND " + Episodes.FIRSTAIRED + "!=''", new String[] {
                        untilDate
                    });
            context.getContentResolver().notifyChange(Episodes.CONTENT_URI, null);
        }
    }

    /**
     * Fetches the row to a given show id and returns the results an Series
     * object. Returns {@code null} if there is no show with that id.
     * 
     * @param seriesid
     * @return
     */
    public static Series getShow(Context context, String showId) {
        Series show = new Series();
        Cursor details = context.getContentResolver().query(Shows.buildShowUri(showId), null, null,
                null, null);
        if (details.moveToFirst()) {
            show.setActors(details.getString(details.getColumnIndexOrThrow(Shows.ACTORS)));
            show.setAirsDayOfWeek(details.getString(details
                    .getColumnIndexOrThrow(Shows.AIRSDAYOFWEEK)));
            show.setAirsTime(details.getLong(details.getColumnIndexOrThrow(Shows.AIRSTIME)));
            show.setContentRating(details.getString(details
                    .getColumnIndexOrThrow(Shows.CONTENTRATING)));
            show.setFirstAired(details.getString(details.getColumnIndexOrThrow(Shows.FIRSTAIRED)));
            show.setGenres(details.getString(details.getColumnIndexOrThrow(Shows.GENRES)));
            show.setId(details.getString(details.getColumnIndexOrThrow(Shows._ID)));
            show.setNetwork(details.getString(details.getColumnIndexOrThrow(Shows.NETWORK)));
            show.setOverview(details.getString(details.getColumnIndexOrThrow(Shows.OVERVIEW)));
            show.setPoster(details.getString(details.getColumnIndexOrThrow(Shows.POSTER)));
            show.setRating(details.getString(details.getColumnIndexOrThrow(Shows.RATING)));
            show.setRuntime(details.getString(details.getColumnIndexOrThrow(Shows.RUNTIME)));
            show.setSeriesId(details.getString(details.getColumnIndexOrThrow(Shows._ID)));
            show.setSeriesName(details.getString(details.getColumnIndexOrThrow(Shows.TITLE)));
            show.setStatus(details.getInt(details.getColumnIndexOrThrow(Shows.STATUS)));
            show.setImdbId(details.getString(details.getColumnIndexOrThrow(Shows.IMDBID)));
            show.setNextEpisode(details.getLong(details.getColumnIndexOrThrow(Shows.NEXTEPISODE)));
        } else {
            show = null;
        }
        details.close();
        return show;
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
    public static ContentProviderOperation buildShowOp(Series show, Context context, boolean isNew) {
        ContentValues values = new ContentValues();
        values = putCommonShowValues(show, values);

        if (isNew) {
            values.put(Shows._ID, show.getId());
            return ContentProviderOperation.newInsert(Shows.CONTENT_URI).withValues(values).build();
        } else {
            return ContentProviderOperation.newUpdate(Shows.buildShowUri(show.getId()))
                    .withValues(values).build();
        }
    }

    /**
     * Adds default show information from given Series object to given
     * ContentValues.
     * 
     * @param show
     * @param values
     * @return
     */
    private static ContentValues putCommonShowValues(Series show, ContentValues values) {
        values.put(Shows.TITLE, show.getSeriesName());
        values.put(Shows.OVERVIEW, show.getOverview());
        values.put(Shows.ACTORS, show.getActors());
        values.put(Shows.AIRSDAYOFWEEK, show.getAirsDayOfWeek());
        values.put(Shows.AIRSTIME, show.getAirsTime());
        values.put(Shows.FIRSTAIRED, show.getFirstAired());
        values.put(Shows.GENRES, show.getGenres());
        values.put(Shows.NETWORK, show.getNetwork());
        values.put(Shows.RATING, show.getRating());
        values.put(Shows.RUNTIME, show.getRuntime());
        values.put(Shows.STATUS, show.getStatus());
        values.put(Shows.CONTENTRATING, show.getContentRating());
        values.put(Shows.POSTER, show.getPoster());
        values.put(Shows.IMDBID, show.getImdbId());
        values.put(Shows.LASTUPDATED, new Date().getTime());
        return values;
    }

    /**
     * Delete a show and manually delete its seasons and episodes. Also cleans
     * up the poster and images.
     * 
     * @param context
     * @param id
     */
    public static void deleteShow(Context context, String id) {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
        final String showId = String.valueOf(id);
        final ImageCache imageCache = ImageCache.getInstance(context);

        // delete images...
        // ...of show
        final Cursor poster = context.getContentResolver().query(Shows.buildShowUri(showId),
                new String[] {
                    Shows.POSTER
                }, null, null, null);
        if (poster.moveToFirst()) {
            final String posterPath = poster.getString(0);
            imageCache.removeFromDisk(posterPath);
        }
        poster.close();

        // ...of episodes
        final Cursor episodes = context.getContentResolver().query(
                Episodes.buildEpisodesOfShowUri(showId), new String[] {
                        Episodes._ID, Episodes.IMAGE
                }, null, null, null);
        String[] episodeIDs = new String[episodes.getCount()];
        episodes.moveToFirst();
        int counter = 0;
        while (!episodes.isAfterLast()) {
            episodeIDs[counter++] = episodes.getString(0);
            imageCache.removeFromDisk(episodes.getString(1));
            episodes.moveToNext();
        }
        episodes.close();

        // delete database entries
        for (String episodeID : episodeIDs) {
            batch.add(ContentProviderOperation.newDelete(
                    EpisodeSearch.buildDocIdUri(String.valueOf(episodeID))).build());
        }

        batch.add(ContentProviderOperation.newDelete(Shows.buildShowUri(showId)).build());
        batch.add(ContentProviderOperation.newDelete(Seasons.buildSeasonsOfShowUri(showId)).build());
        batch.add(ContentProviderOperation.newDelete(Episodes.buildEpisodesOfShowUri(showId))
                .build());

        try {
            context.getContentResolver().applyBatch(SeriesContract.CONTENT_AUTHORITY, batch);
        } catch (RemoteException e) {
            // Failed binder transactions aren't recoverable
            throw new RuntimeException("Problem applying batch operation", e);
        } catch (OperationApplicationException e) {
            // Failures like constraint violation aren't recoverable
            throw new RuntimeException("Problem applying batch operation", e);
        }
    }

    /**
     * Returns the episode IDs for a given show as a efficiently searchable
     * HashMap.
     * 
     * @param seriesid
     * @return HashMap containing the shows existing episodes
     */
    public static HashSet<Long> getEpisodeIDsForShow(String seriesid, Context context) {
        Cursor eptest = context.getContentResolver().query(
                Episodes.buildEpisodesOfShowUri(seriesid), new String[] {
                    Episodes._ID
                }, null, null, null);
        HashSet<Long> episodeIDs = new HashSet<Long>();
        eptest.moveToFirst();
        while (!eptest.isAfterLast()) {
            episodeIDs.add(eptest.getLong(0));
            eptest.moveToNext();
        }
        eptest.close();
        return episodeIDs;
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
     * Update the latest episode fields of the show where {@link Shows._ID}
     * equals the given {@code id}.
     * 
     * @param id
     */
    public static long updateLatestEpisode(Context context, String id) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean onlyFutureEpisodes = prefs.getBoolean(
                SeriesGuidePreferences.KEY_ONLY_FUTURE_EPISODES, false);
        final boolean onlySeasonEpisodes = prefs.getBoolean(
                SeriesGuidePreferences.KEY_ONLY_SEASON_EPISODES, false);

        final String[] projection = new String[] {
                Episodes._ID, Episodes.FIRSTAIRED, Episodes.SEASON, Episodes.NUMBER, Episodes.TITLE
        };
        final String sortBy = Episodes.FIRSTAIRED + " ASC";
        final StringBuilder selection = new StringBuilder();
        String[] selectionArgs = null;

        selection.append(Episodes.WATCHED).append("=0");
        if (onlySeasonEpisodes) {
            selection.append(" AND ").append(Episodes.SEASON).append("!=0");
        }
        if (onlyFutureEpisodes) {
            selection.append(" AND ").append(Episodes.FIRSTAIRED).append(">=?");
            Date date = new Date();
            String today = Constants.theTVDBDateFormat.format(date);
            selectionArgs = new String[] {
                today
            };
        } else {
            selection.append(" AND ").append(Episodes.FIRSTAIRED).append(" like '%-%'");
        }

        final Cursor unwatched = context.getContentResolver().query(
                Episodes.buildEpisodesOfShowUri(id), projection, selection.toString(),
                selectionArgs, sortBy);

        // maybe there are no episodes due to errors, or airdates are just
        // unknown ("")
        long episodeid = 0;
        final ContentValues update = new ContentValues();
        if (unwatched.getCount() != 0) {
            unwatched.moveToFirst();

            // nexttext (0x12 Episode)
            final String season = unwatched.getString(unwatched
                    .getColumnIndexOrThrow(Episodes.SEASON));
            final String number = unwatched.getString(unwatched
                    .getColumnIndexOrThrow(Episodes.NUMBER));
            final String title = unwatched.getString(unwatched
                    .getColumnIndexOrThrow(Episodes.TITLE));
            String nextEpisodeString = Utils.getNextEpisodeString(prefs, season, number, title);

            // nextairdatetext
            String nextAirdateString = "";
            final String firstAired = unwatched.getString(unwatched
                    .getColumnIndexOrThrow(Episodes.FIRSTAIRED));
            if (firstAired.length() != 0) {
                final Series show = getShow(context, id);
                if (show != null) {
                    nextAirdateString += Utils.parseDateToLocalRelative(firstAired,
                            show.getAirsTime(), context);
                }
            }

            episodeid = unwatched.getLong(unwatched.getColumnIndexOrThrow(Episodes._ID));
            update.put(Shows.NEXTEPISODE, episodeid);
            update.put(Shows.NEXTAIRDATE,
                    unwatched.getString(unwatched.getColumnIndexOrThrow(Episodes.FIRSTAIRED)));
            update.put(Shows.NEXTTEXT, nextEpisodeString);
            update.put(Shows.NEXTAIRDATETEXT, nextAirdateString);
        } else {
            update.put(Shows.NEXTEPISODE, "");
            update.put(Shows.NEXTAIRDATE, UNKNOWN_NEXT_AIR_DATE);
            update.put(Shows.NEXTTEXT, "");
            update.put(Shows.NEXTAIRDATETEXT, "");
        }
        unwatched.close();

        context.getContentResolver().update(Shows.buildShowUri(id), update, null, null);

        return episodeid;
    }

}
