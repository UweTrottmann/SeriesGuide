package com.battlelancer.seriesguide.sync;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import com.battlelancer.seriesguide.model.SgEpisodeForTraktSync;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.traktapi.TraktSettings;
import com.battlelancer.seriesguide.traktapi.TraktTools;
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;
import com.battlelancer.seriesguide.ui.shows.ShowTools;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.Errors;
import com.battlelancer.seriesguide.util.TimeTools;
import com.uwetrottmann.trakt5.entities.BaseEpisode;
import com.uwetrottmann.trakt5.entities.BaseSeason;
import com.uwetrottmann.trakt5.entities.BaseShow;
import com.uwetrottmann.trakt5.entities.ShowIds;
import com.uwetrottmann.trakt5.entities.SyncEpisode;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncResponse;
import com.uwetrottmann.trakt5.entities.SyncSeason;
import com.uwetrottmann.trakt5.entities.SyncShow;
import com.uwetrottmann.trakt5.services.Sync;
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
     *                      trakt. If false, all watched and collected (and only those, e.g. not skipped flag) flags will
     *                      be removed prior to getting the actual flags from trakt (season by season).
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
                    Errors.logAndReport("get watched shows", response);
                }
            } catch (Exception e) {
                Errors.logAndReport("get watched shows", e);
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
                    Errors.logAndReport("get collected shows", response);
                }
            } catch (Exception e) {
                Errors.logAndReport("get collected shows", e);
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
                    // Show can be tracked with Trakt.

                    if (isInitialSync) {
                        // upload all watched/collected episodes of the show
                        // do in between processing to stretch uploads over longer time periods
                        upload(localShow, showTraktId, flag);
                        uploadedShowsCount++;
                    } else {
                        // Set all watched/collected episodes of show not watched/collected,
                        // clear plays if watched.
                        ContentProviderOperation.Builder update = ContentProviderOperation
                                .newUpdate(Episodes.buildEpisodesOfShowUri(localShow))
                                .withSelection(flag.clearFlagSelection, null)
                                .withValue(flag.databaseColumn, flag.notFlaggedValue);
                        if (flag == Flag.WATCHED) {
                            update.withValue(Episodes.PLAYS, 0);
                        }
                        batch.add(update.build());
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
     *                      watched/collected on trakt. If {@code false}, will set them not watched/collected (if not
     *                      skipped) to mirror the trakt episode.
     */
    public boolean processTraktSeasons(boolean isInitialSync, int localShow,
            @NonNull BaseShow traktShow, @NonNull Flag flag) {
        HashMap<Integer, BaseSeason> traktSeasons = TraktTools.buildTraktSeasonsMap(
                traktShow.seasons);

        Cursor localSeasonsQuery = context.getContentResolver()
                .query(SeriesGuideContract.Seasons.buildSeasonsOfShowUri(localShow),
                        new String[]{SeriesGuideContract.Seasons._ID,
                                SeriesGuideContract.Seasons.COMBINED}, null, null,
                        null);
        if (localSeasonsQuery == null) {
            return false;
        }
        final ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        List<SyncSeason> syncSeasons = new ArrayList<>();
        while (localSeasonsQuery.moveToNext()) {
            int seasonId = localSeasonsQuery.getInt(0);
            int seasonNumber = localSeasonsQuery.getInt(1);
            if (traktSeasons.containsKey(seasonNumber)) {
                // Season watched/collected on Trakt.
                if (flag == Flag.WATCHED) {
                    if (!processWatchedTraktEpisodes(seasonId,
                            traktSeasons.get(seasonNumber), syncSeasons, isInitialSync)) {
                        return false;
                    }
                } else {
                    if (!processCollectedTraktEpisodes(seasonId,
                            traktSeasons.get(seasonNumber), syncSeasons, isInitialSync)) {
                        return false;
                    }
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
                    // Set all watched/collected episodes of season not watched/collected,
                    // clear plays if watched.
                    ContentProviderOperation.Builder update = ContentProviderOperation
                            .newUpdate(Episodes.buildEpisodesOfSeasonUri(seasonId))
                            .withSelection(flag.clearFlagSelection, null)
                            .withValue(flag.databaseColumn, flag.notFlaggedValue);
                    if (flag == Flag.WATCHED) {
                        update.withValue(Episodes.PLAYS, 0);
                    }
                    batch.add(update.build());
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

    private boolean processWatchedTraktEpisodes(
            int seasonTvdbId,
            BaseSeason traktSeason,
            List<SyncSeason> syncSeasons,
            boolean isInitialSync
    ) {
        HashMap<Integer, BaseEpisode> traktEpisodes = TraktTools
                .buildTraktEpisodesMap(traktSeason.episodes);

        List<SgEpisodeForTraktSync> localEpisodes = SgRoomDatabase.getInstance(context)
                .episodeHelper()
                .getSeasonForTraktSync(seasonTvdbId);

        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        List<SyncEpisode> syncEpisodes = new ArrayList<>();
        int episodesSetOnePlayCount = 0;
        int episodesUnsetCount = 0;
        for (SgEpisodeForTraktSync localEpisode : localEpisodes) {
            int episodeId = localEpisode.getTvdbId();
            int episodeNumber = localEpisode.getNumber();
            boolean isWatchedLocally = EpisodeTools.isWatched(localEpisode.getWatched());

            BaseEpisode traktEpisode = traktEpisodes.get(episodeNumber);
            if (traktEpisode != null) {
                // Episode watched on Trakt.
                if (localEpisode.getWatched() != EpisodeFlags.WATCHED) {
                    // Local episode is skipped or not watched.
                    // Set as watched and store plays.
                    ContentProviderOperation.Builder update = ContentProviderOperation
                            .newUpdate(Episodes.buildEpisodeUri(episodeId))
                            .withValue(Episodes.WATCHED, EpisodeFlags.WATCHED);
                    int plays = traktEpisode.plays != null && traktEpisode.plays > 0
                            ? traktEpisode.plays : 1;
                    update.withValue(Episodes.PLAYS, plays);
                    batch.add(update.build());
                    if (plays == 1) {
                        episodesSetOnePlayCount++;
                    }
                } else if (localEpisode.getWatched() == EpisodeFlags.WATCHED) {
                    // Watched locally: update plays if changed.
                    if (traktEpisode.plays != null && traktEpisode.plays > 0
                            && !traktEpisode.plays.equals(localEpisode.getPlays())) {
                        ContentProviderOperation.Builder update = ContentProviderOperation
                                .newUpdate(Episodes.buildEpisodeUri(episodeId))
                                .withValue(Episodes.PLAYS, traktEpisode.plays);
                        batch.add(update.build());
                    }
                }
            } else {
                // Episode not watched on Trakt.
                // Note: episodes skipped locally are not touched.
                if (isWatchedLocally) {
                    if (isInitialSync) {
                        // Upload to Trakt.
                        syncEpisodes.add(new SyncEpisode().number(episodeNumber));
                    } else {
                        // Set as not watched and remove plays if it is currently watched.
                        ContentProviderOperation.Builder update = ContentProviderOperation
                                .newUpdate(Episodes.buildEpisodeUri(episodeId))
                                .withValue(Episodes.WATCHED, EpisodeFlags.UNWATCHED)
                                .withValue(Episodes.PLAYS, 0);
                        batch.add(update.build());
                        episodesUnsetCount++;
                    }
                }
            }
        }
        int localEpisodeCount = localEpisodes.size();
        boolean setWatchedOnePlayWholeSeason = episodesSetOnePlayCount == localEpisodeCount;
        boolean notWatchedWholeSeason = episodesUnsetCount == localEpisodeCount;

        // Performance improvement especially on initial syncs:
        // if setting the whole season as (not) watched with 1 play, replace with single db op.
        if (setWatchedOnePlayWholeSeason) {
            batch.clear();
            batch.add(ContentProviderOperation
                    .newUpdate(Episodes.buildEpisodesOfSeasonUri(seasonTvdbId))
                    .withValue(Episodes.WATCHED, EpisodeFlags.WATCHED)
                    .withValue(Episodes.PLAYS, 1)
                    .build());
        } else if (notWatchedWholeSeason) {
            batch.clear();
            batch.add(ContentProviderOperation
                    .newUpdate(Episodes.buildEpisodesOfSeasonUri(seasonTvdbId))
                    .withValue(Episodes.WATCHED, EpisodeFlags.UNWATCHED)
                    .withValue(Episodes.PLAYS, 0)
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

    private boolean processCollectedTraktEpisodes(
            int seasonTvdbId,
            BaseSeason traktSeason,
            List<SyncSeason> syncSeasons,
            boolean isInitialSync
    ) {
        HashMap<Integer, BaseEpisode> traktEpisodes = TraktTools
                .buildTraktEpisodesMap(traktSeason.episodes);

        List<SgEpisodeForTraktSync> localEpisodes = SgRoomDatabase.getInstance(context)
                .episodeHelper()
                .getSeasonForTraktSync(seasonTvdbId);

        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        List<SyncEpisode> syncEpisodes = new ArrayList<>();
        int episodesAddCount = 0;
        int episodesRemoveCount = 0;
        for (SgEpisodeForTraktSync localEpisode : localEpisodes) {
            int episodeId = localEpisode.getTvdbId();
            int episodeNumber = localEpisode.getNumber();
            boolean isCollectedLocally = localEpisode.getCollected();

            BaseEpisode traktEpisode = traktEpisodes.get(episodeNumber);
            if (traktEpisode != null) {
                // Episode collected on Trakt.
                if (!isCollectedLocally) {
                    // Set as collected if it is currently not.
                    ContentProviderOperation.Builder update = ContentProviderOperation
                            .newUpdate(Episodes.buildEpisodeUri(episodeId))
                            .withValue(Episodes.COLLECTED, true);
                    batch.add(update.build());
                    episodesAddCount++;
                }
            } else {
                // Episode not collected on Trakt.
                if (isCollectedLocally) {
                    if (isInitialSync) {
                        // Upload to Trakt.
                        syncEpisodes.add(new SyncEpisode().number(episodeNumber));
                    } else {
                        // Set as not collected if it is currently.
                        batch.add(ContentProviderOperation.newUpdate(
                                Episodes.buildEpisodeUri(episodeId))
                                .withValue(Episodes.COLLECTED, false)
                                .build());
                        episodesRemoveCount++;
                    }
                }
            }
        }
        int localEpisodeCount = localEpisodes.size();
        boolean addWholeSeason = episodesAddCount == localEpisodeCount;
        boolean removeWholeSeason = episodesRemoveCount == localEpisodeCount;

        // Performance improvement especially on initial syncs:
        // if setting the whole season as (not) collected, replace with single db op.
        if (addWholeSeason || removeWholeSeason) {
            batch.clear();
            batch.add(ContentProviderOperation.newUpdate(
                    Episodes.buildEpisodesOfSeasonUri(seasonTvdbId))
                    .withValue(Episodes.COLLECTED, addWholeSeason)
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
                Episodes.buildEpisodesOfShowUri(showTvdbId),
                EpisodesQuery.PROJECTION,
                flag.flagSelection,
                null,
                Episodes.SORT_SEASON_ASC);
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
                Errors.logAndReport("add episodes to " + flag.name, response);
            }
        } catch (Exception e) {
            Errors.logAndReport("add episodes to " + flag.name, e);
        }

        return false;
    }

    /**
     * Returns a list of watched/collected episodes of a season. Packaged ready for upload to
     * trakt.
     */
    private SyncSeason buildSyncSeason(int seasonTvdbId, int seasonNumber,
            Flag flag) {
        // query for watched/collected episodes of the given season
        Cursor flaggedEpisodesQuery = context.getContentResolver().query(
                Episodes.buildEpisodesOfSeasonUri(seasonTvdbId),
                new String[]{Episodes.NUMBER},
                flag.flagSelection,
                null,
                Episodes.SORT_NUMBER_ASC);
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

        String[] PROJECTION = new String[]{
                Episodes.SEASON, Episodes.NUMBER
        };

        int SEASON = 0;
        int EPISODE = 1;
    }

    public enum Flag {
        COLLECTED("collected",
                Episodes.COLLECTED,
                // only remove flags for already collected episodes
                Episodes.COLLECTED + "=1",
                Episodes.SELECTION_COLLECTED,
                1, 0),
        WATCHED("watched",
                Episodes.WATCHED,
                // do not remove flags of skipped episodes, only of watched ones
                Episodes.WATCHED + "=" + EpisodeFlags.WATCHED,
                Episodes.SELECTION_WATCHED,
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
