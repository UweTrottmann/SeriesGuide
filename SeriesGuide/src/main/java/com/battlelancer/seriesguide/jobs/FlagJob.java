package com.battlelancer.seriesguide.jobs;

import android.content.Context;
import android.support.annotation.NonNull;

public interface FlagJob {

    boolean supportsHexagon();

    boolean supportsTrakt();

    int getFlagValue();

    /**
     * If requested, prepares a network job. Applies local changes, then persists the network job.
     *
     * @return If local changes were applied successfully and if requested the network op was
     * persisted.
     */
    boolean applyLocalChanges(Context context, boolean requiresNetworkJob);

    /**
     * A message to be shown to the user that an action has completed (locally).
     */
    @NonNull
    String getConfirmationText(Context context);
}
