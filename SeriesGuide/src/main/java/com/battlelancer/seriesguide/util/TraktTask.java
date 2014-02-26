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

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.enums.TraktStatus;
import com.battlelancer.seriesguide.ui.ConnectTraktActivity;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.CheckinResponse;
import com.jakewharton.trakt.entities.Response;
import com.jakewharton.trakt.enumerations.Rating;
import com.jakewharton.trakt.services.CommentService;
import com.jakewharton.trakt.services.MovieService;
import com.jakewharton.trakt.services.RateService;
import com.jakewharton.trakt.services.ShowService;
import com.uwetrottmann.androidutils.AndroidUtils;
import de.greenrobot.event.EventBus;
import retrofit.RetrofitError;
import timber.log.Timber;

public class TraktTask extends AsyncTask<Void, Void, Response> {

    private Bundle mArgs;

    private final Context mContext;

    private TraktAction mAction;

    public interface InitBundle {

        String TRAKTACTION = "traktaction";

        String MOVIE_TMDB_ID = "tmdbid";

        String SHOW_TVDBID = "tvdbid";

        String SEASON = "season";

        String EPISODE = "episode";

        String MESSAGE = "message";

        String RATING = "rating";

        String ISSPOILER = "isspoiler";
    }

    public static class TraktActionCompleteEvent {

        public TraktAction mTraktAction;

        public boolean mWasSuccessful;

        public String mMessage;

        public TraktActionCompleteEvent(TraktAction traktAction, boolean wasSuccessful,
                String message) {
            mTraktAction = traktAction;
            mWasSuccessful = wasSuccessful;
            mMessage = message;
        }

        /**
         * Displays status toasts dependent on the result of the trakt action performed.
         */
        public void handle(Context context) {
            if (TextUtils.isEmpty(mMessage)) {
                return;
            }

            if (!mWasSuccessful) {
                // display error toast
                Toast.makeText(context, mMessage, Toast.LENGTH_LONG).show();
                return;
            }

            // display success toast
            switch (mTraktAction) {
                case CHECKIN_EPISODE:
                case CHECKIN_MOVIE:
                    Toast.makeText(context, mMessage, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(context,
                            mMessage + " " + context.getString(R.string.ontrakt),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    public static class TraktCheckInBlockedEvent {

        public Bundle traktTaskArgs;

        public int waitMinutes;

        public TraktCheckInBlockedEvent(Bundle traktTaskArgs, int waitMinutes) {
            this.traktTaskArgs = traktTaskArgs;
            this.waitMinutes = waitMinutes;
        }
    }

    /**
     * Initial constructor. Call <b>one</b> of the setup-methods like {@link #shoutEpisode(int,
     * int,
     * int, String, boolean)} afterwards.<br> <br> Make sure the user has valid trakt credentials
     * (check with {@link com.battlelancer.seriesguide.settings.TraktCredentials#hasCredentials()}
     * and then possibly launch {@link ConnectTraktActivity}) or execution will fail.
     */
    public TraktTask(Context context) {
        mContext = context;
        mArgs = new Bundle();
    }

    /**
     * Fast constructor, allows passing of an already pre-built {@code args} {@link Bundle}.<br>
     * <br> Make sure the user has valid trakt credentials (check with {@link
     * com.battlelancer.seriesguide.settings.TraktCredentials#hasCredentials()} and then possibly
     * launch {@link ConnectTraktActivity}) or execution will fail.
     */
    public TraktTask(Context context, Bundle args) {
        this(context);
        mArgs = args;
    }

    /**
     * Check into an episode. Optionally provide a checkin message.
     */
    public TraktTask checkInEpisode(int showTvdbid, int season, int episode, String message) {
        mArgs.putString(InitBundle.TRAKTACTION, TraktAction.CHECKIN_EPISODE.name());
        mArgs.putInt(InitBundle.SHOW_TVDBID, showTvdbid);
        mArgs.putInt(InitBundle.SEASON, season);
        mArgs.putInt(InitBundle.EPISODE, episode);
        mArgs.putString(InitBundle.MESSAGE, message);
        return this;
    }

    /**
     * Check into an episode. Optionally provide a checkin message.
     */
    public TraktTask checkInMovie(int tmdbId, String message) {
        mArgs.putString(InitBundle.TRAKTACTION, TraktAction.CHECKIN_MOVIE.name());
        mArgs.putInt(InitBundle.MOVIE_TMDB_ID, tmdbId);
        mArgs.putString(InitBundle.MESSAGE, message);
        return this;
    }

    public TraktTask collectionAddMovie(int movieTmdbId) {
        mArgs.putString(InitBundle.TRAKTACTION, TraktAction.COLLECTION_ADD_MOVIE.name());
        mArgs.putInt(InitBundle.MOVIE_TMDB_ID, movieTmdbId);
        return this;
    }

    public TraktTask collectionRemoveMovie(int movieTmdbId) {
        mArgs.putString(InitBundle.TRAKTACTION, TraktAction.COLLECTION_REMOVE_MOVIE.name());
        mArgs.putInt(InitBundle.MOVIE_TMDB_ID, movieTmdbId);
        return this;
    }

    /**
     * Post a shout for a show.
     */
    public TraktTask shoutShow(int showTvdbid, String shout, boolean isSpoiler) {
        mArgs.putString(InitBundle.TRAKTACTION, TraktAction.SHOUT.name());
        mArgs.putInt(InitBundle.SHOW_TVDBID, showTvdbid);
        mArgs.putString(InitBundle.MESSAGE, shout);
        mArgs.putBoolean(InitBundle.ISSPOILER, isSpoiler);
        return this;
    }

    /**
     * Post a shout for an episode.
     */
    public TraktTask shoutEpisode(int showTvdbid, int season, int episode, String shout,
            boolean isSpoiler) {
        shoutShow(showTvdbid, shout, isSpoiler);
        mArgs.putInt(InitBundle.SEASON, season);
        mArgs.putInt(InitBundle.EPISODE, episode);
        return this;
    }

    /**
     * Post a shout for a movie.
     */
    public TraktTask shoutMovie(int movieTmdbId, String shout, boolean isSpoiler) {
        mArgs.putString(InitBundle.TRAKTACTION, TraktAction.SHOUT.name());
        mArgs.putInt(InitBundle.MOVIE_TMDB_ID, movieTmdbId);
        mArgs.putString(InitBundle.MESSAGE, shout);
        mArgs.putBoolean(InitBundle.ISSPOILER, isSpoiler);
        return this;
    }

    public TraktTask watchedMovie(int movieTmdbId) {
        mArgs.putString(InitBundle.TRAKTACTION, TraktAction.WATCHED_MOVIE.name());
        mArgs.putInt(InitBundle.MOVIE_TMDB_ID, movieTmdbId);
        return this;
    }

    public TraktTask unwatchedMovie(int movieTmdbId) {
        mArgs.putString(InitBundle.TRAKTACTION, TraktAction.UNWATCHED_MOVIE.name());
        mArgs.putInt(InitBundle.MOVIE_TMDB_ID, movieTmdbId);
        return this;
    }

    /**
     * Add a movie to a users watchlist.
     */
    public TraktTask watchlistMovie(int movieTmdbId) {
        mArgs.putString(InitBundle.TRAKTACTION, TraktAction.WATCHLIST_MOVIE.name());
        mArgs.putInt(InitBundle.MOVIE_TMDB_ID, movieTmdbId);
        return this;
    }

    /**
     * Remove a movie from a users watchlist.
     */
    public TraktTask unwatchlistMovie(int movieTmdbId) {
        mArgs.putString(InitBundle.TRAKTACTION, TraktAction.UNWATCHLIST_MOVIE.name());
        mArgs.putInt(InitBundle.MOVIE_TMDB_ID, movieTmdbId);
        return this;
    }

    @Override
    protected Response doInBackground(Void... params) {
        // we need this value in onPostExecute, so preserve it here
        mAction = TraktAction.valueOf(mArgs.getString(InitBundle.TRAKTACTION));

        // check for network connection
        if (!AndroidUtils.isNetworkConnected(mContext)) {
            Response r = new Response();
            r.status = TraktStatus.FAILURE;
            r.error = mContext.getString(R.string.offline);
            return r;
        }

        // get authenticated trakt
        Trakt manager = ServiceUtils.getTraktWithAuth(mContext);
        if (manager == null) {
            // no valid credentials
            Response r = new Response();
            r.status = TraktStatus.FAILURE;
            r.error = mContext.getString(R.string.trakt_error_credentials);
            return r;
        }

        // last chance to abort
        if (isCancelled()) {
            return null;
        }

        Response r = null;
        try {
            switch (mAction) {
                case CHECKIN_EPISODE:
                case CHECKIN_MOVIE: {
                    return doCheckInAction(manager);
                }
                case RATE_EPISODE:
                case RATE_SHOW:
                case RATE_MOVIE: {
                    return doRatingAction(manager);
                }
                case COLLECTION_ADD_MOVIE:
                case COLLECTION_REMOVE_MOVIE:
                case WATCHLIST_MOVIE:
                case UNWATCHLIST_MOVIE:
                case WATCHED_MOVIE:
                case UNWATCHED_MOVIE: {
                    return doMovieAction(manager);
                }
                case SHOUT: {
                    return doShoutAction(manager);
                }
            }
        } catch (RetrofitError e) {
            Timber.e(e, mAction.toString() + " failed");
            r = new Response();
            r.status = TraktStatus.FAILURE;
            r.error = mContext.getString(R.string.trakt_error_general);
        }

        return r;
    }

    private Response doCheckInAction(Trakt manager) {
        final String message = mArgs.getString(InitBundle.MESSAGE);

        switch (mAction) {
            case CHECKIN_EPISODE: {
                Response r;
                final int showTvdbId = mArgs.getInt(InitBundle.SHOW_TVDBID);
                final int season = mArgs.getInt(InitBundle.SEASON);
                final int episode = mArgs.getInt(InitBundle.EPISODE);
                if (TextUtils.isEmpty(message)) {
                    // no message
                    r = manager.showService().checkin(new ShowService.ShowCheckin(
                            showTvdbId, season, episode, Utils.getVersion(mContext), ""
                    ));
                } else {
                    // with social media message
                    r = manager.showService().checkin(new ShowService.ShowCheckin(
                            showTvdbId, season, episode, message, Utils.getVersion(mContext), ""
                    ));
                }

                if (com.jakewharton.trakt.enumerations.Status.SUCCESS.equals(r.status)) {
                    r.message = mContext.getString(R.string.checkin_success_trakt,
                            (r.show != null ? r.show.title + " " : "")
                                    + Utils.getEpisodeNumber(mContext, season, episode));
                }

                return r;
            }
            case CHECKIN_MOVIE: {
                Response r;
                final int tmdbId = mArgs.getInt(InitBundle.MOVIE_TMDB_ID);

                if (TextUtils.isEmpty(message)) {
                    r = manager.movieService().checkin(new MovieService.MovieCheckin(
                            tmdbId, Utils.getVersion(mContext), ""
                    ));
                } else {
                    r = manager.movieService().checkin(new MovieService.MovieCheckin(
                            tmdbId, message, Utils.getVersion(mContext), ""
                    ));
                }

                if (com.jakewharton.trakt.enumerations.Status.SUCCESS.equals(r.status)) {
                    r.message = mContext.getString(R.string.checkin_success_trakt,
                            (r.movie != null ?
                                    r.movie.title : mContext.getString(R.string.unknown)));
                }

                return r;
            }
        }

        return null;
    }

    private Response doRatingAction(Trakt manager) {
        Rating rating = Rating.fromValue(mArgs.getString(InitBundle.RATING));

        switch (mAction) {
            case RATE_EPISODE: {
                final int showTvdbId = mArgs.getInt(InitBundle.SHOW_TVDBID);
                final int season = mArgs.getInt(InitBundle.SEASON);
                final int episode = mArgs.getInt(InitBundle.EPISODE);
                return manager.rateService().episode(new RateService.EpisodeRating(
                        showTvdbId, season, episode, rating
                ));
            }
            case RATE_SHOW: {
                final int showTvdbId = mArgs.getInt(InitBundle.SHOW_TVDBID);
                return manager.rateService().show(new RateService.ShowRating(
                        showTvdbId, rating
                ));
            }
            case RATE_MOVIE: {
                final int tmdbId = mArgs.getInt(InitBundle.MOVIE_TMDB_ID);
                return manager.rateService().movie(new RateService.MovieRating(
                        tmdbId, rating
                ));
            }
        }

        return null;
    }

    private Response doMovieAction(Trakt manager) {
        final int tmdbId = mArgs.getInt(InitBundle.MOVIE_TMDB_ID);

        switch (mAction) {
            case COLLECTION_ADD_MOVIE: {
                Response r = manager.movieService().library(new MovieService.Movies(
                        new MovieService.SeenMovie(tmdbId)
                ));
                // always returns success, even if movie is already in collection
                r.message = mContext.getString(R.string.action_collection_added);
                return r;
            }
            case COLLECTION_REMOVE_MOVIE: {
                Response r = manager.movieService().unlibrary(new MovieService.Movies(
                        new MovieService.SeenMovie(tmdbId)
                ));
                // always returns success, even if movie was never in collection
                r.message = mContext.getString(R.string.action_collection_removed);
                return r;
            }
            case WATCHED_MOVIE: {
                Response r =  manager.movieService()
                        .seen(new MovieService.Movies(new MovieService.SeenMovie(tmdbId)));
                r.message = mContext.getString(R.string.action_watched);
                return r;
            }
            case UNWATCHED_MOVIE: {
                Response r = manager.movieService()
                        .unseen(new MovieService.Movies(new MovieService.SeenMovie(tmdbId)));
                r.message = mContext.getString(R.string.action_unwatched);
                return r;
            }
            case WATCHLIST_MOVIE: {
                Response r = manager.movieService().watchlist(new MovieService.Movies(
                        new MovieService.SeenMovie(tmdbId)
                ));
                // always returns success, even if movie is already on watchlist
                r.message = mContext.getString(R.string.watchlist_added);
                return r;
            }
            case UNWATCHLIST_MOVIE: {
                Response r = manager.movieService().unwatchlist(new MovieService.Movies(
                        new MovieService.SeenMovie(tmdbId)
                ));
                // always returns success, even if movie is not on watchlist anymore
                r.message = mContext.getString(R.string.watchlist_removed);
                return r;
            }
        }

        return null;
    }

    private Response doShoutAction(Trakt manager) {
        Response r;
        final String shout = mArgs.getString(InitBundle.MESSAGE);
        final boolean isSpoiler = mArgs.getBoolean(InitBundle.ISSPOILER);
        final int showTvdbId = mArgs.getInt(InitBundle.SHOW_TVDBID);
        final int season = mArgs.getInt(InitBundle.SEASON);
        final int episode = mArgs.getInt(InitBundle.EPISODE);

        // episode?
        if (episode != 0) {
            r = manager.commentService().episode(new CommentService.EpisodeComment(
                    showTvdbId, season, episode, shout
            ).spoiler(isSpoiler));
            return r;
        }

        // movie?
        int tmdbId = mArgs.getInt(InitBundle.MOVIE_TMDB_ID);
        if (tmdbId != 0) {
            r = manager.commentService().movie(new CommentService.MovieComment(
                    tmdbId, shout
            ).spoiler(isSpoiler));
            return r;
        }

        // show!
        r = manager.commentService().show(new CommentService.ShowComment(
                showTvdbId, shout
        ).spoiler(isSpoiler));
        return r;
    }

    @Override
    protected void onPostExecute(Response r) {
        if (r == null) {
            // unknown error
            return;
        }

        if (TraktStatus.SUCCESS.equals(r.status)) {
            // all good
            EventBus.getDefault().post(new TraktActionCompleteEvent(mAction, true, r.message));
        } else {
            // special handling of blocked check-ins
            if (mAction == TraktAction.CHECKIN_EPISODE
                    || mAction == TraktAction.CHECKIN_MOVIE) {
                if (r instanceof CheckinResponse) {
                    CheckinResponse checkinResponse = (CheckinResponse) r;
                    if (checkinResponse.wait != 0) {
                        // looks like a check in is already in progress
                        EventBus.getDefault().post(
                                new TraktCheckInBlockedEvent(mArgs, checkinResponse.wait));
                        return;
                    }
                }
            }

            // well, something went wrong
            EventBus.getDefault().post(new TraktActionCompleteEvent(mAction, false, r.error));
        }
    }
}
