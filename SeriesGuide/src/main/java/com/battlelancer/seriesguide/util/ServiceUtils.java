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

import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.jakewharton.trakt.Trakt;
import com.battlelancer.seriesguide.R;
import com.uwetrottmann.tmdb.Tmdb;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

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

    private static Trakt sTrakt;

    private static Trakt sTraktWithAuth;

    private static Tmdb sTmdbServiceManagerInstance;

    /* This class is never initialized */
    private ServiceUtils() {
    }

    /**
     * Get a tmdb-java ServiceManager with our API key set.
     */
    public static synchronized Tmdb getTmdbServiceManager(
            Context context) {
        if (sTmdbServiceManagerInstance == null) {
            sTmdbServiceManagerInstance = new Tmdb();
            sTmdbServiceManagerInstance.setApiKey(context.getResources().getString(
                    R.string.tmdb_apikey));
        }

        return sTmdbServiceManagerInstance;
    }

    /**
     * Get a {@link com.jakewharton.trakt.Trakt} service manager with just the API key set. NO user
     * auth data.
     *
     * @return A {@link com.jakewharton.trakt.Trakt} instance.
     */
    public static synchronized Trakt getTrakt(Context context) {
        if (sTrakt == null) {
            sTrakt = new Trakt();
            sTrakt.setApiKey(context.getResources().getString(R.string.trakt_apikey));
        }

        return sTrakt;
    }

    /**
     * Get a {@link com.jakewharton.trakt.Trakt} service manager with user credentials and API key
     * set.
     *
     * @return A {@link com.jakewharton.trakt.Trakt} instance or null if there are no valid
     * credentials.
     */
    public static synchronized Trakt getTraktWithAuth(Context context) {
        if (!TraktCredentials.get(context).hasCredentials()) {
            return null;
        }

        if (sTraktWithAuth == null) {
            sTraktWithAuth = new Trakt();
            sTraktWithAuth.setApiKey(context.getResources().getString(R.string.trakt_apikey));
            final String username = TraktCredentials.get(context).getUsername();
            final String password = TraktCredentials.get(context).getPassword();
            sTraktWithAuth.setAuthentication(username, password);
        }

        return sTraktWithAuth;
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

        Utils.trackAction(context, logTag, "IMDb");
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

        Utils.trackAction(context, logTag, "Google Play");
    }

    /**
     * Sets a {@link OnClickListener} on the given button linking to a Amazon web search for the
     * given title or disabling the button if the title is empty.
     */
    public static void setUpAmazonButton(final String title, final View amazonButton,
            final String logTag) {
        if (amazonButton != null) {

            if (!TextUtils.isEmpty(title)) {
                amazonButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Utils.trackAction(amazonButton.getContext(), logTag, "Amazon");

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
            final View traktButton, final String logTag) {
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

                    Utils.trackAction(traktButton.getContext(), logTag, "trakt");
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
        Utils.trackAction(context, logTag, "YouTube search");
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

        Utils.trackAction(context, logTag, "Web search");
    }

}
