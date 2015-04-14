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
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.enums.TraktStatus;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.traktapi.CheckinResponse;
import com.battlelancer.seriesguide.traktapi.Response;
import com.battlelancer.seriesguide.ui.ConnectTraktActivity;
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
import com.uwetrottmann.trakt.v2.entities.SyncMovie;
import com.uwetrottmann.trakt.v2.exceptions.CheckinInProgressException;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import com.uwetrottmann.trakt.v2.services.Checkin;
import com.uwetrottmann.trakt.v2.services.Comments;
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

        String MESSAGE = "message";

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
                case COMMENT: {
                    return doCommentAction(trakt.comments());
                }
            }
        } catch (RetrofitError e) {
            Timber.e(e, mAction.toString() + " failed");
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
        Response r = new Response();

        try {
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

            r.status = TraktStatus.SUCCESS;
            r.message = mContext.getString(R.string.checkin_success_trakt,
                    mArgs.getString(InitBundle.TITLE));
        } catch (RetrofitError e) {
            // check if item user wants to check into does exist on trakt
            if (e.getKind() == RetrofitError.Kind.HTTP && e.getResponse().getStatus() == 404) {
                r.status = TraktStatus.FAILURE;
                r.error = mContext.getString(R.string.checkin_error_not_exists);
            } else {
                throw e;
            }
        }

        return r;
    }

    private Response doCommentAction(Comments traktComments) throws OAuthUnauthorizedException {
        Response r = new Response();

        try {
            // post comment
            Comment postedComment = traktComments.post(buildComment());

            if (postedComment != null && postedComment.id != null) {
                r.status = TraktStatus.SUCCESS;
            } else {
                r.status = TraktStatus.FAILURE;
                r.error = mContext.getString(R.string.trakt_error_general);
            }
        } catch (RetrofitError e) {
            // check if comment failed validation
            if (e.getKind() == RetrofitError.Kind.HTTP && e.getResponse().getStatus() == 422) {
                r.status = TraktStatus.FAILURE;
                r.error = mContext.getString(R.string.shout_invalid);
            } else {
                throw e;
            }
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
