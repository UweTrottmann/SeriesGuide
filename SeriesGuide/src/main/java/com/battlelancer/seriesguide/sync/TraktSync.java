package com.battlelancer.seriesguide.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
import com.battlelancer.seriesguide.traktapi.TraktSettings;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.ui.movies.MovieTools;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt5.entities.LastActivities;
import com.uwetrottmann.trakt5.entities.LastActivityMore;
import com.uwetrottmann.trakt5.services.Sync;
import java.io.IOException;
import java.util.HashSet;
import retrofit2.Response;
import timber.log.Timber;

public class TraktSync {

    private Context context;
    private MovieTools movieTools;
    private Sync traktSync;
    private SyncProgress progress;

    public TraktSync(Context context, MovieTools movieTools, Sync traktSync,
            SyncProgress progress) {
        this.context = context;
        this.movieTools = movieTools;
        this.traktSync = traktSync;
        this.progress = progress;
    }

    public SgSyncAdapter.UpdateResult sync(HashSet<Integer> localShows, long currentTime) {
        if (!TraktCredentials.get(context).hasCredentials()) {
            Timber.d("performTraktSync: no auth, skip");
            return SgSyncAdapter.UpdateResult.SUCCESS;
        }

        if (!AndroidUtils.isNetworkConnected(context)) {
            return SgSyncAdapter.UpdateResult.INCOMPLETE;
        }

        // get last activity timestamps
        LastActivities lastActivity = getLastActivity();
        if (lastActivity == null) {
            // trakt is likely offline or busy, try later
            Timber.e("performTraktSync: last activity download failed");
            return SgSyncAdapter.UpdateResult.INCOMPLETE;
        }

        if (!AndroidUtils.isNetworkConnected(context)) {
            return SgSyncAdapter.UpdateResult.INCOMPLETE;
        }

        TraktRatingsSync ratingsSync = new TraktRatingsSync(context, traktSync);
        if (localShows.size() == 0) {
            Timber.d("performTraktSync: no local shows, skip shows");
        } else {
            // EPISODES
            // download and upload episode watched and collected flags
            progress.publish(SyncProgress.Step.TRAKT_EPISODES);
            if (!syncEpisodes(localShows, lastActivity.episodes, currentTime)) {
                progress.recordError();
                return SgSyncAdapter.UpdateResult.INCOMPLETE;
            }

            if (!AndroidUtils.isNetworkConnected(context)) {
                return SgSyncAdapter.UpdateResult.INCOMPLETE;
            }

            // download ratings
            if (!ratingsSync.downloadForEpisodes(lastActivity.episodes.rated_at)) {
                progress.recordError();
                return SgSyncAdapter.UpdateResult.INCOMPLETE;
            }

            if (!AndroidUtils.isNetworkConnected(context)) {
                return SgSyncAdapter.UpdateResult.INCOMPLETE;
            }

            // SHOWS
            // download ratings
            progress.publish(SyncProgress.Step.TRAKT_RATINGS);
            if (!ratingsSync.downloadForShows(lastActivity.shows.rated_at)) {
                progress.recordError();
                return SgSyncAdapter.UpdateResult.INCOMPLETE;
            }

            if (!AndroidUtils.isNetworkConnected(context)) {
                return SgSyncAdapter.UpdateResult.INCOMPLETE;
            }
        }

        // MOVIES
        TraktMovieSync movieSync = new TraktMovieSync(context, movieTools, traktSync);

        // sync watchlist and collection with trakt
        progress.publish(SyncProgress.Step.TRAKT_MOVIES);
        if (!movieSync.syncLists(lastActivity.movies)) {
            progress.recordError();
            return SgSyncAdapter.UpdateResult.INCOMPLETE;
        }

        if (!AndroidUtils.isNetworkConnected(context)) {
            return SgSyncAdapter.UpdateResult.INCOMPLETE;
        }

        // download watched movies
        if (!movieSync.downloadWatched(lastActivity.movies.watched_at)) {
            progress.recordError();
            return SgSyncAdapter.UpdateResult.INCOMPLETE;
        }

        // clean up any useless movies (not watched or not in any list)
        MovieTools.deleteUnusedMovies(context);

        if (!AndroidUtils.isNetworkConnected(context)) {
            return SgSyncAdapter.UpdateResult.INCOMPLETE;
        }

        // download movie ratings
        progress.publish(SyncProgress.Step.TRAKT_RATINGS);
        if (!ratingsSync.downloadForMovies(lastActivity.movies.rated_at)) {
            progress.recordError();
            return SgSyncAdapter.UpdateResult.INCOMPLETE;
        }

        return SgSyncAdapter.UpdateResult.SUCCESS;
    }

    /**
     * Downloads and uploads episode watched and collected flags.
     *
     * <p> Do <b>NOT</b> call if there are no local shows to avoid unnecessary work.
     */
    private boolean syncEpisodes(@NonNull HashSet<Integer> localShows,
            @NonNull LastActivityMore lastActivity, long currentTime) {
        if (!TraktCredentials.get(context).hasCredentials()) {
            return false; // auth was removed
        }

        // download flags
        // if initial sync, upload any flags missing on trakt
        // otherwise clear all local flags not on trakt
        boolean isInitialSync = !TraktSettings.hasMergedEpisodes(context);

        // watched episodes
        TraktEpisodeSync episodeSync = new TraktEpisodeSync(context, traktSync);
        if (!episodeSync.syncWatched(localShows, lastActivity.watched_at, isInitialSync)) {
            return false; // failed, give up.
        }

        // collected episodes
        if (!episodeSync.syncCollected(localShows, lastActivity.collected_at, isInitialSync)) {
            return false;
        }

        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(context).edit();
        if (isInitialSync) {
            // success, set initial sync as complete
            editor.putBoolean(TraktSettings.KEY_HAS_MERGED_EPISODES, true);
        }
        // success, set last sync time to now
        editor.putLong(TraktSettings.KEY_LAST_FULL_EPISODE_SYNC, currentTime);
        editor.apply();

        return true;
    }

    @Nullable
    public LastActivities getLastActivity() {
        try {
            Response<LastActivities> response = traktSync
                    .lastActivities()
                    .execute();
            if (response.isSuccessful()) {
                return response.body();
            }
            if (SgTrakt.isUnauthorized(context, response)) {
                return null;
            }
            SgTrakt.trackFailedRequest(context, "get last activity", response);
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(context, "get last activity", e);
        }
        return null;
    }
}
