/*
 * Copyright 2011 Uwe Trottmann
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
import com.uwetrottmann.seriesguide.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

import de.greenrobot.event.EventBus;
import retrofit.RetrofitError;

public class TraktTask extends AsyncTask<Void, Void, Response> {

    private static final String TAG = "TraktTask";

    private Bundle mArgs;

    private final Context mContext;

    private TraktAction mAction;

    private OnTraktActionCompleteListener mListener;

    public interface InitBundle {

        String TRAKTACTION = "traktaction";

        String IMDB_ID = "imdbid";

        String SHOW_TVDBID = "tvdbid";

        String TMDB_ID = "tmdbid";

        String SEASON = "season";

        String EPISODE = "episode";

        String MESSAGE = "message";

        String RATING = "rating";

        String ISSPOILER = "isspoiler";
    }

    public interface OnTraktActionCompleteListener {

        public void onTraktActionComplete(Bundle traktTaskArgs, boolean wasSuccessfull);

        public void onCheckinBlocked(Bundle traktTaskArgs, int wait);
    }

    public static class TraktActionCompleteEvent {

        public Bundle mTraktTaskArgs;

        public boolean mWasSuccessful;

        public TraktActionCompleteEvent(Bundle traktTaskArgs, boolean wasSuccessful) {
            mTraktTaskArgs = traktTaskArgs;
            mWasSuccessful = wasSuccessful;
        }
    }

    /**
     * Initial constructor. Call <b>one</b> of the setup-methods, like {@code shout(tvdbid, shout,
     *isSpoiler)}, afterwards.<br> <br> Make sure the user has valid trakt credentials (check with
     * {@link ServiceUtils#hasTraktCredentials(Context)} and then possibly launch {@link
     * ConnectTraktActivity}) or execution will fail.
     */
    public TraktTask(Context context, OnTraktActionCompleteListener listener) {
        mContext = context;
        mListener = listener;
        mArgs = new Bundle();
    }

    /**
     * Fast constructor, allows passing of an already pre-built {@code args} {@link Bundle}.<br>
     * <br> Make sure the user has valid trakt credentials (check with {@link
     * ServiceUtils#hasTraktCredentials(Context)} and then possibly launch {@link
     * ConnectTraktActivity}) or execution will fail.
     */
    public TraktTask(Context context, Bundle args, OnTraktActionCompleteListener listener) {
        this(context, listener);
        mArgs = args;
    }

    /**
     * Check into an episode. Optionally provide a checkin message.
     */
    public TraktTask checkInEpisode(int showTvdbid, int season, int episode, String message) {
        mArgs.putInt(InitBundle.TRAKTACTION, TraktAction.CHECKIN_EPISODE.index);
        mArgs.putInt(InitBundle.SHOW_TVDBID, showTvdbid);
        mArgs.putInt(InitBundle.SEASON, season);
        mArgs.putInt(InitBundle.EPISODE, episode);
        mArgs.putString(InitBundle.MESSAGE, message);
        return this;
    }

    /**
     * Check into an episode. Optionally provide a checkin message.
     */
    public TraktTask checkInMovie(String imdbId, String message) {
        mArgs.putInt(InitBundle.TRAKTACTION, TraktAction.CHECKIN_MOVIE.index);
        mArgs.putString(InitBundle.IMDB_ID, imdbId);
        mArgs.putString(InitBundle.MESSAGE, message);
        return this;
    }

    /**
     * Rate an episode.
     */
    public TraktTask rateEpisode(int showTvdbid, int season, int episode, Rating rating) {
        mArgs.putInt(InitBundle.TRAKTACTION, TraktAction.RATE_EPISODE.index);
        mArgs.putInt(InitBundle.SHOW_TVDBID, showTvdbid);
        mArgs.putInt(InitBundle.SEASON, season);
        mArgs.putInt(InitBundle.EPISODE, episode);
        mArgs.putString(InitBundle.RATING, rating.toString());
        return this;
    }

    /**
     * Rate a show.
     */
    public TraktTask rateShow(int showTvdbid, Rating rating) {
        mArgs.putInt(InitBundle.TRAKTACTION, TraktAction.RATE_SHOW.index);
        mArgs.putInt(InitBundle.SHOW_TVDBID, showTvdbid);
        mArgs.putString(InitBundle.RATING, rating.toString());
        return this;
    }

    /**
     * Post a shout for a show.
     */
    public TraktTask shout(int showTvdbid, String shout, boolean isSpoiler) {
        mArgs.putInt(InitBundle.TRAKTACTION, TraktAction.SHOUT.index);
        mArgs.putInt(InitBundle.SHOW_TVDBID, showTvdbid);
        mArgs.putString(InitBundle.MESSAGE, shout);
        mArgs.putBoolean(InitBundle.ISSPOILER, isSpoiler);
        return this;
    }

    /**
     * Post a shout for an episode.
     */
    public TraktTask shout(int showTvdbid, int season, int episode, String shout,
            boolean isSpoiler) {
        shout(showTvdbid, shout, isSpoiler);
        mArgs.putInt(InitBundle.SEASON, season);
        mArgs.putInt(InitBundle.EPISODE, episode);
        return this;
    }

    /**
     * Add a movie to a users watchlist.
     */
    public TraktTask watchlistMovie(int tmdbId) {
        mArgs.putInt(InitBundle.TRAKTACTION, TraktAction.WATCHLIST_MOVIE.index);
        mArgs.putInt(InitBundle.TMDB_ID, tmdbId);
        return this;
    }

    /**
     * Remove a movie from a users watchlist.
     */
    public TraktTask unwatchlistMovie(int tmdbId) {
        mArgs.putInt(InitBundle.TRAKTACTION, TraktAction.UNWATCHLIST_MOVIE.index);
        mArgs.putInt(InitBundle.TMDB_ID, tmdbId);
        return this;
    }

    @Override
    protected Response doInBackground(Void... params) {
        // we need this value in onPostExecute, so get it already here
        mAction = TraktAction.values()[mArgs.getInt(InitBundle.TRAKTACTION)];

        // check for network connection
        if (!AndroidUtils.isNetworkConnected(mContext)) {
            Response r = new Response();
            r.status = TraktStatus.FAILURE;
            r.error = mContext.getString(R.string.offline);
            return r;
        }

        // check for valid credentials
        if (!ServiceUtils.hasTraktCredentials(mContext)) {
            return null;
        }

        // get an authenticated trakt-java ServiceManager
        Trakt manager = ServiceUtils.getTraktServiceManagerWithAuth(mContext, false);
        if (manager == null) {
            // password could not be decrypted
            Response r = new Response();
            r.status = TraktStatus.FAILURE;
            r.error = mContext.getString(R.string.trakt_error_credentials);
            return r;
        }

        // get values used by all actions
        final int showTvdbId = mArgs.getInt(InitBundle.SHOW_TVDBID);
        final int season = mArgs.getInt(InitBundle.SEASON);
        final int episode = mArgs.getInt(InitBundle.EPISODE);

        // last chance to abort
        if (isCancelled()) {
            return null;
        }

        try {
            Response r = null;

            switch (mAction) {
                case CHECKIN_EPISODE: {
                    final String message = mArgs.getString(InitBundle.MESSAGE);

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
                        SharedPreferences prefs = PreferenceManager
                                .getDefaultSharedPreferences(mContext);
                        r.message = mContext
                                .getString(R.string.checkin_success_trakt,
                                        (r.show != null ? r.show.title + " " : "")
                                                + Utils.getEpisodeNumber(prefs, season, episode));
                    }

                    break;
                }
                case CHECKIN_MOVIE: {
                    final String imdbId = mArgs.getString(InitBundle.IMDB_ID);
                    final String message = mArgs.getString(InitBundle.MESSAGE);

                    if (TextUtils.isEmpty(message)) {
                        r = manager.movieService().checkin(new MovieService.MovieCheckin(
                                imdbId, Utils.getVersion(mContext), ""
                        ));
                    } else {
                        r = manager.movieService().checkin(new MovieService.MovieCheckin(
                                imdbId, message, Utils.getVersion(mContext), ""
                        ));
                    }

                    if (com.jakewharton.trakt.enumerations.Status.SUCCESS.equals(r.status)) {
                        r.message = mContext
                                .getString(R.string.checkin_success_trakt,
                                        (r.movie != null ? r.movie.title + " " : ""));
                    }

                    break;
                }
                case RATE_EPISODE: {
                    final Rating rating = Rating.fromValue(mArgs.getString(InitBundle.RATING));
                    r = manager.rateService().episode(new RateService.EpisodeRating(
                            showTvdbId, season, episode, rating
                    ));
                    break;
                }
                case RATE_SHOW: {
                    final Rating rating = Rating.fromValue(mArgs.getString(InitBundle.RATING));
                    r = manager.rateService().show(new RateService.ShowRating(
                            showTvdbId, rating
                    ));
                    break;
                }
                case SHOUT: {
                    final String shout = mArgs.getString(InitBundle.MESSAGE);
                    final boolean isSpoiler = mArgs.getBoolean(InitBundle.ISSPOILER);

                    if (episode == 0) {
                        r = manager.commentService().show(new CommentService.ShowComment(
                                showTvdbId, shout
                        ).spoiler(isSpoiler));
                    } else {
                        r = manager.commentService().episode(new CommentService.EpisodeComment(
                                showTvdbId, season, episode, shout
                        ).spoiler(isSpoiler));
                    }
                    break;
                }
                case WATCHLIST_MOVIE: {
                    final int tmdbId = mArgs.getInt(InitBundle.TMDB_ID);
                    manager.movieService().watchlist(new MovieService.Movies(
                            new MovieService.SeenMovie(tmdbId)
                    ));
                    // In case of failure this will just return an exception, so
                    // we need to construct our own response
                    r = new Response();
                    r.status = TraktStatus.SUCCESS;
                    r.message = mContext.getString(R.string.watchlist_added);
                    break;
                }
                case UNWATCHLIST_MOVIE: {
                    final int tmdbId = mArgs.getInt(InitBundle.TMDB_ID);
                    manager.movieService().unwatchlist(new MovieService.Movies(
                            new MovieService.SeenMovie(tmdbId)
                    ));
                    // In case of failure this will just return an exception, so
                    // we need to construct our own response
                    r = new Response();
                    r.status = TraktStatus.SUCCESS;
                    r.message = mContext.getString(R.string.watchlist_removed);
                    break;
                }
                default:
                    break;
            }

            return r;
        } catch (RetrofitError e) {
            Utils.trackExceptionAndLog(mContext, TAG, e);
            Response r = new Response();
            r.status = TraktStatus.FAILURE;
            r.error = mContext.getString(R.string.trakt_error_general);
            return r;
        }
    }

    @Override
    protected void onPostExecute(Response r) {
        if (r != null) {
            if (TraktStatus.SUCCESS.equals(r.status)) {
                // all good

                switch (mAction) {
                    case CHECKIN_EPISODE:
                    case CHECKIN_MOVIE:
                        Toast.makeText(mContext, r.message, Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        Toast.makeText(mContext,
                                r.message + " " + mContext.getString(R.string.ontrakt),
                                Toast.LENGTH_SHORT).show();
                        break;
                }

                EventBus.getDefault().post(new TraktActionCompleteEvent(mArgs, true));
                if (mListener != null) {
                    mListener.onTraktActionComplete(mArgs, true);
                }

            } else if (TraktStatus.FAILURE.equals(r.status)) {
                if (mAction == TraktAction.CHECKIN_EPISODE
                        || mAction == TraktAction.CHECKIN_MOVIE) {
                    CheckinResponse checkinResponse = (CheckinResponse) r;
                    if (checkinResponse.wait != 0) {
                        // looks like a check in is already in progress
                        if (mListener != null) {
                            mListener.onCheckinBlocked(mArgs, checkinResponse.wait);
                        }
                        return;
                    }
                }

                // well, something went wrong
                Toast.makeText(mContext, r.error, Toast.LENGTH_LONG).show();

                if (mListener != null) {
                    mListener.onTraktActionComplete(mArgs, false);
                }

            }
        } else {
            // notify that our first run completed, however due to invalid
            // credentials we have not done anything
            if (mListener != null) {
                mListener.onTraktActionComplete(mArgs, false);
            }
        }
    }
}
