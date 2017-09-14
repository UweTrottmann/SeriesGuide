package com.battlelancer.seriesguide.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.sync.HexagonEpisodeSync;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.ui.BaseNavDrawerActivity;
import com.battlelancer.seriesguide.util.tasks.EpisodeFlagJob;
import com.battlelancer.seriesguide.util.tasks.EpisodeFlagJobs;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.backend.episodes.Episodes;
import com.uwetrottmann.seriesguide.backend.episodes.model.Episode;
import com.uwetrottmann.seriesguide.backend.episodes.model.EpisodeList;
import com.uwetrottmann.trakt5.entities.ShowIds;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncResponse;
import com.uwetrottmann.trakt5.entities.SyncSeason;
import com.uwetrottmann.trakt5.entities.SyncShow;
import com.uwetrottmann.trakt5.services.Sync;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Call;
import retrofit2.Response;

public class EpisodeTools {

    /**
     * Checks the database whether there is an entry for this episode.
     */
    public static boolean isEpisodeExists(Context context, int episodeTvdbId) {
        Cursor query = context.getContentResolver().query(
                SeriesGuideContract.Episodes.buildEpisodeUri(episodeTvdbId), new String[] {
                        SeriesGuideContract.Episodes._ID }, null, null, null
        );
        if (query == null) {
            return false;
        }

        boolean isExists = query.getCount() > 0;
        query.close();

        return isExists;
    }

    public static boolean isCollected(int collectedFlag) {
        return collectedFlag == 1;
    }

    public static boolean isSkipped(int episodeFlags) {
        return episodeFlags == EpisodeFlags.SKIPPED;
    }

    public static boolean isUnwatched(int episodeFlags) {
        return episodeFlags == EpisodeFlags.UNWATCHED;
    }

    public static boolean isWatched(int episodeFlags) {
        return episodeFlags == EpisodeFlags.WATCHED;
    }

    public static void validateFlags(int episodeFlags) {
        if (isUnwatched(episodeFlags)) {
            return;
        }
        if (isSkipped(episodeFlags)) {
            return;
        }
        if (isWatched(episodeFlags)) {
            return;
        }

        throw new IllegalArgumentException(
                "Did not pass a valid episode flag. See EpisodeFlags class for details.");
    }

    public static void episodeWatched(Context context, int showTvdbId, int episodeTvdbId,
            int season, int episode, int episodeFlags) {
        validateFlags(episodeFlags);
        execute(context,
                new EpisodeFlagJobs.EpisodeWatchedJob(context, showTvdbId, episodeTvdbId, season,
                        episode,
                        episodeFlags)
        );
    }

    public static void episodeCollected(Context context, int showTvdbId, int episodeTvdbId,
            int season, int episode, boolean isFlag) {
        execute(context,
                new EpisodeFlagJobs.EpisodeCollectedJob(context, showTvdbId, episodeTvdbId,
                        season, episode,
                        isFlag ? 1 : 0)
        );
    }

    /**
     * Flags all episodes released previous to this one as watched (excluding episodes with no
     * release date).
     */
    public static void episodeWatchedPrevious(Context context, int showTvdbId,
            long episodeFirstAired) {
        execute(context,
                new EpisodeFlagJobs.EpisodeWatchedPreviousJob(context, showTvdbId,
                        episodeFirstAired)
        );
    }

    public static void seasonWatched(Context context, int showTvdbId, int seasonTvdbId, int season,
            int episodeFlags) {
        validateFlags(episodeFlags);
        execute(context,
                new EpisodeFlagJobs.SeasonWatchedJob(context, showTvdbId, seasonTvdbId, season,
                        episodeFlags)
        );
    }

    public static void seasonCollected(Context context, int showTvdbId, int seasonTvdbId,
            int season, boolean isFlag) {
        execute(context,
                new EpisodeFlagJobs.SeasonCollectedJob(context, showTvdbId, seasonTvdbId, season,
                        isFlag ? 1 : 0)
        );
    }

    public static void showWatched(Context context, int showTvdbId, boolean isFlag) {
        execute(context,
                new EpisodeFlagJobs.ShowWatchedJob(context, showTvdbId, isFlag ? 1 : 0)
        );
    }

    public static void showCollected(Context context, int showTvdbId, boolean isFlag) {
        execute(context,
                new EpisodeFlagJobs.ShowCollectedJob(context, showTvdbId, isFlag ? 1 : 0)
        );
    }

    /**
     * Run the task on the thread pool.
     */
    private static void execute(Context context, @NonNull EpisodeFlagJob job) {
        new EpisodeAsyncFlagTask(context, job).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Posted once the episode task has completed. It may not have been successful.
     */
    public static class EpisodeTaskCompletedEvent {
        public final EpisodeFlagJob job;
        public final boolean isSuccessful;

        public EpisodeTaskCompletedEvent(EpisodeFlagJob job, boolean isSuccessful) {
            this.job = job;
            this.isSuccessful = isSuccessful;
        }
    }

    public static class EpisodeAsyncFlagTask extends AsyncTask<Void, Void, Integer> {

        private static final int SUCCESS = 0;
        private static final int ERROR_NETWORK = -1;
        private static final int ERROR_TRAKT_AUTH = -2;
        private static final int ERROR_TRAKT_API = -3;
        private static final int ERROR_HEXAGON_API = -4;

        @SuppressLint("StaticFieldLeak") // using application context
        private final Context context;
        private final EpisodeFlagJob job;

        private boolean shouldSendToTrakt;
        private boolean shouldSendToHexagon;

        private boolean canSendToTrakt;

        public EpisodeAsyncFlagTask(Context context, EpisodeFlagJob job) {
            this.context = context.getApplicationContext();
            this.job = job;
        }

        @Override
        protected void onPreExecute() {
            shouldSendToHexagon = HexagonSettings.isEnabled(context);
            shouldSendToTrakt = TraktCredentials.get(context).hasCredentials()
                    && !isSkipped(job.getFlagValue());

            EventBus.getDefault().postSticky(new BaseNavDrawerActivity.ServiceActiveEvent(
                    shouldSendToHexagon, shouldSendToTrakt));
        }

        @Override
        protected Integer doInBackground(Void... params) {
            // upload to hexagon
            if (shouldSendToHexagon) {
                if (!AndroidUtils.isNetworkConnected(context)) {
                    return ERROR_NETWORK;
                }

                HexagonTools hexagonTools = SgApp.getServicesComponent(context).hexagonTools();
                int result = uploadToHexagon(context, hexagonTools, job.getShowTvdbId(),
                        job.getEpisodesForHexagon());
                if (result < 0) {
                    return result;
                }
            }

            // upload to trakt
            /*
              Do net send skipped episodes, this is not supported by trakt.
              However, if the skipped flag is removed this will be handled identical
              to flagging as unwatched.
             */
            if (shouldSendToTrakt) {
                // Do not send if show has no trakt id (was not on trakt last time we checked).
                Integer traktId = ShowTools.getShowTraktId(context, job.getShowTvdbId());
                canSendToTrakt = traktId != null;
                if (canSendToTrakt) {
                    if (!AndroidUtils.isNetworkConnected(context)) {
                        return ERROR_NETWORK;
                    }

                    int result = uploadToTrakt(context, job, traktId);
                    if (result < 0) {
                        return result;
                    }
                }
            }

            // update local database (if uploading went smoothly or not uploading at all)
            job.applyLocalChanges();

            return SUCCESS;
        }

        public static int uploadToHexagon(Context app, HexagonTools hexagonTools, int showTvdbId,
                @NonNull List<Episode> batch) {
            EpisodeList uploadWrapper = new EpisodeList();
            uploadWrapper.setShowTvdbId(showTvdbId);

            // upload in small batches
            List<Episode> smallBatch = new ArrayList<>();
            while (!batch.isEmpty()) {
                // batch small enough?
                if (batch.size() <= HexagonEpisodeSync.MAX_BATCH_SIZE) {
                    smallBatch = batch;
                } else {
                    // build smaller batch
                    for (int count = 0; count < HexagonEpisodeSync.MAX_BATCH_SIZE; count++) {
                        if (batch.isEmpty()) {
                            break;
                        }
                        smallBatch.add(batch.remove(0));
                    }
                }

                // upload
                uploadWrapper.setEpisodes(smallBatch);
                if (!uploadFlagsToHexagon(app, hexagonTools, uploadWrapper)) {
                    return ERROR_HEXAGON_API;
                }

                // prepare for next batch
                smallBatch.clear();
            }

            return SUCCESS;
        }

        /**
         * Upload the given episodes to Hexagon. Assumes the given episode wrapper has valid
         * values.
         */
        private static boolean uploadFlagsToHexagon(Context context, HexagonTools hexagonTools,
                EpisodeList episodes) {
            try {
                Episodes episodesService = hexagonTools.getEpisodesService();
                if (episodesService == null) {
                    return false;
                }
                episodesService.save(episodes).execute();
            } catch (IOException e) {
                HexagonTools.trackFailedRequest(context, "save episodes", e);
                return false;
            }

            return true;
        }

        public static int uploadToTrakt(Context context, EpisodeFlagJob flagType,
                int showTraktId) {
            List<SyncSeason> flags = flagType.getEpisodesForTrakt();
            if (flags != null && flags.isEmpty()) {
                return SUCCESS; // nothing to upload, done.
            }

            if (!TraktCredentials.get(context).hasCredentials()) {
                return ERROR_TRAKT_AUTH;
            }

            // outer wrapper and show are always required
            SyncShow show = new SyncShow().id(ShowIds.trakt(showTraktId));
            SyncItems items = new SyncItems().shows(show);
            // add season or episodes
            EpisodeFlagJobs.Action flagAction = flagType.getAction();
            if (flagAction == EpisodeFlagJobs.Action.SEASON_WATCHED
                    || flagAction == EpisodeFlagJobs.Action.SEASON_COLLECTED
                    || flagAction == EpisodeFlagJobs.Action.EPISODE_WATCHED
                    || flagAction == EpisodeFlagJobs.Action.EPISODE_COLLECTED
                    || flagAction == EpisodeFlagJobs.Action.EPISODE_WATCHED_PREVIOUS) {
                show.seasons(flags);
            }

            // determine network call
            String action;
            Call<SyncResponse> call;
            Sync traktSync = SgApp.getServicesComponent(context).traktSync();
            boolean isAddNotDelete = !isUnwatched(flagType.getFlagValue());
            switch (flagAction) {
                case SHOW_WATCHED:
                case SEASON_WATCHED:
                case EPISODE_WATCHED:
                case EPISODE_WATCHED_PREVIOUS:
                    if (isAddNotDelete) {
                        action = "set episodes watched";
                        call = traktSync.addItemsToWatchedHistory(items);
                    } else {
                        action = "set episodes not watched";
                        call = traktSync.deleteItemsFromWatchedHistory(items);
                    }
                    break;
                case SHOW_COLLECTED:
                case SEASON_COLLECTED:
                case EPISODE_COLLECTED:
                    if (isAddNotDelete) {
                        action = "add episodes to collection";
                        call = traktSync.addItemsToCollection(items);
                    } else {
                        action = "remove episodes from collection";
                        call = traktSync.deleteItemsFromCollection(items);
                    }
                    break;
                default:
                    return ERROR_TRAKT_API;
            }

            // execute call
            try {
                Response<SyncResponse> response = call.execute();
                if (response.isSuccessful()) {
                    // check if any items were not found
                    if (isSyncSuccessful(response.body())) {
                        return SUCCESS;
                    }
                } else {
                    if (SgTrakt.isUnauthorized(context, response)) {
                        return ERROR_TRAKT_AUTH;
                    }
                    SgTrakt.trackFailedRequest(context, action, response);
                }
            } catch (IOException e) {
                SgTrakt.trackFailedRequest(context, action, e);
            }
            return ERROR_TRAKT_API;
        }

        /**
         * If the {@link SyncResponse} is invalid or any show, season or episode was not found
         * returns {@code false}.
         */
        private static boolean isSyncSuccessful(SyncResponse response) {
            if (response == null || response.not_found == null) {
                // invalid response, assume failure
                return false;
            }

            if (response.not_found.shows != null && !response.not_found.shows.isEmpty()) {
                // show not found
                return false;
            }
            if (response.not_found.seasons != null && !response.not_found.seasons.isEmpty()) {
                // show exists, but seasons not found
                return false;
            }
            //noinspection RedundantIfStatement
            if (response.not_found.episodes != null && !response.not_found.episodes.isEmpty()) {
                // show and season exists, but episodes not found
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Integer result) {
            EventBus.getDefault().removeStickyEvent(BaseNavDrawerActivity.ServiceActiveEvent.class);

            // handle errors
            String error = null;
            switch (result) {
                case ERROR_NETWORK:
                    error = context.getString(R.string.offline);
                    break;
                case ERROR_TRAKT_AUTH:
                    error = context.getString(R.string.trakt_error_credentials);
                    break;
                case ERROR_TRAKT_API:
                    error = context.getString(R.string.api_error_generic,
                            context.getString(R.string.trakt));
                    break;
                case ERROR_HEXAGON_API:
                    error = context.getString(R.string.api_error_generic,
                            context.getString(R.string.hexagon));
                    break;
            }
            boolean isSuccessful = error == null;

            // post completed status
            String confirmationText;
            boolean displaySuccess;
            if (isSuccessful && shouldSendToTrakt && !canSendToTrakt) {
                // tell the user this change can not be sent to trakt for now
                confirmationText = context.getString(R.string.trakt_notice_not_exists);
                displaySuccess = false;
            } else {
                confirmationText = isSuccessful ? job.getConfirmationText() : error;
                displaySuccess = isSuccessful;
            }
            EventBus.getDefault()
                    .post(new BaseNavDrawerActivity.ServiceCompletedEvent(confirmationText,
                            displaySuccess));
            EventBus.getDefault().post(new EpisodeTaskCompletedEvent(job, isSuccessful));

            if (isSuccessful) {
                // update latest episode for the changed show
                new LatestEpisodeUpdateTask(context).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR, job.getShowTvdbId());
            }
        }
    }
}
