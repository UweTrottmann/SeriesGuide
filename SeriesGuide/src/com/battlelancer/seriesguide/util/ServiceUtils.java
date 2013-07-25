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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;

import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.google.analytics.tracking.android.EasyTracker;
import com.jakewharton.trakt.ServiceManager;
import com.uwetrottmann.seriesguide.R;

/**
 * Helper methods to interact with third-party services trakt and The Movie
 * Database used within SeriesGuide.
 */
public final class ServiceUtils {

    private static final String GOOGLE_PLAY = "https://play.google.com/store/search?q=%s&c=movies";

    private static final String TRAKT_SEARCH_BASE_URL = "http://trakt.tv/search/";

    public static final String IMDB_TITLE_URL = "http://imdb.com/title/";

    private static final String TRAKT_SEARCH_MOVIE_URL = TRAKT_SEARCH_BASE_URL + "tmdb?q=";

    private static final String TRAKT_SEARCH_SHOW_URL = TRAKT_SEARCH_BASE_URL + "tvdb?q=";

    private static final String TRAKT_SEARCH_SEASON_ARG = "&s=";

    private static final String TRAKT_SEARCH_EPISODE_ARG = "&e=";

    private static final String TVDB_SHOW_URL = "http://thetvdb.com/?tab=series&id=";

    private static final String TVDB_EPISODE_URL = "http://thetvdb.com/?tab=episode&seriesid=";

    private static final String TVDB_EPISODE_URL_SEASON_PARAM = "&seasonid=";

    private static final String TVDB_EPISODE_URL_EPISODE_PARAM = "&id=";

    private static ServiceManager sTraktServiceManagerInstance;

    private static ServiceManager sTraktServiceManagerWithAuthInstance;

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
     * Get a trakt-java ServiceManager with just our API key set. NO user auth
     * data.
     * 
     * @param context
     * @return
     */
    public static synchronized ServiceManager getTraktServiceManager(Context context) {
        if (ServiceUtils.sTraktServiceManagerInstance == null) {
            ServiceUtils.sTraktServiceManagerInstance = new ServiceManager();
            ServiceUtils.sTraktServiceManagerInstance.setReadTimeout(10000);
            ServiceUtils.sTraktServiceManagerInstance.setConnectionTimeout(15000);
            ServiceUtils.sTraktServiceManagerInstance.setApiKey(context.getResources().getString(
                    R.string.trakt_apikey));
            // this made some problems, so sadly disabled for now
            // manager.setUseSsl(true);
        }

        return ServiceUtils.sTraktServiceManagerInstance;
    }

    /**
     * Get the trakt-java ServiceManger with user credentials and our API key
     * set.
     * 
     * @param context
     * @param refreshCredentials Set this flag to refresh the user credentials.
     * @return
     * @throws Exception When decrypting the password failed.
     */
    public static synchronized ServiceManager getTraktServiceManagerWithAuth(Context context,
            boolean refreshCredentials) {
        if (ServiceUtils.sTraktServiceManagerWithAuthInstance == null) {
            ServiceUtils.sTraktServiceManagerWithAuthInstance = new ServiceManager();
            ServiceUtils.sTraktServiceManagerWithAuthInstance.setReadTimeout(10000);
            ServiceUtils.sTraktServiceManagerWithAuthInstance.setConnectionTimeout(15000);
            ServiceUtils.sTraktServiceManagerWithAuthInstance.setApiKey(context.getResources()
                    .getString(
                            R.string.trakt_apikey));
            // this made some problems, so sadly disabled for now
            // manager.setUseSsl(true);

            refreshCredentials = true;
        }

        if (refreshCredentials) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            final String username = prefs.getString(SeriesGuidePreferences.KEY_TRAKTUSER, null);
            String password = prefs.getString(SeriesGuidePreferences.KEY_TRAKTPWD, null);

            if (!TextUtils.isEmpty(password)) {
                // decryption might return null, so wrap in separate condition
                password = SimpleCrypto.decrypt(password, context);
            }

            if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
                ServiceUtils.sTraktServiceManagerWithAuthInstance.setAuthentication(username,
                        password);
            } else {
                // clear all trakt credentials
                clearTraktCredentials(context);
                ServiceUtils.sTraktServiceManagerWithAuthInstance.setAuthentication(null, null);
                return null;
            }
        }

        return ServiceUtils.sTraktServiceManagerWithAuthInstance;
    }

    public static String getTraktUsername(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                .getApplicationContext());

        return prefs.getString(SeriesGuidePreferences.KEY_TRAKTUSER, "");
    }

    public static boolean isTraktCredentialsValid(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                .getApplicationContext());
        String username = prefs.getString(SeriesGuidePreferences.KEY_TRAKTUSER, "");
        String password = prefs.getString(SeriesGuidePreferences.KEY_TRAKTPWD, "");

        return (!username.equalsIgnoreCase("") && !password.equalsIgnoreCase(""));
    }

    public static void clearTraktCredentials(Context context) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context
                .getApplicationContext()).edit();
        editor.putString(SeriesGuidePreferences.KEY_TRAKTUSER, "").putString(
                SeriesGuidePreferences.KEY_TRAKTPWD, "");
        editor.commit();
    }

    /**
     * Displays the IMDb page for the given id (show or episode) in the IMDb app
     * or on the imdb.com web page. If the IMDb id is empty, disables the
     * button.
     * 
     * @param imdbId
     * @param imdbButton
     * @param logTag
     * @param context
     */
    public static void setUpImdbButton(final String imdbId, View imdbButton, final String logTag,
            final Context context) {
        if (imdbButton != null) {
            if (!TextUtils.isEmpty(imdbId)) {
                imdbButton.setEnabled(true);
                imdbButton.setOnClickListener(new OnClickListener() {
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
                .parse("imdb:///title/" + imdbId + "/"));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(IMDB_TITLE_URL
                    + imdbId));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            context.startActivity(intent);
        }

        EasyTracker.getTracker().sendEvent(logTag, "Action Item", "IMDb", (long) 0);
    }

    /**
     * Sets a {@link OnClickListener} on the given button linking to a Google
     * Play Store search for the given title or disabling the button if the
     * title is empty.
     */
    public static void setUpGooglePlayButton(final String title, View playButton,
            final String logTag) {
        if (playButton != null) {

            if (!TextUtils.isEmpty(title)) {
                playButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EasyTracker.getTracker()
                                .sendEvent(logTag, "Action Item", "Google Play", (long) 0);

                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        try {
                            String shopTV = String.format(GOOGLE_PLAY, Uri.encode(title));
                            intent.setData(Uri.parse(shopTV));
                            v.getContext().startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            intent.setData(Uri.parse("http://play.google.com/store/search?q="
                                    + title));
                            v.getContext().startActivity(intent);
                        }
                    }
                });
            } else {
                playButton.setEnabled(false);
            }

        }
    }

    /**
     * Sets a {@link OnClickListener} on the given button linking to a Amazon
     * web search for the given title or disabling the button if the title is
     * empty.
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
                                .parse("http://www.amazon.com/gp/search?ie=UTF8&keywords=" + title));
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        v.getContext().startActivity(intent);
                    }
                });
            } else {
                amazonButton.setEnabled(false);
            }

        }
    }

    /**
     * Starts activity with {@link Intent#ACTION_VIEW} to display the given
     * shows or episodes trakt.tv page.<br>
     * If any of the season or episode numbers is below 0, displays the show
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
                    v.getContext().startActivity(intent);

                    EasyTracker.getTracker()
                            .sendEvent(logTag, "Action Item", "trakt", (long) 0);
                }
            });
        }
    }

    /**
     * Starts activity with {@link Intent#ACTION_VIEW} to display the given
     * shows trakt.tv page.
     */
    public static void setUpTraktButton(int showTvdbId, View traktButton, String logTag) {
        setUpTraktButton(showTvdbId, -1, -1, traktButton, logTag);
    }

    /**
     * Starts activity with {@link Intent#ACTION_VIEW} to display the given
     * movies trakt.tv page.
     */
    public static void openTraktMovie(Context context, int tmdbId, String logTag) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(TRAKT_SEARCH_MOVIE_URL + tmdbId));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        context.startActivity(intent);

        EasyTracker.getTracker()
                .sendEvent(logTag, "Action Item", "trakt", (long) 0);
    }

    /**
     * Starts activity with {@link Intent#ACTION_VIEW} to display the given show
     * or episodes TVDb.com page.<br>
     * If any of the season or episode numbers is below 0, displays the show
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
                    v.getContext().startActivity(intent);

                    EasyTracker.getTracker().sendEvent(logTag, "Action Item", "TVDb", (long) 0);
                }
            });
        }
    }

    /**
     * Starts activity with {@link Intent#ACTION_VIEW} to display the given
     * shows TVDb.com page.
     */
    public static void setUpTvdbButton(final int showTvdbId, View tvdbButton, final String logTag) {
        setUpTvdbButton(showTvdbId, -1, -1, tvdbButton, logTag);
    }
}
