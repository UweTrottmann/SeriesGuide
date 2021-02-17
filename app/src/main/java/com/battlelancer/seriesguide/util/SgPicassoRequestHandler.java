package com.battlelancer.seriesguide.util;

import static com.squareup.picasso.Picasso.LoadedFrom.DISK;
import static com.squareup.picasso.Picasso.LoadedFrom.NETWORK;

import android.content.Context;
import android.net.Uri;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.TmdbSettings;
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2;
import com.battlelancer.seriesguide.ui.movies.MovieTools;
import com.squareup.picasso.Downloader;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;
import com.uwetrottmann.tmdb2.entities.Movie;
import com.uwetrottmann.tmdb2.entities.TvShow;
import java.io.IOException;
import okhttp3.CacheControl;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * This is mostly a copy of {@code com.squareup.picasso.NetworkRequestHandler} that is not visible.
 * Extended to fetch the image url from a given show TVDB id or movie TMDB id.
 */
public class SgPicassoRequestHandler extends RequestHandler {

    public static final String SCHEME_SHOW_TMDB = "showtmdb";
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
        return SCHEME_SHOW_TMDB.equals(scheme) || SCHEME_MOVIE_TMDB.equals(scheme);
    }

    @Override
    public Result load(Request request, int networkPolicy) throws IOException {
        String scheme = request.uri.getScheme();
        String host = request.uri.getHost();
        if (host == null) return null;

        if (SCHEME_SHOW_TMDB.equals(scheme)) {
            int showTmdbId = Integer.parseInt(host);
            String language = request.uri.getQueryParameter(QUERY_LANGUAGE);
            if (language == null || language.length() == 0) {
                language = DisplaySettings.LANGUAGE_EN;
            }
            TvShow showDetails = new TmdbTools2().getShowDetails(showTmdbId, language, context);
            if (showDetails != null) {
                String url = ImageTools
                        .tmdbOrTvdbPosterUrl(showDetails.poster_path, context, false);
                if (url != null) {
                    return loadFromNetwork(Uri.parse(url), networkPolicy);
                }
            }
        }

        if (SCHEME_MOVIE_TMDB.equals(scheme)) {
            int movieTmdbId = Integer.valueOf(host);

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
        // because retry-count is fixed to 0 for custom request handlers
        // BitmapHunter forces the network policy to OFFLINE
        // https://github.com/square/picasso/issues/2038
        // until fixed, re-set the network policy here also (like ServiceUtils.loadWithPicasso)
        if (Utils.isAllowedLargeDataConnection(context)) {
            networkPolicy = 0; // no policy
        } else {
            // avoid the network, hit the cache immediately + accept stale images.
            networkPolicy = 1 << 2; // NetworkPolicy.OFFLINE
        }

        okhttp3.Request downloaderRequest = createRequest(uri, networkPolicy);
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

    private static okhttp3.Request createRequest(Uri uri, int networkPolicy) {
        CacheControl cacheControl = null;
        if (networkPolicy != 0) {
            if (NetworkPolicy.isOfflineOnly(networkPolicy)) {
                cacheControl = CacheControl.FORCE_CACHE;
            } else {
                CacheControl.Builder builder = new CacheControl.Builder();
                if (!NetworkPolicy.shouldReadFromDiskCache(networkPolicy)) {
                    builder.noCache();
                }
                if (!NetworkPolicy.shouldWriteToDiskCache(networkPolicy)) {
                    builder.noStore();
                }
                cacheControl = builder.build();
            }
        }

        okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(uri.toString());
        if (cacheControl != null) {
            builder.cacheControl(cacheControl);
        }
        return builder.build();
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
