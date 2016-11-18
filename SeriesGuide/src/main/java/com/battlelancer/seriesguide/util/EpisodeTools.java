package com.battlelancer.seriesguide.util;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v4.util.SparseArrayCompat;
import android.text.TextUtils;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.util.tasks.EpisodeTaskTypes;
import com.google.api.client.util.DateTime;
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
import dagger.Lazy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class EpisodeTools {

    private static final int EPISODE_MAX_BATCH_SIZE = 500;

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

    public static void episodeWatched(SgApp app, int showTvdbId, int episodeTvdbId,
            int season, int episode, int episodeFlags) {
        validateFlags(episodeFlags);
        execute(app,
                new EpisodeTaskTypes.EpisodeWatchedType(app, showTvdbId, episodeTvdbId, season,
                        episode,
                        episodeFlags)
        );
    }

    public static void episodeCollected(SgApp app, int showTvdbId, int episodeTvdbId,
            int season, int episode, boolean isFlag) {
        execute(app,
                new EpisodeTaskTypes.EpisodeCollectedType(app, showTvdbId, episodeTvdbId,
                        season, episode,
                        isFlag ? 1 : 0)
        );
    }

    /**
     * Flags all episodes released previous to this one as watched (excluding episodes with no
     * release date).
     */
    public static void episodeWatchedPrevious(SgApp app, int showTvdbId,
            long episodeFirstAired) {
        execute(app,
                new EpisodeTaskTypes.EpisodeWatchedPreviousType(app, showTvdbId,
                        episodeFirstAired)
        );
    }

    public static void seasonWatched(SgApp app, int showTvdbId, int seasonTvdbId, int season,
            int episodeFlags) {
        validateFlags(episodeFlags);
        execute(app,
                new EpisodeTaskTypes.SeasonWatchedType(app, showTvdbId, seasonTvdbId, season,
                        episodeFlags)
        );
    }

    public static void seasonCollected(SgApp app, int showTvdbId, int seasonTvdbId,
            int season, boolean isFlag) {
        execute(app,
                new EpisodeTaskTypes.SeasonCollectedType(app, showTvdbId, seasonTvdbId, season,
                        isFlag ? 1 : 0)
        );
    }

    public static void showWatched(SgApp app, int showTvdbId, boolean isFlag) {
        execute(app,
                new EpisodeTaskTypes.ShowWatchedType(app, showTvdbId, isFlag ? 1 : 0)
        );
    }

    public static void showCollected(SgApp app, int showTvdbId, boolean isFlag) {
        execute(app,
                new EpisodeTaskTypes.ShowCollectedType(app, showTvdbId, isFlag ? 1 : 0)
        );
    }

    /**
     * Run the task on the thread pool.
     */
    private static void execute(SgApp app, @NonNull EpisodeTaskTypes.FlagType type) {
        AsyncTaskCompat.executeParallel(new EpisodeFlagTask(app, type));
    }

    /**
     * Sent once sending to services and the database ops are finished.
     */
    public static class EpisodeActionCompletedEvent {

        public EpisodeTaskTypes.FlagType flagType;

        public EpisodeActionCompletedEvent(EpisodeTaskTypes.FlagType type) {
            flagType = type;
        }
    }

    public static class EpisodeFlagTask extends AsyncTask<Void, Void, Integer> {

        private static final int SUCCESS = 0;
        private static final int ERROR_NETWORK = -1;
        private static final int ERROR_TRAKT_AUTH = -2;
        private static final int ERROR_TRAKT_API = -3;
        private static final int ERROR_HEXAGON_API = -4;

        private final Context context;
        @Inject Lazy<Sync> traktSync;
        private final EpisodeTaskTypes.FlagType flagType;

        private boolean shouldSendToTrakt;
        private boolean shouldSendToHexagon;

        private boolean canSendToTrakt;

        public EpisodeFlagTask(SgApp app, EpisodeTaskTypes.FlagType type) {
            this.context = app;
            app.getServicesComponent().inject(this);
            flagType = type;
        }

        @Override
        protected void onPreExecute() {
            // network ops may run long, so immediately show a status toast
            shouldSendToHexagon = HexagonTools.isSignedIn(context);
            if (shouldSendToHexagon) {
                Toast.makeText(context, R.string.hexagon_api_queued, Toast.LENGTH_SHORT).show();
            }
            shouldSendToTrakt = TraktCredentials.get(context).hasCredentials()
                    && !isSkipped(flagType.getFlagValue());
        }

        @Override
        protected Integer doInBackground(Void... params) {
            // upload to hexagon
            if (shouldSendToHexagon) {
                if (!AndroidUtils.isNetworkConnected(context)) {
                    return ERROR_NETWORK;
                }

                int result = uploadToHexagon(context, flagType.getShowTvdbId(),
                        flagType.getEpisodesForHexagon());
                if (result < 0) {
                    return result;
                }
            }

            // upload to trakt
            /**
             * Do net send skipped episodes, this is not supported by trakt.
             * However, if the skipped flag is removed this will be handled identical
             * to flagging as unwatched.
             */
            if (shouldSendToTrakt) {
                // Do not send if show has no trakt id (was not on trakt last time we checked).
                Integer traktId = ShowTools.getShowTraktId(context, flagType.getShowTvdbId());
                canSendToTrakt = traktId != null;
                if (canSendToTrakt) {
                    if (!AndroidUtils.isNetworkConnected(context)) {
                        return ERROR_NETWORK;
                    }

                    int result = uploadToTrakt(traktId);
                    if (result < 0) {
                        return result;
                    }
                }
            }

            // update local database (if uploading went smoothly or not uploading at all)
            flagType.updateDatabase();
            flagType.onPostExecute();

            return SUCCESS;
        }

        private static int uploadToHexagon(Context context, int showTvdbId,
                @NonNull List<Episode> batch) {
            EpisodeList uploadWrapper = new EpisodeList();
            uploadWrapper.setShowTvdbId(showTvdbId);

            // upload in small batches
            List<Episode> smallBatch = new ArrayList<>();
            while (!batch.isEmpty()) {
                // batch small enough?
                if (batch.size() <= EPISODE_MAX_BATCH_SIZE) {
                    smallBatch = batch;
                } else {
                    // build smaller batch
                    for (int count = 0; count < EPISODE_MAX_BATCH_SIZE; count++) {
                        if (batch.isEmpty()) {
                            break;
                        }
                        smallBatch.add(batch.remove(0));
                    }
                }

                // upload
                uploadWrapper.setEpisodes(smallBatch);
                if (!Upload.flagsToHexagon(context, uploadWrapper)) {
                    return ERROR_HEXAGON_API;
                }

                // prepare for next batch
                smallBatch.clear();
            }

            return SUCCESS;
        }

        private int uploadToTrakt(int showTraktId) {
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
            EpisodeTaskTypes.Action flagAction = flagType.getAction();
            if (flagAction == EpisodeTaskTypes.Action.SEASON_WATCHED
                    || flagAction == EpisodeTaskTypes.Action.SEASON_COLLECTED
                    || flagAction == EpisodeTaskTypes.Action.EPISODE_WATCHED
                    || flagAction == EpisodeTaskTypes.Action.EPISODE_COLLECTED
                    || flagAction == EpisodeTaskTypes.Action.EPISODE_WATCHED_PREVIOUS) {
                show.seasons(flags);
            }

            // determine network call
            String action;
            Call<SyncResponse> call;
            boolean isAddNotDelete = !isUnwatched(flagType.getFlagValue());
            switch (flagAction) {
                case SHOW_WATCHED:
                case SEASON_WATCHED:
                case EPISODE_WATCHED:
                case EPISODE_WATCHED_PREVIOUS:
                    if (isAddNotDelete) {
                        action = "set episodes watched";
                        call = traktSync.get().addItemsToWatchedHistory(items);
                    } else {
                        action = "set episodes not watched";
                        call = traktSync.get().deleteItemsFromWatchedHistory(items);
                    }
                    break;
                case SHOW_COLLECTED:
                case SEASON_COLLECTED:
                case EPISODE_COLLECTED:
                    if (isAddNotDelete) {
                        action = "add episodes to collection";
                        call = traktSync.get().addItemsToCollection(items);
                    } else {
                        action = "remove episodes from collection";
                        call = traktSync.get().deleteItemsFromCollection(items);
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
            if (error != null) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show();
                return;
            }

            // success!
            // notify UI it may do relevant updates
            EventBus.getDefault().post(new EpisodeActionCompletedEvent(flagType));

            // update latest episode for the changed show
            AsyncTaskCompat.executeParallel(new LatestEpisodeUpdateTask(context),
                    flagType.getShowTvdbId());

            // display success message
            if (shouldSendToTrakt) {
                if (canSendToTrakt) {
                    int status = R.string.trakt_success;
                    EpisodeTaskTypes.Action action = flagType.getAction();
                    if (action == EpisodeTaskTypes.Action.SHOW_WATCHED
                            || action == EpisodeTaskTypes.Action.SHOW_COLLECTED
                            || action == EpisodeTaskTypes.Action.EPISODE_WATCHED_PREVIOUS) {
                        // simple ack
                        Toast.makeText(context,
                                context.getString(status),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        // detailed ack
                        String message = flagType.getNotificationText();
                        Toast.makeText(context,
                                message + " " + context.getString(status),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // tell the user this change can not be sent to trakt for now
                    Toast.makeText(context, R.string.trakt_notice_not_exists, Toast.LENGTH_LONG)
                            .show();
                }
            }
        }
    }

    public static class Download {

        /**
         * Downloads all episodes changed since the last time this was called and applies changes to
         * the database.
         */
        public static boolean flagsFromHexagon(Context context) {
            List<Episode> episodes;
            boolean hasMoreEpisodes = true;
            String cursor = null;
            long currentTime = System.currentTimeMillis();
            DateTime lastSyncTime = new DateTime(HexagonSettings.getLastEpisodesSyncTime(context));

            Timber.d("flagsFromHexagon: downloading changed episode flags since %s", lastSyncTime);

            SparseArrayCompat<Long> showsLastWatchedMs = new SparseArrayCompat<>();
            while (hasMoreEpisodes) {
                try {
                    Episodes episodesService = HexagonTools.getEpisodesService(context);
                    if (episodesService == null) {
                        return false;
                    }

                    Episodes.Get request = episodesService.get()
                            .setUpdatedSince(lastSyncTime); // use default server limit
                    if (!TextUtils.isEmpty(cursor)) {
                        request.setCursor(cursor);
                    }

                    EpisodeList response = request.execute();
                    if (response == null) {
                        // we're done here
                        Timber.d("flagsFromHexagon: response was null, done here");
                        break;
                    }

                    episodes = response.getEpisodes();

                    // check for more items
                    if (response.getCursor() != null) {
                        cursor = response.getCursor();
                    } else {
                        hasMoreEpisodes = false;
                    }
                } catch (IOException e) {
                    HexagonTools.trackFailedRequest(context, "get updated episodes", e);
                    return false;
                }

                if (episodes == null || episodes.size() == 0) {
                    // nothing to do here
                    break;
                }

                // build batch of episode flag updates
                ArrayList<ContentProviderOperation> batch = new ArrayList<>();
                for (Episode episode : episodes) {
                    ContentValues values = new ContentValues();
                    Integer showTvdbId = episode.getShowTvdbId();
                    Integer watchedFlag = episode.getWatchedFlag();
                    if (watchedFlag != null) {
                        values.put(SeriesGuideContract.Episodes.WATCHED, watchedFlag);
                        // record the latest last watched time for a show
                        if (!EpisodeTools.isUnwatched(watchedFlag)) {
                            Long lastWatchedMs = showsLastWatchedMs.get(showTvdbId);
                            // episodes returned in reverse chrono order, so just get the first time
                            if (lastWatchedMs == null && episode.getUpdatedAt() != null) {
                                long updatedAtMs = episode.getUpdatedAt().getValue();
                                showsLastWatchedMs.put(showTvdbId, updatedAtMs);
                            }
                        }
                    }
                    if (episode.getIsInCollection() != null) {
                        values.put(SeriesGuideContract.Episodes.COLLECTED,
                                episode.getIsInCollection());
                    }

                    ContentProviderOperation op = ContentProviderOperation
                            .newUpdate(SeriesGuideContract.Episodes.CONTENT_URI)
                            .withSelection(SeriesGuideContract.Shows.REF_SHOW_ID + "="
                                    + showTvdbId + " AND "
                                    + SeriesGuideContract.Episodes.SEASON + "="
                                    + episode.getSeasonNumber() + " AND "
                                    + SeriesGuideContract.Episodes.NUMBER + "="
                                    + episode.getEpisodeNumber(), null)
                            .withValues(values)
                            .build();

                    batch.add(op);
                }

                // execute database update
                try {
                    DBUtils.applyInSmallBatches(context, batch);
                } catch (OperationApplicationException e) {
                    Timber.e(e, "flagsFromHexagon: failed to apply changed episode flag updates");
                    return false;
                }
            }

            if (!updateLastWatchedTimeOfShows(context, showsLastWatchedMs)) {
                return false;
            }

            // store new last sync time
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putLong(HexagonSettings.KEY_LAST_SYNC_EPISODES, currentTime)
                    .commit();

            return true;
        }

        private static boolean updateLastWatchedTimeOfShows(Context context,
                SparseArrayCompat<Long> showsLastWatchedMs) {
            if (showsLastWatchedMs.size() == 0) {
                return true; // no episodes were watched, no last watched time to update
            }

            ArrayList<ContentProviderOperation> batch = new ArrayList<>();
            for (int i = 0; i < showsLastWatchedMs.size(); i++) {
                int showTvdbId = showsLastWatchedMs.keyAt(i);
                long lastWatchedMsNew = showsLastWatchedMs.valueAt(i);
                if (!ShowTools.addLastWatchedUpdateOpIfNewer(context, batch, showTvdbId,
                        lastWatchedMsNew)) {
                    return false; // failed to query current last watched ms
                }
            }

            try {
                DBUtils.applyInSmallBatches(context, batch);
            } catch (OperationApplicationException e) {
                Timber.e(e,
                        "updateShowsLastWatchedTime: failed to apply last watched time updates");
                return false;
            }

            return true;
        }

        /**
         * Downloads watched, skipped or collected episodes of this show from Hexagon and applies
         * those flags to episodes in the database.
         *
         * @return Whether the download was successful and all changes were applied to the database.
         */
        public static boolean flagsFromHexagon(Context context, int showTvdbId) {
            Timber.d("flagsFromHexagon: downloading episode flags for show %s", showTvdbId);
            List<Episode> episodes;
            boolean hasMoreEpisodes = true;
            String cursor = null;

            Uri episodesOfShowUri = SeriesGuideContract.Episodes.buildEpisodesOfShowUri(showTvdbId);
            Long lastWatchedMs = null;
            while (hasMoreEpisodes) {
                // abort if connection is lost
                if (!AndroidUtils.isNetworkConnected(context)) {
                    Timber.e("flagsFromHexagon: no network connection");
                    return false;
                }

                try {
                    Episodes episodesService = HexagonTools.getEpisodesService(context);
                    if (episodesService == null) {
                        return false;
                    }

                    // build request
                    Episodes.Get request = episodesService.get()
                            .setShowTvdbId(showTvdbId); // use default server limit
                    if (!TextUtils.isEmpty(cursor)) {
                        request.setCursor(cursor);
                    }

                    // execute request
                    EpisodeList response = request.execute();
                    if (response == null) {
                        break;
                    }

                    episodes = response.getEpisodes();

                    // check for more items
                    if (response.getCursor() != null) {
                        cursor = response.getCursor();
                    } else {
                        hasMoreEpisodes = false;
                    }
                } catch (IOException e) {
                    HexagonTools.trackFailedRequest(context, "get episodes of show", e);
                    return false;
                }

                if (episodes == null || episodes.size() == 0) {
                    // nothing to do here
                    break;
                }

                // build batch of episode flag updates
                ArrayList<ContentProviderOperation> batch = new ArrayList<>();
                for (Episode episode : episodes) {
                    ContentValues values = new ContentValues();
                    if (episode.getWatchedFlag() != null
                            && episode.getWatchedFlag() != EpisodeFlags.UNWATCHED) {
                        values.put(SeriesGuideContract.Episodes.WATCHED, episode.getWatchedFlag());
                        // record last watched time by taking latest updatedAt of watched/skipped
                        DateTime updatedAt = episode.getUpdatedAt();
                        if (updatedAt != null) {
                            long lastWatchedMsNew = updatedAt.getValue();
                            if (lastWatchedMs == null || lastWatchedMs < lastWatchedMsNew) {
                                lastWatchedMs = lastWatchedMsNew;
                            }
                        }
                    }
                    if (episode.getIsInCollection() != null
                            && episode.getIsInCollection()) {
                        values.put(SeriesGuideContract.Episodes.COLLECTED,
                                episode.getIsInCollection());
                    }

                    if (values.size() == 0) {
                        // skip if episode has neither a watched flag or is in collection
                        continue;
                    }

                    ContentProviderOperation op = ContentProviderOperation
                            .newUpdate(episodesOfShowUri)
                            .withSelection(SeriesGuideContract.Episodes.SEASON + "="
                                    + episode.getSeasonNumber() + " AND "
                                    + SeriesGuideContract.Episodes.NUMBER + "="
                                    + episode.getEpisodeNumber(), null)
                            .withValues(values)
                            .build();

                    batch.add(op);
                }

                // execute database update
                try {
                    DBUtils.applyInSmallBatches(context, batch);
                } catch (OperationApplicationException e) {
                    Timber.e(e,
                            "flagsFromHexagon: failed to apply episode flag updates for show %s",
                            showTvdbId);
                    return false;
                }
            }

            //noinspection RedundantIfStatement
            if (!updateLastWatchedTimeOfShow(context, showTvdbId, lastWatchedMs)) {
                return false; // failed to update last watched time
            }

            return true;
        }

        private static boolean updateLastWatchedTimeOfShow(Context context, int showTvdbId,
                @Nullable Long lastWatchedMs) {
            if (lastWatchedMs == null) {
                return true; // no last watched time, nothing to update
            }

            ArrayList<ContentProviderOperation> batch = new ArrayList<>(1);
            if (!ShowTools.addLastWatchedUpdateOpIfNewer(context, batch, showTvdbId,
                    lastWatchedMs)) {
                return false; // failed to query current last watched ms
            }

            try {
                DBUtils.applyInSmallBatches(context, batch);
            } catch (OperationApplicationException e) {
                Timber.e(e,
                        "updateLastWatchedTimeOfShow: failed to update last watched ms for show %s",
                        showTvdbId);
                return false;
            }

            return true;
        }
    }

    public static class Upload {

        private interface FlaggedEpisodesQuery {
            String[] PROJECTION = new String[] {
                    SeriesGuideContract.Episodes._ID,
                    SeriesGuideContract.Episodes.SEASON,
                    SeriesGuideContract.Episodes.NUMBER,
                    SeriesGuideContract.Episodes.WATCHED,
                    SeriesGuideContract.Episodes.COLLECTED
            };

            String SELECTION = SeriesGuideContract.Episodes.WATCHED + "!=" + EpisodeFlags.UNWATCHED
                    + " OR " + SeriesGuideContract.Episodes.COLLECTED + "=1";

            int SEASON = 1;
            int NUMBER = 2;
            int WATCHED = 3;
            int IN_COLLECTION = 4;
        }

        /**
         * Uploads all watched, skipped or collected episodes of this show to Hexagon.
         *
         * @return Whether the upload was successful.
         */
        public static boolean flagsToHexagon(Context context, int showTvdbId) {
            Timber.d("flagsToHexagon: uploading episode flags for show %s", showTvdbId);

            // query for watched, skipped or collected episodes
            Cursor query = context.getContentResolver()
                    .query(SeriesGuideContract.Episodes.buildEpisodesOfShowUri(showTvdbId),
                            FlaggedEpisodesQuery.PROJECTION, FlaggedEpisodesQuery.SELECTION,
                            null, null
                    );
            if (query == null) {
                Timber.e("flagsToHexagon: episode flags query was null");
                return false;
            }
            if (query.getCount() == 0) {
                Timber.d("flagsToHexagon: no episode flags to upload");
                query.close();
                return true;
            }

            // build list of episodes to upload
            List<Episode> episodes = new ArrayList<>();
            while (query.moveToNext()) {
                Episode episode = new Episode();
                episode.setSeasonNumber(query.getInt(FlaggedEpisodesQuery.SEASON));
                episode.setEpisodeNumber(query.getInt(FlaggedEpisodesQuery.NUMBER));

                int watchedFlag = query.getInt(FlaggedEpisodesQuery.WATCHED);
                if (!EpisodeTools.isUnwatched(watchedFlag)) {
                    episode.setWatchedFlag(watchedFlag);
                }

                boolean isInCollection = EpisodeTools.isCollected(
                        query.getInt(FlaggedEpisodesQuery.IN_COLLECTION));
                if (isInCollection) {
                    episode.setIsInCollection(true);
                }

                episodes.add(episode);

                // upload a batch
                if (episodes.size() == EPISODE_MAX_BATCH_SIZE || query.isLast()) {
                    EpisodeList episodeList = new EpisodeList();
                    episodeList.setEpisodes(episodes);
                    episodeList.setShowTvdbId(showTvdbId);

                    try {
                        Episodes episodesService = HexagonTools.getEpisodesService(context);
                        if (episodesService == null) {
                            return false;
                        }
                        episodesService.save(episodeList).execute();
                    } catch (IOException e) {
                        // abort
                        HexagonTools.trackFailedRequest(context, "save episodes of show", e);
                        query.close();
                        return false;
                    }

                    // clear array
                    episodes = new ArrayList<>();
                }
            }

            query.close();

            return true;
        }

        /**
         * Upload the given episodes to Hexagon. Assumes the given episode wrapper has valid
         * values.
         */
        public static boolean flagsToHexagon(Context context, EpisodeList episodes) {
            try {
                Episodes episodesService = HexagonTools.getEpisodesService(context);
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
    }
}
