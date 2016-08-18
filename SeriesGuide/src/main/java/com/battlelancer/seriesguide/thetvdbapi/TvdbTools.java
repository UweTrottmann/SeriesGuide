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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Xml;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.ShowStatusExport;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.AppSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.LanguageTools;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.TraktTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.thetvdb.entities.Series;
import com.uwetrottmann.thetvdb.entities.SeriesImageQueryResults;
import com.uwetrottmann.thetvdb.entities.SeriesResultsWrapper;
import com.uwetrottmann.thetvdb.entities.SeriesWrapper;
import com.uwetrottmann.thetvdb.services.Search;
import com.uwetrottmann.thetvdb.services.SeriesService;
import com.uwetrottmann.trakt5.entities.BaseShow;
import com.uwetrottmann.trakt5.enums.Extended;
import com.uwetrottmann.trakt5.enums.IdType;
import dagger.Lazy;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.ZipInputStream;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import okhttp3.Request;
import okhttp3.Response;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import timber.log.Timber;

/**
 * Provides access to the TheTVDb.com XML API throwing in some additional data from trakt.tv here
 * and there.
 */
public class TvdbTools {

    private static final String TVDB_MIRROR_BANNERS = "http://thetvdb.com/banners/";

    private static final String TVDB_MIRROR_BANNERS_CACHE = TVDB_MIRROR_BANNERS + "_cache/";

    private static final String TVDB_API_URL = "http://thetvdb.com/api/";

    private static final String TVDB_API_GETSERIES = TVDB_API_URL + "GetSeries.php?seriesname=";

    private static final String TVDB_API_SERIES = TVDB_API_URL + BuildConfig.TVDB_API_KEY
            + "/series/";

    private static final String TVDB_PATH_ALL = "all/";
    private static final String TVDB_PARAM_LANGUAGE = "&language=";
    private static final String TVDB_EXTENSION_COMPRESSED = ".zip";
    private static final String TVDB_FILE_DEFAULT = "en" + TVDB_EXTENSION_COMPRESSED;
    private static final String[] LANGUAGE_QUERY_PROJECTION = new String[] { Shows.LANGUAGE };

    private static TvdbTools tvdbTools;
    private final SgApp app;
    @Inject Lazy<Search> searchService;
    @Inject Lazy<com.uwetrottmann.trakt5.services.Search> traktSearch;
    @Inject Lazy<com.uwetrottmann.trakt5.services.Shows> traktShows;

    public static synchronized TvdbTools getInstance(SgApp app) {
        if (tvdbTools == null) {
            tvdbTools = new TvdbTools(app);
        }
        return tvdbTools;
    }

    @Inject
    public TvdbTools(SgApp app) {
        this.app = app;
        app.getServicesComponent().inject(this);
    }

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
    public boolean addShow(int showTvdbId, @Nullable String language,
            @Nullable HashMap<Integer, BaseShow> traktCollection,
            @Nullable HashMap<Integer, BaseShow> traktWatched)
            throws TvdbException {
        boolean isShowExists = DBUtils.isShowExists(app, showTvdbId);
        if (isShowExists) {
            return false;
        }

        // get show and determine the language to use
        Show show = getShowDetailsWithHexagon(showTvdbId, language);
        language = show.language;

        // get episodes and store everything to the database
        final ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        batch.add(DBUtils.buildShowOp(app, show, true));
        getEpisodesAndUpdateDatabase(app, show, language, batch);

        // restore episode flags...
        if (HexagonTools.isSignedIn(app)) {
            // ...from Hexagon
            boolean success = EpisodeTools.Download.flagsFromHexagon(app, showTvdbId);
            if (!success) {
                // failed to download episode flags
                // flag show as needing an episode merge
                ContentValues values = new ContentValues();
                values.put(Shows.HEXAGON_MERGE_COMPLETE, false);
                app.getContentResolver()
                        .update(Shows.buildShowUri(showTvdbId), values, null, null);
            }

            // flag show to be auto-added (again), send (new) language to Hexagon
            ShowTools.get(app).sendIsAdded(showTvdbId, language);
        } else {
            // ...from trakt
            TraktTools traktTools = TraktTools.getInstance(app);
            if (!traktTools.storeEpisodeFlags(traktWatched, showTvdbId,
                    TraktTools.Flag.WATCHED)) {
                throw new TvdbException("addShow: storing trakt watched episodes failed.");
            }
            if (!traktTools.storeEpisodeFlags(traktCollection, showTvdbId,
                    TraktTools.Flag.COLLECTED)) {
                throw new TvdbException("addShow: storing trakt collected episodes failed.");
            }
        }

        // calculate next episode
        DBUtils.updateLatestEpisode(app, showTvdbId);

        return true;
    }

    /**
     * Updates a show. Adds new, updates changed and removes orphaned episodes.
     */
    public void updateShow(int showTvdbId) throws TvdbException {
        // determine which translation to get
        String language = getShowLanguage(app, showTvdbId);
        if (language == null) {
            return;
        }

        final ArrayList<ContentProviderOperation> batch = new ArrayList<>();

        Show show = getShowDetails(showTvdbId, language);
        batch.add(DBUtils.buildShowOp(app, show, false));

        // get episodes in the language as returned in the TVDB show entry
        // the show might not be available in the desired language
        getEpisodesAndUpdateDatabase(app, show, show.language, batch);
    }

    private static String getShowLanguage(Context context, int showTvdbId) {
        Cursor languageQuery = context.getContentResolver()
                .query(Shows.buildShowUri(showTvdbId), LANGUAGE_QUERY_PROJECTION, null, null, null);
        if (languageQuery == null) {
            // query failed, abort
            return null;
        }
        String language = null;
        if (languageQuery.moveToFirst()) {
            language = languageQuery.getString(0);
        }
        languageQuery.close();

        if (TextUtils.isEmpty(language)) {
            // fall back to preferred language
            language = DisplaySettings.getContentLanguage(context);
        }

        return language;
    }

    @Nullable
    public List<SearchResult> searchSeries(@NonNull String query, @Nullable final String language)
            throws TvdbException {
        retrofit2.Response<SeriesResultsWrapper> response;
        try {
            response = searchService.get()
                    .series(query, null, null, language)
                    .execute();
        } catch (IOException e) {
            throw new TvdbException("searchSeries: " + e.getMessage(), e);
        }

        if (response.code() == 404) {
            return null; // API returns 404 if there are no search results
        }

        ensureSuccessfulResponse(response.raw(), "searchSeries: ");

        List<Series> tvdbResults = response.body().data;
        if (tvdbResults == null || tvdbResults.size() == 0) {
            return null; // no results from tvdb
        }

        // parse into our data format
        List<SearchResult> results = new ArrayList<>(tvdbResults.size());
        for (Series tvdbResult : tvdbResults) {
            SearchResult result = new SearchResult();
            result.tvdbid = tvdbResult.id;
            result.title = tvdbResult.seriesName;
            result.overview = tvdbResult.overview;
            result.language = language;
            results.add(result);
        }
        return results;
    }

    /**
     * Search TheTVDB for shows which include a certain keyword in their title.
     *
     * @param language If not provided, will query for results in all languages.
     * @return At most 100 results (limited by TheTVDB API).
     */
    @Nonnull
    public static List<SearchResult> searchShow(@NonNull Context context, @NonNull String query,
            @Nullable final String language) throws TvdbException {
        final List<SearchResult> series = new ArrayList<>();
        final SearchResult currentShow = new SearchResult();

        RootElement root = new RootElement("Data");
        Element item = root.getChild("Series");
        // set handlers for elements we want to react to
        item.setEndElementListener(new EndElementListener() {
            public void end() {
                // only take results in the selected language
                if (language == null || language.equals(currentShow.language)) {
                    series.add(currentShow.copy());
                }
            }
        });
        item.getChild("id").setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                currentShow.tvdbid = Integer.valueOf(body);
            }
        });
        item.getChild("language").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                currentShow.language = body.trim();
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

        // build search URL: encode query...
        String url;
        try {
            url = TVDB_API_GETSERIES + URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new TvdbException("searchShow: " + e.getMessage(), e);
        }
        // ...and set language filter
        if (language == null) {
            url += TVDB_PARAM_LANGUAGE + "all";
        } else {
            url += TVDB_PARAM_LANGUAGE + language;
        }

        downloadAndParse(context, root.getContentHandler(), url, false, "searchShow: ");

        return series;
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
    private static void getEpisodesAndUpdateDatabase(Context context, Show show,
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
            throw new TvdbException("getEpisodesAndUpdateDatabase: " + e.getMessage(), e);
        }

        // insert all new episodes in bulk
        context.getContentResolver().bulkInsert(Episodes.CONTENT_URI, newEpisodesValues);
    }

    /**
     * Like {@link #getShowDetails(int, String)}, but if signed in and available adds properties
     * stored on Hexagon.
     */
    @NonNull
    private Show getShowDetailsWithHexagon(int showTvdbId, @Nullable String language)
            throws TvdbException {
        // check for show on hexagon
        com.uwetrottmann.seriesguide.backend.shows.model.Show hexagonShow;
        try {
            hexagonShow = ShowTools.Download.showFromHexagon(app, showTvdbId);
        } catch (IOException e) {
            HexagonTools.trackFailedRequest(app, "get show details", e);
            throw new TvdbException("getShowDetailsWithHexagon: " + e.getMessage(), e);
        }

        // if no language is given, try to get the language stored on hexagon
        if (language == null && hexagonShow != null) {
            language = hexagonShow.getLanguage();
        }
        // if we still have no language, use the users default language
        if (TextUtils.isEmpty(language)) {
            language = DisplaySettings.getContentLanguage(app);
        }

        // get show info from TVDb and trakt
        Show show = getShowDetails(showTvdbId, language);

        if (hexagonShow != null) {
            // restore properties from hexagon
            if (hexagonShow.getIsFavorite() != null) {
                show.favorite = hexagonShow.getIsFavorite();
            }
            if (hexagonShow.getIsHidden() != null) {
                show.hidden = hexagonShow.getIsHidden();
            }
        }

        return show;
    }

    /**
     * Get show details from TVDb in the user preferred language. Tries to fetch additional
     * information from trakt.
     *
     * @param language A TVDb language code (ISO 639-1 two-letter format, see <a
     * href="http://www.thetvdb.com/wiki/index.php/API:languages.xml">TVDb wiki</a>). If not
     * supplied, TVDb falls back to English.
     */
    @NonNull
    public Show getShowDetails(int showTvdbId, @NonNull String language) throws TvdbException {
        // try to get some details from trakt
        com.uwetrottmann.trakt5.entities.Show traktShow = null;
        // always look up the trakt id based on the TVDb id
        // e.g. a TVDb id might be linked against the wrong trakt entry, then get fixed
        String showTraktId = lookupShowTraktId(showTvdbId);
        if (showTraktId != null) {
            traktShow = SgTrakt.executeCall(app,
                    traktShows.get().summary(showTraktId, Extended.FULL),
                    "get show summary"
            );
        }

        // get full show details from TVDb
        final Show show = downloadAndParseShow(app, showTvdbId, language);

        // fill in data from trakt
        if (traktShow != null) {
            if (traktShow.ids != null && traktShow.ids.trakt != null) {
                show.traktId = traktShow.ids.trakt;
            }
            if (traktShow.airs != null) {
                show.release_time = TimeTools.parseShowReleaseTime(traktShow.airs.time);
                show.release_weekday = TimeTools.parseShowReleaseWeekDay(traktShow.airs.day);
                show.release_timezone = traktShow.airs.timezone;
            }
            show.country = traktShow.country;
            show.firstAired = TimeTools.parseShowFirstRelease(traktShow.first_aired);
            show.rating = traktShow.rating == null ? 0.0 : traktShow.rating;
        } else {
            // keep any pre-existing trakt id (e.g. trakt call above might have failed temporarily)
            Timber.w("getShowDetails: failed to get trakt show details.");
            show.traktId = ShowTools.getShowTraktId(app, showTvdbId);
            // set default values
            show.release_time = -1;
            show.release_weekday = -1;
            show.firstAired = "";
            show.rating = 0.0;
        }

        return show;
    }

    /**
     * Look up a show's trakt id, may return {@code null} if not found.
     */
    private String lookupShowTraktId(int showTvdbId) {
        // get up to 3 results: may be a show, season or episode (TVDb ids are not unique)
        List<com.uwetrottmann.trakt5.entities.SearchResult> searchResults = SgTrakt.executeCall(
                app,
                traktSearch.get().idLookup(IdType.TVDB, String.valueOf(showTvdbId), 1, 3),
                "show trakt id lookup"
        );

        if (searchResults == null) {
            return null;
        }

        for (com.uwetrottmann.trakt5.entities.SearchResult result : searchResults) {
            if (result.episode != null) {
                // not a show result
                continue;
            }
            com.uwetrottmann.trakt5.entities.Show show = result.show;
            if (show != null && show.ids != null && show.ids.trakt != null) {
                return String.valueOf(show.ids.trakt);
            }
        }

        return null;
    }

    /**
     * Get a show from TVDb. Tries to fetch in the desired language, but will fall back to the
     * default entry if no translation exists. The returned entity will still have its <b>language
     * property set to the desired language</b>, which might not be the language of the actual
     * content.
     */
    @NonNull
    private static Show downloadAndParseShow(@NonNull Context context, int showTvdbId,
            @NonNull String desiredLanguage) throws TvdbException {
        SeriesService seriesService = ServiceUtils.getTheTvdb(context).series();

        Series series = getSeries(seriesService, showTvdbId, desiredLanguage);
        // title is null if no translation exists
        boolean noTranslation = TextUtils.isEmpty(series.seriesName);
        if (noTranslation) {
            // try to fetch default entry
            series = getSeries(seriesService, showTvdbId, null);
        }

        Show result = new Show();
        result.tvdbId = showTvdbId;
        // actors are unused, are fetched from tmdb
        result.title = series.seriesName;
        result.network = series.network;
        result.contentRating = series.rating;
        result.imdbId = series.imdbId;
        result.genres = TextTools.mendTvdbStrings(series.genre);
        result.language = desiredLanguage; // requested language, might not be the content language.
        result.lastEdited = series.lastUpdated;
        if (noTranslation || TextUtils.isEmpty(series.overview)) {
            // add note about non-translated or non-existing overview
            String untranslatedOverview = series.overview;
            result.overview = context.getString(R.string.no_translation,
                    LanguageTools.getLanguageStringForCode(context, desiredLanguage),
                    context.getString(R.string.tvdb));
            if (!TextUtils.isEmpty(untranslatedOverview)) {
                result.overview += "\n\n" + untranslatedOverview;
            }
        } else {
            result.overview = series.overview;
        }
        try {
            result.runtime = Integer.parseInt(series.runtime);
        } catch (NumberFormatException e) {
            // an hour is always a good estimate...
            result.runtime = 60;
        }
        String status = series.status;
        if (status != null) {
            if (status.length() == 10) {
                result.status = ShowStatusExport.CONTINUING;
            } else if (status.length() == 5) {
                result.status = ShowStatusExport.ENDED;
            } else {
                result.status = ShowStatusExport.UNKNOWN;
            }
        }

        // poster
        retrofit2.Response<SeriesImageQueryResults> posterResponse;
        posterResponse = getSeriesPosters(seriesService, showTvdbId, desiredLanguage);
        if (posterResponse.code() == 404) {
            // no posters for this language, fall back to default
            posterResponse = getSeriesPosters(seriesService, showTvdbId, null);
        }

        if (posterResponse.isSuccessful()) {
            result.poster = getHighestRatedPoster(posterResponse.body().data);
        }

        return result;
    }

    @NonNull
    private static Series getSeries(@NonNull SeriesService seriesService, int showTvdbId,
            @Nullable String language) throws TvdbException {
        retrofit2.Response<SeriesWrapper> response;
        try {
            response = seriesService
                    .series(showTvdbId, language)
                    .execute();
        } catch (IOException e) {
            throw new TvdbException("getSeries: " + e.getMessage(), e);
        }

        ensureSuccessfulResponse(response.raw(), "getSeries: ");

        return response.body().data;
    }

    private static retrofit2.Response<SeriesImageQueryResults> getSeriesPosters(
            @NonNull SeriesService seriesService, int showTvdbId, @Nullable String language)
            throws TvdbException {
        try {
            return seriesService
                    .imagesQuery(showTvdbId, "poster", null, null, language)
                    .execute();
        } catch (IOException e) {
            throw new TvdbException("getSeriesPosters: " + e.getMessage(), e);
        }
    }

    @Nullable
    private static String getHighestRatedPoster(
            List<SeriesImageQueryResults.SeriesImageQueryResult> posters) {
        int highestRatedIndex = 0;
        double highestRating = 0.0;
        for (int i = 0; i < posters.size(); i++) {
            SeriesImageQueryResults.SeriesImageQueryResult poster = posters.get(i);
            if (poster.ratingsInfo == null || poster.ratingsInfo.average == null) {
                continue;
            }
            double rating = poster.ratingsInfo.average;
            if (rating >= highestRating) {
                highestRating = poster.ratingsInfo.average;
                highestRatedIndex = i;
            }
        }
        return posters.get(highestRatedIndex).fileName;
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
            final Context context) throws TvdbException {
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
                long releaseDateTime = TimeTools.parseEpisodeReleaseDate(context, showTimeZone,
                        releaseDate, showReleaseTime, show.country, show.network, deviceTimeZone);
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

        downloadAndParse(context, root.getContentHandler(), url, true, "parseEpisodes: ");

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
            boolean isZipFile, String logTag) throws TvdbException {
        Request request = new Request.Builder().url(urlString).build();

        Response response;
        try {
            response = ServiceUtils.getCachingOkHttpClient(context)
                    .newCall(request)
                    .execute();
        } catch (IOException e) {
            throw new TvdbException(logTag + e.getMessage(), e);
        }

        ensureSuccessfulResponse(response, logTag);

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
            throw new TvdbException(logTag + e.getMessage(), e);
        }
    }

    private static void ensureSuccessfulResponse(Response response, String logTag)
            throws TvdbException {
        if (response.code() == 404) {
            // special case: item does not exist (any longer)
            throw new TvdbException(
                    logTag + response.code() + " " + response.message(),
                    true, null);
        } else if (!response.isSuccessful()) {
            // other non-2xx response
            throw new TvdbException(
                    logTag + response.code() + " " + response.message()
            );
        }
    }
}
