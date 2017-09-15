package com.battlelancer.seriesguide.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.jobs.episodes.JobAction;
import com.battlelancer.seriesguide.sync.HexagonEpisodeSync;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.uwetrottmann.seriesguide.backend.episodes.Episodes;
import com.uwetrottmann.seriesguide.backend.episodes.model.Episode;
import com.uwetrottmann.seriesguide.backend.episodes.model.EpisodeList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.battlelancer.seriesguide.jobs.episodes.JobAction.*;

public class HexagonEpisodeJob extends NetworkJob {

    @NonNull private final HexagonTools hexagonTools;

    public HexagonEpisodeJob(@NonNull HexagonTools hexagonTools, JobAction action,
            SgJobInfo jobInfo) {
        super(action, jobInfo);
        this.hexagonTools = hexagonTools;
    }

    public int upload(Context context) {
        EpisodeList uploadWrapper = new EpisodeList();
        uploadWrapper.setShowTvdbId(jobInfo.showTvdbId());

        // upload in small batches
        List<Episode> smallBatch = new ArrayList<>();
        final List<Episode> episodes = getEpisodesForHexagon();
        while (!episodes.isEmpty()) {
            // batch small enough?
            if (episodes.size() <= HexagonEpisodeSync.MAX_BATCH_SIZE) {
                smallBatch = episodes;
            } else {
                // build smaller batch
                for (int count = 0; count < HexagonEpisodeSync.MAX_BATCH_SIZE; count++) {
                    if (episodes.isEmpty()) {
                        break;
                    }
                    smallBatch.add(episodes.remove(0));
                }
            }

            // upload
            uploadWrapper.setEpisodes(smallBatch);
            if (!uploadFlagsToHexagon(context, hexagonTools, uploadWrapper)) {
                return NetworkJob.ERROR_HEXAGON_API;
            }

            // prepare for next batch
            smallBatch.clear();
        }

        return NetworkJob.SUCCESS;
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

    /**
     * Builds a list of episodes ready to upload to hexagon. However, the show TVDb id is not set.
     * It should be set in a wrapping {@link com.uwetrottmann.seriesguide.backend.episodes.model.EpisodeList}.
     */
    @NonNull
    public List<Episode> getEpisodesForHexagon() {
        boolean isWatchedNotCollected;
        if (action == EPISODE_WATCHED_FLAG) {
            isWatchedNotCollected = true;
        } else if (action == EPISODE_COLLECTION) {
            isWatchedNotCollected = false;
        } else {
            throw new IllegalArgumentException("Action " + action + " not supported.");
        }

        List<Episode> episodes = new ArrayList<>();
        for (int i = 0; i < jobInfo.episodesLength(); i++) {
            EpisodeInfo episodeInfo = jobInfo.episodes(i);

            Episode episode = new Episode();
            episode.setSeasonNumber(episodeInfo.season());
            episode.setEpisodeNumber(episodeInfo.number());
            if (isWatchedNotCollected) {
                episode.setWatchedFlag(jobInfo.flagValue());
            } else {
                episode.setIsInCollection(EpisodeTools.isCollected(jobInfo.flagValue()));
            }

            episodes.add(episode);
        }
        return episodes;
    }
}
