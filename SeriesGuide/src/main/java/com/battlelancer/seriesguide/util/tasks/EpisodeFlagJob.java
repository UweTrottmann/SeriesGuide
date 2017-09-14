package com.battlelancer.seriesguide.util.tasks;

import android.support.annotation.Nullable;
import com.uwetrottmann.seriesguide.backend.episodes.model.Episode;
import com.uwetrottmann.trakt5.entities.SyncSeason;
import java.util.List;

public interface EpisodeFlagJob {
    int getShowTvdbId();

    int getFlagValue();

    EpisodeFlagJobs.Action getAction();

    List<Episode> getEpisodesForHexagon();

    /**
     * Return {@code null} to upload the complete show.
     */
    @Nullable
    List<SyncSeason> getEpisodesForTrakt();

    boolean applyLocalChanges();

    /**
     * Tells for example which episode was flagged watched.
     */
    @Nullable
    String getConfirmationText();
}
