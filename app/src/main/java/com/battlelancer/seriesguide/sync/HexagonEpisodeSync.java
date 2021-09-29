package com.battlelancer.seriesguide.sync;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.preference.PreferenceManager;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.provider.SgEpisode2ForSync;
import com.battlelancer.seriesguide.provider.SgEpisode2UpdateByNumber;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;
import com.battlelancer.seriesguide.util.Errors;
import com.google.api.client.util.DateTime;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.backend.episodes.Episodes;
import com.uwetrottmann.seriesguide.backend.episodes.model.Episode;
import com.uwetrottmann.seriesguide.backend.episodes.model.EpisodeList;
import com.uwetrottmann.seriesguide.backend.episodes.model.SgCloudEpisode;
import com.uwetrottmann.seriesguide.backend.episodes.model.SgCloudEpisodeList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import timber.log.Timber;

public class HexagonEpisodeSync {

    public static final int MAX_BATCH_SIZE = 500;

    private final Context context;
    private final HexagonTools hexagonTools;

    public HexagonEpisodeSync(Context context, HexagonTools hexagonTools) {
        this.context = context;
        this.hexagonTools = hexagonTools;
    }

    /**
     * Downloads all episodes changed since the last time this was called and applies changes to
     * the database.
     */
    public boolean downloadChangedFlags(@NonNull Map<Integer, Long> tmdbIdsToShowIds) {
        long currentTime = System.currentTimeMillis();
        SgRoomDatabase database = SgRoomDatabase.getInstance(context);
        DateTime lastSyncTime = new DateTime(HexagonSettings.getLastEpisodesSyncTime(context));
        Timber.d("downloadChangedFlags: since %s", lastSyncTime);

        List<SgCloudEpisode> episodes;
        String cursor = null;
        boolean hasMoreEpisodes = true;
        Map<Long, ShowLastWatchedInfo> showIdsToLastWatched = new HashMap<>();
        while (hasMoreEpisodes) {
            try {
                // get service each time to check if auth was removed
                Episodes episodesService = hexagonTools.getEpisodesService();
                if (episodesService == null) {
                    return false;
                }

                Episodes.GetSgEpisodes request = episodesService.getSgEpisodes()
                        .setUpdatedSince(lastSyncTime); // use default server limit
                if (!TextUtils.isEmpty(cursor)) {
                    request.setCursor(cursor);
                }

                SgCloudEpisodeList response = request.execute();
                if (response == null) {
                    // we're done here
                    Timber.d("downloadChangedFlags: response was null, done here");
                    break;
                }

                episodes = response.getEpisodes();

                // check for more items
                if (response.getCursor() != null) {
                    cursor = response.getCursor();
                } else {
                    hasMoreEpisodes = false;
                }
            } catch (IOException | IllegalArgumentException e) {
                // Note: JSON parser may throw IllegalArgumentException.
                Errors.logAndReportHexagon("get updated episodes", e);
                return false;
            }

            if (episodes == null || episodes.size() == 0) {
                // nothing to do here
                break;
            }

            // build batch of episode flag updates
            ArrayList<SgEpisode2UpdateByNumber> batch = new ArrayList<>();
            for (SgCloudEpisode episode : episodes) {
                Integer showTmdbId = episode.getShowTmdbId();
                Long showId = tmdbIdsToShowIds.get(showTmdbId);
                if (showId == null) {
                    continue; // ignore, show not added on this device
                }

                Integer watchedFlag = episode.getWatchedFlag();
                Integer playsOrNull = null;
                if (watchedFlag != null) {
                    if (watchedFlag == EpisodeFlags.WATCHED) {
                        // Watched.
                        // Note: plays may be null for legacy data. Protect against invalid data.
                        if (episode.getPlays() != null && episode.getPlays() >= 1) {
                            playsOrNull = episode.getPlays();
                        } else {
                            playsOrNull = 1;
                        }
                    } else {
                        // Skipped or not watched.
                        playsOrNull = 0;
                    }

                    // record the latest last watched time and episode ID for a show
                    if (!EpisodeTools.isUnwatched(watchedFlag)) {
                        ShowLastWatchedInfo lastWatchedInfo = showIdsToLastWatched.get(showId);
                        // episodes returned in reverse chrono order, so just get the first time
                        if (lastWatchedInfo == null && episode.getUpdatedAt() != null) {
                            long updatedAtMs = episode.getUpdatedAt().getValue();
                            showIdsToLastWatched.put(showId,
                                    new ShowLastWatchedInfo(updatedAtMs, episode.getSeasonNumber(),
                                            episode.getEpisodeNumber()
                                    ));
                        }
                    }
                }

                batch.add(new SgEpisode2UpdateByNumber(
                        showId,
                        episode.getEpisodeNumber(),
                        episode.getSeasonNumber(),
                        watchedFlag,
                        playsOrNull,
                        episode.getIsInCollection()
                ));
            }

            // execute database update
            database.sgEpisode2Helper().updateWatchedAndCollectedByNumber(batch);
        }

        if (!showIdsToLastWatched.isEmpty()) {
            // Note: it is possible that this overwrites a more recently watched episode,
            // however, the next sync should contain this episode and restore it.
            database.sgShow2Helper()
                    .updateLastWatchedMsIfLaterAndLastWatchedEpisodeId(showIdsToLastWatched,
                            database.sgEpisode2Helper());
        }

        // store new last sync time
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putLong(HexagonSettings.KEY_LAST_SYNC_EPISODES, currentTime)
                .apply();

        return true;
    }

    /**
     * Downloads watched, skipped or collected episodes of this show from Hexagon and applies
     * those flags and plays to episodes in the database.
     * <p>
     * If a TVDB ID is given tries to use legacy data if no data using TMDB ID is found.
     *
     * @return Whether the download was successful and all changes were applied to the database.
     */
    public boolean downloadFlags(long showId, int showTmdbId, Integer showTvdbId) {
        Timber.d("downloadFlags: for show %s", showId);

        DownloadFlagsResult result = downloadFlagsByTmdbId(showId, showTmdbId);
        if (result.getNoData() && showTvdbId != null) {
            // If no data by TMDB ID, try to get legacy data by TVDB ID.
            Timber.d("downloadFlags: no data by TMDB ID, trying by TVDB ID");
            result = downloadFlagsByTvdbId(showId, showTvdbId);
            if (result.getSuccess()) {
                // If had to use legacy show data, schedule episode upload (using TMDB IDs).
                SgRoomDatabase.getInstance(context).sgShow2Helper()
                        .setHexagonMergeNotCompleted(showId);
            }
        }

        if (result.getLastWatchedMs() != null) {
            SgRoomDatabase.getInstance(context).sgShow2Helper()
                    .updateLastWatchedMsIfLater(showId, result.getLastWatchedMs());
        }

        return result.getSuccess();
    }

    private DownloadFlagsResult downloadFlagsByTmdbId(long showId, int showTmdbId) {
        List<SgCloudEpisode> episodes;
        boolean onFirstPage = true;
        boolean hasMoreEpisodes = true;
        String cursor = null;

        Long lastWatchedMs = null;
        while (hasMoreEpisodes) {
            // abort if connection is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                Timber.e("downloadFlags: no network connection");
                return DownloadFlagsResult.FAILED;
            }

            try {
                // get service each time to check if auth was removed
                Episodes episodesService = hexagonTools.getEpisodesService();
                if (episodesService == null) {
                    return DownloadFlagsResult.FAILED;
                }

                // build request
                Episodes.GetSgEpisodes request = episodesService.getSgEpisodes()
                        .setShowTmdbId(showTmdbId); // use default server limit
                if (!TextUtils.isEmpty(cursor)) {
                    request.setCursor(cursor);
                }

                // execute request
                SgCloudEpisodeList response = request.execute();
                if (response == null) {
                    // If empty should send status 200 and empty list, so no body is a failure.
                    return DownloadFlagsResult.FAILED;
                }

                episodes = response.getEpisodes();

                // check for more items
                if (response.getCursor() != null) {
                    cursor = response.getCursor();
                } else {
                    hasMoreEpisodes = false;
                }
            } catch (IOException | IllegalArgumentException e) {
                // Note: JSON parser may throw IllegalArgumentException.
                Errors.logAndReportHexagon("get episodes of show", e);
                return DownloadFlagsResult.FAILED;
            }

            if (episodes == null || episodes.size() == 0) {
                if (onFirstPage) {
                    // If there is no data by TMDB ID at all, try again using TVDB ID.
                    return DownloadFlagsResult.NO_DATA;
                } else {
                    // no more updates to apply
                    break;
                }
            }
            onFirstPage = false;

            // build batch of episode flag updates
            ArrayList<SgEpisode2UpdateByNumber> batch = new ArrayList<>();
            for (SgCloudEpisode episode : episodes) {
                Pair<SgEpisode2UpdateByNumber, Long> update = buildSgEpisodeUpdate(
                        episode.getWatchedFlag(),
                        episode.getPlays(),
                        episode.getIsInCollection(),
                        episode.getUpdatedAt(),
                        episode.getEpisodeNumber(),
                        episode.getSeasonNumber(),
                        showId,
                        lastWatchedMs
                );
                if (update != null) {
                    batch.add(update.first);
                    lastWatchedMs = update.second;
                }
            }

            // execute database update
            SgRoomDatabase.getInstance(context).sgEpisode2Helper()
                    .updateWatchedAndCollectedByNumber(batch);
        }

        return new DownloadFlagsResult(true, false, lastWatchedMs);
    }

    private DownloadFlagsResult downloadFlagsByTvdbId(long showId, int showTvdbId) {
        List<Episode> episodes;
        boolean hasMoreEpisodes = true;
        String cursor = null;

        Long lastWatchedMs = null;
        while (hasMoreEpisodes) {
            // abort if connection is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                Timber.e("downloadFlags: no network connection");
                return DownloadFlagsResult.FAILED;
            }

            try {
                // get service each time to check if auth was removed
                Episodes episodesService = hexagonTools.getEpisodesService();
                if (episodesService == null) {
                    return DownloadFlagsResult.FAILED;
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
                    // If empty should send status 200 and empty list, so no body is a failure.
                    return DownloadFlagsResult.FAILED;
                }

                episodes = response.getEpisodes();

                // check for more items
                if (response.getCursor() != null) {
                    cursor = response.getCursor();
                } else {
                    hasMoreEpisodes = false;
                }
            } catch (IOException | IllegalArgumentException e) {
                // Note: JSON parser may throw IllegalArgumentException.
                Errors.logAndReportHexagon("get episodes of show", e);
                return DownloadFlagsResult.FAILED;
            }

            if (episodes == null || episodes.size() == 0) {
                // nothing to do here
                break;
            }

            // build batch of episode flag updates
            ArrayList<SgEpisode2UpdateByNumber> batch = new ArrayList<>();
            for (Episode episode : episodes) {
                Pair<SgEpisode2UpdateByNumber, Long> update = buildSgEpisodeUpdate(
                        episode.getWatchedFlag(),
                        episode.getPlays(),
                        episode.getIsInCollection(),
                        episode.getUpdatedAt(),
                        episode.getEpisodeNumber(),
                        episode.getSeasonNumber(),
                        showId,
                        lastWatchedMs
                );
                if (update != null) {
                    batch.add(update.first);
                    lastWatchedMs = update.second;
                }
            }

            // execute database update
            SgRoomDatabase.getInstance(context).sgEpisode2Helper()
                    .updateWatchedAndCollectedByNumber(batch);
        }

        return new DownloadFlagsResult(true, false, lastWatchedMs);
    }

    @Nullable
    private Pair<SgEpisode2UpdateByNumber, Long> buildSgEpisodeUpdate(
            Integer watchedFlag,
            Integer plays,
            Boolean isInCollection,
            DateTime updatedAt,
            int episodeNumber,
            int seasonNumber,
            long showId,
            Long lastWatchedMs
    ) {
        Integer watchedFlagOrNull = null;
        Integer playsOrNull = null;
        if (watchedFlag != null && watchedFlag != EpisodeFlags.UNWATCHED) {
            // Watched or skipped.
            watchedFlagOrNull = watchedFlag;
            if (watchedFlag == EpisodeFlags.WATCHED) {
                // Note: plays may be null for legacy data. Protect against invalid data.
                if (plays != null && plays >= 1) {
                    playsOrNull = plays;
                } else {
                    playsOrNull = 1;
                }
            }
            // record last watched time by taking latest updatedAt of watched/skipped
            if (updatedAt != null) {
                long lastWatchedMsNew = updatedAt.getValue();
                if (lastWatchedMs == null || lastWatchedMs < lastWatchedMsNew) {
                    lastWatchedMs = lastWatchedMsNew;
                }
            }
        }

        boolean inCollection = isInCollection != null && isInCollection;

        if (watchedFlag == null && !inCollection) {
            // skip if episode has no watched flag and is not in collection
            return null;
        }

        return new Pair<>(
                new SgEpisode2UpdateByNumber(
                        showId,
                        episodeNumber,
                        seasonNumber,
                        watchedFlagOrNull,
                        playsOrNull,
                        isInCollection
                ),
                lastWatchedMs
        );
    }

    /**
     * Uploads all watched, skipped including plays or collected episodes of this show to Hexagon.
     *
     * @return Whether the upload was successful.
     */
    boolean uploadFlags(long showId, int showTmdbId) {
        // query for watched, skipped or collected episodes
        List<SgEpisode2ForSync> episodesForSync = SgRoomDatabase.getInstance(context)
                .sgEpisode2Helper()
                .getEpisodesForHexagonSync(showId);
        if (episodesForSync.isEmpty()) {
            Timber.d("uploadFlags: uploading none for show %d", showId);
            return true;
        } else {
            // Issues with some requests failing at Cloud due to
            // EOFException: Unexpected end of ZLIB input stream
            // Using info log to report sizes that are uploaded to determine
            // if MAX_BATCH_SIZE is actually too large.
            // https://github.com/UweTrottmann/SeriesGuide/issues/781
            Timber.i("uploadFlags: uploading %d for show %d", episodesForSync.size(), showId);
        }

        // build list of episodes to upload
        List<SgCloudEpisode> episodes = new ArrayList<>();
        int count = episodesForSync.size();
        for (int i = 0; i < count; i++) {
            SgEpisode2ForSync episodeForSync = episodesForSync.get(i);

            SgCloudEpisode episode = new SgCloudEpisode();
            episode.setSeasonNumber(episodeForSync.getSeason());
            episode.setEpisodeNumber(episodeForSync.getNumber());

            int watchedFlag = episodeForSync.getWatched();
            if (!EpisodeTools.isUnwatched(watchedFlag)) {
                // Skipped or watched.
                episode.setWatchedFlag(watchedFlag);
                episode.setPlays(episodeForSync.getPlays());
            }

            if (episodeForSync.getCollected()) {
                episode.setIsInCollection(true);
            }

            episodes.add(episode);

            // upload a batch
            boolean isLast = i + 1 == count;
            if (episodes.size() == MAX_BATCH_SIZE || isLast) {
                SgCloudEpisodeList episodeList = new SgCloudEpisodeList();
                episodeList.setEpisodes(episodes);
                episodeList.setShowTmdbId(showTmdbId);

                try {
                    // get service each time to check if auth was removed
                    Episodes episodesService = hexagonTools.getEpisodesService();
                    if (episodesService == null) {
                        return false;
                    }
                    episodesService.saveSgEpisodes(episodeList).execute();
                } catch (IOException e) {
                    // abort
                    Errors.logAndReportHexagon("save episodes of show", e);
                    return false;
                }

                // clear array
                episodes = new ArrayList<>();
            }
        }

        return true;
    }
}
