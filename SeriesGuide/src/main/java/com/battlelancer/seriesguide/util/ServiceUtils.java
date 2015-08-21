/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.util;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.settings.TraktOAuthSettings;
import com.battlelancer.seriesguide.tmdbapi.SgTmdb;
import com.battlelancer.seriesguide.traktapi.SgTraktV2;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.uwetrottmann.tmdb.Tmdb;
import com.uwetrottmann.trakt.v2.TraktV2;
import java.io.File;
import java.util.concurrent.TimeUnit;
import timber.log.Timber;

/**
 * Helper methods to interact with third-party services trakt and The Movie Database used within
 * SeriesGuide.
 */
public final class ServiceUtils {

    public static final String TAG = "Service Utils";

    static final int CONNECT_TIMEOUT_MILLIS = 15 * 1000; // 15s
    static final int READ_TIMEOUT_MILLIS = 20 * 1000; // 20s
    private static final String API_CACHE = "api-cache";
    private static final int MIN_DISK_API_CACHE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final int MAX_DISK_API_CACHE_SIZE = 10 * 1024 * 1024; // 10MB

    private static final String IMDB_APP_TITLE_URI_POSTFIX = "/";

    private static final String IMDB_APP_TITLE_URI = "imdb:///title/";

    public static final String IMDB_TITLE_URL = "http://imdb.com/title/";

    private static final String TVDB_SHOW_URL = "http://thetvdb.com/?tab=series&id=";

    private static final String TVDB_EPISODE_URL = "http://thetvdb.com/?tab=episode&seriesid=";

    private static final String TVDB_EPISODE_URL_SEASON_PARAM = "&seasonid=";

    private static final String TVDB_EPISODE_URL_EPISODE_PARAM = "&id=";

    private static final String YOUTUBE_BASE_URL = "http://www.youtube.com/watch?v=";

    private static final String YOUTUBE_SEARCH = "http://www.youtube.com/results?search_query=%s";

    private static final String YOUTUBE_PACKAGE = "com.google.android.youtube";

    private static OkHttpClient cachingHttpClient;
    private static OkUrlFactory cachingUrlFactory;

    private static Picasso sPicasso;

    private static TraktV2 traktV2;

    private static TraktV2 traktV2WithAuth;

    private static Tmdb tmdb;

    /* This class is never initialized */
    private ServiceUtils() {
    }

    /**
     * Returns this apps {@link com.squareup.okhttp.OkHttpClient} with enabled response cache.
     * Should be used with API calls.
     */
    @NonNull
    public static synchronized OkHttpClient getCachingOkHttpClient(Context context) {
        if (cachingHttpClient == null) {
            cachingHttpClient = new OkHttpClient();
            cachingHttpClient.setConnectTimeout(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            cachingHttpClient.setReadTimeout(READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            File cacheDir = createApiCacheDir(context);
            cachingHttpClient.setCache(new Cache(cacheDir, calculateApiDiskCacheSize(cacheDir)));
        }
        return cachingHttpClient;
    }

    static File createApiCacheDir(Context context) {
        File cache = new File(context.getApplicationContext().getCacheDir(), API_CACHE);
        if (!cache.exists()) {
            cache.mkdirs();
        }
        return cache;
    }

    static long calculateApiDiskCacheSize(File dir) {
        long size = MIN_DISK_API_CACHE_SIZE;

        try {
            StatFs statFs = new StatFs(dir.getAbsolutePath());
            long available;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                available = statFs.getBlockCountLong() * statFs.getBlockSizeLong();
            } else {
                available = ((long) statFs.getBlockCount()) * statFs.getBlockSize();
            }
            // Target 2% of the total space.
            size = available / 50;
        } catch (IllegalArgumentException ignored) {
        }

        // Bound inside min/max size for disk cache.
        return Math.max(Math.min(size, MAX_DISK_API_CACHE_SIZE), MIN_DISK_API_CACHE_SIZE);
    }

    @NonNull
    public static synchronized OkUrlFactory getCachingUrlFactory(Context context) {
        if (cachingUrlFactory == null) {
            cachingUrlFactory = new OkUrlFactory(getCachingOkHttpClient(context));
        }
        return cachingUrlFactory;
    }

    @NonNull
    public static synchronized Picasso getPicasso(Context context) {
        if (sPicasso == null) {
            sPicasso = new Picasso.Builder(context).build();
        }
        return sPicasso;
    }

    /**
     * Build Picasso {@link com.squareup.picasso.RequestCreator} which respects user requirement of
     * only loading images over WiFi.
     *
     * <p>If {@link Utils#isAllowedLargeDataConnection} is false, will set {@link
     * com.squareup.picasso.NetworkPolicy#OFFLINE} (which will set {@link
     * com.squareup.okhttp.CacheControl#FORCE_CACHE} on requests) to skip the network and accept
     * stale images.
     */
    @NonNull
    public static RequestCreator loadWithPicasso(Context context, String path) {
        RequestCreator requestCreator = ServiceUtils.getPicasso(context).load(path);
        if (!Utils.isAllowedLargeDataConnection(context)) {
            // avoid the network, hit the cache immediately + accept stale images.
            requestCreator.networkPolicy(NetworkPolicy.OFFLINE);
        }
        return requestCreator;
    }

    /**
     * Get a tmdb-java instance with our API key set.
     */
    @NonNull
    public static synchronized Tmdb getTmdb(Context context) {
        if (tmdb == null) {
            tmdb = new SgTmdb(context).setApiKey(BuildConfig.TMDB_API_KEY);
        }
        return tmdb;
    }

    /**
     * Get a {@link com.uwetrottmann.trakt.v2.TraktV2} service manager with just the API key set. NO
     * user auth data.
     *
     * @return A {@link com.uwetrottmann.trakt.v2.TraktV2} instance.
     */
    @NonNull
    public static synchronized TraktV2 getTraktV2(Context context) {
        if (traktV2 == null) {
            traktV2 = new SgTraktV2(context).setApiKey(BuildConfig.TRAKT_CLIENT_ID);
        }
        return traktV2;
    }

    /**
     * Get a {@link com.uwetrottmann.trakt.v2.TraktV2} service manager with OAuth access token and
     * API key set.
     *
     * <p>If the current access token will or is expired, tries to refresh it.
     *
     * @return A {@link com.uwetrottmann.trakt.v2.TraktV2} instance or null if there are no valid
     * credentials.
     */
    @Nullable
    public static synchronized TraktV2 getTraktV2WithAuth(Context context) {
        if (!TraktCredentials.get(context).hasCredentials()) {
            Timber.e("getTraktV2WithAuth: no auth");
            return null;
        }

        // try to refresh access token if it is about to expire or has expired
        if (TraktOAuthSettings.isTimeToRefreshAccessToken(context)) {
            if (!TraktCredentials.get(context).refreshAccessToken()) {
                return null;
            }

            // set new access token
            if (traktV2WithAuth != null) {
                traktV2WithAuth.setAccessToken(TraktCredentials.get(context).getAccessToken());
            }
        }

        if (traktV2WithAuth == null) {
            TraktV2 trakt = new SgTraktV2(context).setApiKey(BuildConfig.TRAKT_CLIENT_ID);
            trakt.setAccessToken(TraktCredentials.get(context).getAccessToken());
            traktV2WithAuth = trakt;
        }

        return traktV2WithAuth;
    }

    /**
     * Return the existing instance of a {@link com.uwetrottmann.trakt.v2.TraktV2} service manager
     * with auth or {@code null}.
     *
     * <p>In most cases, use {@link #getTraktV2WithAuth(android.content.Context)} instead.
     */
    @Nullable
    public static synchronized TraktV2 getTraktV2WithAuth() {
        return traktV2WithAuth;
    }

    /**
     * Displays the IMDb page for the given id (show or episode) in the IMDb app or on the imdb.com
     * web page. If the IMDb id is empty, disables the button.
     */
    public static void setUpImdbButton(final String imdbId, View imdbButton, final String logTag) {
        if (imdbButton != null) {
            if (!TextUtils.isEmpty(imdbId)) {
                imdbButton.setEnabled(true);
                imdbButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openImdb(imdbId, logTag, v.getContext());
                    }
                });
            } else {
                imdbButton.setEnabled(false);
            }
        }
    }

    /**
     * Open the IMDb app or web page for the given IMDb id.
     */
    public static void openImdb(String imdbId, String logTag, Context context) {
        if (context == null || TextUtils.isEmpty(imdbId)) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri
                .parse(IMDB_APP_TITLE_URI + imdbId + IMDB_APP_TITLE_URI_POSTFIX));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        // try launching IMDb app
        if (!Utils.tryStartActivity(context, intent, false)) {
            // on failure, try launching the web page
            intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(IMDB_TITLE_URL + imdbId));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            Utils.tryStartActivity(context, intent, true);
        }

        Utils.trackAction(context, logTag, "IMDb");
    }

    /**
     * Returns a view {@link android.content.Intent} for a search of Google Play's movies category
     * (includes TV shows).
     */
    public static Intent buildGooglePlayIntent(String title, Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String playStoreQuery = String.format(context.getString(R.string.url_movies_search),
                Uri.encode(title));
        intent.setData(Uri.parse(playStoreQuery));
        return intent;
    }

    /**
     * Tries to open Google Play to search for the given tv show, episode or movie title.
     */
    public static void searchGooglePlay(final String title, final String logTag, Context context) {
        Intent intent = buildGooglePlayIntent(title, context);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        Utils.tryStartActivity(context, intent, true);

        Utils.trackAction(context, logTag, "Google Play");
    }

    /**
     * Starts activity with {@link Intent#ACTION_VIEW} to display the given show or episode trakt.tv
     * page.
     */
    public static void setUpTraktButton(final int showTvdbId, final View traktButton,
            final String logTag) {
        if (traktButton != null) {
            traktButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    String uri = TraktTools.buildEpisodeOrShowUrl(showTvdbId);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(uri));
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    Utils.tryStartActivity(v.getContext(), intent, true);

                    Utils.trackAction(traktButton.getContext(), logTag, "trakt");
                }
            });
        }
    }

    /**
     * Starts activity with {@link Intent#ACTION_VIEW} to display the given movies trakt.tv page.
     */
    public static void openTraktMovie(Context context, int tmdbId, String logTag) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(TraktTools.buildMovieUrl(tmdbId)));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        Utils.tryStartActivity(context, intent, true);

        Utils.trackAction(context, logTag, "trakt");
    }

    /**
     * Starts activity with {@link Intent#ACTION_VIEW} to display the given show or episodes
     * TVDb.com page.<br> If any of the season or episode numbers is below 0, displays the show
     * page.
     */
    public static void setUpTvdbButton(final int showTvdbId, final int seasonTvdbId,
            final int episodeTvdbId, final View tvdbButton, final String logTag) {
        if (tvdbButton != null) {
            tvdbButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    String uri;
                    if (seasonTvdbId < 0 || episodeTvdbId < 0) {
                        // look just for the show page
                        uri = TVDB_SHOW_URL + showTvdbId;
                    } else {
                        // look for the episode page
                        uri = TVDB_EPISODE_URL + showTvdbId
                                + TVDB_EPISODE_URL_SEASON_PARAM + seasonTvdbId
                                + TVDB_EPISODE_URL_EPISODE_PARAM + episodeTvdbId;
                    }

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(uri));
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    Utils.tryStartActivity(v.getContext(), intent, true);

                    Utils.trackAction(tvdbButton.getContext(), logTag, "TVDb");
                }
            });
        }
    }

    /**
     * Starts activity with {@link Intent#ACTION_VIEW} to display the given shows TVDb.com page.
     */
    public static void setUpTvdbButton(final int showTvdbId, View tvdbButton, final String logTag) {
        setUpTvdbButton(showTvdbId, -1, -1, tvdbButton, logTag);
    }

    /**
     * Opens the YouTube app or web page for the given video.
     */
    public static void openYoutube(String videoId, String logTag, Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(YOUTUBE_BASE_URL + videoId));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        Utils.tryStartActivity(context, intent, true);

        Utils.trackAction(context, logTag, "YouTube");
    }

    /**
     * Used to open the YouTube app and search for <code>query</code>
     *
     * @param query The search query for the YouTube app
     * @param button The {@link Button} used to invoke the {@link android.view.View.OnClickListener}
     * @param logTag The log tag to use, for Analytics
     */
    public static void setUpYouTubeButton(final String query, View button, final String logTag) {
        if (button == null) {
            // Return if the button isn't initialized
            return;
        } else if (TextUtils.isEmpty(query)) {
            // Disable the button if there's nothing to search for
            button.setEnabled(false);
            return;
        }

        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                searchYoutube(v.getContext(), query, logTag);
            }
        });
    }

    /**
     * Builds a search {@link android.content.Intent} to open the YouTube application to search for
     * <code>query</code>. If the YouTube app is unavailable, a view {@link android.content.Intent}
     * with the web search URL is returned instead.
     */
    public static Intent buildYouTubeIntent(Context context, String query) {
        PackageManager pm = context.getPackageManager();
        boolean hasYouTube;
        try {
            pm.getPackageInfo(YOUTUBE_PACKAGE, PackageManager.GET_ACTIVITIES);
            hasYouTube = true;
        } catch (PackageManager.NameNotFoundException notInstalled) {
            hasYouTube = false;
        }

        Intent intent;
        if (hasYouTube) {
            // Directly search the YouTube app
            intent = new Intent(Intent.ACTION_SEARCH);
            intent.setPackage(YOUTUBE_PACKAGE);
            intent.putExtra("query", query);
        } else {
            // Launch a web search
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(String.format(YOUTUBE_SEARCH, Uri.encode(query))));
        }
        return intent;
    }

    /**
     * Attempts to open the YouTube application to search for <code>query</code>. If the app is
     * unavailable, a web search if performed instead.
     *
     * @param context The {@link Context} to use
     * @param query The search query
     * @param logTag The log tag to use, for Analytics
     */
    public static void searchYoutube(Context context, String query, String logTag) {
        Intent intent = buildYouTubeIntent(context, query);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        Utils.tryStartActivity(context, intent, true);
        Utils.trackAction(context, logTag, "YouTube search");
    }

    /**
     * Used to search the web for <code>query</code>
     *
     * @param query The search query for the YouTube app
     * @param button The {@link Button} used to invoke the {@link android.view.View.OnClickListener}
     * @param logTag The log tag to use, for Analytics
     */
    public static void setUpWebSearchButton(final String query, View button, final String logTag) {
        if (button == null) {
            // Return if the button isn't initialized
            return;
        } else if (TextUtils.isEmpty(query)) {
            // Disable the button if there's nothing to search for
            button.setEnabled(false);
            return;
        }

        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                performWebSearch(v.getContext(), query, logTag);
            }
        });
    }

    /**
     * Builds a search {@link android.content.Intent} using {@link Intent#ACTION_WEB_SEARCH} and
     * <code>query</code> as {@link android.app.SearchManager#QUERY} extra.
     */
    public static Intent buildWebSearchIntent(String query) {
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra(SearchManager.QUERY, query);
        return intent;
    }

    /**
     * Attempts to search the web for <code>query</code>.
     *
     * @param context The {@link Context} to use
     * @param query The search query
     * @param logTag The log tag to use, for Analytics
     */
    public static void performWebSearch(Context context, String query, String logTag) {
        Intent intent = buildWebSearchIntent(query);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        Utils.tryStartActivity(context, intent, true);

        Utils.trackAction(context, logTag, "Web search");
    }
}
