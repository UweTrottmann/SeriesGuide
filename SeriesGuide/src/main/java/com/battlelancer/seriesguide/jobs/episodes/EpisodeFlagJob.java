package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import android.support.annotation.Nullable;
import com.uwetrottmann.seriesguide.backend.episodes.model.Episode;
import com.uwetrottmann.trakt5.entities.SyncSeason;
import java.util.List;

public interface EpisodeFlagJob {
    int getShowTvdbId();

    int getFlagValue();

    JobAction getAction();

    List<Episode> getEpisodesForHexagon(Context context);

    /**
     * Return {@code null} to upload the complete show.
     */
    @Nullable
    List<SyncSeason> getEpisodesForTrakt(Context context);

    boolean applyLocalChanges(Context context);

    /**
     * Tells for example which episode was flagged watched.
     */
    @Nullable
    String getConfirmationText(Context context);
}
