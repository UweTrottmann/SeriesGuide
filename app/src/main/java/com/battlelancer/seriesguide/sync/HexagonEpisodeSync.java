package com.battlelancer.seriesguide.sync;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
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
    public boolean downloadChangedFlags(@NonNull Map<Integer, Long> tvdbIdsToShowIds) {
        long currentTime = System.currentTimeMillis();
        SgRoomDatabase database = SgRoomDatabase.getInstance(context);
        DateTime lastSyncTime = new DateTime(HexagonSettings.getLastEpisodesSyncTime(context));
        Timber.d("downloadChangedFlags: since %s", lastSyncTime);

        List<Episode> episodes;
        String cursor = null;
        boolean hasMoreEpisodes = true;
        Map<Long, Long> showIdsToLastWatched = new HashMap<>();
        while (hasMoreEpisodes) {
            try {
                // get service each time to check if auth was removed
                Episodes episodesService = hexagonTools.getEpisodesService();
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
            for (Episode episode : episodes) {
                Integer showTvdbId = episode.getShowTvdbId();
                Long showId = tvdbIdsToShowIds.get(showTvdbId);
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

                    // record the latest last watched time for a show
                    if (!EpisodeTools.isUnwatched(watchedFlag)) {
                        Long lastWatchedMs = showIdsToLastWatched.get(showId);
                        // episodes returned in reverse chrono order, so just get the first time
                        if (lastWatchedMs == null && episode.getUpdatedAt() != null) {
                            long updatedAtMs = episode.getUpdatedAt().getValue();
                            showIdsToLastWatched.put(showId, updatedAtMs);
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
            database.sgShow2Helper().updateLastWatchedMsIfLater(showIdsToLastWatched);
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
     *
     * @return Whether the download was successful and all changes were applied to the database.
     */
    public boolean downloadFlags(long showId, int showTvdbId) {
        Timber.d("downloadFlags: for show %s", showTvdbId);
        List<Episode> episodes;
        boolean hasMoreEpisodes = true;
        String cursor = null;

        Long lastWatchedMs = null;
        while (hasMoreEpisodes) {
            // abort if connection is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                Timber.e("downloadFlags: no network connection");
                return false;
            }

            try {
                // get service each time to check if auth was removed
                Episodes episodesService = hexagonTools.getEpisodesService();
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
            } catch (IOException | IllegalArgumentException e) {
                // Note: JSON parser may throw IllegalArgumentException.
                Errors.logAndReportHexagon("get episodes of show", e);
                return false;
            }

            if (episodes == null || episodes.size() == 0) {
                // nothing to do here
                break;
            }

            // build batch of episode flag updates
            ArrayList<SgEpisode2UpdateByNumber> batch = new ArrayList<>();
            for (Episode episode : episodes) {
                Integer watchedFlag = episode.getWatchedFlag();
                Integer watchedFlagOrNull = null;
                Integer playsOrNull = null;
                if (watchedFlag != null && watchedFlag != EpisodeFlags.UNWATCHED) {
                    // Watched or skipped.
                    watchedFlagOrNull = watchedFlag;
                    if (watchedFlag == EpisodeFlags.WATCHED) {
                        // Note: plays may be null for legacy data. Protect against invalid data.
                        if (episode.getPlays() != null && episode.getPlays() >= 1) {
                            playsOrNull = episode.getPlays();
                        } else {
                            playsOrNull = 1;
                        }
                    }
                    // record last watched time by taking latest updatedAt of watched/skipped
                    DateTime updatedAt = episode.getUpdatedAt();
                    if (updatedAt != null) {
                        long lastWatchedMsNew = updatedAt.getValue();
                        if (lastWatchedMs == null || lastWatchedMs < lastWatchedMsNew) {
                            lastWatchedMs = lastWatchedMsNew;
                        }
                    }
                }

                boolean inCollection = episode.getIsInCollection() != null
                        && episode.getIsInCollection();

                if (watchedFlag == null && !inCollection) {
                    // skip if episode has no watched flag and is not in collection
                    continue;
                }

                batch.add(new SgEpisode2UpdateByNumber(
                        showId,
                        episode.getEpisodeNumber(),
                        episode.getSeasonNumber(),
                        watchedFlagOrNull,
                        playsOrNull,
                        episode.getIsInCollection()
                ));
            }

            // execute database update
            SgRoomDatabase.getInstance(context).sgEpisode2Helper()
                    .updateWatchedAndCollectedByNumber(batch);
        }

        if (lastWatchedMs != null) {
            SgRoomDatabase.getInstance(context).sgShow2Helper()
                    .updateLastWatchedMsIfLater(showId, lastWatchedMs);
        }

        return true;
    }

    /**
     * Uploads all watched, skipped including plays or collected episodes of this show to Hexagon.
     *
     * @return Whether the upload was successful.
     */
    public boolean uploadFlags(long showId, int showTvdbId) {
        Timber.d("uploadFlags: for show %s", showId);

        // query for watched, skipped or collected episodes
        List<SgEpisode2ForSync> episodesForSync = SgRoomDatabase.getInstance(context)
                .sgEpisode2Helper()
                .getEpisodesForHexagonSync(showId);
        if (episodesForSync.isEmpty()) {
            Timber.d("uploadFlags: no flags to upload");
            return true;
        }

        // build list of episodes to upload
        List<Episode> episodes = new ArrayList<>();
        int count = episodesForSync.size();
        for (int i = 0; i < count; i++) {
            SgEpisode2ForSync episodeForSync = episodesForSync.get(i);

            Episode episode = new Episode();
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
                EpisodeList episodeList = new EpisodeList();
                episodeList.setEpisodes(episodes);
                episodeList.setShowTvdbId(showTvdbId);

                try {
                    // get service each time to check if auth was removed
                    Episodes episodesService = hexagonTools.getEpisodesService();
                    if (episodesService == null) {
                        return false;
                    }
                    episodesService.save(episodeList).execute();
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
