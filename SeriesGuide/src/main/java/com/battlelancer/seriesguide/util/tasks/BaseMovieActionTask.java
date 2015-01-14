/*
 * Copyright 2015 Uwe Trottmann
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

package com.battlelancer.seriesguide.util.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.backend.movies.model.Movie;
import com.uwetrottmann.seriesguide.backend.movies.model.MovieList;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.MovieIds;
import com.uwetrottmann.trakt.v2.entities.SyncItems;
import com.uwetrottmann.trakt.v2.entities.SyncMovie;
import com.uwetrottmann.trakt.v2.entities.SyncResponse;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import com.uwetrottmann.trakt.v2.services.Sync;
import de.greenrobot.event.EventBus;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Base class for executing movie actions.
 */
public abstract class BaseMovieActionTask extends AsyncTask<Void, Void, Integer> {

    private static final int SUCCESS = 0;
    private static final int ERROR_NETWORK = -1;
    private static final int ERROR_DATABASE = -2;
    private static final int ERROR_TRAKT_AUTH = -3;
    private static final int ERROR_TRAKT_API = -4;
    private static final int ERROR_HEXAGON_API = -5;

    private final Context context;
    private final int movieTmdbId;
    private boolean isSendingToHexagon;
    private boolean isSendingToTrakt;

    public BaseMovieActionTask(Context context, int movieTmdbId) {
        this.context = context.getApplicationContext();
        this.movieTmdbId = movieTmdbId;
    }

    @Override
    protected void onPreExecute() {
        isSendingToHexagon = HexagonTools.isSignedIn(context);
        if (isSendingToHexagon) {
            Toast.makeText(context, R.string.hexagon_api_queued, Toast.LENGTH_SHORT).show();
        }
        isSendingToTrakt = TraktCredentials.get(context).hasCredentials();
        if (isSendingToTrakt) {
            Toast.makeText(context, R.string.trakt_submitqueued, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected Integer doInBackground(Void... params) {
        // if sending to service, check for connection
        if (isSendingToHexagon || isSendingToTrakt) {
            if (!AndroidUtils.isNetworkConnected(context)) {
                return ERROR_NETWORK;
            }
        }

        // send to hexagon
        if (isSendingToHexagon) {
            Movie movie = new Movie();
            movie.setTmdbId(movieTmdbId);

            setHexagonMovieProperties(movie);

            List<Movie> movies = new ArrayList<>();
            movies.add(movie);

            MovieList movieList = new MovieList();
            movieList.setMovies(movies);

            try {
                HexagonTools.getMoviesService(context).save(movieList).execute();
            } catch (IOException e) {
                Timber.e(e, "doInBackground: failed to upload movie to hexagon.");
                return ERROR_HEXAGON_API;
            }
        }

        // send to trakt
        if (isSendingToTrakt) {
            TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(context);
            if (trakt == null) {
                return ERROR_TRAKT_AUTH;
            }

            Sync traktSync = trakt.sync();
            SyncItems items = new SyncItems().movies(
                    new SyncMovie().id(MovieIds.tmdb(movieTmdbId)));

            SyncResponse response;
            try {
                response = doTraktAction(traktSync, items);
            } catch (RetrofitError e) {
                Timber.e(e, "doInBackground: sending to trakt failed");
                return ERROR_TRAKT_API;
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(context).setCredentialsInvalid();
                return ERROR_TRAKT_AUTH;
            }

            if (!isTraktActionSuccessful(response)) {
                return ERROR_TRAKT_API;
            }
        }

        // update local state
        if (!doDatabaseUpdate(context, movieTmdbId)) {
            return ERROR_DATABASE;
        }

        // post success event
        EventBus.getDefault().post(new MovieTools.MovieChangedEvent(movieTmdbId));

        return SUCCESS;
    }

    @Override
    protected void onPostExecute(Integer result) {
        // handle errors
        Integer errorResId = null;
        switch (result) {
            case ERROR_NETWORK:
                errorResId = R.string.offline;
                break;
            case ERROR_DATABASE:
                errorResId = R.string.database_error;
                break;
            case ERROR_TRAKT_AUTH:
                errorResId = R.string.trakt_error_credentials;
                break;
            case ERROR_TRAKT_API:
                errorResId = R.string.trakt_error_general;
                break;
            case ERROR_HEXAGON_API:
                errorResId = R.string.hexagon_api_error;
                break;
        }
        if (errorResId != null) {
            Toast.makeText(context, errorResId, Toast.LENGTH_LONG).show();
            return;
        }

        // success!
        Toast.makeText(context, getSuccessTextResId(), Toast.LENGTH_SHORT).show();
    }

    private static boolean isTraktActionSuccessful(SyncResponse response) {
        if (response == null) {
            // invalid response
            return false;
        }
        if (response.not_found != null && response.not_found.movies != null
                && response.not_found.movies.size() != 0) {
            // movie was not found on trakt
            return false;
        }
        return true;
    }

    protected abstract boolean doDatabaseUpdate(Context context, int movieTmdbId);

    protected abstract void setHexagonMovieProperties(Movie movie);

    protected abstract SyncResponse doTraktAction(Sync traktSync, SyncItems items)
            throws OAuthUnauthorizedException;

    protected abstract int getSuccessTextResId();
}
