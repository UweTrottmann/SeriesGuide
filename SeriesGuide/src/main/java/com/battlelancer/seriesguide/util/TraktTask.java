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
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.enums.TraktStatus;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.ConnectTraktActivity;
import com.jakewharton.trakt.entities.CheckinResponse;
import com.jakewharton.trakt.entities.Response;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.Comment;
import com.uwetrottmann.trakt.v2.entities.Episode;
import com.uwetrottmann.trakt.v2.entities.EpisodeCheckin;
import com.uwetrottmann.trakt.v2.entities.EpisodeIds;
import com.uwetrottmann.trakt.v2.entities.Movie;
import com.uwetrottmann.trakt.v2.entities.MovieCheckin;
import com.uwetrottmann.trakt.v2.entities.MovieIds;
import com.uwetrottmann.trakt.v2.entities.Show;
import com.uwetrottmann.trakt.v2.entities.ShowIds;
import com.uwetrottmann.trakt.v2.entities.SyncEpisode;
import com.uwetrottmann.trakt.v2.entities.SyncErrors;
import com.uwetrottmann.trakt.v2.entities.SyncItems;
import com.uwetrottmann.trakt.v2.entities.SyncMovie;
import com.uwetrottmann.trakt.v2.entities.SyncResponse;
import com.uwetrottmann.trakt.v2.entities.SyncSeason;
import com.uwetrottmann.trakt.v2.entities.SyncShow;
import com.uwetrottmann.trakt.v2.enums.Rating;
import com.uwetrottmann.trakt.v2.exceptions.CheckinInProgressException;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import com.uwetrottmann.trakt.v2.services.Checkin;
import com.uwetrottmann.trakt.v2.services.Comments;
import com.uwetrottmann.trakt.v2.services.Sync;
import de.greenrobot.event.EventBus;
import retrofit.RetrofitError;
import timber.log.Timber;

public class TraktTask extends AsyncTask<Void, Void, Response> {

    private static final String APP_VERSION = "SeriesGuide " + BuildConfig.VERSION_NAME;

    private Bundle mArgs;

    private final Context mContext;

    private TraktAction mAction;

    public interface InitBundle {

        String TRAKTACTION = "traktaction";

        String TITLE = "title";

        String MOVIE_TMDB_ID = "movie-tmdbid";

        String SHOW_TVDBID = "show-tvdbid";

        String EPISODE_TVDBID = "episode-tvdbid";

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
     * Initial constructor. Call <b>one</b> of the setup-methods like {@link #commentEpisode(int,
     * String, boolean)} afterwards.<br> <br> Make sure the user has valid trakt credentials (check
     * with {@link com.battlelancer.seriesguide.settings.TraktCredentials#hasCredentials()} and then
     * possibly launch {@link ConnectTraktActivity}) or execution will fail.
     */
    public TraktTask(Context context) {
        mContext = context.getApplicationContext();
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
    public TraktTask checkInEpisode(int episodeTvdbId, String title, String message) {
        mArgs.putString(InitBundle.TRAKTACTION, TraktAction.CHECKIN_EPISODE.name());
        mArgs.putInt(InitBundle.EPISODE_TVDBID, episodeTvdbId);
        mArgs.putString(InitBundle.TITLE, title);
        mArgs.putString(InitBundle.MESSAGE, message);
        return this;
    }

    /**
     * Check into an episode. Optionally provide a checkin message.
     */
    public TraktTask checkInMovie(int tmdbId, String title, String message) {
        mArgs.putString(InitBundle.TRAKTACTION, TraktAction.CHECKIN_MOVIE.name());
        mArgs.putInt(InitBundle.MOVIE_TMDB_ID, tmdbId);
        mArgs.putString(InitBundle.TITLE, title);
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
     * Post a comment for a show.
     */
    public TraktTask commentShow(int showTvdbid, String comment, boolean isSpoiler) {
        mArgs.putString(InitBundle.TRAKTACTION, TraktAction.COMMENT.name());
        mArgs.putInt(InitBundle.SHOW_TVDBID, showTvdbid);
        mArgs.putString(InitBundle.MESSAGE, comment);
        mArgs.putBoolean(InitBundle.ISSPOILER, isSpoiler);
        return this;
    }

    /**
     * Post a comment for an episode.
     */
    public TraktTask commentEpisode(int episodeTvdbid, String comment, boolean isSpoiler) {
        mArgs.putString(InitBundle.TRAKTACTION, TraktAction.COMMENT.name());
        mArgs.putInt(InitBundle.EPISODE_TVDBID, episodeTvdbid);
        mArgs.putString(InitBundle.MESSAGE, comment);
        mArgs.putBoolean(InitBundle.ISSPOILER, isSpoiler);
        return this;
    }

    /**
     * Post a comment for a movie.
     */
    public TraktTask commentMovie(int movieTmdbId, String comment, boolean isSpoiler) {
        mArgs.putString(InitBundle.TRAKTACTION, TraktAction.COMMENT.name());
        mArgs.putInt(InitBundle.MOVIE_TMDB_ID, movieTmdbId);
        mArgs.putString(InitBundle.MESSAGE, comment);
        mArgs.putBoolean(InitBundle.ISSPOILER, isSpoiler);
        return this;
    }

    /**
     * Rate an episode.
     */
    public TraktTask rateEpisode(int episodeTvdbId, Rating rating) {
        mArgs.putString(InitBundle.TRAKTACTION, TraktAction.RATE_EPISODE.name());
        mArgs.putInt(InitBundle.RATING, rating.value);
        mArgs.putInt(InitBundle.EPISODE_TVDBID, episodeTvdbId);
        return this;
    }

    /**
     * Rate a show.
     */
    public TraktTask rateShow(int showTvdbId, Rating rating) {
        mArgs.putString(InitBundle.TRAKTACTION, TraktAction.RATE_SHOW.name());
        mArgs.putInt(InitBundle.RATING, rating.value);
        mArgs.putInt(InitBundle.SHOW_TVDBID, showTvdbId);
        return this;
    }

    /**
     * Rate a movie.
     */
    public TraktTask rateMovie(int movieTmdbId, Rating rating) {
        mArgs.putString(InitBundle.TRAKTACTION, TraktAction.RATE_MOVIE.name());
        mArgs.putInt(InitBundle.RATING, rating.value);
        mArgs.putInt(InitBundle.MOVIE_TMDB_ID, movieTmdbId);
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
        TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(mContext);
        if (trakt == null) {
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
                    return doCheckInAction(trakt.checkin());
                }
                case RATE_EPISODE:
                case RATE_SHOW:
                case RATE_MOVIE: {
                    return doRatingAction(trakt.sync());
                }
                case COLLECTION_ADD_MOVIE:
                case COLLECTION_REMOVE_MOVIE:
                case WATCHLIST_MOVIE:
                case UNWATCHLIST_MOVIE:
                case WATCHED_MOVIE:
                case UNWATCHED_MOVIE: {
                    return doMovieAction(trakt.sync());
                }
                case COMMENT: {
                    return doCommentAction(trakt.comments());
                }
            }
        } catch (RetrofitError e) {
            Timber.e(e, mAction.toString() + " failed " + e.getUrl());
            r = new Response();
            r.status = TraktStatus.FAILURE;
            r.error = mContext.getString(R.string.trakt_error_general);
        } catch (OAuthUnauthorizedException e) {
            TraktCredentials.get(mContext).setCredentialsInvalid();
            r = new Response();
            r.status = TraktStatus.FAILURE;
            r.error = mContext.getString(R.string.trakt_error_credentials);
        } catch (CheckinInProgressException e) {
            CheckinResponse checkinResponse = new CheckinResponse();
            checkinResponse.status = TraktStatus.FAILURE;
            checkinResponse.wait = (int) (
                    (e.getExpiresAt().getMillis() - System.currentTimeMillis()) / 1000);
            r = checkinResponse;
        }

        return r;
    }

    private Response doCheckInAction(Checkin traktCheckin)
            throws CheckinInProgressException, OAuthUnauthorizedException {
        String message = mArgs.getString(InitBundle.MESSAGE);
        switch (mAction) {
            case CHECKIN_EPISODE: {
                int episodeTvdbId = mArgs.getInt(InitBundle.EPISODE_TVDBID);
                EpisodeCheckin checkin = new EpisodeCheckin.Builder(
                        new SyncEpisode().id(EpisodeIds.tvdb(episodeTvdbId)), APP_VERSION, null)
                        .message(message)
                        .build();

                traktCheckin.checkin(checkin);
                break;
            }
            case CHECKIN_MOVIE: {
                int movieTmdbId = mArgs.getInt(InitBundle.MOVIE_TMDB_ID);
                MovieCheckin checkin = new MovieCheckin.Builder(
                        new SyncMovie().id(MovieIds.tmdb(movieTmdbId)), APP_VERSION, null)
                        .message(message)
                        .build();

                traktCheckin.checkin(checkin);
                break;
            }
        }

        Response r = new Response();
        r.status = TraktStatus.SUCCESS;
        r.message = mContext.getString(R.string.checkin_success_trakt,
                mArgs.getString(InitBundle.TITLE));
        return r;
    }

    private Response doRatingAction(Sync traktSync) throws OAuthUnauthorizedException {
        Rating rating = Rating.fromValue(mArgs.getInt(InitBundle.RATING));
        SyncItems ratedItems = new SyncItems();

        switch (mAction) {
            case RATE_EPISODE: {
                int episodeTvdbId = mArgs.getInt(InitBundle.EPISODE_TVDBID);
                int season = -1;
                int episode = -1;
                int showTvdbId = -1;
                Cursor query = mContext.getContentResolver()
                        .query(SeriesGuideContract.Episodes.buildEpisodeUri(episodeTvdbId),
                                new String[] {
                                        SeriesGuideContract.Episodes.SEASON,
                                        SeriesGuideContract.Episodes.NUMBER,
                                        SeriesGuideContract.Shows.REF_SHOW_ID }, null, null, null);
                if (query != null) {
                    if (query.moveToFirst()) {
                        season = query.getInt(0);
                        episode = query.getInt(1);
                        showTvdbId = query.getInt(2);
                    }
                    query.close();
                }

                if (season == -1 || episode == -1 || showTvdbId == -1) {
                    Response r = new Response();
                    r.status = TraktStatus.FAILURE;
                    r.error = mContext.getString(R.string.trakt_error);
                    return r;
                }

                ratedItems.shows(new SyncShow().id(ShowIds.tvdb(showTvdbId))
                        .seasons(new SyncSeason().number(season)
                                .episodes(new SyncEpisode().number(episode)
                                        .rating(rating))));
                break;
            }
            case RATE_SHOW: {
                int showTvdbId = mArgs.getInt(InitBundle.SHOW_TVDBID);
                ratedItems.shows(new SyncShow().id(ShowIds.tvdb(showTvdbId)).rating(rating));
                break;
            }
            case RATE_MOVIE: {
                int tmdbId = mArgs.getInt(InitBundle.MOVIE_TMDB_ID);
                ratedItems.movies(new SyncMovie().id(MovieIds.tmdb(tmdbId)).rating(rating));
                break;
            }
        }

        // upload ratings
        SyncResponse response = traktSync.addRatings(ratedItems);

        // handle errors
        Response r = new Response();
        r.status = TraktStatus.SUCCESS;
        r.message = mContext.getString(R.string.trakt_submitqueued);

        if (response == null) {
            r.status = TraktStatus.FAILURE;
            r.error = mContext.getString(R.string.trakt_error_general);
        } else {
            SyncErrors notFound = response.not_found;
            if (notFound != null) {
                if ((notFound.movies != null && notFound.movies.size() != 0)
                        || (notFound.shows != null && notFound.shows.size() != 0)
                        || (notFound.episodes != null && notFound.episodes.size() != 0)) {
                    r.status = TraktStatus.FAILURE;
                    r.error = mContext.getString(R.string.trakt_error_general);
                }
            }
        }

        return r;
    }

    private Response doMovieAction(Sync traktSync) throws OAuthUnauthorizedException {
        int tmdbId = mArgs.getInt(InitBundle.MOVIE_TMDB_ID);
        SyncItems items = new SyncItems().movies(new SyncMovie().id(MovieIds.tmdb(tmdbId)));

        SyncResponse response = null;
        Response r = new Response();
        switch (mAction) {
            case COLLECTION_ADD_MOVIE: {
                response = traktSync.addItemsToCollection(items);
                // always returns success, even if movie is already in collection
                r.message = mContext.getString(R.string.action_collection_added);
                break;
            }
            case COLLECTION_REMOVE_MOVIE: {
                response = traktSync.deleteItemsFromCollection(items);
                // always returns success, even if movie was never in collection
                r.message = mContext.getString(R.string.action_collection_removed);
                break;
            }
            case WATCHED_MOVIE: {
                response = traktSync.addItemsToWatchedHistory(items);
                r.message = mContext.getString(R.string.action_watched);
                break;
            }
            case UNWATCHED_MOVIE: {
                response = traktSync.deleteItemsFromWatchedHistory(items);
                r.message = mContext.getString(R.string.action_unwatched);
                break;
            }
            case WATCHLIST_MOVIE: {
                response = traktSync.addItemsToWatchlist(items);
                // always returns success, even if movie is already on watchlist
                r.message = mContext.getString(R.string.watchlist_added);
                break;
            }
            case UNWATCHLIST_MOVIE: {
                response = traktSync.deleteItemsFromWatchlist(items);
                // always returns success, even if movie is not on watchlist anymore
                r.message = mContext.getString(R.string.watchlist_removed);
                break;
            }
        }

        if (response == null ||
                (response.not_found != null && response.not_found.movies != null
                        && response.not_found.movies.size() != 0)) {
            r.status = TraktStatus.FAILURE;
            r.error = mContext.getString(R.string.trakt_error_general);
        } else {
            r.status = TraktStatus.SUCCESS;
        }

        return r;
    }

    private Response doCommentAction(Comments traktComments) throws OAuthUnauthorizedException {
        // post comment
        Comment postedComment = traktComments.post(buildComment());

        Response r = new Response();
        if (postedComment != null && postedComment.id != null) {
            r.status = TraktStatus.SUCCESS;
        } else {
            r.status = TraktStatus.FAILURE;
            r.error = mContext.getString(R.string.trakt_error_general);
        }

        return r;
    }

    private Comment buildComment() {
        Comment comment = new Comment();
        comment.comment = mArgs.getString(InitBundle.MESSAGE);
        comment.spoiler = mArgs.getBoolean(InitBundle.ISSPOILER);

        // as determined by "science", episode comments are most likely, so check for them first
        // episode?
        int episodeTvdbId = mArgs.getInt(InitBundle.EPISODE_TVDBID);
        if (episodeTvdbId != 0) {
            comment.episode = new Episode();
            comment.episode.ids = EpisodeIds.tvdb(episodeTvdbId);
            return comment;
        }

        // show?
        int showTvdbId = mArgs.getInt(InitBundle.SHOW_TVDBID);
        if (showTvdbId != 0) {
            comment.show = new Show();
            comment.show.ids = ShowIds.tvdb(showTvdbId);
            return comment;
        }

        // movie!
        int movieTmdbId = mArgs.getInt(InitBundle.MOVIE_TMDB_ID);
        comment.movie = new Movie();
        comment.movie.ids = MovieIds.tmdb(movieTmdbId);
        return comment;
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
                    if (checkinResponse.wait > 0) {
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
