package com.battlelancer.seriesguide.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.jobs.episodes.JobAction;
import com.battlelancer.seriesguide.modules.ServicesComponent;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
import com.battlelancer.seriesguide.sync.NetworkJobProcessor;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.MovieIds;
import com.uwetrottmann.trakt5.entities.SyncErrors;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncMovie;
import com.uwetrottmann.trakt5.entities.SyncResponse;
import com.uwetrottmann.trakt5.services.Sync;
import java.io.IOException;
import org.threeten.bp.Instant;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneOffset;
import retrofit2.Call;
import retrofit2.Response;

public class TraktMovieJob extends BaseNetworkEpisodeJob {

    private final long actionAtMs;

    public TraktMovieJob(JobAction action, SgJobInfo jobInfo, long actionAtMs) {
        super(action, jobInfo);
        this.actionAtMs = actionAtMs;
    }

    @NonNull
    @Override
    public NetworkJobProcessor.JobResult execute(Context context) {
        return buildResult(context, upload(context));
    }

    private int upload(Context context) {
        if (!TraktCredentials.get(context).hasCredentials()) {
            return NetworkJob.ERROR_TRAKT_AUTH;
        }

        SyncMovie movie = new SyncMovie().id(MovieIds.tmdb(jobInfo.movieTmdbId()));

        // send time of action to avoid adding duplicate plays/collection events at trakt
        // if this job re-runs due to failure, but trakt already applied changes (it happens)
        // also if execution is delayed to due being offline this will ensure
        // the actual action time is stored at trakt
        Instant instant = Instant.ofEpochMilli(actionAtMs);
        OffsetDateTime actionAtDateTime = instant.atOffset(ZoneOffset.UTC);
        // only send timestamp if adding, not if removing to save data
        // note: timestamp currently not supported for watchlist action
        if (action == JobAction.MOVIE_COLLECTION_ADD) {
            movie.collectedAt(actionAtDateTime);
        } else if (action == JobAction.MOVIE_WATCHED_SET) {
            movie.watchedAt(actionAtDateTime);
        }

        SyncItems items = new SyncItems().movies(movie);

        // determine network call
        String errorLabel;
        Call<SyncResponse> call;
        ServicesComponent component = SgApp.getServicesComponent(context);
        TraktV2 trakt = component.trakt();
        Sync traktSync = component.traktSync();
        switch (action) {
            case MOVIE_COLLECTION_ADD:
                errorLabel = "add movie to collection";
                call = traktSync.addItemsToCollection(items);
                break;
            case MOVIE_COLLECTION_REMOVE:
                errorLabel = "remove movie from collection";
                call = traktSync.deleteItemsFromCollection(items);
                break;
            case MOVIE_WATCHLIST_ADD:
                errorLabel = "add movie to watchlist";
                call = traktSync.addItemsToWatchlist(items);
                break;
            case MOVIE_WATCHLIST_REMOVE:
                errorLabel = "remove movie from watchlist";
                call = traktSync.deleteItemsFromWatchlist(items);
                break;
            case MOVIE_WATCHED_SET:
                errorLabel = "set movie watched";
                call = traktSync.addItemsToWatchedHistory(items);
                break;
            case MOVIE_WATCHED_REMOVE:
                errorLabel = "set movie not watched";
                call = traktSync.deleteItemsFromWatchedHistory(items);
                break;
            default:
                throw new IllegalArgumentException("Action " + action + " not supported.");
        }

        // execute call
        try {
            Response<SyncResponse> response = call.execute();
            if (response.isSuccessful()) {
                // check if any items were not found
                if (!isSyncSuccessful(response.body())) {
                    return NetworkJob.ERROR_TRAKT_NOT_FOUND;
                }
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return NetworkJob.ERROR_TRAKT_AUTH;
                }
                SgTrakt.trackFailedRequest(context, trakt, errorLabel, response);

                int code = response.code();
                if (code == 429 /* Rate Limit Exceeded */ || code >= 500) {
                    return NetworkJob.ERROR_TRAKT_SERVER;
                } else {
                    return NetworkJob.ERROR_TRAKT_CLIENT;
                }
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(context, errorLabel, e);
            return NetworkJob.ERROR_CONNECTION;
        }

        return NetworkJob.SUCCESS;
    }

    /**
     * If the {@link SyncErrors} indicates any show, season or episode was not found returns {@code
     * false}.
     */
    private static boolean isSyncSuccessful(@Nullable SyncResponse response) {
        if (response == null || response.not_found == null) {
            return true;
        }

        //noinspection RedundantIfStatement
        if (response.not_found.movies != null && !response.not_found.movies.isEmpty()) {
            return false; // movie not found
        }

        return true;
    }
}
