package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.TmdbSettings;
import com.battlelancer.seriesguide.thetvdbapi.TvdbException;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.battlelancer.seriesguide.thetvdbapi.TvdbTools;
import com.battlelancer.seriesguide.ui.movies.MovieTools;
import com.squareup.picasso.Downloader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;
import com.uwetrottmann.thetvdb.entities.SeriesImageQueryResultResponse;
import com.uwetrottmann.tmdb2.entities.Movie;
import java.io.IOException;
import java.io.InputStream;

/**
 * This is mostly a copy of {@link com.squareup.picasso.NetworkRequestHandler} that is not visible.
 * Extended to fetch the image url from a given show TVDB id or movie TMDB id.
 */
public class SgPicassoRequestHandler extends RequestHandler {

    public static final String SCHEME_SHOW_TVDB = "showtvdb";
    public static final String SCHEME_MOVIE_TMDB = "movietmdb";
    public static final String QUERY_LANGUAGE = "language";

    private final Downloader downloader;
    private final Context context;

    public SgPicassoRequestHandler(Downloader downloader, Context context) {
        this.downloader = downloader;
        this.context = context.getApplicationContext();
    }

    @Override
    public boolean canHandleRequest(Request data) {
        String scheme = data.uri.getScheme();
        return SCHEME_SHOW_TVDB.equals(scheme) || SCHEME_MOVIE_TMDB.equals(scheme);
    }

    @Override
    public Result load(Request request, int networkPolicy) throws IOException {
        String scheme = request.uri.getScheme();

        if (SCHEME_SHOW_TVDB.equals(scheme)) {
            int showTvdbId = Integer.valueOf(request.uri.getHost());

            String language = request.uri.getQueryParameter(QUERY_LANGUAGE);
            if (TextUtils.isEmpty(language)) {
                language = DisplaySettings.getShowsLanguage(context);
            }

            TvdbTools tvdbTools = SgApp.getServicesComponent(context).tvdbTools();
            try {
                retrofit2.Response<SeriesImageQueryResultResponse> posterResponse
                        = tvdbTools.getSeriesPosters(showTvdbId, language);
                if (posterResponse.code() == 404) {
                    // no posters for this language, fall back to default
                    posterResponse = tvdbTools.getSeriesPosters(showTvdbId, null);
                }
                if (posterResponse.isSuccessful()) {
                    //noinspection ConstantConditions
                    String imagePath = TvdbTools.getHighestRatedPoster(posterResponse.body().data);
                    String imageUrl = TvdbImageTools.smallSizeUrl(imagePath);
                    if (imageUrl != null) {
                        return loadFromNetwork(Uri.parse(imageUrl), networkPolicy);
                    }
                }
            } catch (TvdbException ignored) {
            }
        }

        if (SCHEME_MOVIE_TMDB.equals(scheme)) {
            int movieTmdbId = Integer.valueOf(request.uri.getHost());

            MovieTools movieTools = SgApp.getServicesComponent(context).movieTools();
            Movie movieSummary = movieTools.getMovieSummary(movieTmdbId);
            if (movieSummary != null && movieSummary.poster_path != null) {
                final String imageUrl = TmdbSettings.getImageBaseUrl(context)
                        + TmdbSettings.POSTER_SIZE_SPEC_W342 + movieSummary.poster_path;
                return loadFromNetwork(Uri.parse(imageUrl), networkPolicy);
            }
        }

        return null;
    }

    private Result loadFromNetwork(Uri uri, int networkPolicy) throws IOException {
        Downloader.Response response = downloader.load(uri, networkPolicy);
        if (response == null) {
            return null;
        }
        // OkHttp3Downloader we use always returns a stream
        InputStream is = response.getInputStream();
        if (is == null) {
            return null;
        }
        return new Result(is, Picasso.LoadedFrom.NETWORK);
    }
}
