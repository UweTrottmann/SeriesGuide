package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import android.support.annotation.NonNull;

public interface EpisodeFlagJob {

    int getFlagValue();

    /**
     * If requested, prepares a network job. Applies local changes, then persists the network job.
     *
     * @return If local changes were applied successfully and if requested the network op was
     * persisted.
     */
    boolean applyLocalChanges(Context context, boolean requiresNetworkJob);

    /**
     * Tells for example which episode was flagged watched.
     */
    @NonNull
    String getConfirmationText(Context context);
}
