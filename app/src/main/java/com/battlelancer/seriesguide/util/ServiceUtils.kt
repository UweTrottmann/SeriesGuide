package com.battlelancer.seriesguide.util;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.R;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

/**
 * Helper methods to interact with third-party services trakt and The Movie Database used within
 * SeriesGuide.
 */
public final class ServiceUtils {

    private static final String IMDB_APP_TITLE_URI_POSTFIX = "/";

    private static final String IMDB_APP_TITLE_URI = "imdb:///title/";

    public static final String IMDB_TITLE_URL = "http://imdb.com/title/";

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
        RequestCreator requestCreator = Picasso.get().load(path);
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
    public static void setUpImdbButton(final String imdbId, View imdbButton) {
        if (imdbButton == null) {
            return;
        }
        if (TextUtils.isEmpty(imdbId)) {
            imdbButton.setEnabled(false);
        } else {
            imdbButton.setEnabled(true);
            imdbButton.setOnClickListener(v -> openImdb(imdbId, v.getContext()));
            ClipboardTools.copyTextToClipboardOnLongClick(imdbButton, imdbLink(imdbId));
        }
    }

    /**
     * Open the IMDb app or web page for the given IMDb id.
     */
    public static void openImdb(@Nullable String imdbId, @Nullable Context context) {
        if (context == null || TextUtils.isEmpty(imdbId)) {
            return;
        }

        // try launching the IMDb app
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri
                .parse(IMDB_APP_TITLE_URI + imdbId + IMDB_APP_TITLE_URI_POSTFIX))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        if (!Utils.tryStartActivity(context, intent, false)) {
            // on failure, try launching the web page
            Utils.launchWebsite(context, imdbLink(imdbId));
        }
    }

    public static String imdbLink(@NonNull String imdbId) {
        return IMDB_TITLE_URL + imdbId;
    }

    /**
     * Returns a view {@link android.content.Intent} for a search of Google Play's movies category
     * (includes TV shows).
     */
    @SuppressLint("StringFormatInvalid")
    public static Intent buildGooglePlayIntent(String title, Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String playStoreQuery = String.format(context.getString(R.string.url_movies_search),
                Uri.encode(title));
        intent.setData(Uri.parse(playStoreQuery));
        return intent;
    }

    /**
     * Opens the YouTube app or web page for the given video.
     */
    public static void openYoutube(String videoId, Context context) {
        Utils.launchWebsite(context, YOUTUBE_BASE_URL + videoId);
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
     */
    public static void performWebSearch(Context context, String query) {
        Utils.openNewDocument(context, buildWebSearchIntent(query));
    }

    /**
     * Used to search the web for <code>query</code>
     *
     * @param query The search query for the YouTube app
     * @param button The {@link Button} used to invoke the {@link android.view.View.OnClickListener}
     */
    public static void setUpWebSearchButton(final String query, View button) {
        if (button == null) {
            // Return if the button isn't initialized
            return;
        } else if (TextUtils.isEmpty(query)) {
            // Disable the button if there's nothing to search for
            button.setEnabled(false);
            return;
        }

        button.setOnClickListener(v -> performWebSearch(v.getContext(), query));
    }
}
