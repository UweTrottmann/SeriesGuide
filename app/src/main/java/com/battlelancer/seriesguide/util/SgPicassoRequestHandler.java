package com.battlelancer.seriesguide.util;

import static com.squareup.picasso.Picasso.LoadedFrom.DISK;
import static com.squareup.picasso.Picasso.LoadedFrom.NETWORK;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import com.battlelancer.seriesguide.SgApp;
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
import okhttp3.Response;
import okhttp3.ResponseBody;

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
                language = null;
            }

            TvdbTools tvdbTools = SgApp.getServicesComponent(context).tvdbTools();
            try {
                retrofit2.Response<SeriesImageQueryResultResponse> posterResponse
                        = tvdbTools.getSeriesPosters(showTvdbId, language);
                if (language != null && posterResponse.code() == 404) {
                    // no posters for this language, fall back to default
                    posterResponse = tvdbTools.getSeriesPosters(showTvdbId, null);
                }
                if (posterResponse.isSuccessful()) {
                    //noinspection ConstantConditions
                    String imagePath = TvdbTools.getHighestRatedPoster(posterResponse.body().data);
                    String imageUrl = TvdbImageTools.smallSizeUrl(imagePath);
                    if (imageUrl != null) {
                        return loadFromNetwork(Uri.parse(imageUrl));
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
                return loadFromNetwork(Uri.parse(imageUrl));
            }
        }

        return null;
    }

    private Result loadFromNetwork(Uri uri) throws IOException {
        // because retry-count is fixed to 0 for custom request handlers
        // BitmapHunter forces the network policy to OFFLINE
        // but we want to load from the network, so just ignore it and build a regular request
        okhttp3.Request downloaderRequest = createRequest(uri);
        Response response = downloader.load(downloaderRequest);
        ResponseBody body = response.body();

        if (body == null || !response.isSuccessful()) {
            if (body != null) {
                body.close();
            }
            throw new ResponseException(response.code());
        }

        // Cache response is only null when the response comes fully from the network. Both completely
        // cached and conditionally cached responses will have a non-null cache response.
        Picasso.LoadedFrom loadedFrom = response.cacheResponse() == null ? NETWORK : DISK;

        // Sometimes response content length is zero when requests are being replayed. Haven't found
        // root cause to this but retrying the request seems safe to do so.
        if (loadedFrom == DISK && body.contentLength() == 0) {
            body.close();
            throw new ContentLengthException("Received response with 0 content-length header.");
        }
        return new Result(body.source(), loadedFrom);
    }

    private static okhttp3.Request createRequest(Uri uri) {
        return new okhttp3.Request.Builder().url(uri.toString()).build();
    }

    static class ContentLengthException extends IOException {
        ContentLengthException(String message) {
            super(message);
        }
    }

    static final class ResponseException extends IOException {
        final int code;

        ResponseException(int code) {
            super("HTTP " + code);
            this.code = code;
        }
    }
}
