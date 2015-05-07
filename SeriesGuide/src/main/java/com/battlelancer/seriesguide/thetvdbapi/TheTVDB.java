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

package com.battlelancer.seriesguide.thetvdbapi;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.text.format.DateUtils;
import android.util.Xml;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.ShowStatusExport;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.AppSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.TraktTools;
import com.battlelancer.seriesguide.util.Utils;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.BaseShow;
import com.uwetrottmann.trakt.v2.enums.Extended;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.ZipInputStream;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Provides access to the TheTVDb.com XML API throwing in some additional data from trakt.tv here
 * and there.
 */
public class TheTVDB {

    private static final String TVDB_MIRROR_BANNERS = "http://thetvdb.com/banners/";

    private static final String TVDB_MIRROR_BANNERS_CACHE = TVDB_MIRROR_BANNERS + "_cache/";

    private static final String TVDB_API_URL = "http://thetvdb.com/api/";

    private static final String TVDB_API_SERIES = TVDB_API_URL + BuildConfig.TVDB_API_KEY
            + "/series/";

    private static final String TVDB_PATH_ALL = "all/";
    private static final String TVDB_EXTENSION_UNCOMPRESSED = ".xml";
    private static final String TVDB_EXTENSION_COMPRESSED = ".zip";
    private static final String TVDB_FILE_DEFAULT = "en" + TVDB_EXTENSION_COMPRESSED;

    /**
     * Builds a full url for a TVDb show poster using the given image path.
     */
    public static String buildPosterUrl(String imagePath) {
        return TVDB_MIRROR_BANNERS_CACHE + imagePath;
    }

    /**
     * Builds a full url for a TVDb screenshot (episode still) using the given image path.
     *
     * <p> May also be used with posters, but a much larger version than {@link
     * #buildPosterUrl(String)} will be downloaded as a result.
     */
    public static String buildScreenshotUrl(String imagePath) {
        return TVDB_MIRROR_BANNERS + imagePath;
    }

    /**
     * Returns true if the given show has not been updated in the last 12 hours.
     */
    public static boolean isUpdateShow(Context context, int showTvdbId) {
        final Cursor show = context.getContentResolver().query(Shows.buildShowUri(showTvdbId),
                new String[] {
                        Shows._ID, Shows.LASTUPDATED
                }, null, null, null
        );
        boolean isUpdate = false;
        if (show != null) {
            if (show.moveToFirst()) {
                long lastUpdateTime = show.getLong(1);
                if (System.currentTimeMillis() - lastUpdateTime > DateUtils.HOUR_IN_MILLIS * 12) {
                    isUpdate = true;
                }
            }
            show.close();
        }
        return isUpdate;
    }

    /**
     * Adds a show and its episodes to the database. If the show already exists, does nothing.
     *
     * <p> If signed in to Hexagon, gets show properties and episode flags.
     *
     * <p> If connected to trakt, but not signed in to Hexagon, gets episode flags from trakt
     * instead.
     *
     * @return True, if the show and its episodes were added to the database.
     */
    public static boolean addShow(Context context, int showTvdbId, List<BaseShow> traktWatched,
            List<BaseShow> traktCollection) throws TvdbException {
        boolean isShowExists = DBUtils.isShowExists(context, showTvdbId);
        if (isShowExists) {
            return false;
        }

        // get show info from TVDb and trakt
        String language = DisplaySettings.getContentLanguage(context);
        Show show = fetchShow(context, showTvdbId, language);

        // get show properties from hexagon
        if (HexagonTools.isSignedIn(context)) {
            try {
                ShowTools.Download.showPropertiesFromHexagon(context, show);
            } catch (IOException e) {
                throw new TvdbException("Failed to download show properties from Hexagon.");
            }
        }

        // get episodes from TVDb and do database update
        final ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        batch.add(DBUtils.buildShowOp(show, true));
        getEpisodesAndUpdateDatabase(context, show, language, batch);

        // download episode flags...
        if (HexagonTools.isSignedIn(context)) {
            // ...from Hexagon
            boolean success = EpisodeTools.Download.flagsFromHexagon(context, showTvdbId);
            if (!success) {
                // failed to download episode flags
                // flag show as needing an episode merge
                ContentValues values = new ContentValues();
                values.put(Shows.HEXAGON_MERGE_COMPLETE, false);
                context.getContentResolver()
                        .update(Shows.buildShowUri(showTvdbId), values, null, null);
            }

            // remove any isRemoved flag on Hexagon
            ShowTools.get(context).sendIsRemoved(showTvdbId, false);
        } else {
            // ...from trakt
            storeTraktFlags(context, traktWatched, showTvdbId, true);
            storeTraktFlags(context, traktCollection, showTvdbId, false);
        }

        // calculate next episode
        DBUtils.updateLatestEpisode(context, showTvdbId);

        return true;
    }

    private static void storeTraktFlags(Context context, List<BaseShow> shows, int showTvdbId,
            boolean isWatchedList) {
        // try to find seen episodes from trakt of the given show
        for (BaseShow show : shows) {
            if (show.show == null || show.show.ids == null || show.show.ids.tvdb == null
                    || show.show.ids.tvdb != showTvdbId) {
                continue; // skip
            }

            try {
                TraktTools.applyEpisodeFlagChanges(context, show,
                        isWatchedList ? TraktTools.Flag.WATCHED : TraktTools.Flag.COLLECTED, false,
                        null);
            } catch (OAuthUnauthorizedException ignored) {
                // we do not enable merging, so no trakt interaction will occur
            }

            // done, found the show we were looking for
            return;
        }
    }

    /**
     * Updates show. Adds new, updates changed and removes orphaned episodes.
     */

    public static void updateShow(Context context, int showTvdbId) throws TvdbException {
        String language = DisplaySettings.getContentLanguage(context);
        final ArrayList<ContentProviderOperation> batch = new ArrayList<>();

        Show show = fetchShow(context, showTvdbId, language);
        batch.add(DBUtils.buildShowOp(show, false));

        getEpisodesAndUpdateDatabase(context, show, language, batch);
    }

    // Values based on the assumption that sync runs about every 24 hours
    private static final long UPDATE_THRESHOLD_WEEKLYS_MS = 6 * DateUtils.DAY_IN_MILLIS +
            12 * DateUtils.HOUR_IN_MILLIS;
    private static final long UPDATE_THRESHOLD_DAILYS_MS = DateUtils.DAY_IN_MILLIS
            + 12 * DateUtils.HOUR_IN_MILLIS;

    /**
     * Return list of show TVDb ids hitting a x-day limit.
     */
    public static int[] deltaUpdateShows(long currentTime, Context context) {
        final List<Integer> updatableShowIds = new ArrayList<>();

        // get existing show ids
        final Cursor shows = context.getContentResolver().query(Shows.CONTENT_URI, new String[] {
                Shows._ID, Shows.LASTUPDATED, Shows.RELEASE_WEEKDAY
        }, null, null, null);

        if (shows != null) {
            while (shows.moveToNext()) {
                boolean isDailyShow = shows.getInt(2) == TimeTools.RELEASE_WEEKDAY_DAILY;
                long lastUpdatedTime = shows.getLong(1);
                // update daily shows more frequently than weekly shows
                if (currentTime - lastUpdatedTime >
                        (isDailyShow ? UPDATE_THRESHOLD_DAILYS_MS : UPDATE_THRESHOLD_WEEKLYS_MS)) {
                    // add shows that are due for updating
                    updatableShowIds.add(shows.getInt(0));
                }
            }

            int showCount = shows.getCount();
            if (showCount > 0 && AppSettings.shouldReportStats(context)) {
                Utils.trackCustomEvent(context, "Statistics", "Shows", String.valueOf(showCount));
            }

            shows.close();
        }

        // copy to int array
        int[] showTvdbIds = new int[updatableShowIds.size()];
        for (int i = 0; i < updatableShowIds.size(); i++) {
            showTvdbIds[i] = updatableShowIds.get(i);
        }
        return showTvdbIds;
    }

    /**
     * Fetches episodes for the given show from TVDb, adds database ops for them. Then adds all
     * information to the database.
     */
    private static boolean getEpisodesAndUpdateDatabase(Context context, Show show,
            String language, final ArrayList<ContentProviderOperation> batch)
            throws TvdbException {
        // get ops for episodes of this show
        ArrayList<ContentValues> importShowEpisodes = fetchEpisodes(batch, show, language,
                context);
        ContentValues[] newEpisodesValues = new ContentValues[importShowEpisodes.size()];
        newEpisodesValues = importShowEpisodes.toArray(newEpisodesValues);

        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            throw new TvdbException("Problem applying batch operation for " + show.tvdbId, e);
        }

        // insert all new episodes in bulk
        context.getContentResolver().bulkInsert(Episodes.CONTENT_URI, newEpisodesValues);

        return true;
    }

    /**
     * Get show details from TVDb in the user preferred language ({@link
     * DisplaySettings#getContentLanguage(android.content.Context)}). Tries to fetch additional
     * information from trakt.
     */
    public static Show getShow(Context context, int showTvdbId) throws TvdbException {
        String language = DisplaySettings.getContentLanguage(context);
        return fetchShow(context, showTvdbId, language);
    }

    /**
     * Get show details from TVDb and trakt.
     *
     * @param language A TVDb language code (see <a href="http://www.thetvdb.com/wiki/index.php/API:languages.xml"
     * >TVDb wiki</a>).
     */
    private static Show fetchShow(Context context, int showTvdbId, String language)
            throws TvdbException {
        // get show details from TVDb
        Show show = downloadAndParseShow(context, showTvdbId, language);

        // get some more details from trakt
        com.uwetrottmann.trakt.v2.entities.Show traktShow = null;
        try {
            // look up trakt id
            String showTraktId = TraktTools.lookupShowTraktId(context, showTvdbId);
            if (showTraktId != null) {
                // fetch details
                TraktV2 trakt = ServiceUtils.getTraktV2(context);
                traktShow = trakt.shows().summary(showTraktId, Extended.FULL);
            } else {
                traktShow = null;
            }
        } catch (RetrofitError e) {
            Timber.e(e, "Loading summary failed");
        }
        if (traktShow == null || traktShow.airs == null) {
            throw new TvdbException("Could not load show from trakt: " + showTvdbId);
        }

        show.release_time = TimeTools.parseShowReleaseTime(traktShow.airs.time);
        show.release_weekday = TimeTools.parseShowReleaseWeekDay(traktShow.airs.day);
        show.release_timezone = traktShow.airs.timezone;
        show.country = traktShow.country;
        show.firstAired = TimeTools.parseShowFirstRelease(traktShow.first_aired);
        show.rating = traktShow.rating == null ? 0.0 : traktShow.rating;

        return show;
    }

    /**
     * Get a show from TVDb.
     */
    private static Show downloadAndParseShow(Context context, int showTvdbId, String language)
            throws TvdbException {
        final Show currentShow = new Show();
        RootElement root = new RootElement("Data");
        Element show = root.getChild("Series");

        // set handlers for elements we want to react to
        show.getChild("id").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                // NumberFormatException may be thrown, will stop parsing
                currentShow.tvdbId = Integer.parseInt(body);
            }
        });
        show.getChild("SeriesName").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.title = body;
            }
        });
        show.getChild("Overview").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.overview = body;
            }
        });
        show.getChild("Actors").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.actors = body.trim();
            }
        });
        show.getChild("Genre").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.genres = body.trim();
            }
        });
        show.getChild("Network").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.network = body;
            }
        });
        show.getChild("Runtime").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                try {
                    currentShow.runtime = Integer.parseInt(body);
                } catch (NumberFormatException e) {
                    // an hour is always a good estimate...
                    currentShow.runtime = 60;
                }
            }
        });
        show.getChild("Status").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                if (body.length() == 10) {
                    currentShow.status = ShowStatusExport.CONTINUING;
                } else if (body.length() == 5) {
                    currentShow.status = ShowStatusExport.ENDED;
                } else {
                    currentShow.status = ShowStatusExport.UNKNOWN;
                }
            }
        });
        show.getChild("ContentRating").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.contentRating = body;
            }
        });
        show.getChild("poster").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.poster = body != null ? body.trim() : "";
            }
        });
        show.getChild("IMDB_ID").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.imdbId = body.trim();
            }
        });
        show.getChild("lastupdated").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                try {
                    currentShow.lastEdited = Long.parseLong(body);
                } catch (NumberFormatException e) {
                    currentShow.lastEdited = 0;
                }
            }
        });

        // build TVDb url, get localized content when possible
        String url = TVDB_API_SERIES + showTvdbId + "/"
                + (language != null ? language + TVDB_EXTENSION_UNCOMPRESSED : "");
        downloadAndParse(context, root.getContentHandler(), url, false);

        return currentShow;
    }

    private static ArrayList<ContentValues> fetchEpisodes(
            ArrayList<ContentProviderOperation> batch, Show show, String language, Context context)
            throws TvdbException {
        String url = TVDB_API_SERIES + show.tvdbId + "/" + TVDB_PATH_ALL
                + (language != null ? language + TVDB_EXTENSION_COMPRESSED : TVDB_FILE_DEFAULT);

        return parseEpisodes(batch, url, show, context);
    }

    /**
     * Loads the given zipped XML and parses containing episodes to create an array of {@link
     * ContentValues} for new episodes.<br> Adds update ops for updated episodes and delete ops for
     * local orphaned episodes to the given {@link ContentProviderOperation} batch.
     */
    private static ArrayList<ContentValues> parseEpisodes(
            final ArrayList<ContentProviderOperation> batch, String url, final Show show,
            Context context) throws TvdbException {
        final long dateLastMonthEpoch = (System.currentTimeMillis()
                - (DateUtils.DAY_IN_MILLIS * 30)) / 1000;
        final DateTimeZone showTimeZone = TimeTools.getDateTimeZone(show.release_timezone);
        final LocalTime showReleaseTime = TimeTools.getShowReleaseTime(show.release_time);
        final String deviceTimeZone = TimeZone.getDefault().getID();

        RootElement root = new RootElement("Data");
        Element episode = root.getChild("Episode");

        final ArrayList<ContentValues> newEpisodesValues = new ArrayList<>();

        final HashMap<Integer, Long> localEpisodeIds = DBUtils
                .getEpisodeMapForShow(context, show.tvdbId);
        final HashMap<Integer, Long> removableEpisodeIds = new HashMap<>(
                localEpisodeIds); // just copy episodes list, then remove valid ones
        final HashSet<Integer> localSeasonIds = DBUtils.getSeasonIdsOfShow(context, show.tvdbId);
        // store updated seasons to avoid duplicate ops
        final HashSet<Integer> seasonIdsToUpdate = new HashSet<>();
        final ContentValues values = new ContentValues();

        // set handlers for elements we want to react to
        episode.setEndElementListener(new EndElementListener() {
            public void end() {
                Integer episodeId = values.getAsInteger(Episodes._ID);
                if (episodeId == null || episodeId <= 0) {
                    // invalid id, skip
                    return;
                }

                // don't clean up this episode
                removableEpisodeIds.remove(episodeId);

                // decide whether to insert or update
                if (localEpisodeIds.containsKey(episodeId)) {
                    /*
                     * Update uses provider ops which take a long time. Only
                     * update if episode was edited on TVDb or is not older than
                     * a month (ensures show air time changes get stored).
                     */
                    Long lastEditEpoch = localEpisodeIds.get(episodeId);
                    Long lastEditEpochNew = values.getAsLong(Episodes.LAST_EDITED);
                    if (lastEditEpoch != null && lastEditEpochNew != null
                            && (lastEditEpoch < lastEditEpochNew
                            || dateLastMonthEpoch < lastEditEpoch)) {
                        // complete update op for episode
                        batch.add(DBUtils.buildEpisodeUpdateOp(values));
                    }
                } else {
                    // episode does not exist, yet
                    newEpisodesValues.add(new ContentValues(values));
                }

                Integer seasonId = values.getAsInteger(Seasons.REF_SEASON_ID);
                if (seasonId != null && !seasonIdsToUpdate.contains(seasonId)) {
                    // add insert/update op for season
                    batch.add(DBUtils.buildSeasonOp(values, !localSeasonIds.contains(seasonId)));
                    seasonIdsToUpdate.add(values.getAsInteger(Seasons.REF_SEASON_ID));
                }

                values.clear();
            }
        });
        episode.getChild("id").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                values.put(Episodes._ID, body.trim());
            }
        });
        episode.getChild("EpisodeNumber").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                values.put(Episodes.NUMBER, body.trim());
            }
        });
        episode.getChild("absolute_number").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                values.put(Episodes.ABSOLUTE_NUMBER, body.trim());
            }
        });
        episode.getChild("SeasonNumber").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                values.put(Episodes.SEASON, body.trim());
            }
        });
        episode.getChild("DVD_episodenumber").setEndTextElementListener(
                new EndTextElementListener() {
                    public void end(String body) {
                        values.put(Episodes.DVDNUMBER, body.trim());
                    }
                }
        );
        episode.getChild("FirstAired").setEndTextElementListener(new EndTextElementListener() {
            public void end(String releaseDate) {
                long releaseDateTime = TimeTools.parseEpisodeReleaseDate(showTimeZone, releaseDate,
                        showReleaseTime, show.country, deviceTimeZone);
                values.put(Episodes.FIRSTAIREDMS, releaseDateTime);
            }
        });
        episode.getChild("EpisodeName").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                values.put(Episodes.TITLE, body.trim());
            }
        });
        episode.getChild("Overview").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                values.put(Episodes.OVERVIEW, body.trim());
            }
        });
        episode.getChild("seasonid").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                values.put(Seasons.REF_SEASON_ID, body.trim());
            }
        });
        episode.getChild("seriesid").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                values.put(Shows.REF_SHOW_ID, body.trim());
            }
        });
        episode.getChild("Director").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                values.put(Episodes.DIRECTORS, body.trim());
            }
        });
        episode.getChild("GuestStars").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                values.put(Episodes.GUESTSTARS, body.trim());
            }
        });
        episode.getChild("Writer").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                values.put(Episodes.WRITERS, body.trim());
            }
        });
        episode.getChild("filename").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                values.put(Episodes.IMAGE, body.trim());
            }
        });
        episode.getChild("IMDB_ID").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                values.put(Episodes.IMDBID, body.trim());
            }
        });
        episode.getChild("lastupdated").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                // system populated field, trimming not necessary
                try {
                    values.put(Episodes.LAST_EDITED, Long.valueOf(body));
                } catch (NumberFormatException e) {
                    values.put(Episodes.LAST_EDITED, 0);
                }
            }
        });

        downloadAndParse(context, root.getContentHandler(), url, true);

        // add delete ops for leftover episodeIds in our db
        for (Integer episodeId : removableEpisodeIds.keySet()) {
            batch.add(ContentProviderOperation.newDelete(Episodes.buildEpisodeUri(episodeId))
                    .build());
        }

        return newEpisodesValues;
    }

    /**
     * Downloads the XML or ZIP file from the given URL, passing a valid response to {@link
     * Xml#parse(InputStream, android.util.Xml.Encoding, ContentHandler)} using the given {@link
     * ContentHandler}.
     */
    private static void downloadAndParse(Context context, ContentHandler handler, String urlString,
            boolean isZipFile) throws TvdbException {
        Request request = new Request.Builder().url(urlString).build();

        Response response;
        try {
            response = ServiceUtils.getCachingOkHttpClient(context)
                    .newCall(request)
                    .execute();
        } catch (IOException e) {
            throw new TvdbException(e.getMessage() + " " + urlString, e);
        }

        int statusCode = response.code();
        if (statusCode == 404) {
            // special case: item does not exist (any longer)
            throw new TvdbException(response.code() + " " + response.message() + " " + urlString,
                    true, null);
        }
        if (!response.isSuccessful()) {
            // other non-2xx response
            throw new TvdbException(response.code() + " " + response.message() + " " + urlString);
        }

        try {
            final InputStream input = response.body().byteStream();
            if (isZipFile) {
                // We downloaded the compressed file from TheTVDB
                final ZipInputStream zipin = new ZipInputStream(input);
                zipin.getNextEntry();
                try {
                    Xml.parse(zipin, Xml.Encoding.UTF_8, handler);
                } finally {
                    zipin.close();
                }
            } else {
                try {
                    Xml.parse(input, Xml.Encoding.UTF_8, handler);
                } finally {
                    if (input != null) {
                        input.close();
                    }
                }
            }
        } catch (SAXException | IOException | AssertionError e) {
            throw new TvdbException(e.getMessage() + " " + urlString, e);
        }
    }
}
