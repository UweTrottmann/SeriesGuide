package com.battlelancer.seriesguide.loaders;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.enums.MoviesDiscoverLink;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.tmdbapi.SgTmdb;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb2.Tmdb;
import com.uwetrottmann.tmdb2.entities.DiscoverFilter;
import com.uwetrottmann.tmdb2.entities.Movie;
import com.uwetrottmann.tmdb2.entities.MovieResultsPage;
import com.uwetrottmann.tmdb2.entities.TmdbDate;
import com.uwetrottmann.tmdb2.enumerations.ReleaseType;
import com.uwetrottmann.tmdb2.services.MoviesService;
import com.uwetrottmann.tmdb2.services.SearchService;
import dagger.Lazy;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Loads a list of movies from TMDb.
 */
public class TmdbMoviesLoader extends GenericSimpleLoader<TmdbMoviesLoader.Result> {

    public static class Result {
        /** If loading failed, is null. Empty if no results. */
        @Nullable public List<Movie> results;
        @NonNull public String emptyText;

        public Result(@Nullable List<Movie> results, @NonNull String emptyText) {
            this.results = results;
            this.emptyText = emptyText;
        }
    }

    @Inject Lazy<Tmdb> tmdb;
    @Inject Lazy<MoviesService> moviesService;
    @Inject Lazy<SearchService> searchService;
    @NonNull private final MoviesDiscoverLink link;
    @Nullable private String query;

    /**
     * If a query is given, will load search results for that query. Otherwise will load a list of
     * movies based on the given link.
     */
    public TmdbMoviesLoader(SgApp app, @NonNull MoviesDiscoverLink link, @Nullable String query) {
        super(app);
        app.getServicesComponent().inject(this);
        this.link = link;
        this.query = query;
    }

    @Override
    public Result loadInBackground() {
        String languageCode = DisplaySettings.getMoviesLanguage(getContext());
        String regionCode = DisplaySettings.getMoviesRegion(getContext());

        List<Movie> results = null;
        String action = null;
        try {
            Response<MovieResultsPage> response;

            if (TextUtils.isEmpty(query)) {
                MoviesService moviesService = this.moviesService.get();
                Call<MovieResultsPage> call;
                switch (link) {
                    case POPULAR:
                        action = "get popular movies";
                        call = moviesService.popular(null, languageCode);
                        break;
                    case DIGITAL:
                        action = "get movie digital releases";
                        call = tmdb.get().discoverMovie()
                                .with_release_type(new DiscoverFilter(DiscoverFilter.Separator.AND,
                                        ReleaseType.DIGITAL))
                                .release_date_lte(getDateNow())
                                .release_date_gte(getDateOneMonthAgo())
                                .language(languageCode)
                                .region(regionCode)
                                .build();
                        break;
                    case DISC:
                        action = "get movie disc releases";
                        call = tmdb.get().discoverMovie()
                                .with_release_type(new DiscoverFilter(DiscoverFilter.Separator.AND,
                                        ReleaseType.PHYSICAL))
                                .release_date_lte(getDateNow())
                                .release_date_gte(getDateOneMonthAgo())
                                .language(languageCode)
                                .region(regionCode)
                                .build();
                        break;
                    case IN_THEATERS:
                    default:
                        action = "get now playing movies";
                        call = tmdb.get().discoverMovie()
                                .with_release_type(new DiscoverFilter(DiscoverFilter.Separator.OR,
                                        ReleaseType.THEATRICAL, ReleaseType.THEATRICAL_LIMITED))
                                .release_date_lte(getDateNow())
                                .release_date_gte(getDateOneMonthAgo())
                                .language(languageCode)
                                .region(regionCode)
                                .build();
                        break;
                }
                response = call.execute();
            } else {
                action = "search for movies";
                response = searchService.get()
                        .movie(query, null, languageCode, false, null, null, null)
                        .execute();
            }

            if (response.isSuccessful()) {
                MovieResultsPage page = response.body();
                if (page != null) {
                    results = page.results;
                }
            } else {
                SgTmdb.trackFailedRequest(getContext(), action, response);
                return buildErrorResult();
            }
        } catch (IOException e) {
            SgTmdb.trackFailedRequest(getContext(), action, e);
            // only check for connection here to allow hitting the response cache
            return AndroidUtils.isNetworkConnected(getContext())
                    ? buildErrorResult()
                    : new Result(null, getContext().getString(R.string.offline));
        }

        return new Result(results, getContext().getString(R.string.no_results));
    }

    private TmdbDate getDateNow() {
        return new TmdbDate(new Date());
    }

    private TmdbDate getDateOneMonthAgo() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -30);
        return new TmdbDate(calendar.getTime());
    }

    private Result buildErrorResult() {
        return new Result(null, getContext().getString(R.string.api_error_generic,
                getContext().getString(R.string.tmdb)));
    }
}
