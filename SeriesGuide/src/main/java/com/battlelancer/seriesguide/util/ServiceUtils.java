/*
 * Copyright 2013 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.util;

import com.google.analytics.tracking.android.EasyTracker;

import com.battlelancer.seriesguide.enums.TraktStatus;
import com.battlelancer.seriesguide.ui.ConnectTraktActivity;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.Response;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import retrofit.RetrofitError;

/**
 * Helper methods to interact with third-party services trakt and The Movie Database used within
 * SeriesGuide.
 */
public final class ServiceUtils {

    public static final String TAG = "Service Utils";

    private static final String GOOGLE_PLAY = "https://play.google.com/store/search?q=%s&c=movies";

    private static final String TRAKT_SEARCH_BASE_URL = "http://trakt.tv/search/";

    private static final String IMDB_APP_TITLE_URI_POSTFIX = "/";

    private static final String IMDB_APP_TITLE_URI = "imdb:///title/";

    public static final String IMDB_TITLE_URL = "http://imdb.com/title/";

    private static final String TRAKT_SEARCH_MOVIE_URL = TRAKT_SEARCH_BASE_URL + "tmdb?q=";

    private static final String TRAKT_SEARCH_SHOW_URL = TRAKT_SEARCH_BASE_URL + "tvdb?q=";

    private static final String TRAKT_SEARCH_SEASON_ARG = "&s=";

    private static final String TRAKT_SEARCH_EPISODE_ARG = "&e=";

    private static final String TVDB_SHOW_URL = "http://thetvdb.com/?tab=series&id=";

    private static final String TVDB_EPISODE_URL = "http://thetvdb.com/?tab=episode&seriesid=";

    private static final String TVDB_EPISODE_URL_SEASON_PARAM = "&seasonid=";

    private static final String TVDB_EPISODE_URL_EPISODE_PARAM = "&id=";

    private static final String YOUTUBE_BASE_URL = "http://www.youtube.com/watch?v=";

    private static final String YOUTUBE_SEARCH = "http://www.youtube.com/results?search_query=%s";

    private static final String YOUTUBE_PACKAGE = "com.google.android.youtube";

    private static Trakt sTraktServiceManagerInstance;

    private static Trakt sTraktServiceManagerWithAuthInstance;

    private static com.uwetrottmann.tmdb.ServiceManager sTmdbServiceManagerInstance;

    /* This class is never initialized */
    private ServiceUtils() {
    }

    /**
     * Get a tmdb-java ServiceManager with our API key set.
     */
    public static synchronized com.uwetrottmann.tmdb.ServiceManager getTmdbServiceManager(
            Context context) {
        if (sTmdbServiceManagerInstance == null) {
            sTmdbServiceManagerInstance = new com.uwetrottmann.tmdb.ServiceManager();
            sTmdbServiceManagerInstance.setReadTimeout(10000);
            sTmdbServiceManagerInstance.setConnectionTimeout(15000);
            sTmdbServiceManagerInstance.setApiKey(context.getResources().getString(
                    R.string.tmdb_apikey));
        }

        return sTmdbServiceManagerInstance;
    }

    /**
     * Get a trakt-java ServiceManager with just our API key set. NO user auth data.
     */
    public static synchronized Trakt getTraktServiceManager(Context context) {
        if (ServiceUtils.sTraktServiceManagerInstance == null) {
            ServiceUtils.sTraktServiceManagerInstance = new Trakt();
            ServiceUtils.sTraktServiceManagerInstance.setApiKey(context.getResources().getString(
                    R.string.trakt_apikey));
            // this made some problems, so sadly disabled for now
            // manager.setUseSsl(true);
        }

        return ServiceUtils.sTraktServiceManagerInstance;
    }

    /**
     * Get the trakt-java ServiceManger with user credentials and our API key set.
     *
     * @param refreshCredentials Set this flag to refresh the user credentials.
     * @throws Exception When decrypting the password failed.
     */
    public static synchronized Trakt getTraktServiceManagerWithAuth(Context context,
            boolean refreshCredentials) {
        if (ServiceUtils.sTraktServiceManagerWithAuthInstance == null) {
            ServiceUtils.sTraktServiceManagerWithAuthInstance = new Trakt();
            ServiceUtils.sTraktServiceManagerWithAuthInstance.setApiKey(context.getResources()
                    .getString(
                            R.string.trakt_apikey));
            refreshCredentials = true;
        }

        if (refreshCredentials) {
            final String username = getTraktUsername(context);
            final String password = getTraktPasswordHash(context);

            if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
                ServiceUtils.sTraktServiceManagerWithAuthInstance.setAuthentication(username,
                        password);
            } else {
                clearTraktCredentials(context);
                return null;
            }
        }

        return ServiceUtils.sTraktServiceManagerWithAuthInstance;
    }

    public static String getTraktUsername(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
                SeriesGuidePreferences.KEY_TRAKTUSER, "");
    }

    /**
     * Returns the SHA hash of the users trakt password.<br> <b>Never</b> store this yourself,
     * always call this method.
     */
    private static String getTraktPasswordHash(Context context) {
        String hash = PreferenceManager.getDefaultSharedPreferences(context).getString(
                SeriesGuidePreferences.KEY_TRAKTPWD, null);

        // try decrypting the hash
        if (!TextUtils.isEmpty(hash)) {
            hash = SimpleCrypto.decrypt(hash, context);
        }

        return hash;
    }

    /**
     * Checks if there are a non-empty trakt username and password. Returns false if either one is
     * empty.
     */
    public static boolean hasTraktCredentials(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                .getApplicationContext());
        String username = prefs.getString(SeriesGuidePreferences.KEY_TRAKTUSER, "");
        String password = prefs.getString(SeriesGuidePreferences.KEY_TRAKTPWD, "");

        return (!username.equalsIgnoreCase("") && !password.equalsIgnoreCase(""));
    }

    /**
     * Checks for existing trakt credentials. If there aren't any valid ones (determined by {@link
     * #hasTraktCredentials(Context)}), launches the trakt connect flow.
     *
     * @return <b>true</b> if credentials are valid, <b>false</b> if invalid and launching trakt
     * connect flow.
     */
    public static boolean ensureTraktCredentials(Context context) {
        if (!hasTraktCredentials(context)) {
            // launch trakt connect process
            context.startActivity(new Intent(context, ConnectTraktActivity.class));
            return false;
        }
        return true;
    }

    /**
     * Creates a network request to check if the current trakt credentials are still valid. Will
     * assume valid credentials if there was no response from trakt (due to a network error,
     * etc.).<br> <b>Never</b> run this on the main thread.
     */
    public static void checkTraktCredentials(Context context) {
        Log.d(TAG, "Checking trakt credentials...");

        // no username or password? stop right here
        if (!hasTraktCredentials(context)) {
            return;
        }

        // check for connectivity
        if (!AndroidUtils.isNetworkConnected(context)) {
            return;
        }

        Trakt manager = getTraktServiceManagerWithAuth(context, false);
        if (manager == null) {
            return;
        }
        try {
            Response r = manager.accountService().test();
            if (r != null && TraktStatus.FAILURE.equals(r.status)) {
                // credentials invalid according to trakt, remove them
                clearTraktCredentials(context);
            }
        } catch (RetrofitError e) {
        }
        /*
         * Ignore exceptions, trakt may be offline, etc. We expect the user to
         * disconnect and reconnect himself.
         */
    }

    /**
     * Removes trakt username and password from settings as well as from the authenticated {@link
     * com.jakewharton.trakt.Trakt} instance.
     */
    public static void clearTraktCredentials(Context context) {
        Log.d(TAG, "Clearing trakt credentials...");

        // remove from settings
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context
                .getApplicationContext()).edit();
        editor.putString(SeriesGuidePreferences.KEY_TRAKTUSER, "").putString(
                SeriesGuidePreferences.KEY_TRAKTPWD, "");
        editor.commit();

        // remove from memory
        if (sTraktServiceManagerWithAuthInstance != null) {
            sTraktServiceManagerWithAuthInstance.setAuthentication(null, null);
        }
    }

    /**
     * Displays the IMDb page for the given id (show or episode) in the IMDb app or on the imdb.com
     * web page. If the IMDb id is empty, disables the button.
     */
    public static void setUpImdbButton(final String imdbId, View imdbButton, final String logTag,
            final Context context) {
        if (imdbButton != null) {
            if (!TextUtils.isEmpty(imdbId)) {
                imdbButton.setEnabled(true);
                imdbButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openImdb(imdbId, logTag, context);
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

        EasyTracker.getTracker().sendEvent(logTag, "Action Item", "IMDb", (long) 0);
    }

    /**
     * Sets a {@link OnClickListener} on the given button linking to a Google Play Store search for
     * the given title or disabling the button if the title is empty.
     */
    public static void setUpGooglePlayButton(final String title, View playButton,
            final String logTag) {
        if (playButton != null) {

            if (!TextUtils.isEmpty(title)) {
                playButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        searchGooglePlay(title, logTag, v.getContext());
                    }
                });
            } else {
                playButton.setEnabled(false);
            }

        }
    }

    /**
     * Tries to open Google Play to search for the given tv show, episode or movie title.
     */
    public static void searchGooglePlay(final String title, final String logTag, Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String playStoreQuery = String.format(GOOGLE_PLAY, Uri.encode(title));
        intent.setData(Uri.parse(playStoreQuery));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        Utils.tryStartActivity(context, intent, true);

        EasyTracker.getTracker().sendEvent(logTag, "Action Item", "Google Play", (long) 0);
    }

    /**
     * Sets a {@link OnClickListener} on the given button linking to a Amazon web search for the
     * given title or disabling the button if the title is empty.
     */
    public static void setUpAmazonButton(final String title, View amazonButton,
            final String logTag) {
        if (amazonButton != null) {

            if (!TextUtils.isEmpty(title)) {
                amazonButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EasyTracker.getTracker()
                                .sendEvent(logTag, "Action Item", "Amazon", (long) 0);

                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri
                                .parse("http://www.amazon.com/gp/search?ie=UTF8&keywords="
                                        + title));
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        Utils.tryStartActivity(v.getContext(), intent, true);
                    }
                });
            } else {
                amazonButton.setEnabled(false);
            }

        }
    }

    /**
     * Starts activity with {@link Intent#ACTION_VIEW} to display the given shows or episodes
     * trakt.tv page.<br> If any of the season or episode numbers is below 0, displays the show
     * page.
     */
    public static void setUpTraktButton(final int showTvdbId, final int seasonNumber,
            final int episodeNumber,
            View traktButton, final String logTag) {
        if (traktButton != null) {
            traktButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    String uri;
                    if (seasonNumber < 0 || episodeNumber < 0) {
                        // look just for the show page
                        uri = TRAKT_SEARCH_SHOW_URL + showTvdbId;
                    } else {
                        // look for the episode page
                        uri = TRAKT_SEARCH_SHOW_URL + showTvdbId
                                + TRAKT_SEARCH_SEASON_ARG + seasonNumber
                                + TRAKT_SEARCH_EPISODE_ARG + episodeNumber;
                    }

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(uri));
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    Utils.tryStartActivity(v.getContext(), intent, true);

                    EasyTracker.getTracker()
                            .sendEvent(logTag, "Action Item", "trakt", (long) 0);
                }
            });
        }
    }

    /**
     * Starts activity with {@link Intent#ACTION_VIEW} to display the given shows trakt.tv page.
     */
    public static void setUpTraktButton(int showTvdbId, View traktButton, String logTag) {
        setUpTraktButton(showTvdbId, -1, -1, traktButton, logTag);
    }

    /**
     * Starts activity with {@link Intent#ACTION_VIEW} to display the given movies trakt.tv page.
     */
    public static void openTraktMovie(Context context, int tmdbId, String logTag) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(TRAKT_SEARCH_MOVIE_URL + tmdbId));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        Utils.tryStartActivity(context, intent, true);

        EasyTracker.getTracker().sendEvent(logTag, "Action Item", "trakt", (long) 0);
    }

    /**
     * Starts activity with {@link Intent#ACTION_VIEW} to display the given show or episodes
     * TVDb.com page.<br> If any of the season or episode numbers is below 0, displays the show
     * page.
     */
    public static void setUpTvdbButton(final int showTvdbId, final int seasonTvdbId,
            final int episodeTvdbId, View tvdbButton, final String logTag) {
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

                    EasyTracker.getTracker().sendEvent(logTag, "Action Item", "TVDb", (long) 0);
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

        EasyTracker.getTracker().sendEvent(logTag, "Action Item", "YouTube", (long) 0);
    }

    /**
     * Used to open the YouTube app and search for <code>query</code>
     *
     * @param query  The search query for the YouTube app
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
     * Attempts to open the YouTube application to search for <code>query</code> . If the app is
     * unavailable, a web search if performed instead
     *
     * @param context The {@link Context} to use
     * @param query   The search query
     * @param logTag  The log tag to use, for Analytics
     */
    public static void searchYoutube(Context context, String query, String logTag) {
        PackageManager pm = context.getPackageManager();
        boolean hasYouTube = false;
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

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        Utils.tryStartActivity(context, intent, true);
        EasyTracker.getTracker().sendEvent(logTag, "Action Item", "YouTube search", (long) 0);
    }

    /**
     * Used to search the web for <code>query</code>
     *
     * @param query  The search query for the YouTube app
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
     * Attempts to search the web for <code>query</code>
     *
     * @param context The {@link Context} to use
     * @param query   The search query
     * @param logTag  The log tag to use, for Analytics
     */
    public static void performWebSearch(Context context, String query, String logTag) {
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra(SearchManager.QUERY, query);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        Utils.tryStartActivity(context, intent, true);
        EasyTracker.getTracker().sendEvent(logTag, "Action Item", "Web search", (long) 0);
    }

}
