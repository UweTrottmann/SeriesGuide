package com.battlelancer.seriesguide.sync;

import android.content.Context;
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SgEpisode2CollectedUpdate;
import com.battlelancer.seriesguide.provider.SgEpisode2ForSync;
import com.battlelancer.seriesguide.provider.SgEpisode2Helper;
import com.battlelancer.seriesguide.provider.SgEpisode2WatchedUpdate;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.provider.SgSeason2Numbers;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.traktapi.TraktSettings;
import com.battlelancer.seriesguide.traktapi.TraktTools;
import com.battlelancer.seriesguide.traktapi.TraktTools2;
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;
import com.battlelancer.seriesguide.ui.shows.ShowTools;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
     * Similar to the sync methods, but does not download anything and only processes a single show.
     */
    public boolean storeEpisodeFlags(@Nullable Map<Integer, BaseShow> tmdbIdsToTraktShow,
            int showTmdbId, long showRowId, @NonNull TraktEpisodeSync.Flag flag) {
        if (tmdbIdsToTraktShow == null || tmdbIdsToTraktShow.isEmpty()) {
            return true; // no watched/collected shows on trakt, done.
        }
        BaseShow traktShow = tmdbIdsToTraktShow.get(showTmdbId);
        if (traktShow == null) {
            return true; // show is not watched/collected on trakt, done.
        }
        return processTraktSeasons(false, showRowId, traktShow, flag);
    }

    /**
     * @param isInitialSync If true, will upload any episodes flagged locally, but not flagged on
     *                      trakt. If false, all watched and collected (and only those, e.g. not skipped flag) flags will
     */
    public boolean syncWatched(@NonNull Map<Integer, Long> tmdbIdsToShowIds,
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
            boolean success = processTraktShows(watchedShowsTrakt, tmdbIdsToShowIds, Flag.WATCHED,
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

    public boolean syncCollected(@NonNull Map<Integer, Long> tmdbIdsToShowIds,
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
            boolean success = processTraktShows(collectedShowsTrakt, tmdbIdsToShowIds,
                    Flag.COLLECTED, isInitialSync);
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
            @NonNull Map<Integer, Long> tmdbIdsToShowIds, Flag flag, boolean isInitialSync) {
        Map<Integer, BaseShow> tmdbIdsToTraktShow = TraktTools2.mapByTmdbId(remoteShows);

        int uploadedShowsCount = 0;
        Map<Long, Long> showIdsToLastWatched = new HashMap<>();
        final ArrayList<Long> showsToClear = new ArrayList<>();

        for (Map.Entry<Integer, Long> tmdbIdToShowId : tmdbIdsToShowIds.entrySet()) {
            int tmdbId = tmdbIdToShowId.getKey();
            long showId = tmdbIdToShowId.getValue();

            BaseShow traktShow = tmdbIdsToTraktShow.get(tmdbId);
            if (traktShow != null) {
                // show watched/collected on trakt
                if (!processTraktSeasons(isInitialSync, showId, traktShow, flag)) {
                    return false; // processing seasons failed, give up.
                }
                if (flag == Flag.WATCHED) {
                    if (traktShow.last_watched_at != null) {
                        showIdsToLastWatched
                                .put(showId, traktShow.last_watched_at.toInstant().toEpochMilli());
                    }
                }
            } else {
                // show not watched/collected on trakt
                // check if this is because the show can not be tracked with trakt (yet)
                // some shows only exist on TheTVDB, keep state local and maybe upload in the future
                Integer showTraktId = ShowTools.getShowTraktId(context, showId);
                if (showTraktId != null) {
                    // Show can be tracked with Trakt.

                    if (isInitialSync) {
                        // upload all watched/collected episodes of the show
                        // do in between processing to stretch uploads over longer time periods
                        uploadShow(showId, showTraktId, flag);
                        uploadedShowsCount++;
                    } else {
                        // Set all watched/collected episodes of show not watched/collected,
                        // clear plays if watched.
                        showsToClear.add(showId);
                    }
                }
            }
        }

        // Clear all watched/collected episodes of marked shows.
        SgRoomDatabase database = SgRoomDatabase.getInstance(context);
        if (!showsToClear.isEmpty()) {
            if (flag == Flag.WATCHED) {
                database.sgEpisode2Helper().setShowsNotWatchedExcludeSkipped(showsToClear);
            } else {
                database.sgEpisode2Helper().updateCollectedOfShows(showsToClear, false);
            }
        }

        // Update last watched timestamps.
        database.sgShow2Helper().updateLastWatchedMsIfLater(showIdsToLastWatched);

        if (uploadedShowsCount > 0) {
            Timber.d("processTraktShows: uploaded %s flags for %s complete shows.", flag.name,
                    uploadedShowsCount);
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
    public boolean processTraktSeasons(boolean isInitialSync, long showRowId,
            @NonNull BaseShow traktShow, @NonNull Flag flag) {
        HashMap<Integer, BaseSeason> traktSeasons = TraktTools.mapSeasonsByNumber(traktShow.seasons);

        SgRoomDatabase database = SgRoomDatabase.getInstance(context);
        List<SgSeason2Numbers> localSeasons = database
                .sgSeason2Helper()
                .getSeasonNumbersOfShow(showRowId);

        final ArrayList<Long> seasonsToClear = new ArrayList<>();
        List<SyncSeason> syncSeasons = new ArrayList<>();
        for (SgSeason2Numbers localSeason : localSeasons) {
            long seasonId = localSeason.getId();
            int seasonNumber = localSeason.getNumber();
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
                    seasonsToClear.add(seasonId);
                }
            }
        }

        if (!seasonsToClear.isEmpty()) {
            if (flag == Flag.WATCHED) {
                database.sgEpisode2Helper().setSeasonsNotWatchedExcludeSkipped(seasonsToClear);
            } else {
                database.sgEpisode2Helper().updateCollectedOfSeasons(seasonsToClear, false);
            }
        }

        if (isInitialSync && syncSeasons.size() > 0) {
            // upload watched/collected episodes for this show
            Integer showTraktId = ShowTools.getShowTraktId(context, showRowId);
            if (showTraktId == null) {
                return false; // show should have a trakt id, give up
            }
            return upload(showTraktId, syncSeasons, flag);
        } else {
            return true;
        }
    }

    private boolean processWatchedTraktEpisodes(
            long seasonId,
            BaseSeason traktSeason,
            List<SyncSeason> syncSeasons,
            boolean isInitialSync
    ) {
        HashMap<Integer, BaseEpisode> traktEpisodes = TraktTools
                .buildTraktEpisodesMap(traktSeason.episodes);

        SgEpisode2Helper helper = SgRoomDatabase.getInstance(context).sgEpisode2Helper();
        List<SgEpisode2ForSync> localEpisodes = helper.getEpisodesForTraktSync(seasonId);

        ArrayList<SgEpisode2WatchedUpdate> batch = new ArrayList<>();
        List<SyncEpisode> syncEpisodes = new ArrayList<>();
        int episodesSetOnePlayCount = 0;
        int episodesUnsetCount = 0;
        for (SgEpisode2ForSync localEpisode : localEpisodes) {
            long episodeId = localEpisode.getId();
            int episodeNumber = localEpisode.getNumber();
            boolean isWatchedLocally = EpisodeTools.isWatched(localEpisode.getWatched());

            BaseEpisode traktEpisode = traktEpisodes.get(episodeNumber);
            if (traktEpisode != null) {
                // Episode watched on Trakt.
                if (localEpisode.getWatched() != EpisodeFlags.WATCHED) {
                    // Local episode is skipped or not watched.
                    // Set as watched and store plays.
                    int plays = traktEpisode.plays != null && traktEpisode.plays > 0
                            ? traktEpisode.plays : 1;
                    batch.add(new SgEpisode2WatchedUpdate(episodeId,
                            EpisodeFlags.WATCHED, plays));
                    if (plays == 1) {
                        episodesSetOnePlayCount++;
                    }
                } else if (localEpisode.getWatched() == EpisodeFlags.WATCHED) {
                    // Watched locally: update plays if changed.
                    if (traktEpisode.plays != null && traktEpisode.plays > 0
                            && !traktEpisode.plays.equals(localEpisode.getPlays())) {
                        batch.add(new SgEpisode2WatchedUpdate(episodeId,
                                EpisodeFlags.WATCHED, traktEpisode.plays));
                    }
                }
            } else {
                // Episode not watched on Trakt.
                // Note: episodes skipped locally are not touched.
                if (isWatchedLocally) {
                    if (isInitialSync) {
                        // Upload to Trakt.
                        int plays = localEpisode.getPlays();
                        // Add an episode for each play, Trakt will create a separate play for each.
                        SyncEpisode syncEpisode = new SyncEpisode().number(episodeNumber);
                        for (int i = 0; i < plays; i++) {
                            syncEpisodes.add(syncEpisode);
                        }
                    } else {
                        // Set as not watched and remove plays if it is currently watched.
                        batch.add(new SgEpisode2WatchedUpdate(episodeId,
                                EpisodeFlags.UNWATCHED, 0));
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
            helper.setSeasonWatched(seasonId);
        } else if (notWatchedWholeSeason) {
            helper.setSeasonNotWatchedAndRemovePlays(seasonId);
        } else {
            // Or apply individual episode updates.
            helper.updateEpisodesWatched(batch);
        }

        if (isInitialSync && syncEpisodes.size() > 0) {
            syncSeasons.add(new SyncSeason()
                    .number(traktSeason.number)
                    .episodes(syncEpisodes));
        }

        return true;
    }

    private boolean processCollectedTraktEpisodes(
            long seasonId,
            BaseSeason traktSeason,
            List<SyncSeason> syncSeasons,
            boolean isInitialSync
    ) {
        HashMap<Integer, BaseEpisode> traktEpisodes = TraktTools
                .buildTraktEpisodesMap(traktSeason.episodes);

        SgEpisode2Helper helper = SgRoomDatabase.getInstance(context).sgEpisode2Helper();
        List<SgEpisode2ForSync> localEpisodes = helper.getEpisodesForTraktSync(seasonId);

        ArrayList<SgEpisode2CollectedUpdate> batch = new ArrayList<>();
        List<SyncEpisode> syncEpisodes = new ArrayList<>();
        int episodesAddCount = 0;
        int episodesRemoveCount = 0;
        for (SgEpisode2ForSync localEpisode : localEpisodes) {
            long episodeId = localEpisode.getId();
            int episodeNumber = localEpisode.getNumber();
            boolean isCollectedLocally = localEpisode.getCollected();

            BaseEpisode traktEpisode = traktEpisodes.get(episodeNumber);
            if (traktEpisode != null) {
                // Episode collected on Trakt.
                if (!isCollectedLocally) {
                    // Set as collected if it is currently not.
                    batch.add(new SgEpisode2CollectedUpdate(episodeId, true));
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
                        batch.add(new SgEpisode2CollectedUpdate(episodeId, false));
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
            helper.updateCollectedOfSeason(seasonId, addWholeSeason);
        } else {
            // Or apply individual episode updates.
            helper.updateEpisodesCollected(batch);
        }

        if (isInitialSync && syncEpisodes.size() > 0) {
            syncSeasons.add(new SyncSeason()
                    .number(traktSeason.number)
                    .episodes(syncEpisodes));
        }

        return true;
    }

    /**
     * Uploads all watched/collected episodes for the given show to trakt.
     */
    private boolean uploadShow(long showId, int showTraktId, Flag flag) {
        List<SgSeason2Numbers> localSeasons = SgRoomDatabase.getInstance(context)
                .sgSeason2Helper()
                .getSeasonNumbersOfShow(showId);

        List<SyncSeason> syncSeasons = new ArrayList<>();
        for (SgSeason2Numbers localSeason : localSeasons) {
            syncSeasons.add(
                    buildSyncSeason(localSeason.getId(), localSeason.getNumber(), flag));
        }

        if (syncSeasons.isEmpty()) {
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
    private SyncSeason buildSyncSeason(long seasonId, int seasonNumber, Flag flag) {
        // query for watched/collected episodes of the given season
        SgEpisode2Helper helper = SgRoomDatabase.getInstance(context).sgEpisode2Helper();
        List<SgEpisode2ForSync> episodes;
        if (flag == Flag.WATCHED) {
            episodes = helper.getWatchedEpisodesForTraktSync(seasonId);
        } else if (flag == Flag.COLLECTED){
            episodes = helper.getCollectedEpisodesForTraktSync(seasonId);
        } else {
            throw new IllegalArgumentException("Flag not supported: " + flag);
        }

        List<SyncEpisode> syncEpisodes = new ArrayList<>();
        for (SgEpisode2ForSync episode : episodes) {
            SyncEpisode syncEpisode = new SyncEpisode().number(episode.getNumber());

            // Add an episode for each play, Trakt will create a separate play for each.
            // Or only a single one if sending collected flag.
            int count = flag == Flag.WATCHED ? episode.getPlays() : 1;
            for (int i = 0; i < count; i++) {
                syncEpisodes.add(syncEpisode);
            }
        }

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
            int plays = episodesCursor.getInt(EpisodesQuery.PLAYS);

            // create new season if none exists or number has changed
            if (currentSeason == null || currentSeason.number != season) {
                currentSeason = new SyncSeason().number(season);
                currentSeason.episodes = new LinkedList<>();
                seasons.add(currentSeason);
            }

            // Add an episode for each play, Trakt will create a separate play for each.
            SyncEpisode syncEpisode = new SyncEpisode().number(episode);
            for (int i = 0; i < plays; i++) {
                currentSeason.episodes.add(syncEpisode);
            }
        }

        return seasons;
    }

    private interface EpisodesQuery {

        String[] PROJECTION = new String[]{
                Episodes.SEASON, Episodes.NUMBER, Episodes.PLAYS
        };

        int SEASON = 0;
        int EPISODE = 1;
        int PLAYS = 2;
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
