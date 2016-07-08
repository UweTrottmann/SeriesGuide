package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.ui.ConnectTraktActivity;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.CheckinError;
import com.uwetrottmann.trakt5.entities.Comment;
import com.uwetrottmann.trakt5.entities.Episode;
import com.uwetrottmann.trakt5.entities.EpisodeCheckin;
import com.uwetrottmann.trakt5.entities.EpisodeIds;
import com.uwetrottmann.trakt5.entities.Movie;
import com.uwetrottmann.trakt5.entities.MovieCheckin;
import com.uwetrottmann.trakt5.entities.MovieIds;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.entities.ShowIds;
import com.uwetrottmann.trakt5.entities.SyncEpisode;
import com.uwetrottmann.trakt5.entities.SyncMovie;
import com.uwetrottmann.trakt5.services.Comments;
import de.greenrobot.event.EventBus;
import java.io.IOException;
import org.joda.time.DateTime;

public class TraktTask extends AsyncTask<Void, Void, TraktTask.TraktResponse> {

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

    /**
     * trakt response status class.
     */
    public static class TraktResponse {
        public boolean succesful;
        public String message;

        public TraktResponse(boolean successful, String message) {
            this.succesful = successful;
            this.message = message;
        }
    }

    /**
     * trakt checkin response status class.
     */
    public static class CheckinBlockedResponse extends TraktResponse {
        public int waitTimeMin;

        public CheckinBlockedResponse(int waitTimeMin) {
            super(false, null);
            this.waitTimeMin = waitTimeMin;
        }
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
    protected TraktResponse doInBackground(Void... params) {
        // we need this value in onPostExecute, so preserve it here
        mAction = TraktAction.valueOf(mArgs.getString(InitBundle.TRAKTACTION));

        // check for network connection
        if (!AndroidUtils.isNetworkConnected(mContext)) {
            return new TraktResponse(false, mContext.getString(R.string.offline));
        }

        // check for credentials
        if (!TraktCredentials.get(mContext).hasCredentials()) {
            return new TraktResponse(false, mContext.getString(R.string.trakt_error_credentials));
        }

        // get trakt
        TraktV2 trakt = ServiceUtils.getTrakt(mContext);

        // last chance to abort
        if (isCancelled()) {
            return null;
        }

        switch (mAction) {
            case CHECKIN_EPISODE:
            case CHECKIN_MOVIE: {
                return doCheckInAction(ServiceUtils.getTrakt(mContext));
            }
            case COMMENT: {
                return doCommentAction(trakt.comments());
            }
            default:
                return null;
        }
    }

    private TraktResponse doCheckInAction(TraktV2 trakt) {
        try {
            retrofit2.Response response;
            String message = mArgs.getString(InitBundle.MESSAGE);
            switch (mAction) {
                case CHECKIN_EPISODE: {
                    int episodeTvdbId = mArgs.getInt(InitBundle.EPISODE_TVDBID);
                    EpisodeCheckin checkin = new EpisodeCheckin.Builder(
                            new SyncEpisode().id(EpisodeIds.tvdb(episodeTvdbId)), APP_VERSION, null)
                            .message(message)
                            .build();

                    response = trakt.checkin().checkin(checkin).execute();
                    break;
                }
                case CHECKIN_MOVIE: {
                    int movieTmdbId = mArgs.getInt(InitBundle.MOVIE_TMDB_ID);
                    MovieCheckin checkin = new MovieCheckin.Builder(
                            new SyncMovie().id(MovieIds.tmdb(movieTmdbId)), APP_VERSION, null)
                            .message(message)
                            .build();

                    response = trakt.checkin().checkin(checkin).execute();
                    break;
                }
                default:
                    throw new IllegalArgumentException("check-in action unknown.");
            }

            if (response.isSuccessful()) {
                return new TraktResponse(true, mContext.getString(R.string.checkin_success_trakt,
                        mArgs.getString(InitBundle.TITLE)));
            } else {
                // check if the user wants to check-in, but there is already a check-in in progress
                CheckinError checkinError = trakt.checkForCheckinError(response);
                if (checkinError != null) {
                    DateTime expiresAt = checkinError.expires_at;
                    int waitTimeMin = expiresAt == null ? -1
                            : (int) ((expiresAt.getMillis() - System.currentTimeMillis()) / 1000);
                    return new CheckinBlockedResponse(waitTimeMin);
                }
                // check if item does not exist on trakt (yet)
                else if (response.code() == 404) {
                    return new TraktResponse(false,
                            mContext.getString(R.string.trakt_error_not_exists));
                } else if (SgTrakt.isUnauthorized(mContext, response)) {
                    return new TraktResponse(false,
                            mContext.getString(R.string.trakt_error_credentials));
                } else {
                    SgTrakt.trackFailedRequest(mContext, "check-in", response);
                }
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(mContext, "check-in", e);
        }

        // return generic failure message
        return new TraktResponse(false, mContext.getString(R.string.trakt_error_general));
    }

    private TraktResponse doCommentAction(Comments traktComments) {
        try {
            // post comment
            retrofit2.Response<Comment> response = traktComments.post(buildComment()).execute();
            if (response.isSuccessful()) {
                Comment postedComment = response.body();
                if (postedComment.id != null) {
                    return new TraktResponse(true, null);
                }
            } else {
                // check if comment failed validation or item does not exist on trakt
                if (response.code() == 422) {
                    return new TraktResponse(false, mContext.getString(R.string.shout_invalid));
                } else if (response.code() == 404) {
                    return new TraktResponse(false, mContext.getString(R.string.shout_invalid));
                } else if (SgTrakt.isUnauthorized(mContext, response)) {
                    return new TraktResponse(false,
                            mContext.getString(R.string.trakt_error_credentials));
                } else {
                    SgTrakt.trackFailedRequest(mContext, "post comment", response);
                }
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(mContext, "post comment", e);
        }

        // return generic failure message
        return new TraktResponse(false, mContext.getString(R.string.trakt_error_general));
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
    protected void onPostExecute(TraktResponse r) {
        if (r == null) {
            // unknown error
            return;
        }

        if (r.succesful) {
            // all good
            EventBus.getDefault().post(new TraktActionCompleteEvent(mAction, true, r.message));
        } else {
            // special handling of blocked check-ins
            if (mAction == TraktAction.CHECKIN_EPISODE
                    || mAction == TraktAction.CHECKIN_MOVIE) {
                if (r instanceof CheckinBlockedResponse) {
                    CheckinBlockedResponse checkinBlockedResponse = (CheckinBlockedResponse) r;
                    if (checkinBlockedResponse.waitTimeMin > 0) {
                        // looks like a check in is already in progress
                        EventBus.getDefault().post(
                                new TraktCheckInBlockedEvent(mArgs, checkinBlockedResponse.waitTimeMin));
                        return;
                    }
                }
            }

            // well, something went wrong
            EventBus.getDefault().post(new TraktActionCompleteEvent(mAction, false, r.message));
        }
    }
}
