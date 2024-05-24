// SPDX-License-Identifier: Apache-2.0
// Copyright 2023-2024 Uwe Trottmann

package com.battlelancer.seriesguide.traktapi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.shows.database.SgEpisode2Numbers;
import com.battlelancer.seriesguide.util.Errors;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.CheckinError;
import com.uwetrottmann.trakt5.entities.EpisodeCheckin;
import com.uwetrottmann.trakt5.entities.MovieCheckin;
import com.uwetrottmann.trakt5.entities.MovieIds;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.entities.ShowIds;
import com.uwetrottmann.trakt5.entities.SyncEpisode;
import com.uwetrottmann.trakt5.entities.SyncMovie;
import org.greenrobot.eventbus.EventBus;
import org.threeten.bp.OffsetDateTime;
import timber.log.Timber;

/** @noinspection deprecation*/
// AsyncTask is still fine to use, only new code should better alternatives.
public class TraktTask extends AsyncTask<Void, Void, TraktTask.TraktResponse> {

    private static final String APP_VERSION = "SeriesGuide " + BuildConfig.VERSION_NAME;

    public interface InitBundle {

        String TRAKTACTION = "traktaction";

        String TITLE = "title";

        String MOVIE_TMDB_ID = "movie-tmdbid";

        String EPISODE_ID = "episode-id";

        String MESSAGE = "message";
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

        public TraktAction traktAction;
        public boolean wasSuccessful;
        public String message;

        public TraktActionCompleteEvent(TraktAction traktAction, boolean wasSuccessful,
                String message) {
            this.traktAction = traktAction;
            this.wasSuccessful = wasSuccessful;
            this.message = message;
        }

        /**
         * Displays status toasts dependent on the result of the trakt action performed.
         */
        public void handle(Context context) {
            if (TextUtils.isEmpty(message)) {
                return;
            }

            if (!wasSuccessful) {
                // display error toast
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                return;
            }

            // display success toast
            switch (traktAction) {
                case CHECKIN_EPISODE:
                case CHECKIN_MOVIE:
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(context,
                            message + " " + context.getString(R.string.ontrakt),
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

    @SuppressLint("StaticFieldLeak") private final Context context;
    private final Bundle args;
    private TraktAction action;

    /**
     * Initial constructor. Call <b>one</b> of the setup-methods like {@link #checkInEpisode}
     * afterwards.<br> <br> Make sure the user has valid trakt credentials (check with
     * {@link TraktCredentials#hasCredentials()} and then possibly launch
     * {@link ConnectTraktActivity}) or execution will fail.
     */
    public TraktTask(Context context) {
        this(context, new Bundle());
    }

    /**
     * Fast constructor, allows passing of an already pre-built {@code args} {@link Bundle}.<br>
     * <br> Make sure the user has valid trakt credentials (check with {@link
     * TraktCredentials#hasCredentials()} and then possibly launch {@link ConnectTraktActivity}) or
     * execution will fail.
     */
    public TraktTask(Context context, Bundle args) {
        this.context = context.getApplicationContext();
        this.args = args;
    }

    /**
     * Check into an episode. Optionally provide a checkin message.
     */
    public TraktTask checkInEpisode(long episodeId, String title, String message) {
        args.putString(InitBundle.TRAKTACTION, TraktAction.CHECKIN_EPISODE.name());
        args.putLong(InitBundle.EPISODE_ID, episodeId);
        args.putString(InitBundle.TITLE, title);
        args.putString(InitBundle.MESSAGE, message);
        return this;
    }

    /**
     * Check into an episode. Optionally provide a checkin message.
     */
    public TraktTask checkInMovie(int tmdbId, String title, String message) {
        args.putString(InitBundle.TRAKTACTION, TraktAction.CHECKIN_MOVIE.name());
        args.putInt(InitBundle.MOVIE_TMDB_ID, tmdbId);
        args.putString(InitBundle.TITLE, title);
        args.putString(InitBundle.MESSAGE, message);
        return this;
    }

    @Override
    protected TraktResponse doInBackground(Void... params) {
        // we need this value in onPostExecute, so preserve it here
        action = TraktAction.valueOf(args.getString(InitBundle.TRAKTACTION));

        // check for network connection
        if (!AndroidUtils.isNetworkConnected(context)) {
            return new TraktResponse(false, context.getString(R.string.offline));
        }

        // check for credentials
        if (!TraktCredentials.get(context).hasCredentials()) {
            return new TraktResponse(false, context.getString(R.string.trakt_error_credentials));
        }

        // last chance to abort
        if (isCancelled()) {
            return null;
        }

        switch (action) {
            case CHECKIN_EPISODE:
            case CHECKIN_MOVIE: {
                return doCheckInAction();
            }
            default:
                return null;
        }
    }

    private TraktResponse doCheckInAction() {
        TraktV2 trakt = SgApp.getServicesComponent(context).trakt();
        try {
            retrofit2.Response<?> response;
            String message = args.getString(InitBundle.MESSAGE);
            switch (action) {
                case CHECKIN_EPISODE: {
                    // Check in using show Trakt ID
                    // and season and episode number (likely most reliable).
                    long episodeId = args.getLong(InitBundle.EPISODE_ID);
                    SgRoomDatabase database = SgRoomDatabase.getInstance(context);
                    SgEpisode2Numbers episode = database.sgEpisode2Helper()
                            .getEpisodeNumbers(episodeId);
                    if (episode == null) {
                        Timber.e("Failed to get episode %d", episodeId);
                        return buildErrorResponse();
                    }
                    Integer showTraktId = SgApp.getServicesComponent(context)
                            .showTools()
                            .getShowTraktId(episode.getShowId());
                    if (showTraktId == null) {
                        Timber.e("Failed to get show %d", episode.getShowId());
                        return buildErrorResponse();
                    }
                    SyncEpisode traktEpisode = new SyncEpisode()
                            .season(episode.getSeason())
                            .number(episode.getEpisodenumber());
                    Show traktShow = new Show();
                    traktShow.ids = ShowIds.trakt(showTraktId);
                    EpisodeCheckin checkin = new EpisodeCheckin.Builder(
                            traktEpisode,
                            APP_VERSION, null)
                            .show(traktShow)
                            .message(message)
                            .build();
                    response = trakt.checkin().checkin(checkin).execute();
                    break;
                }
                case CHECKIN_MOVIE: {
                    int movieTmdbId = args.getInt(InitBundle.MOVIE_TMDB_ID);
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
                return new TraktResponse(true, context.getString(R.string.checkin_success_trakt,
                        args.getString(InitBundle.TITLE)));
            } else {
                // check if the user wants to check-in, but there is already a check-in in progress
                CheckinError checkinError = trakt.checkForCheckinError(response);
                if (checkinError != null) {
                    OffsetDateTime expiresAt = checkinError.expires_at;
                    int waitTimeMin = expiresAt == null ? -1
                            : (int) ((expiresAt.toInstant().toEpochMilli()
                                    - System.currentTimeMillis()) / 1000);
                    return new CheckinBlockedResponse(waitTimeMin);
                }
                // check if item does not exist on trakt (yet)
                else if (response.code() == 404) {
                    return new TraktResponse(false,
                            context.getString(R.string.trakt_error_not_exists));
                } else if (SgTrakt.isUnauthorized(context, response)) {
                    return new TraktResponse(false,
                            context.getString(R.string.trakt_error_credentials));
                } else {
                    Errors.logAndReport("check-in", response);
                }
            }
        } catch (Exception e) {
            Errors.logAndReport("check-in", e);
        }

        // return generic failure message
        return buildErrorResponse();
    }

    private TraktResponse buildErrorResponse() {
        return new TraktResponse(false,
                context.getString(R.string.api_error_generic, context.getString(R.string.trakt)));
    }

    @Override
    protected void onPostExecute(TraktResponse r) {
        if (r == null) {
            // unknown error
            return;
        }

        if (r.succesful) {
            // all good
            EventBus.getDefault().post(new TraktActionCompleteEvent(action, true, r.message));
        } else {
            // special handling of blocked check-ins
            if (action == TraktAction.CHECKIN_EPISODE
                    || action == TraktAction.CHECKIN_MOVIE) {
                if (r instanceof CheckinBlockedResponse) {
                    CheckinBlockedResponse checkinBlockedResponse = (CheckinBlockedResponse) r;
                    if (checkinBlockedResponse.waitTimeMin > 0) {
                        // looks like a check in is already in progress
                        EventBus.getDefault().post(
                                new TraktCheckInBlockedEvent(args,
                                        checkinBlockedResponse.waitTimeMin));
                        return;
                    }
                }
            }

            // well, something went wrong
            EventBus.getDefault().post(new TraktActionCompleteEvent(action, false, r.message));
        }
    }
}
