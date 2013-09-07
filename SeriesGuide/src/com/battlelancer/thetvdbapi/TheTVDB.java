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

package com.battlelancer.thetvdbapi;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Xml;

import com.battlelancer.seriesguide.SeriesGuideApplication;
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.ShowStatusExport;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.provider.SeriesContract.EpisodeSearch;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.TvShow;
import com.jakewharton.trakt.entities.TvShowSeason;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.Lists;
import com.uwetrottmann.seriesguide.R;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.zip.ZipInputStream;

/**
 * Provides access to the TheTVDb.com XML API throwing in some additional data
 * from trakt.tv here and there.
 */
public class TheTVDB {

    public interface ShowStatus {
        int CONTINUING = 1;
        int ENDED = 0;
        int UNKNOWN = -1;
    }

    private static final String TVDB_MIRROR_BANNERS = "http://thetvdb.com/banners";

    private static final String TVDB_API_URL = "http://thetvdb.com/api/";

    private static final String TAG = "TheTVDB";

    /**
     * Returns true if the given show has not been updated in the last 12 hours.
     */
    public static boolean isUpdateShow(String showId, long currentTime, Context context) {
        final Cursor show = context.getContentResolver().query(Shows.buildShowUri(showId),
                new String[] {
                        Shows._ID, Shows.LASTUPDATED
                }, null, null, null);
        boolean isUpdate = false;
        if (show != null) {
            if (show.moveToFirst()) {
                long lastUpdateTime = show.getLong(1);
                if (currentTime - lastUpdateTime > DateUtils.HOUR_IN_MILLIS * 12) {
                    isUpdate = true;
                }
            }
            show.close();
        }
        return isUpdate;
    }

    /**
     * Adds a show and its episodes. If it already exists updates them. This
     * uses two consequent connections. The first one downloads the base series
     * record, to check if the show is already in the database. The second
     * downloads all episode information. This allows for a smaller download, if
     * a show already exists in your database.
     * 
     * @return true if show and its episodes were added, false if it already
     *         exists
     */
    public static boolean addShow(String showId, List<TvShow> seenShows,
            List<TvShow> collectedShows, Context context) throws SAXException {
        String language = getTheTVDBLanguage(context);
        Show show = fetchShow(showId, language, context);

        boolean isShowExists = DBUtils.isShowExists(showId, context);

        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
        batch.add(DBUtils.buildShowOp(show, context, !isShowExists));
        getEpisodesAndUpdateDatabase(context, show, language, batch);

        storeTraktFlags(showId, seenShows, context, true);
        storeTraktFlags(showId, collectedShows, context, false);

        DBUtils.updateLatestEpisode(context, showId);

        return !isShowExists;
    }

    /**
     * Updates all show information. Adds new, updates changed and removes
     * orphaned episodes.
     */
    public static void updateShow(int showId, Context context) throws SAXException {
        String language = getTheTVDBLanguage(context);
        Show show = fetchShow(String.valueOf(showId), language, context);

        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
        batch.add(DBUtils.buildShowOp(show, context, false));
        getEpisodesAndUpdateDatabase(context, show, language, batch);
    }

    /**
     * Search for shows which include a certain keyword in their title.
     * Dependent on the TheTVDB search algorithms.
     * 
     * @return a List with SearchResult objects, max 100
     */
    public static List<SearchResult> searchShow(String title, Context context) throws IOException,
            SAXException {
        String language = getTheTVDBLanguage(context);

        URL url;
        try {
            url = new URL(TVDB_API_URL + "GetSeries.php?seriesname="
                    + URLEncoder.encode(title, "UTF-8")
                    + (language != null ? "&language=" + language : ""));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final List<SearchResult> series = new ArrayList<SearchResult>();
        final SearchResult currentShow = new SearchResult();

        RootElement root = new RootElement("Data");
        Element item = root.getChild("Series");
        // set handlers for elements we want to react to
        item.setEndElementListener(new EndElementListener() {
            public void end() {
                series.add(currentShow.copy());
            }
        });
        item.getChild("id").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.tvdbid = body;
            }
        });
        item.getChild("SeriesName").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.title = body.trim();
            }
        });
        item.getChild("Overview").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.overview = body.trim();
            }
        });

        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(25000);
        connection.setReadTimeout(90000);
        InputStream in = connection.getInputStream();
        try {
            Xml.parse(in, Xml.Encoding.UTF_8, root.getContentHandler());
        } catch (Exception e) {
            throw new IOException();
        }
        in.close();

        return series;
    }

    /**
     * Return list of show TVDb ids hitting a x-day limit.
     */
    public static int[] deltaUpdateShows(long currentTime, Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final int updateAtLeastEvery = prefs.getInt(SeriesGuidePreferences.KEY_UPDATEATLEASTEVERY,
                7);

        final List<Integer> updatableShowIds = Lists.newArrayList();

        // get existing show ids
        final Cursor shows = context.getContentResolver().query(Shows.CONTENT_URI, new String[] {
                Shows._ID, Shows.LASTUPDATED
        }, null, null, null);

        if (shows != null) {
            while (shows.moveToNext()) {
                long lastUpdatedTime = shows.getLong(1);
                if (currentTime - lastUpdatedTime > DateUtils.DAY_IN_MILLIS * updateAtLeastEvery) {
                    // add shows that are due for updating
                    updatableShowIds.add(shows.getInt(0));
                }
            }

            Long showCount = (long) shows.getCount();
            EasyTracker.getTracker().sendEvent("Statistics", "Shows", String.valueOf(showCount),
                    showCount);

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
     * Fetches episodes for the given show from TVDb, adds database ops for
     * them. Then adds all information to the database.
     */
    private static void getEpisodesAndUpdateDatabase(Context context, Show show,
            String language, final ArrayList<ContentProviderOperation> batch)
            throws SAXException {
        // get ops for episodes of this show
        ArrayList<ContentValues> importShowEpisodes = fetchEpisodes(batch, show, language,
                context);
        ContentValues[] newEpisodesValues = new ContentValues[importShowEpisodes.size()];
        newEpisodesValues = importShowEpisodes.toArray(newEpisodesValues);

        try {
            context.getContentResolver()
                    .applyBatch(SeriesGuideApplication.CONTENT_AUTHORITY, batch);
            context.getContentResolver()
                    .bulkInsert(Episodes.CONTENT_URI, newEpisodesValues);
        } catch (RemoteException e) {
            // Failed binder transactions aren't recoverable
            throw new RuntimeException("Problem applying batch operation", e);
        } catch (OperationApplicationException e) {
            // Failures like constraint violation aren't recoverable
            throw new RuntimeException("Problem applying batch operation", e);
        }
    }

    private static String getTheTVDBLanguage(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                .getApplicationContext());
        return prefs.getString(SeriesGuidePreferences.KEY_LANGUAGE, "en");
    }

    private static void storeTraktFlags(String showId, List<TvShow> shows, Context context,
            boolean isSeenFlags) {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

        // try to find seen episodes from trakt
        for (TvShow tvShow : shows) {
            if (showId.equals(tvShow.tvdbId)) {
                batch.clear();

                // try to find matching seasons
                final List<TvShowSeason> seasons = tvShow.seasons;
                for (TvShowSeason season : seasons) {
                    final Cursor seasonMatch = context.getContentResolver().query(
                            Seasons.buildSeasonsOfShowUri(showId), new String[] {
                                Seasons._ID
                            }, Seasons.COMBINED + "=?", new String[] {
                                season.season.toString()
                            }, null);

                    // add ops to flag episodes
                    if (seasonMatch.moveToFirst()) {
                        final String seasonId = seasonMatch.getString(0);

                        for (Integer episode : season.episodes.numbers) {
                            // flag as watched or collected depending on call
                            // parameter
                            batch.add(ContentProviderOperation
                                    .newUpdate(Episodes.buildEpisodesOfSeasonUri(seasonId))
                                    .withSelection(Episodes.NUMBER + "=?", new String[] {
                                            episode.toString()
                                    })
                                    .withValue(isSeenFlags ? Episodes.WATCHED : Episodes.COLLECTED,
                                            true).build());
                        }
                    }

                    seasonMatch.close();
                }

                // apply ops for this show
                try {
                    context.getContentResolver()
                            .applyBatch(SeriesGuideApplication.CONTENT_AUTHORITY, batch);
                } catch (RemoteException e) {
                    // Failed binder transactions aren't recoverable
                    throw new RuntimeException("Problem applying batch operation", e);
                } catch (OperationApplicationException e) {
                    // Failures like constraint violation aren't
                    // recoverable
                    throw new RuntimeException("Problem applying batch operation", e);
                }

                break;
            }
        }
    }

    /**
     * Get details for one show, identified by the given series TVDb id. Tries
     * to fetch additional information from trakt.
     * 
     * @param language A TVDb language code (see <a
     *            href="http://www.thetvdb.com/wiki/index.php/API:languages.xml"
     *            >TVDb wiki</a>).
     */
    private static Show fetchShow(String showTvdbId, String language, Context context)
            throws SAXException {
        // Try to get some show details from trakt
        TvShow traktShow = null;
        ServiceManager manager = ServiceUtils.getTraktServiceManager(context);
        if (manager != null) {
            try {
                traktShow = manager.showService().summary(showTvdbId).fire();
            } catch (TraktException e) {
                Utils.trackExceptionAndLog(TAG, e);
            } catch (ApiException e) {
                Utils.trackExceptionAndLog(TAG, e);
            }
        }

        String url = TVDB_API_URL + context.getResources().getString(R.string.tvdb_apikey)
                + "/series/" + showTvdbId + "/" + (language != null ? language + ".xml" : "");

        Show show = parseShow(url, context);

        // correct air times for non-US shows
        if (traktShow != null && traktShow.country != null) {
            if ("United States".equals(traktShow.country)) {
                // catch US, is already correct then
            } else if ("United Kingdom".equals(traktShow.country)) {
                // Correct to BST (no summer time)
                // Sample: Doctor Who (2005)
                show.airtime -= 8 * DateUtils.HOUR_IN_MILLIS;
            } else if ("Germany".equals(traktShow.country)) {
                // Correct to EST
                // Sample: heute-show
                show.airtime -= 9 * DateUtils.HOUR_IN_MILLIS;
            }
        }

        return show;
    }

    /**
     * Get a show from TVDb. Already tries to download the show poster if there
     * is one.
     * 
     * @param url API call to get the show.
     * @return The show wrapped in a {@link Show} object
     * @throws SAXException If anything goes wrong.
     */
    private static Show parseShow(String url, final Context context) throws SAXException {
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
        show.getChild("Airs_DayOfWeek").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.airday = body.trim();
            }
        });
        show.getChild("Airs_Time").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.airtime = Utils.parseTimeToMilliseconds(body.trim());
            }
        });
        show.getChild("FirstAired").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.firstAired = body;
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
        show.getChild("Rating").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                try {
                    currentShow.rating = Double.parseDouble(body);
                } catch (NumberFormatException e) {
                    currentShow.rating = 0.0;
                }
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
                currentShow.poster = body;
                if (body.length() != 0) {
                    fetchArt(body, true, context);
                }
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

        downloadAndParse(url, root.getContentHandler(), false);

        return currentShow;
    }

    private static ArrayList<ContentValues> fetchEpisodes(
            ArrayList<ContentProviderOperation> batch, Show show, String language, Context context)
            throws SAXException {
        String url = TVDB_API_URL + context.getResources().getString(R.string.tvdb_apikey)
                + "/series/" + show.tvdbId + "/all/"
                + (language != null ? language + ".zip" : "en.zip");

        return parseEpisodes(batch, url, show, context);
    }

    /**
     * Loads the given zipped XML and parses containing episodes to create an
     * array of {@link ContentValues} for new episodes.<br>
     * Adds update ops for updated episodes and delete ops for local orphaned
     * episodes to the given {@link ContentProviderOperation} batch.
     */
    private static ArrayList<ContentValues> parseEpisodes(
            final ArrayList<ContentProviderOperation> batch, String url, final Show show,
            Context context) throws SAXException {
        final ArrayList<ContentValues> newEpisodesValues = Lists.newArrayList();
        final long dateLastMonthEpoch = (System.currentTimeMillis()
                - (DateUtils.DAY_IN_MILLIS * 30)) / 1000;

        RootElement root = new RootElement("Data");
        Element episode = root.getChild("Episode");

        final HashMap<Long, Long> episodeIDs = DBUtils.getEpisodeMapForShow(context, show.tvdbId);
        final HashMap<Long, Long> existingEpisodeIds = new HashMap<Long, Long>(episodeIDs);
        final HashSet<Long> existingSeasonIDs = DBUtils.getSeasonIDsForShow(context, show.tvdbId);
        // store updated seasons to avoid duplicate ops
        final HashSet<Long> updatedSeasonIDs = new HashSet<Long>();
        final ContentValues values = new ContentValues();

        // set handlers for elements we want to react to
        episode.setEndElementListener(new EndElementListener() {
            public void end() {
                long episodeId = values.getAsLong(Episodes._ID);
                existingEpisodeIds.remove(episodeId);

                if (episodeIDs.containsKey(episodeId)) {
                    /*
                     * Update uses provider ops which take a long time. Only
                     * update if episode was edited on TVDb or is not older than
                     * a month (ensures show air time changes get stored).
                     */
                    long lastEditEpoch = episodeIDs.get(episodeId);
                    if (lastEditEpoch < values.getAsLong(Episodes.LAST_EDITED)
                            || dateLastMonthEpoch < lastEditEpoch) {
                        // complete update op for episode
                        batch.add(DBUtils.buildEpisodeUpdateOp(values));
                    }
                } else {
                    // episode does not exist, yet
                    newEpisodesValues.add(new ContentValues(values));
                }

                long seasonid = values.getAsLong(Seasons.REF_SEASON_ID);
                if (!updatedSeasonIDs.contains(seasonid)) {
                    // add insert/update op for season
                    batch.add(DBUtils.buildSeasonOp(values,
                            !existingSeasonIDs.contains(seasonid)));
                    updatedSeasonIDs.add(values.getAsLong(Seasons.REF_SEASON_ID));
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
                });
        episode.getChild("FirstAired").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                long episodeAirTime = Utils.buildEpisodeAirtime(body, show.airtime);
                values.put(Episodes.FIRSTAIREDMS, episodeAirTime);
                values.put(Episodes.FIRSTAIRED, body.trim());
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
        episode.getChild("Rating").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                values.put(Episodes.RATING, body.trim());
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

        downloadAndParse(url, root.getContentHandler(), true);

        // add delete ops for left over episodeIds in our db
        for (Long id : existingEpisodeIds.keySet()) {
            batch.add(ContentProviderOperation.newDelete(Episodes.buildEpisodeUri(String
                    .valueOf(id))).build());
        }

        return newEpisodesValues;
    }

    /**
     * Downloads the XML or ZIP file from the given URL, passing a valid
     * response to
     * {@link Xml#parse(InputStream, android.util.Xml.Encoding, ContentHandler)}
     * using the given {@link ContentHandler}.
     */
    private static void downloadAndParse(String urlString,
            ContentHandler handler, boolean isZipFile) throws SAXException {
        try {
            final InputStream input = AndroidUtils.downloadUrl(urlString);

            if (isZipFile) {
                // We downloaded the compressed file from TheTVDB
                final ZipInputStream zipin = new ZipInputStream(input);
                zipin.getNextEntry();
                try {
                    Xml.parse(zipin, Xml.Encoding.UTF_8, handler);
                } finally {
                    if (zipin != null) {
                        zipin.close();
                    }
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
        } catch (SAXException e) {
            throw new SAXException("Malformed response for " + urlString, e);
        } catch (IOException e) {
            throw new SAXException("Problem reading remote response for "
                    + urlString, e);
        } catch (AssertionError ae) {
            // looks like Xml.parse is throwing AssertionErrors instead of
            // IOExceptions
            throw new SAXException("Problem reading remote response for "
                    + urlString);
        } catch (Exception e) {
            throw new SAXException("Problem reading remote response for "
                    + urlString, e);
        }
    }

    /**
     * Tries to download art from the thetvdb banner TVDB_MIRROR. Ignores blank
     * ("") or null paths and skips existing images. Returns true even if there
     * was no art downloaded.
     * 
     * @param fileName of image
     * @param isPoster
     * @param context
     * @return false if not all images could be fetched. true otherwise, even if
     *         nothing was downloaded
     */
    public static boolean fetchArt(String fileName, boolean isPoster, Context context) {
        if (TextUtils.isEmpty(fileName) || context == null) {
            return true;
        }

        final ImageProvider imageProvider = ImageProvider.getInstance(context);

        if (!imageProvider.exists(fileName)) {
            final String imageUrl;
            if (isPoster) {
                // the cached version is a lot smaller, but still big enough for
                // our purposes
                imageUrl = TVDB_MIRROR_BANNERS + "/_cache/" + fileName;
            } else {
                imageUrl = TVDB_MIRROR_BANNERS + "/" + fileName;
            }

            // try to download, decode and store the image
            final Bitmap bitmap = downloadBitmap(imageUrl, context);
            if (bitmap != null) {
                imageProvider.storeImage(fileName, bitmap, isPoster);
            } else {
                return false;
            }
        }

        return true;
    }

    private static Bitmap downloadBitmap(String url, Context context) {
        InputStream inputStream = null;
        try {
            HttpURLConnection conn = AndroidUtils.buildHttpUrlConnection(url);
            conn.connect();
            long imageSize = conn.getContentLength();
            // allow images up to 300K (although size is always around
            // 30K for posters and 100K for episode images)
            if (imageSize > 300000) {
                return null;
            } else {
                inputStream = conn.getInputStream();
                // return BitmapFactory.decodeStream(inputStream);
                // Bug on slow connections, fixed in future release.
                return BitmapFactory.decodeStream(new FlushedInputStream(inputStream));
            }
        } catch (IOException e) {
            Log.w(TAG, "I/O error retrieving bitmap from " + url, e);
            Utils.trackException(TAG + " I/O error retrieving bitmap from " + url, e);
        } catch (IllegalStateException e) {
            Log.w(TAG, "Incorrect URL: " + url);
            Utils.trackException(TAG + " Incorrect URL " + url, e);
        } catch (Exception e) {
            Log.w(TAG, "Error while retrieving bitmap from " + url, e);
            Utils.trackException(TAG + " Error while retrieving bitmap from " + url, e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.w(TAG, "I/O error while retrieving bitmap from " + url, e);
                    Utils.trackException(TAG + " I/O error retrieving bitmap from " + url,
                            e);
                }
            }
        }
        return null;
    }

    /*
     * An InputStream that skips the exact number of bytes provided, unless it
     * reaches EOF.
     */
    static class FlushedInputStream extends FilterInputStream {
        public FlushedInputStream(InputStream inputStream) {
            super(inputStream);
        }

        @Override
        public long skip(long n) throws IOException {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n) {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L) {
                    int b = read();
                    if (b < 0) {
                        break; // we reached EOF
                    } else {
                        bytesSkipped = 1; // we read one byte
                    }
                }
                totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }
    }

    public static void onRenewFTSTable(Context context) {
        Log.d(TAG, "Query to renew FTS table");
        context.getContentResolver().query(EpisodeSearch.CONTENT_URI_RENEWFTSTABLE, null, null,
                null, null);
    }
}
