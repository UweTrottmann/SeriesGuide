package com.battlelancer.seriesguide.sync;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
import com.battlelancer.seriesguide.traktapi.TraktSettings;
import com.battlelancer.seriesguide.ui.movies.MovieTools;
import com.battlelancer.seriesguide.util.Errors;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt5.entities.LastActivities;
import com.uwetrottmann.trakt5.entities.LastActivityMore;
import com.uwetrottmann.trakt5.services.Sync;
import java.util.Map;
import retrofit2.Response;
import timber.log.Timber;

public class TraktSync {

    private Context context;
    private MovieTools movieTools;
    private Sync traktSync;
    private SyncProgress progress;

    TraktSync(Context context, MovieTools movieTools, Sync traktSync, SyncProgress progress) {
        this.context = context;
        this.movieTools = movieTools;
        this.traktSync = traktSync;
        this.progress = progress;
    }

    /**
     * @param onlyRatings To not conflict with Hexagon sync, can turn on so only
     *                    ratings are synced.
     */
    public SgSyncAdapter.UpdateResult sync(long currentTime, boolean onlyRatings) {
        // get last activity timestamps
        progress.publish(SyncProgress.Step.TRAKT);
        if (!AndroidUtils.isNetworkConnected(context)) {
            progress.recordError();
            return SgSyncAdapter.UpdateResult.INCOMPLETE;
        }
        LastActivities lastActivity = getLastActivity();
        if (lastActivity == null
                || lastActivity.episodes == null
                || lastActivity.shows == null
                || lastActivity.movies == null) {
            // trakt is offline or busy, or there are server errors, try later.
            progress.recordError();
            Timber.e("performTraktSync: last activity download failed");
            return SgSyncAdapter.UpdateResult.INCOMPLETE;
        }

        TraktRatingsSync ratingsSync = new TraktRatingsSync(context, traktSync);
        Map<Integer, Long> tmdbIdsToShowIds = SgApp.getServicesComponent(context).showTools()
                .getTmdbIdsToShowIds();
        if (tmdbIdsToShowIds.size() == 0) {
            Timber.d("performTraktSync: no local shows, skip shows");
        } else {
            if (!onlyRatings) {
                // EPISODES
                // download and upload episode watched and collected flags
                progress.publish(SyncProgress.Step.TRAKT_EPISODES);
                if (!AndroidUtils.isNetworkConnected(context)) {
                    progress.recordError();
                    return SgSyncAdapter.UpdateResult.INCOMPLETE;
                }
                if (!syncEpisodes(tmdbIdsToShowIds, lastActivity.episodes, currentTime)) {
                    progress.recordError();
                    return SgSyncAdapter.UpdateResult.INCOMPLETE;
                }
            }

            // download ratings
            progress.publish(SyncProgress.Step.TRAKT_RATINGS);
            if (!AndroidUtils.isNetworkConnected(context)) {
                progress.recordError();
                return SgSyncAdapter.UpdateResult.INCOMPLETE;
            }
            if (!ratingsSync.downloadForEpisodes(lastActivity.episodes.rated_at)) {
                progress.recordError();
                return SgSyncAdapter.UpdateResult.INCOMPLETE;
            }

            // SHOWS
            // download ratings
            if (!AndroidUtils.isNetworkConnected(context)) {
                progress.recordError();
                return SgSyncAdapter.UpdateResult.INCOMPLETE;
            }
            if (!ratingsSync.downloadForShows(lastActivity.shows.rated_at)) {
                progress.recordError();
                return SgSyncAdapter.UpdateResult.INCOMPLETE;
            }
        }

        // MOVIES
        progress.publish(SyncProgress.Step.TRAKT_MOVIES);
        TraktMovieSync movieSync = new TraktMovieSync(context, movieTools, traktSync);

        // sync watchlist, collection and watched movies with trakt
        if (!onlyRatings) {
            if (!AndroidUtils.isNetworkConnected(context)) {
                progress.recordError();
                return SgSyncAdapter.UpdateResult.INCOMPLETE;
            }
            if (!movieSync.syncLists(lastActivity.movies)) {
                progress.recordError();
                return SgSyncAdapter.UpdateResult.INCOMPLETE;
            }
            // clean up any useless movies (not watched or not in any list)
            MovieTools.deleteUnusedMovies(context);
        }

        // download movie ratings
        progress.publish(SyncProgress.Step.TRAKT_RATINGS);
        if (!AndroidUtils.isNetworkConnected(context)) {
            progress.recordError();
            return SgSyncAdapter.UpdateResult.INCOMPLETE;
        }
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
    private boolean syncEpisodes(@NonNull Map<Integer, Long> tmdbIdsToShowIds,
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
        if (!episodeSync.syncWatched(tmdbIdsToShowIds, lastActivity.watched_at, isInitialSync)) {
            return false; // failed, give up.
        }

        // collected episodes
        if (!episodeSync
                .syncCollected(tmdbIdsToShowIds, lastActivity.collected_at, isInitialSync)) {
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
    private LastActivities getLastActivity() {
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
            Errors.logAndReport("get last activity", response);
        } catch (Exception e) {
            Errors.logAndReport("get last activity", e);
        }
        return null;
    }
}
