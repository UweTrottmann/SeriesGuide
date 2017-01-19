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
import com.uwetrottmann.tmdb2.entities.Movie;
import com.uwetrottmann.tmdb2.entities.MovieResultsPage;
import com.uwetrottmann.tmdb2.services.MoviesService;
import com.uwetrottmann.tmdb2.services.SearchService;
import dagger.Lazy;
import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Loads a list of movies from TMDb.
 */
public class TmdbMoviesLoader extends GenericSimpleLoader<TmdbMoviesLoader.Result> {

    public static class Result {
        public List<Movie> results;
        public String emptyText;

        public Result(List<Movie> results, String emptyText) {
            this.results = results;
            this.emptyText = emptyText;
        }
    }

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
                    case DISC:
                    case IN_THEATERS:
                    default:
                        action = "get now playing movies";
                        call = moviesService.nowPlaying(null, languageCode);
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

    private Result buildErrorResult() {
        return new Result(null, getContext().getString(R.string.api_error_generic,
                getContext().getString(R.string.tmdb)));
    }
}
