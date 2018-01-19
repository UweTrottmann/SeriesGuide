package com.battlelancer.seriesguide.util;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.traktapi.TraktTools;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

/**
 * Helper methods to interact with third-party services trakt and The Movie Database used within
 * SeriesGuide.
 */
public final class ServiceUtils {

    public static final String TAG = "Service Utils";

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

    /* This class is never initialized */
    private ServiceUtils() {
    }

    /**
     * Build Picasso {@link com.squareup.picasso.RequestCreator} which respects user requirement of
     * only loading images over WiFi.
     *
     * <p>If {@link Utils#isAllowedLargeDataConnection} is false, will set {@link
     * com.squareup.picasso.NetworkPolicy#OFFLINE} (which will set {@link
     * okhttp3.CacheControl#FORCE_CACHE} on requests) to skip the network and accept stale images.
     *
     * @param context {@link Context#getApplicationContext() context.getApplicationContext()} will
     * be used.
     */
    @NonNull
    public static RequestCreator loadWithPicasso(Context context, String path) {
        RequestCreator requestCreator = Picasso.with(context).load(path);
        if (!Utils.isAllowedLargeDataConnection(context.getApplicationContext())) {
            // avoid the network, hit the cache immediately + accept stale images.
            requestCreator.networkPolicy(NetworkPolicy.OFFLINE);
        }
        return requestCreator;
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

        // try launching the IMDb app
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri
                .parse(IMDB_APP_TITLE_URI + imdbId + IMDB_APP_TITLE_URI_POSTFIX));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        } else {
            //noinspection deprecation
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        }
        if (Utils.tryStartActivity(context, intent, false)) {
            Utils.trackAction(context, logTag, "IMDb");
        } else {
            // on failure, try launching the web page
            Utils.launchWebsite(context, IMDB_TITLE_URL + imdbId, logTag, "IMDb");
        }
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
        Utils.openNewDocument(context, intent, logTag, "Google Play");
    }

    public static void setUpTraktShowButton(@Nullable View button, final int showTvdbId,
            @NonNull final String logTag) {
        if (button != null) {
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // build url on demand
                    String uri = TraktTools.buildShowUrl(showTvdbId);
                    Utils.launchWebsite(v.getContext(), uri, logTag, "trakt");
                }
            });
        }
    }

    public static void setUpTraktEpisodeButton(@Nullable View button, final int episodeTvdbId,
            @NonNull final String logTag) {
        if (button != null) {
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // build url on demand
                    String uri = TraktTools.buildEpisodeUrl(episodeTvdbId);
                    Utils.launchWebsite(v.getContext(), uri, logTag, "trakt");
                }
            });
        }
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

                    Utils.launchWebsite(v.getContext(), uri, logTag, "TVDb");
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
        Utils.launchWebsite(context, YOUTUBE_BASE_URL + videoId, logTag, "YouTube");
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
        Utils.openNewDocument(context, buildWebSearchIntent(query), logTag, "Web search");
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
}
