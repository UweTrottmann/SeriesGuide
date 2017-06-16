package com.battlelancer.seriesguide.thetvdbapi;

import android.annotation.SuppressLint;
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
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.ShowStatusExport;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.modules.ApplicationContext;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.LanguageTools;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.TraktTools;
import com.uwetrottmann.thetvdb.entities.Episode;
import com.uwetrottmann.thetvdb.entities.EpisodesResponse;
import com.uwetrottmann.thetvdb.entities.Series;
import com.uwetrottmann.thetvdb.entities.SeriesImageQueryResult;
import com.uwetrottmann.thetvdb.entities.SeriesImageQueryResultResponse;
import com.uwetrottmann.thetvdb.entities.SeriesResponse;
import com.uwetrottmann.thetvdb.entities.SeriesResultsResponse;
import com.uwetrottmann.thetvdb.services.TheTvdbSearch;
import com.uwetrottmann.thetvdb.services.TheTvdbSeries;
import com.uwetrottmann.trakt5.entities.BaseShow;
import com.uwetrottmann.trakt5.enums.Extended;
import com.uwetrottmann.trakt5.enums.IdType;
import com.uwetrottmann.trakt5.enums.Type;
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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import timber.log.Timber;

/**
 * Provides access to the TheTVDb.com XML API throwing in some additional data from trakt.tv here
 * and there.
 */
public class TvdbTools {

    private static final String TVDB_API_URL = "http://thetvdb.com/api/";
    private static final String TVDB_API_GETSERIES = TVDB_API_URL + "GetSeries.php?seriesname=";
    private static final String TVDB_PARAM_LANGUAGE = "&language=";
    private static final String[] LANGUAGE_QUERY_PROJECTION = new String[] { Shows.LANGUAGE };

    private final Context context;
    Lazy<HexagonTools> hexagonTools;
    Lazy<ShowTools> showTools;
    Lazy<TraktTools> traktTools;
    Lazy<TheTvdbSearch> tvdbSearch;
    Lazy<TheTvdbSeries> tvdbSeries;
    Lazy<com.uwetrottmann.trakt5.services.Search> traktSearch;
    Lazy<com.uwetrottmann.trakt5.services.Shows> traktShows;
    Lazy<OkHttpClient> okHttpClient;

    @Inject
    public TvdbTools(
            @ApplicationContext Context context,
            Lazy<HexagonTools> hexagonTools,
            Lazy<ShowTools> showTools,
            Lazy<TraktTools> traktTools,
            Lazy<TheTvdbSearch> tvdbSearch,
            Lazy<TheTvdbSeries> tvdbSeries,
            Lazy<com.uwetrottmann.trakt5.services.Search> traktSearch,
            Lazy<com.uwetrottmann.trakt5.services.Shows> traktShows,
            Lazy<OkHttpClient> okHttpClient
    ) {
        this.context = context;
        this.hexagonTools = hexagonTools;
        this.showTools = showTools;
        this.traktTools = traktTools;
        this.tvdbSearch = tvdbSearch;
        this.tvdbSeries = tvdbSeries;
        this.traktSearch = traktSearch;
        this.traktShows = traktShows;
        this.okHttpClient = okHttpClient;
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
        boolean isShowExists = DBUtils.isShowExists(context, showTvdbId);
        if (isShowExists) {
            return false;
        }

        // get show and determine the language to use
        boolean hexagonEnabled = HexagonSettings.isEnabled(context);
        Show show = getShowDetailsWithHexagon(showTvdbId, language, hexagonEnabled);
        language = show.language;

        // get episodes and store everything to the database
        final ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        batch.add(DBUtils.buildShowOp(context, show, true));
        getEpisodesAndUpdateDatabase(batch, show, language);

        // restore episode flags...
        if (hexagonEnabled) {
            // ...from Hexagon
            boolean success = EpisodeTools.Download.flagsFromHexagon(context, hexagonTools.get(),
                    showTvdbId);
            if (!success) {
                // failed to download episode flags
                // flag show as needing an episode merge
                ContentValues values = new ContentValues();
                values.put(Shows.HEXAGON_MERGE_COMPLETE, false);
                context.getContentResolver()
                        .update(Shows.buildShowUri(showTvdbId), values, null, null);
            }

            // flag show to be auto-added (again), send (new) language to Hexagon
            showTools.get().sendIsAdded(showTvdbId, language);
        } else {
            // ...from trakt
            TraktTools traktTools = this.traktTools.get();
            if (!traktTools.storeEpisodeFlags(traktWatched, showTvdbId,
                    TraktTools.Flag.WATCHED)) {
                throw new TvdbDataException("addShow: storing trakt watched episodes failed.");
            }
            if (!traktTools.storeEpisodeFlags(traktCollection, showTvdbId,
                    TraktTools.Flag.COLLECTED)) {
                throw new TvdbDataException("addShow: storing trakt collected episodes failed.");
            }
        }

        // calculate next episode
        DBUtils.updateLatestEpisode(context, showTvdbId);

        return true;
    }

    /**
     * Updates a show. Adds new, updates changed and removes orphaned episodes.
     */
    public void updateShow(int showTvdbId) throws TvdbException {
        // determine which translation to get
        String language = getShowLanguage(context, showTvdbId);
        if (language == null) {
            return;
        }

        final ArrayList<ContentProviderOperation> batch = new ArrayList<>();

        Show show = getShowDetails(showTvdbId, language);
        batch.add(DBUtils.buildShowOp(context, show, false));

        // get episodes in the language as returned in the TVDB show entry
        // the show might not be available in the desired language
        getEpisodesAndUpdateDatabase(batch, show, show.language);
    }

    public static String getShowLanguage(Context context, int showTvdbId) {
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
        retrofit2.Response<SeriesResultsResponse> response;
        try {
            response = tvdbSearch.get()
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
    public List<SearchResult> searchShow(@NonNull String query, @Nullable final String language)
            throws TvdbException {
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
            throw new TvdbDataException("searchShow: " + e.getMessage(), e);
        }
        // ...and set language filter
        if (language == null) {
            url += TVDB_PARAM_LANGUAGE + "all";
        } else {
            url += TVDB_PARAM_LANGUAGE + language;
        }

        downloadAndParse(root.getContentHandler(), url, false, "searchShow: ");

        return series;
    }

    /**
     * Fetches episodes for the given show from TVDb, adds database ops for them. Then adds all
     * information to the database.
     */
    private void getEpisodesAndUpdateDatabase(final ArrayList<ContentProviderOperation> batch,
            Show show, String language) throws TvdbException {
        // get ops for episodes of this show
        ArrayList<ContentValues> importShowEpisodes = fetchEpisodes(batch, show, language);
        ContentValues[] newEpisodesValues = new ContentValues[importShowEpisodes.size()];
        newEpisodesValues = importShowEpisodes.toArray(newEpisodesValues);

        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            throw new TvdbDataException("getEpisodesAndUpdateDatabase: " + e.getMessage(), e);
        }

        // insert all new episodes in bulk
        context.getContentResolver().bulkInsert(Episodes.CONTENT_URI, newEpisodesValues);
    }

    /**
     * Like {@link #getShowDetails(int, String)}, but if signed in and available adds properties
     * stored on Hexagon.
     */
    @NonNull
    private Show getShowDetailsWithHexagon(int showTvdbId, @Nullable String language,
            boolean hexagonEnabled)
            throws TvdbException {
        // check for show on hexagon
        com.uwetrottmann.seriesguide.backend.shows.model.Show hexagonShow = null;
        if (hexagonEnabled) {
            try {
                com.uwetrottmann.seriesguide.backend.shows.Shows showsService =
                        hexagonTools.get().getShowsService();
                if (showsService != null) {
                    hexagonShow = showsService.getShow().setShowTvdbId(showTvdbId).execute();
                }
            } catch (IOException e) {
                HexagonTools.trackFailedRequest(context, "get show details", e);
                throw new TvdbCloudException("getShowDetailsWithHexagon: " + e.getMessage(), e);
            }
        }

        // if no language is given, try to get the language stored on hexagon
        if (language == null && hexagonShow != null) {
            language = hexagonShow.getLanguage();
        }
        // if we still have no language, use the users default language
        if (TextUtils.isEmpty(language)) {
            language = DisplaySettings.getContentLanguage(context);
        }

        // get show info from TVDb and trakt
        Show show = getShowDetails(showTvdbId, language);

        if (hexagonShow != null) {
            // restore properties from hexagon
            if (hexagonShow.getIsFavorite() != null) {
                show.favorite = hexagonShow.getIsFavorite();
            }
            if (hexagonShow.getNotify() != null) {
                show.notify = hexagonShow.getNotify();
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
     * @throws TvdbException If a request fails or a response appears to be corrupted.
     */
    @NonNull
    public Show getShowDetails(int showTvdbId, @NonNull String language) throws TvdbException {
        // always look up the trakt id
        // a TVDb id might be linked against the wrong trakt entry, then get fixed
        Integer showTraktId = lookupShowTraktId(showTvdbId);

        // get show from TVDb
        final Show show = downloadAndParseShow(showTvdbId, language);

        if (showTraktId != null) {
            // get some more details from trakt
            com.uwetrottmann.trakt5.entities.Show traktShow = SgTrakt.executeCall(context,
                    traktShows.get().summary(String.valueOf(showTraktId), Extended.FULL),
                    "get show summary"
            );
            if (traktShow == null) {
                throw new TvdbTraktException("getShowDetails: failed to get trakt show details.");
            }
            if (traktShow.ids != null && traktShow.ids.trakt != null) {
                show.trakt_id = traktShow.ids.trakt;
            }
            if (traktShow.airs != null) {
                show.release_time = TimeTools.parseShowReleaseTime(traktShow.airs.time);
                show.release_weekday = TimeTools.parseShowReleaseWeekDay(traktShow.airs.day);
                show.release_timezone = traktShow.airs.timezone;
            }
            show.country = traktShow.country;
            show.first_aired = TimeTools.parseShowFirstRelease(traktShow.first_aired);
            show.rating = traktShow.rating == null ? 0.0 : traktShow.rating;
        } else {
            // no trakt id (show not on trakt): set default values
            Timber.w("getShowDetails: no trakt id found, using default values.");
            show.trakt_id = null;
            show.release_time = -1;
            show.release_weekday = -1;
            show.first_aired = "";
            show.rating = 0.0;
        }

        return show;
    }

    /**
     * Look up a show's trakt id, may return {@code null} if not found.
     *
     * @throws TvdbException If the request failed or the response appears to be corrupted.
     */
    @Nullable
    private Integer lookupShowTraktId(int showTvdbId) throws TvdbException {
        List<com.uwetrottmann.trakt5.entities.SearchResult> searchResults = SgTrakt.executeCall(
                context,
                traktSearch.get().idLookup(IdType.TVDB, String.valueOf(showTvdbId), Type.SHOW,
                        null, 1, 1),
                "show trakt id lookup"
        );

        if (searchResults == null) {
            throw new TvdbTraktException("lookupShowTraktId: failed.");
        }

        if (searchResults.size() != 1) {
            return null; // no results
        }

        com.uwetrottmann.trakt5.entities.SearchResult result = searchResults.get(0);
        if (result.show != null && result.show.ids != null) {
            return result.show.ids.trakt;
        } else {
            throw new TvdbTraktException("lookupShowTraktId: response corrupted.");
        }
    }

    /**
     * Get a show from TVDb. Tries to fetch in the desired language, but will fall back to the
     * default entry if no translation exists. The returned entity will still have its <b>language
     * property set to the desired language</b>, which might not be the language of the actual
     * content.
     */
    @NonNull
    private Show downloadAndParseShow(int showTvdbId, @NonNull String desiredLanguage)
            throws TvdbException {
        Series series = getSeries(showTvdbId, desiredLanguage);
        // title is null if no translation exists
        boolean noTranslation = TextUtils.isEmpty(series.seriesName);
        if (noTranslation) {
            // try to fetch default entry
            series = getSeries(showTvdbId, null);
        }

        Show result = new Show();
        result.tvdb_id = showTvdbId;
        // actors are unused, are fetched from tmdb
        result.title = series.seriesName != null ? series.seriesName.trim() : null;
        result.network = series.network;
        result.content_rating = series.rating;
        result.imdb_id = series.imdbId;
        result.genres = TextTools.mendTvdbStrings(series.genre);
        result.language = desiredLanguage; // requested language, might not be the content language.
        result.last_edited = series.lastUpdated;
        if (noTranslation || TextUtils.isEmpty(series.overview)) {
            // add note about non-translated or non-existing overview
            String untranslatedOverview = series.overview;
            result.overview = context.getString(R.string.no_translation,
                    LanguageTools.getShowLanguageStringFor(context, desiredLanguage),
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
        retrofit2.Response<SeriesImageQueryResultResponse> posterResponse;
        posterResponse = getSeriesPosters(showTvdbId, desiredLanguage);
        if (posterResponse.code() == 404) {
            // no posters for this language, fall back to default
            posterResponse = getSeriesPosters(showTvdbId, null);
        }

        if (posterResponse.isSuccessful()) {
            result.poster = getHighestRatedPoster(posterResponse.body().data);
        }

        return result;
    }

    @NonNull
    private Series getSeries(int showTvdbId, @Nullable String language) throws TvdbException {
        retrofit2.Response<SeriesResponse> response;
        try {
            response = tvdbSeries.get().series(showTvdbId, language).execute();
        } catch (IOException e) {
            throw new TvdbException("getSeries: " + e.getMessage(), e);
        }

        ensureSuccessfulResponse(response.raw(), "getSeries: ");

        return response.body().data;
    }

    private retrofit2.Response<SeriesImageQueryResultResponse> getSeriesPosters(int showTvdbId,
            @Nullable String language) throws TvdbException {
        try {
            return tvdbSeries.get()
                    .imagesQuery(showTvdbId, "poster", null, null, language)
                    .execute();
        } catch (IOException e) {
            throw new TvdbException("getSeriesPosters: " + e.getMessage(), e);
        }
    }

    @Nullable
    private static String getHighestRatedPoster(List<SeriesImageQueryResult> posters) {
        int highestRatedIndex = 0;
        double highestRating = 0.0;
        for (int i = 0; i < posters.size(); i++) {
            SeriesImageQueryResult poster = posters.get(i);
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

    /**
     * Loads and parses episodes for the given show and language to create an array of {@link
     * ContentValues} for new episodes.<br> Adds update ops for updated episodes and delete ops for
     * local orphaned episodes to the given {@link ContentProviderOperation} batch.
     */
    private ArrayList<ContentValues> fetchEpisodes(ArrayList<ContentProviderOperation> batch,
            Show show, @NonNull String language) throws TvdbException {
        final int showTvdbId = show.tvdb_id;
        final ArrayList<ContentValues> newEpisodesValues = new ArrayList<>();
        final HashMap<Integer, Long> localEpisodeIds = DBUtils.getEpisodeMapForShow(context,
                showTvdbId);
        @SuppressLint("UseSparseArrays") final HashMap<Integer, Long> removableEpisodeIds =
                new HashMap<>(localEpisodeIds); // just copy episodes list, then remove valid ones
        final HashSet<Integer> localSeasonIds = DBUtils.getSeasonIdsOfShow(context, showTvdbId);
        // store updated seasons to avoid duplicate ops
        final HashSet<Integer> seasonIdsToUpdate = new HashSet<>();

        final long dateLastMonthEpoch = (System.currentTimeMillis()
                - (DateUtils.DAY_IN_MILLIS * 30)) / 1000;
        final ZoneId showTimeZone = TimeTools.getDateTimeZone(show.release_timezone);
        final LocalTime showReleaseTime = TimeTools.getShowReleaseTime(show.release_time);
        final String deviceTimeZone = TimeZone.getDefault().getID();

        Integer page = 0;
        while (page != null) {
            EpisodesResponse response = getEpisodes(showTvdbId, page, language);
            page = response.links.next;

            final ContentValues values = new ContentValues();
            for (Episode episode : response.data) {
                Integer episodeId = episode.id;
                if (episodeId == null || episodeId <= 0) {
                    continue; // invalid id, skip
                }

                // don't clean up this episode
                removableEpisodeIds.remove(episodeId);

                boolean insert = true;
                if (localEpisodeIds.containsKey(episodeId)) {
                    /*
                     * Update uses provider ops which take a long time. Only
                     * update if episode was edited on TVDb or is not older than
                     * a month (ensures show air time changes get stored).
                     */
                    Long lastEditEpoch = localEpisodeIds.get(episodeId);
                    Long lastEditEpochNew = episode.lastUpdated;
                    if (lastEditEpoch != null && lastEditEpochNew != null
                            && (lastEditEpoch < lastEditEpochNew
                            || dateLastMonthEpoch < lastEditEpoch)) {
                        // update episode
                        insert = false;
                    } else {
                        continue; // too old to update, skip
                    }
                }

                // extract values
                values.put(Episodes._ID, episode.id);
                Integer seasonId = episode.airedSeasonID;
                values.put(Seasons.REF_SEASON_ID, seasonId);
                values.put(Shows.REF_SHOW_ID, showTvdbId);

                values.put(Episodes.NUMBER, episode.airedEpisodeNumber);
                values.put(Episodes.ABSOLUTE_NUMBER, episode.absoluteNumber);
                values.put(Episodes.SEASON, episode.airedSeason);
                values.put(Episodes.DVDNUMBER, episode.dvdEpisodeNumber);

                long releaseDateTime = TimeTools.parseEpisodeReleaseDate(context, showTimeZone,
                        episode.firstAired, showReleaseTime, show.country, show.network,
                        deviceTimeZone);
                values.put(Episodes.FIRSTAIREDMS, releaseDateTime);
                values.put(Episodes.TITLE, episode.episodeName == null ? "" : episode.episodeName);
                values.put(Episodes.OVERVIEW, episode.overview);
                values.put(Episodes.LAST_EDITED, episode.lastUpdated);

                if (insert) {
                    // episode does not exist, yet: insert
                    newEpisodesValues.add(new ContentValues(values));
                } else {
                    // episode exists: update
                    batch.add(DBUtils.buildEpisodeUpdateOp(values));
                }

                if (seasonId != null && !seasonIdsToUpdate.contains(seasonId)) {
                    // add insert/update op for season
                    batch.add(DBUtils.buildSeasonOp(values, !localSeasonIds.contains(seasonId)));
                    seasonIdsToUpdate.add(seasonId);
                }

                values.clear();
            }
        }

        // add delete ops for leftover episodeIds in our db
        for (Integer episodeId : removableEpisodeIds.keySet()) {
            batch.add(ContentProviderOperation.newDelete(Episodes.buildEpisodeUri(episodeId))
                    .build());
        }

        return newEpisodesValues;
    }

    @NonNull
    private EpisodesResponse getEpisodes(int showTvdbId, int page, @Nullable String language)
            throws TvdbException {
        retrofit2.Response<EpisodesResponse> response;
        try {
            response = tvdbSeries.get().episodes(showTvdbId, page, language).execute();
        } catch (IOException e) {
            throw new TvdbException("getEpisodes: " + e.getMessage(), e);
        }

        ensureSuccessfulResponse(response.raw(), "getEpisodes: ");

        return response.body();
    }

    /**
     * Downloads the XML or ZIP file from the given URL, passing a valid response to {@link
     * Xml#parse(InputStream, android.util.Xml.Encoding, ContentHandler)} using the given {@link
     * ContentHandler}.
     */
    private void downloadAndParse(ContentHandler handler, String urlString, boolean isZipFile,
            String logTag) throws TvdbException {
        Request request = new Request.Builder().url(urlString).build();

        Response response;
        try {
            response = okHttpClient.get().newCall(request).execute();
        } catch (IOException e) {
            throw new TvdbException(logTag + e.getMessage(), e);
        }

        ensureSuccessfulResponse(response, logTag);

        try {
            //noinspection ConstantConditions
            final InputStream input = response.body().byteStream();
            if (isZipFile) {
                // We downloaded the compressed file from TheTVDB
                final ZipInputStream zipin = new ZipInputStream(input);
                zipin.getNextEntry();
                try {
                    Xml.parse(zipin, Xml.Encoding.UTF_8, handler);
                } finally {
                    //noinspection ThrowFromFinallyBlock
                    zipin.close();
                }
            } else {
                try {
                    Xml.parse(input, Xml.Encoding.UTF_8, handler);
                } finally {
                    if (input != null) {
                        //noinspection ThrowFromFinallyBlock
                        input.close();
                    }
                }
            }
        } catch (SAXException | IOException | AssertionError e) {
            throw new TvdbDataException(logTag + e.getMessage(), e);
        }
    }

    private static void ensureSuccessfulResponse(Response response, String logTag)
            throws TvdbException {
        if (response.code() == 404) {
            // special case: item does not exist (any longer)
            throw new TvdbException(
                    logTag + response.code() + " " + response.message(),
                    true
            );
        } else if (!response.isSuccessful()) {
            // other non-2xx response
            throw new TvdbException(
                    logTag + response.code() + " " + response.message()
            );
        }
    }
}
