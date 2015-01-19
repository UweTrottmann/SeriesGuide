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
import timber.log.Timber;

/**
 * Base class for executing movie actions.
 */
public abstract class BaseMovieActionTask extends BaseActionTask {

    private final int movieTmdbId;

    public BaseMovieActionTask(Context context, int movieTmdbId) {
        super(context);
        this.movieTmdbId = movieTmdbId;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        if (isCancelled()) {
            return null;
        }

        // if sending to service, check for connection
        if (isSendingToHexagon() || isSendingToTrakt()) {
            if (!AndroidUtils.isNetworkConnected(getContext())) {
                return ERROR_NETWORK;
            }
        }

        // send to hexagon
        if (isSendingToHexagon()) {
            Movie movie = new Movie();
            movie.setTmdbId(movieTmdbId);

            setHexagonMovieProperties(movie);

            List<Movie> movies = new ArrayList<>();
            movies.add(movie);

            MovieList movieList = new MovieList();
            movieList.setMovies(movies);

            try {
                HexagonTools.getMoviesService(getContext()).save(movieList).execute();
            } catch (IOException e) {
                Timber.e(e, "doInBackground: failed to upload movie to hexagon.");
                return ERROR_HEXAGON_API;
            }
        }

        // send to trakt
        if (isSendingToTrakt()) {
            TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(getContext());
            if (trakt == null) {
                return ERROR_TRAKT_AUTH;
            }

            Sync traktSync = trakt.sync();
            SyncItems items = new SyncItems().movies(
                    new SyncMovie().id(MovieIds.tmdb(movieTmdbId)));

            SyncResponse response;
            try {
                response = doTraktAction(traktSync, items);
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(getContext()).setCredentialsInvalid();
                return ERROR_TRAKT_AUTH;
            }

            if (!isTraktActionSuccessful(response)) {
                return ERROR_TRAKT_API;
            }
        }

        // update local state
        if (!doDatabaseUpdate(getContext(), movieTmdbId)) {
            return ERROR_DATABASE;
        }

        return SUCCESS;
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);

        // always post event so UI releases locks
        EventBus.getDefault().post(new MovieTools.MovieChangedEvent(movieTmdbId));
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

    /**
     * Set properties to send to hexagon. To disable hexagon uploading, override {@link
     * #isSendingToHexagon()}}.
     */
    protected abstract void setHexagonMovieProperties(Movie movie);

    /**
     * Ensure to catch {@link retrofit.RetrofitError} and return {@code null} in that case.
     */
    protected abstract SyncResponse doTraktAction(Sync traktSync, SyncItems items)
            throws OAuthUnauthorizedException;
}
