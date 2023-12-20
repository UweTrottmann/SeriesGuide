// SPDX-License-Identifier: Apache-2.0
// Copyright 2017, 2018, 2022, 2023 Uwe Trottmann

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

}