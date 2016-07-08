package com.battlelancer.seriesguide.loaders;

import android.content.Context;
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.tmdbapi.SgTmdb;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb2.Tmdb;
import com.uwetrottmann.tmdb2.entities.Movie;
import com.uwetrottmann.tmdb2.entities.MovieResultsPage;
import java.io.IOException;
import java.util.List;
import retrofit2.Response;

/**
 * Loads a list of movies from TMDb.
 */
public class TmdbMoviesLoader extends GenericSimpleLoader<TmdbMoviesLoader.Result> {

    public static class Result {
        public List<Movie> results;
        public int emptyTextResId;

        public Result(List<Movie> results, int emptyTextResId) {
            this.results = results;
            this.emptyTextResId = emptyTextResId;
        }
    }

    private String mQuery;

    public TmdbMoviesLoader(Context context, String query) {
        super(context);
        mQuery = query;
    }

    @Override
    public Result loadInBackground() {
        Tmdb tmdb = ServiceUtils.getTmdb(getContext());
        String languageCode = DisplaySettings.getContentLanguage(getContext());

        List<Movie> results = null;
        String action = null;
        try {
            Response<MovieResultsPage> response;

            if (TextUtils.isEmpty(mQuery)) {
                action = "get now playing movies";
                response = tmdb.moviesService()
                        .nowPlaying(null, languageCode)
                        .execute();
            } else {
                action = "search for movies";
                response = tmdb.searchService()
                        .movie(mQuery, null, languageCode, false, null, null, null)
                        .execute();
            }

            if (response.isSuccessful()) {
                MovieResultsPage page = response.body();
                if (page != null) {
                    results = page.results;
                }
            } else {
                SgTmdb.trackFailedRequest(getContext(), action, response);
            }
        } catch (IOException e) {
            SgTmdb.trackFailedRequest(getContext(), action, e);
            // only check for connection here to allow hitting the response cache
            return new Result(null,
                    AndroidUtils.isNetworkConnected(getContext()) ? R.string.search_error
                            : R.string.offline);
        }

        return new Result(results, R.string.no_results);
    }
}
