package com.battlelancer.seriesguide.sync;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.traktapi.TraktSettings;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;
import com.battlelancer.seriesguide.ui.shows.ShowTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.traktapi.TraktTools;
import com.uwetrottmann.trakt5.entities.BaseSeason;
import com.uwetrottmann.trakt5.entities.BaseShow;
import com.uwetrottmann.trakt5.entities.ShowIds;
import com.uwetrottmann.trakt5.entities.SyncEpisode;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncResponse;
import com.uwetrottmann.trakt5.entities.SyncSeason;
import com.uwetrottmann.trakt5.entities.SyncShow;
import com.uwetrottmann.trakt5.services.Sync;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import org.threeten.bp.OffsetDateTime;
import retrofit2.Response;
import timber.log.Timber;

public class TraktEpisodeSync {

    private Context context;
    private Sync traktSync;

    public TraktEpisodeSync(Context context, Sync traktSync) {
        this.context = context;
        this.traktSync = traktSync;
    }

    /**
     * Similar to the sync methods, but only processes a single show and only downloads
     * watched/collected episodes from trakt.
     */
    public boolean storeEpisodeFlags(@Nullable HashMap<Integer, BaseShow> traktShows,
            int showTvdbId, @NonNull TraktEpisodeSync.Flag flag) {
        if (traktShows == null || traktShows.isEmpty()) {
            return true; // no watched/collected shows on trakt, done.
        }
        if (!traktShows.containsKey(showTvdbId)) {
            return true; // show is not watched/collected on trakt, done.
        }
        BaseShow traktShow = traktShows.get(showTvdbId);
        return processTraktSeasons(false, showTvdbId, traktShow, flag);
    }

    /**
     * @param isInitialSync If true, will upload any episodes flagged locally, but not flagged on
     * trakt. If false, all watched and collected (and only those, e.g. not skipped flag) flags will
     * be removed prior to getting the actual flags from trakt (season by season).
     */
    public boolean syncWatched(@NonNull HashSet<Integer> localShows,
            @Nullable OffsetDateTime watchedAt, boolean isInitialSync) {
        if (watchedAt == null) {
            Timber.e("syncWatched: null watched_at");
            return false;
        }

        long lastWatchedAt = TraktSettings.getLastEpisodesWatchedAt(context);
        if (isInitialSync || TimeTools.isAfterMillis(watchedAt, lastWatchedAt)) {
            List<BaseShow> watchedShowsTrakt = null;
            try {
                // get watched episodes from trakt
                Response<List<BaseShow>> response = traktSync
                        .watchedShows(null)
                        .execute();
                if (response.isSuccessful()) {
                    watchedShowsTrakt = response.body();
                } else {
                    if (SgTrakt.isUnauthorized(context, response)) {
                        return false;
                    }
                    SgTrakt.trackFailedRequest(context, "get watched shows", response);
                }
            } catch (IOException e) {
                SgTrakt.trackFailedRequest(context, "get watched shows", e);
            }

            if (watchedShowsTrakt == null) {
                return false;
            }

            // apply database updates, if initial sync upload diff
            long startTime = System.currentTimeMillis();
            boolean success = processTraktShows(watchedShowsTrakt, localShows, Flag.WATCHED,
                    isInitialSync);
            Timber.d("syncWatched: processing took %s ms", System.currentTimeMillis() - startTime);
            if (!success) {
                return false;
            }

            // store new last activity time
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putLong(TraktSettings.KEY_LAST_EPISODES_WATCHED_AT,
                            watchedAt.toInstant().toEpochMilli())
                    .apply();

            Timber.d("syncWatched: success");
        } else {
            Timber.d("syncWatched: no changes since %tF %tT", lastWatchedAt, lastWatchedAt);
        }
        return true;
    }

    public boolean syncCollected(@NonNull HashSet<Integer> localShows,
            @Nullable OffsetDateTime collectedAt, boolean isInitialSync) {
        if (collectedAt == null) {
            Timber.e("syncCollected: null collected_at");
            return false;
        }

        long lastCollectedAt = TraktSettings.getLastEpisodesCollectedAt(context);
        if (isInitialSync || TimeTools.isAfterMillis(collectedAt, lastCollectedAt)) {
            List<BaseShow> collectedShowsTrakt = null;
            try {
                // get collected episodes from trakt
                Response<List<BaseShow>> response = traktSync
                        .collectionShows(null)
                        .execute();
                if (response.isSuccessful()) {
                    collectedShowsTrakt = response.body();
                } else {
                    if (SgTrakt.isUnauthorized(context, response)) {
                        return false;
                    }
                    SgTrakt.trackFailedRequest(context, "get collected shows", response);
                }
            } catch (IOException e) {
                SgTrakt.trackFailedRequest(context, "get collected shows", e);
            }

            if (collectedShowsTrakt == null) {
                return false;
            }

            // apply database updates, if initial sync upload diff
            long startTime = System.currentTimeMillis();
            boolean success = processTraktShows(collectedShowsTrakt, localShows, Flag.COLLECTED,
                    isInitialSync);
            Timber.d("syncCollected: processing took %s ms",
                    System.currentTimeMillis() - startTime);
            if (!success) {
                return false;
            }

            // store new last activity time
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putLong(TraktSettings.KEY_LAST_EPISODES_COLLECTED_AT,
                            collectedAt.toInstant().toEpochMilli())
                    .apply();

            Timber.d("syncCollected: success");
        } else {
            Timber.d("syncCollected: no changes since %tF %tT", lastCollectedAt, lastCollectedAt);
        }
        return true;
    }

    private boolean processTraktShows(@NonNull List<BaseShow> remoteShows,
            @NonNull HashSet<Integer> localShows, Flag flag, boolean isInitialSync) {
        HashMap<Integer, BaseShow> traktShows = TraktTools.buildTraktShowsMap(remoteShows);

        int uploadedShowsCount = 0;
        final ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        for (Integer localShow : localShows) {
            if (traktShows.containsKey(localShow)) {
                // show watched/collected on trakt
                BaseShow traktShow = traktShows.get(localShow);
                if (!processTraktSeasons(isInitialSync, localShow, traktShow, flag)) {
                    return false; // processing seasons failed, give up.
                }
                if (flag == Flag.WATCHED) {
                    updateLastWatchedTime(localShow, traktShow, batch);
                }
            } else {
                // show not watched/collected on trakt
                // check if this is because the show can not be tracked with trakt (yet)
                // some shows only exist on TheTVDB, keep state local and maybe upload in the future
                Integer showTraktId = ShowTools.getShowTraktId(context, localShow);
                if (showTraktId != null) {
                    if (isInitialSync) {
                        // upload all watched/collected episodes of the show
                        // do in between processing to stretch uploads over longer time periods
                        upload(localShow, showTraktId, flag);
                        uploadedShowsCount++;
                    } else {
                        // set all watched/collected episodes of show not watched/collected
                        batch.add(ContentProviderOperation.newUpdate(
                                SeriesGuideContract.Episodes.buildEpisodesOfShowUri(localShow))
                                .withSelection(flag.clearFlagSelection, null)
                                .withValue(flag.databaseColumn, flag.notFlaggedValue)
                                .build());
                    }
                }
            }
        }

        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "processTraktShows: failed to remove flag for %s.", flag.name);
        }

        if (uploadedShowsCount > 0) {
            Timber.d("processTraktShows: uploaded %s flags for %s complete shows.", flag.name,
                    localShows.size());
        }
        return true;
    }

    /**
     * Sync the watched/collected episodes of the given trakt show with the local episodes. The
     * given show has to be watched/collected on trakt.
     *
     * @param isInitialSync If {@code true}, will upload watched/collected episodes that are not
     * watched/collected on trakt. If {@code false}, will set them not watched/collected (if not
     * skipped) to mirror the trakt episode.
     */
    public boolean processTraktSeasons(boolean isInitialSync, int localShow,
            @NonNull BaseShow traktShow, @NonNull Flag flag) {
        HashMap<Integer, BaseSeason> traktSeasons = TraktTools.buildTraktSeasonsMap(
                traktShow.seasons);

        Cursor localSeasonsQuery = context.getContentResolver()
                .query(SeriesGuideContract.Seasons.buildSeasonsOfShowUri(localShow),
                        new String[] { SeriesGuideContract.Seasons._ID,
                                SeriesGuideContract.Seasons.COMBINED }, null, null,
                        null);
        if (localSeasonsQuery == null) {
            return false;
        }
        final ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        List<SyncSeason> syncSeasons = new ArrayList<>();
        while (localSeasonsQuery.moveToNext()) {
            String seasonId = localSeasonsQuery.getString(0);
            int seasonNumber = localSeasonsQuery.getInt(1);
            if (traktSeasons.containsKey(seasonNumber)) {
                // season watched/collected on trakt
                if (!processTraktEpisodes(seasonId,
                        traktSeasons.get(seasonNumber), syncSeasons, flag, isInitialSync)) {
                    return false;
                }
            } else {
                // season not watched/collected on trakt
                if (isInitialSync) {
                    // schedule all watched/collected episodes of this season for upload
                    SyncSeason syncSeason = buildSyncSeason(seasonId, seasonNumber, flag);
                    if (syncSeason != null) {
                        syncSeasons.add(syncSeason);
                    }
                } else {
                    // set all watched/collected episodes of season not watched/collected
                    batch.add(ContentProviderOperation.newUpdate(
                            SeriesGuideContract.Episodes.buildEpisodesOfSeasonUri(seasonId))
                            .withSelection(flag.clearFlagSelection, null)
                            .withValue(flag.databaseColumn, flag.notFlaggedValue)
                            .build());
                }
            }
        }
        localSeasonsQuery.close();

        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "Setting seasons unwatched failed.");
        }

        if (isInitialSync && syncSeasons.size() > 0) {
            // upload watched/collected episodes for this show
            Integer showTraktId = ShowTools.getShowTraktId(context, localShow);
            //noinspection SimplifiableIfStatement
            if (showTraktId == null) {
                return false; // show should have a trakt id, give up
            }
            return upload(showTraktId, syncSeasons, flag);
        } else {
            return true;
        }
    }

    private boolean processTraktEpisodes(String seasonId, BaseSeason traktSeason,
            List<SyncSeason> syncSeasons, Flag flag, boolean isInitialSync) {
        HashSet<Integer> traktEpisodes = TraktTools.buildTraktEpisodesMap(traktSeason.episodes);

        Cursor localEpisodesQuery = context.getContentResolver()
                .query(SeriesGuideContract.Episodes.buildEpisodesOfSeasonUri(
                        seasonId), new String[] {
                        SeriesGuideContract.Episodes._ID,
                        SeriesGuideContract.Episodes.NUMBER,
                        flag.databaseColumn }, null, null, null);
        if (localEpisodesQuery == null) {
            return false;
        }
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        List<SyncEpisode> syncEpisodes = new ArrayList<>();
        int episodesAddFlagCount = 0;
        int episodesRemoveFlagCount = 0;
        while (localEpisodesQuery.moveToNext()) {
            int episodeId = localEpisodesQuery.getInt(0);
            int episodeNumber = localEpisodesQuery.getInt(1);
            int flagValue = localEpisodesQuery.getInt(2);
            boolean isFlagged = flag == Flag.WATCHED ?
                    EpisodeTools.isWatched(flagValue) : EpisodeTools.isCollected(flagValue);
            if (traktEpisodes.contains(episodeNumber)) {
                // episode watched/collected on trakt
                if (!isFlagged) {
                    // set as watched/collected
                    batch.add(ContentProviderOperation.newUpdate(
                            SeriesGuideContract.Episodes.buildEpisodeUri(episodeId))
                            .withValue(flag.databaseColumn, flag.flaggedValue)
                            .build());
                    episodesAddFlagCount++;
                }
            } else {
                // episode not watched/collected on trakt
                if (isFlagged) {
                    if (isInitialSync) {
                        // upload to trakt
                        syncEpisodes.add(new SyncEpisode().number(episodeNumber));
                    } else {
                        // set as not watched/collected if it is currently watched/collected
                        boolean isSkipped = flag == Flag.WATCHED
                                && EpisodeTools.isSkipped(
                                flagValue);
                        if (!isSkipped) {
                            batch.add(ContentProviderOperation.newUpdate(
                                    SeriesGuideContract.Episodes.buildEpisodeUri(episodeId))
                                    .withValue(flag.databaseColumn, flag.notFlaggedValue)
                                    .build());
                            episodesRemoveFlagCount++;
                        }
                    }
                }
            }
        }
        int localEpisodeCount = localEpisodesQuery.getCount();
        boolean addFlagToWholeSeason = episodesAddFlagCount == localEpisodeCount;
        boolean removeFlagFromWholeSeason = episodesRemoveFlagCount == localEpisodeCount;
        localEpisodesQuery.close();

        // performance improvement especially on initial syncs:
        // if setting the whole season as (not) watched/collected, replace with single db op
        if (addFlagToWholeSeason || removeFlagFromWholeSeason) {
            batch.clear();
            batch.add(ContentProviderOperation.newUpdate(
                    SeriesGuideContract.Episodes.buildEpisodesOfSeasonUri(seasonId))
                    .withValue(flag.databaseColumn,
                            addFlagToWholeSeason ? flag.flaggedValue : flag.notFlaggedValue)
                    .build());
        }

        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "Episodes watched/collected values database update failed.");
        }

        if (isInitialSync && syncEpisodes.size() > 0) {
            syncSeasons.add(new SyncSeason()
                    .number(traktSeason.number)
                    .episodes(syncEpisodes));
        }

        return true;
    }

    /**
     * Adds an update op for the last watched time of the given show if the last watched time on
     * trakt is later.
     */
    private void updateLastWatchedTime(Integer showTvdbId, BaseShow traktShow,
            ArrayList<ContentProviderOperation> batch) {
        if (traktShow.last_watched_at == null) {
            return;
        }
        ShowTools.addLastWatchedUpdateOpIfNewer(context, batch, showTvdbId,
                traktShow.last_watched_at.toInstant().toEpochMilli());
    }

    /**
     * Uploads all watched/collected episodes for the given show to trakt.
     *
     * @return Any of the {@link TraktTools} result codes.
     */
    private boolean upload(int showTvdbId, int showTraktId, Flag flag) {
        // query for watched/collected episodes
        Cursor localEpisodes = context.getContentResolver().query(
                SeriesGuideContract.Episodes.buildEpisodesOfShowUri(showTvdbId),
                EpisodesQuery.PROJECTION,
                flag.flagSelection,
                null,
                SeriesGuideContract.Episodes.SORT_SEASON_ASC);
        if (localEpisodes == null) {
            Timber.e("upload: query failed");
            return false;
        }

        // build a list of watched/collected episodes
        List<SyncSeason> syncSeasons = buildSyncSeasons(localEpisodes);
        localEpisodes.close();

        //noinspection SimplifiableIfStatement
        if (syncSeasons.size() == 0) {
            return true; // nothing to upload for this show
        }

        return upload(showTraktId, syncSeasons, flag);
    }

    /**
     * Uploads all the given watched/collected episodes of the given show to trakt.
     *
     * @return Any of the {@link TraktTools} result codes.
     */
    private boolean upload(int showTraktId, List<SyncSeason> syncSeasons, Flag flag) {
        SyncShow syncShow = new SyncShow();
        syncShow.id(ShowIds.trakt(showTraktId));
        syncShow.seasons = syncSeasons;

        // upload
        SyncItems syncItems = new SyncItems().shows(syncShow);
        try {
            Response<SyncResponse> response;
            if (flag == Flag.WATCHED) {
                // uploading watched episodes
                response = traktSync.addItemsToWatchedHistory(syncItems).execute();
            } else {
                // uploading collected episodes
                response = traktSync.addItemsToCollection(syncItems).execute();
            }
            if (response.isSuccessful()) {
                return true;
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return false;
                }
                SgTrakt.trackFailedRequest(context, "add episodes to " + flag.name, response);
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(context, "add episodes to " + flag.name, e);
        }

        return false;
    }

    /**
     * Returns a list of watched/collected episodes of a season. Packaged ready for upload to
     * trakt.
     */
    private SyncSeason buildSyncSeason(String seasonTvdbId, int seasonNumber,
            Flag flag) {
        // query for watched/collected episodes of the given season
        Cursor flaggedEpisodesQuery = context.getContentResolver().query(
                SeriesGuideContract.Episodes.buildEpisodesOfSeasonUri(seasonTvdbId),
                new String[] { SeriesGuideContract.Episodes.NUMBER },
                flag.flagSelection,
                null,
                SeriesGuideContract.Episodes.SORT_NUMBER_ASC);
        if (flaggedEpisodesQuery == null) {
            // query failed
            return null;
        }

        List<SyncEpisode> syncEpisodes = new ArrayList<>();
        while (flaggedEpisodesQuery.moveToNext()) {
            int episodeNumber = flaggedEpisodesQuery.getInt(0);
            syncEpisodes.add(new SyncEpisode().number(episodeNumber));
        }
        flaggedEpisodesQuery.close();

        if (syncEpisodes.size() == 0) {
            return null; // no episodes watched/collected
        }

        return new SyncSeason().number(seasonNumber).episodes(syncEpisodes);
    }

    /**
     * @param episodesCursor Cursor of episodes sorted by season (ascending).
     */
    private static List<SyncSeason> buildSyncSeasons(Cursor episodesCursor) {
        List<SyncSeason> seasons = new LinkedList<>();

        SyncSeason currentSeason = null;
        while (episodesCursor.moveToNext()) {
            int season = episodesCursor.getInt(EpisodesQuery.SEASON);
            int episode = episodesCursor.getInt(EpisodesQuery.EPISODE);

            // create new season if none exists or number has changed
            if (currentSeason == null || currentSeason.number != season) {
                currentSeason = new SyncSeason().number(season);
                currentSeason.episodes = new LinkedList<>();
                seasons.add(currentSeason);
            }

            // add episode
            currentSeason.episodes.add(new SyncEpisode().number(episode));
        }

        return seasons;
    }

    private interface EpisodesQuery {

        String[] PROJECTION = new String[] {
                SeriesGuideContract.Episodes.SEASON, SeriesGuideContract.Episodes.NUMBER
        };

        int SEASON = 0;
        int EPISODE = 1;
    }

    public enum Flag {
        COLLECTED("collected",
                SeriesGuideContract.Episodes.COLLECTED,
                // only remove flags for already collected episodes
                SeriesGuideContract.Episodes.COLLECTED + "=1",
                SeriesGuideContract.Episodes.SELECTION_COLLECTED,
                1, 0),
        WATCHED("watched",
                SeriesGuideContract.Episodes.WATCHED,
                // do not remove flags of skipped episodes, only of watched ones
                SeriesGuideContract.Episodes.WATCHED + "=" + EpisodeFlags.WATCHED,
                SeriesGuideContract.Episodes.SELECTION_WATCHED,
                EpisodeFlags.WATCHED, EpisodeFlags.UNWATCHED);

        final String name;
        final String databaseColumn;
        final String clearFlagSelection;
        final String flagSelection;
        final int flaggedValue;
        final int notFlaggedValue;

        Flag(String name, String databaseColumn, String clearFlagSelection, String flagSelection,
                int flaggedValue, int notFlaggedValue) {
            this.name = name;
            this.databaseColumn = databaseColumn;
            this.clearFlagSelection = clearFlagSelection;
            this.flagSelection = flagSelection;
            this.flaggedValue = flaggedValue;
            this.notFlaggedValue = notFlaggedValue;
        }
    }
}
