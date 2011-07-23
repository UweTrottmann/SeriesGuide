
package com.battlelancer.thetvdbapi;

import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.SeriesDatabase;
import com.battlelancer.seriesguide.SeriesGuideApplication;
import com.battlelancer.seriesguide.SeriesGuideData;
import com.battlelancer.seriesguide.provider.SeriesContract;
import com.battlelancer.seriesguide.provider.SeriesContract.EpisodeSearch;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.util.Lists;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.zip.ZipInputStream;

public class TheTVDB {

    private static final String mirror = "http://www.thetvdb.com";

    private static final String mirror_banners = "http://www.thetvdb.com/banners";

    private static final String xmlMirror = mirror + "/api/";

    private static final String TAG = "TheTVDB";

    private static final int SECOND_IN_MILLIS = (int) DateUtils.SECOND_IN_MILLIS;

    /**
     * Adds a show and its episodes. If it already exists updates them. This
     * uses two consequent connections. The first one downloads the base series
     * record, to check if the show is already in the database. The second
     * downloads all episode information. This allows for a smaller download, if
     * a show already exists in your database.
     * 
     * @param showId
     * @return true if show and its episodes were added, false if it already
     *         exists
     * @throws IOException
     * @throws SAXException
     */
    public static boolean addShow(String showId, Context context) throws SAXException {
        String language = getTheTVDBLanguage(context);
        Series show = fetchShow(showId, language, context);

        boolean isShowExists = SeriesDatabase.isShowExists(showId, context);

        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
        batch.add(SeriesDatabase.buildShowOp(show, context, !isShowExists));
        batch.addAll(importShowEpisodes(showId, language, context));

        try {
            context.getContentResolver().applyBatch(SeriesContract.CONTENT_AUTHORITY, batch);
        } catch (RemoteException e) {
            // Failed binder transactions aren't recoverable
            throw new RuntimeException("Problem applying batch operation", e);
        } catch (OperationApplicationException e) {
            // Failures like constraint violation aren't recoverable
            throw new RuntimeException("Problem applying batch operation", e);
        }

        SeriesDatabase.updateLatestEpisode(context, showId);

        return !isShowExists;
    }

    /**
     * Just fetch all series and episode details and overwrite, add.
     * 
     * @param showId
     * @throws SAXException
     * @throws IOException
     */
    public static void updateShow(String showId, Context context) throws SAXException {
        String language = getTheTVDBLanguage(context);
        Series show = fetchShow(showId, language, context);

        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
        batch.add(SeriesDatabase.buildShowOp(show, context, false));
        batch.addAll(importShowEpisodes(showId, language, context));

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
     * Get details for one show, identified by the given seriesid.
     * 
     * @param seriesid
     * @param language
     * @return a Series object, holding the information about the show
     * @throws SAXException
     * @throws IOException
     */
    public static Series fetchShow(String seriesid, String language, Context context)
            throws SAXException {
        String url = xmlMirror + Constants.API_KEY + "/series/" + seriesid + "/"
                + (language != null ? language + ".xml" : "");

        return parseShow(url, context);
    }

    /**
     * Search for shows which include a certain keyword in their title.
     * Dependent on the TheTVDB search algorithms.
     * 
     * @param title
     * @return a List with SearchResult objects, max 100
     * @throws SAXException
     * @throws IOException
     */
    public static List<SearchResult> searchShow(String title, Context context) throws IOException,
            SAXException {
        String language = getTheTVDBLanguage(context);

        URL url;
        try {
            url = new URL(xmlMirror + "GetSeries.php?seriesname="
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
                currentShow.setId(Integer.valueOf(body));
            }
        });
        item.getChild("SeriesName").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.setSeriesName(body.trim());
            }
        });
        item.getChild("Overview").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.setOverview(body.trim());
            }
        });

        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(25000);
        connection.setReadTimeout(90000);
        InputStream in = connection.getInputStream();
        try {
            Xml.parse(in, Xml.Encoding.UTF_8, root.getContentHandler());
        } catch (SocketTimeoutException se) {
            throw new IOException();
        }
        in.close();

        return series;
    }

    /**
     * Parses the xml downloaded from the given url into a {@link Series}
     * object. Downloads the poster if there is one.
     * 
     * @param url
     * @param context
     * @return the show wrapped in a {@link Series} object
     * @throws SAXException
     */
    public static Series parseShow(String url, final Context context) throws SAXException {
        final Series currentShow = new Series();
        RootElement root = new RootElement("Data");
        Element show = root.getChild("Series");

        // set handlers for elements we want to react to
        show.getChild("id").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.setId(body.trim());
            }
        });
        show.getChild("Language").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.setLanguage(body.trim());
            }
        });
        show.getChild("SeriesName").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.setSeriesName(body.trim());
            }
        });
        show.getChild("Overview").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.setOverview(body.trim());
            }
        });
        show.getChild("Actors").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.setActors(body.trim());
            }
        });
        show.getChild("Airs_DayOfWeek").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.setAirsDayOfWeek(body.trim());
            }
        });
        show.getChild("Airs_Time").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.setAirsTime(SeriesGuideData.parseTimeToMilliseconds(body.trim()));
            }
        });
        show.getChild("FirstAired").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.setFirstAired(body.trim());
            }
        });
        show.getChild("Genre").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.setGenres(body.trim());
            }
        });
        show.getChild("Network").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.setNetwork(body.trim());
            }
        });
        show.getChild("Rating").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.setRating(body.trim());
            }
        });
        show.getChild("Runtime").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.setRuntime(body.trim());
            }
        });
        show.getChild("Status").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                final String status = body.trim();
                if (status.length() == 10) {
                    currentShow.setStatus("1");
                } else if (status.length() == 5) {
                    currentShow.setStatus("0");
                } else {
                    currentShow.setStatus("");
                }
            }
        });
        show.getChild("ContentRating").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.setContentRating(body.trim());
            }
        });
        show.getChild("poster").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                String posterurl = body.trim();
                currentShow.setPoster(posterurl);
                if (posterurl.length() != 0) {
                    fetchArt(posterurl, true, context);
                }
            }
        });
        show.getChild("IMDB_ID").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.setImdbId(body.trim());
            }
        });

        HttpUriRequest request = new HttpGet(url);
        HttpClient httpClient = getHttpClient(context);
        execute(request, httpClient, root.getContentHandler(), false);

        return currentShow;
    }

    public static ArrayList<ContentProviderOperation> importShowEpisodes(String seriesid,
            String language, Context context) throws SAXException {
        String url = xmlMirror + Constants.API_KEY + "/series/" + seriesid + "/all/"
                + (language != null ? language + ".zip" : "en.zip");

        return parseEpisodes(url, seriesid, context);
    }

    public static ArrayList<ContentProviderOperation> parseEpisodes(String url, String seriesid,
            Context context) throws SAXException {
        RootElement root = new RootElement("Data");
        Element episode = root.getChild("Episode");
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
        final HashSet<Long> episodeIDs = SeriesDatabase.getEpisodeIDsForShow(seriesid, context);
        final HashSet<Long> existingSeasonIDs = SeriesDatabase.getSeasonIDsForShow(seriesid,
                context);
        final HashSet<Long> updatedSeasonIDs = new HashSet<Long>();
        final ContentValues values = new ContentValues();

        // set handlers for elements we want to react to
        episode.setEndElementListener(new EndElementListener() {
            public void end() {
                // add insert/update op for episode
                batch.add(SeriesDatabase.buildEpisodeOp(values,
                        !episodeIDs.contains(values.getAsLong(Episodes._ID))));

                long seasonid = values.getAsLong(Seasons.REF_SEASON_ID);
                if (!updatedSeasonIDs.contains(seasonid)) {
                    // add insert/update op for season
                    batch.add(SeriesDatabase.buildSeasonOp(values,
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

        HttpUriRequest request = new HttpGet(url);
        HttpClient httpClient = getHttpClient(context);
        execute(request, httpClient, root.getContentHandler(), true);

        return batch;
    }

    /**
     * Tries to download art from the thetvdb banner mirror. Ignores blank ("")
     * or null paths and skips existing images. Returns true even if there was
     * no art downloaded.
     * 
     * @param fileName of image
     * @param isPoster
     * @param context
     * @return false if not all images could be fetched. true otherwise, even if
     *         nothing was downloaded
     */
    public static boolean fetchArt(String fileName, boolean isPoster, Context context) {
        if (fileName == null || context == null) {
            return true;
        }

        Bitmap bitmap = null;
        ImageCache imageCache = ((SeriesGuideApplication) context.getApplicationContext())
                .getImageCache();
        boolean resultCode = true;

        if (fileName.length() != 0 && imageCache.get(fileName) == null) {
            try {
                String imageUrl;
                if (isPoster) {
                    imageUrl = mirror_banners + "/_cache/" + fileName;
                } else {
                    imageUrl = mirror_banners + "/" + fileName;
                }

                // try to download and decode the image
                bitmap = downloadBitmap(imageUrl);
                if (bitmap != null) {
                    imageCache.put(fileName, bitmap);
                    if (isPoster) {
                        // create thumbnail
                        imageCache.getThumbHelper(fileName);
                    }
                } else {
                    resultCode = false;
                }
            } catch (MalformedURLException e) {
                // do nothing, just skip this one (should only happen if there
                // are faulty paths in thetvdb)
            }
        }

        return resultCode;
    }

    static Bitmap downloadBitmap(String url) throws MalformedURLException {
        final DefaultHttpClient client = new DefaultHttpClient();
        final HttpGet getRequest = new HttpGet(url);

        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                Log.w(TAG, "Error " + statusCode + " while retrieving bitmap from " + url);
                return null;
            }

            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    inputStream = entity.getContent();
                    return BitmapFactory.decodeStream(inputStream);
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    entity.consumeContent();
                }
            }
        } catch (IOException e) {
            getRequest.abort();
            Log.w(TAG, "I/O error while retrieving bitmap from " + url, e);
        } catch (IllegalStateException e) {
            getRequest.abort();
            Log.w(TAG, "Incorrect URL: " + url);
        } catch (Exception e) {
            getRequest.abort();
            Log.w(TAG, "Error while retrieving bitmap from " + url, e);
        }
        return null;
    }

    private static String getTheTVDBLanguage(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                .getApplicationContext());
        return prefs.getString("language", "en");
    }

    public static void onRenewFTSTable(Context context) {
        context.getContentResolver().query(EpisodeSearch.CONTENT_URI_RENEWFTSTABLE, null, null,
                null, null);
    }

    /**
     * Generate and return a {@link HttpClient} configured for general use,
     * including setting an application-specific user-agent string.
     */
    public static HttpClient getHttpClient(Context context) {
        final HttpParams params = new BasicHttpParams();

        // Use generous timeouts for slow mobile networks
        HttpConnectionParams.setConnectionTimeout(params, 20 * SECOND_IN_MILLIS);
        HttpConnectionParams.setSoTimeout(params, 20 * SECOND_IN_MILLIS);

        HttpConnectionParams.setSocketBufferSize(params, 8192);

        final DefaultHttpClient client = new DefaultHttpClient(params);

        return client;
    }

    /**
     * Execute this {@link HttpUriRequest}, passing a valid response through
     * {@link XmlHandler#parseAndApply(XmlPullParser, ContentResolver)}.
     */
    private static void execute(HttpUriRequest request, HttpClient httpClient,
            ContentHandler handler, boolean isZipFile) throws SAXException {
        try {
            final HttpResponse resp = httpClient.execute(request);
            final int status = resp.getStatusLine().getStatusCode();
            if (status != HttpStatus.SC_OK) {
                throw new SAXException("Unexpected server response " + resp.getStatusLine()
                        + " for " + request.getRequestLine());
            }

            final InputStream input = resp.getEntity().getContent();
            if (isZipFile) {
                // We downloaded the compressed file from TheTVDB
                final ZipInputStream zipin = new ZipInputStream(input);
                zipin.getNextEntry();
                try {
                    Xml.parse(zipin, Xml.Encoding.UTF_8, handler);
                } catch (SAXException e) {
                    throw new SAXException("Malformed response for " + request.getRequestLine(), e);
                } catch (IOException ioe) {
                    throw new SAXException("Problem reading remote response for "
                            + request.getRequestLine(), ioe);
                } finally {
                    if (zipin != null) {
                        zipin.close();
                    }
                }
            } else {
                try {
                    Xml.parse(input, Xml.Encoding.UTF_8, handler);
                } catch (SAXException e) {
                    throw new SAXException("Malformed response for " + request.getRequestLine(), e);
                } catch (IOException ioe) {
                    throw new SAXException("Problem reading remote response for "
                            + request.getRequestLine(), ioe);
                } finally {
                    if (input != null) {
                        input.close();
                    }
                }
            }
        } catch (IOException e) {
            throw new SAXException("Problem reading remote response for "
                    + request.getRequestLine(), e);
        }
    }
}
