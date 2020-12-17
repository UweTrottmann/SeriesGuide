package com.battlelancer.seriesguide.thetvdbapi;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.text.TextUtils;
import android.text.format.DateUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.ShowStatusExport;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.modules.ApplicationContext;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.sync.HexagonEpisodeSync;
import com.battlelancer.seriesguide.sync.TraktEpisodeSync;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.ui.search.SearchResult;
import com.battlelancer.seriesguide.ui.shows.ShowTools;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.Errors;
import com.battlelancer.seriesguide.util.LanguageTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;
import timber.log.Timber;

/**
 * Provides access to the TheTVDb.com XML API throwing in some additional data from trakt.tv here
 * and there.
 */
public class TvdbTools {

    private static final String[] LANGUAGE_QUERY_PROJECTION = new String[]{Shows.LANGUAGE};

    private final Context context;
    private Lazy<HexagonTools> hexagonTools;
    private Lazy<ShowTools> showTools;
    private Lazy<TheTvdbSearch> tvdbSearch;
    private Lazy<TheTvdbSeries> tvdbSeries;
    private Lazy<com.uwetrottmann.trakt5.services.Search> traktSearch;
    private Lazy<com.uwetrottmann.trakt5.services.Shows> traktShows;

    @Inject
    public TvdbTools(
            @ApplicationContext Context context,
            Lazy<HexagonTools> hexagonTools,
            Lazy<ShowTools> showTools,
            Lazy<TheTvdbSearch> tvdbSearch,
            Lazy<TheTvdbSeries> tvdbSeries,
            Lazy<com.uwetrottmann.trakt5.services.Search> traktSearch,
            Lazy<com.uwetrottmann.trakt5.services.Shows> traktShows
    ) {
        this.context = context;
        this.hexagonTools = hexagonTools;
        this.showTools = showTools;
        this.tvdbSearch = tvdbSearch;
        this.tvdbSeries = tvdbSeries;
        this.traktSearch = traktSearch;
        this.traktShows = traktShows;
    }

    /**
     * Returns true if the given show has not been updated in the last 12 hours.
     */
    public static boolean isUpdateShow(Context context, int showTvdbId) {
        final Cursor show = context.getContentResolver().query(Shows.buildShowUri(showTvdbId),
                new String[]{
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
            @Nullable HashMap<Integer, BaseShow> traktWatched,
            HexagonEpisodeSync hexagonEpisodeSync)
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
        getEpisodesAndUpdateDatabase(batch, show, null, language);

        // FIXME below

        // restore episode flags...
        if (hexagonEnabled) {
            // ...from Hexagon
            boolean success = hexagonEpisodeSync.downloadFlags(showTvdbId);
            if (!success) {
                // failed to download episode flags
                // flag show as needing an episode merge
                ContentValues values = new ContentValues();
                values.put(Shows.HEXAGON_MERGE_COMPLETE, 0);
                context.getContentResolver()
                        .update(Shows.buildShowUri(showTvdbId), values, null, null);
            }

            // flag show to be auto-added (again), send (new) language to Hexagon
            showTools.get().sendIsAdded(showTvdbId, language);
        } else {
            // ...from trakt
            TraktEpisodeSync traktEpisodeSync = new TraktEpisodeSync(context, null);
            if (!traktEpisodeSync.storeEpisodeFlags(traktWatched, showTvdbId,
                    TraktEpisodeSync.Flag.WATCHED)) {
                throw new TvdbDataException("addShow: storing trakt watched episodes failed.");
            }
            if (!traktEpisodeSync.storeEpisodeFlags(traktCollection, showTvdbId,
                    TraktEpisodeSync.Flag.COLLECTED)) {
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
        int showId = SgRoomDatabase.getInstance(context).showHelper().getShowId(showTvdbId);
        getEpisodesAndUpdateDatabase(batch, show, showId, show.language);
    }

    /**
     * @return {@code null} if the query failed, should try again later.
     */
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

        // handle legacy records
        if (TextUtils.isEmpty(language)) {
            // default to 'en' for consistent behavior across devices
            // and to encourage users to set language
            language = DisplaySettings.LANGUAGE_EN;
        }

        return language;
    }

    @Nullable
    public List<SearchResult> searchSeries(@NonNull String query, @NonNull final String language)
            throws TvdbException {
        retrofit2.Response<SeriesResultsResponse> response;
        try {
            response = tvdbSearch.get()
                    .series(query, null, null, null, language)
                    .execute();
        } catch (Exception e) {
            Errors.logAndReport("searchSeries", e);
            throw new TvdbException("searchSeries", e);
        }

        if (response.code() == 404) {
            return null; // API returns 404 if there are no search results
        }

        Errors.throwAndReportIfNotSuccessfulTvdb("searchSeries", response.raw());

        List<Series> tvdbResults = response.body().data;
        if (tvdbResults == null || tvdbResults.size() == 0) {
            return null; // no results from tvdb
        }

        // parse into our data format
        List<SearchResult> results = new ArrayList<>(tvdbResults.size());
        for (Series tvdbResult : tvdbResults) {
            SearchResult result = new SearchResult();
            result.setTvdbid(tvdbResult.id);
            result.setTitle(tvdbResult.seriesName);
            result.setOverview(tvdbResult.overview);
            result.setLanguage(language);
            results.add(result);
        }
        return results;
    }

    /**
     * Fetches episodes for the given show from TVDb, adds database ops for them. Then adds all
     * information to the database.
     */
    private void getEpisodesAndUpdateDatabase(final ArrayList<ContentProviderOperation> batch,
            Show show, @Nullable Integer showId, String language) throws TvdbException {
        // get ops for episodes of this show
        TvdbEpisodeTools episodeTools = new TvdbEpisodeTools(context, tvdbSeries);
        ArrayList<ContentValues> importShowEpisodes = episodeTools
                .fetchEpisodes(batch, show, showId, language);
        ContentValues[] newEpisodesValues = new ContentValues[importShowEpisodes.size()];
        newEpisodesValues = importShowEpisodes.toArray(newEpisodesValues);

        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            Errors.logAndReport("getEpisodesAndUpdateDatabase", e);
            throw new TvdbDataException("getEpisodesAndUpdateDatabase", e);
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
            } catch (IOException | IllegalArgumentException e) {
                // Note: JSON parser may throw IllegalArgumentException.
                Errors.logAndReportHexagon("get show details", e);
                throw new TvdbCloudException("getShowDetailsWithHexagon", e);
            }
        }

        // if no language is given, try to get the language stored on hexagon
        if (language == null && hexagonShow != null) {
            language = hexagonShow.getLanguage();
        }
        // handle legacy records without language
        if (language == null || language.length() == 0) {
            // default to 'en' to ensure consistency across devices,
            // and to encourage users to set a language
            language = DisplaySettings.LANGUAGE_EN;
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
     * href="http://www.thetvdb.com/wiki/index.php/API:languages.xml">TVDb wiki</a>).
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
            com.uwetrottmann.trakt5.entities.Show traktShow = SgTrakt.executeCall(
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
     * Get a show from TVDb. Tries to fetch in the desired language, but will use
     * {@link DisplaySettings#getShowsLanguageFallback(Context)} if no translation or poster in that
     * language exists. The returned entity will still have its <b>language property set to the
     * desired language</b>, which might not be the language of the actual content.
     */
    @NonNull
    private Show downloadAndParseShow(int showTvdbId, @NonNull String desiredLanguage)
            throws TvdbException {
        Series series = getSeries(showTvdbId, desiredLanguage);
        // title is null if no translation exists
        boolean noTranslation = TextUtils.isEmpty(series.seriesName);
        if (noTranslation) {
            // try to fetch using fall back language
            series = getSeries(showTvdbId, DisplaySettings.getShowsLanguageFallback(context));
        }

        Show result = new Show();
        result.tvdb_id = showTvdbId;
        result.tvdb_slug = series.slug;
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
            // instead of comparing the string, just compare for length
            // faster and unlikely new values of same length are added at TheTVDB
            if (status.length() == 10) {
                result.status = ShowStatusExport.CONTINUING;
            } else if (status.length() == 5) {
                result.status = ShowStatusExport.ENDED;
            } else if (status.length() == 8) {
                result.status = ShowStatusExport.UPCOMING;
            } else {
                result.status = ShowStatusExport.UNKNOWN;
            }
        }

        // poster
        retrofit2.Response<SeriesImageQueryResultResponse> posterResponse;
        posterResponse = getSeriesPosters(showTvdbId, desiredLanguage);
        if (posterResponse.code() == 404) {
            // no posters for this language, try fall back language
            posterResponse = getSeriesPosters(showTvdbId,
                    DisplaySettings.getShowsLanguageFallback(context));
        }

        if (posterResponse.isSuccessful() && posterResponse.body() != null) {
            List<SeriesImageQueryResult> data = posterResponse.body().data;
            if (data != null && !data.isEmpty()) {
                TvdbPoster poster = getHighestRatedPoster(data);
                result.poster = poster.fullSize;
                result.poster_small = poster.smallSize;
            }
        }

        return result;
    }

    @NonNull
    private Series getSeries(int showTvdbId, @Nullable String language) throws TvdbException {
        retrofit2.Response<SeriesResponse> response;
        try {
            response = tvdbSeries.get().series(showTvdbId, language).execute();
        } catch (Exception e) {
            Errors.logAndReport("getSeries", e);
            throw new TvdbException("getSeries", e);
        }

        Errors.throwAndReportIfNotSuccessfulTvdb("getSeries", response.raw());

        return response.body().data;
    }

    public retrofit2.Response<SeriesImageQueryResultResponse> getSeriesPosters(int showTvdbId,
            @Nullable String language) throws TvdbException {
        try {
            return tvdbSeries.get()
                    .imagesQuery(showTvdbId, "poster", null, null, language)
                    .execute();
        } catch (Exception e) {
            Errors.logAndReport("getSeriesPosters", e);
            throw new TvdbException("getSeriesPosters", e);
        }
    }

    public static class TvdbPoster {
        @Nullable
        public final String fullSize;
        @Nullable
        public final String smallSize;
        TvdbPoster(@Nullable String fullSize, @Nullable String smallSize) {
            this.fullSize = fullSize;
            this.smallSize = smallSize;
        }
    }

    /**
     * Returns the first poster in the list with the highest number of favorites.
     * Assumes the given list is not empty.
     */
    public static TvdbPoster getHighestRatedPoster(List<SeriesImageQueryResult> posters) {
        int mostFavsIndex = 0;
        int mostFavs = 0;
        for (int i = 0; i < posters.size(); i++) {
            SeriesImageQueryResult poster = posters.get(i);
            if (poster.ratingsInfo == null || poster.ratingsInfo.count == null) {
                continue;
            }
            int favs = poster.ratingsInfo.count;
            if (favs > mostFavs) {
                mostFavs = favs;
                mostFavsIndex = i;
            }
        }
        SeriesImageQueryResult image = posters.get(mostFavsIndex);
        return new TvdbPoster(image.fileName, image.thumbnail);
    }
}
