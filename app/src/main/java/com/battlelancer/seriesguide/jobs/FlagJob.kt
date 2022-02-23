package com.battlelancer.seriesguide.jobs

import android.content.Context

interface FlagJob {

    fun supportsHexagon(): Boolean

    fun supportsTrakt(): Boolean

    /**
     * If requested, prepares a network job. Applies local changes, then persists the network job.
     *
     * @return If local changes were applied successfully and if requested the network op was
     * persisted.
     */
    fun applyLocalChanges(context: Context, requiresNetworkJob: Boolean): Boolean

    /**
     * A message to be shown to the user that an action has completed (locally).
     */
    fun getConfirmationText(context: Context): String
}