// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.streaming

import androidx.room.Entity

@Entity(
    tableName = "sg_watch_provider_show_mappings",
    primaryKeys = ["provider_id", "show_id"],
)
data class SgWatchProviderShowMapping(
    /**
     * The row ID of a [SgWatchProvider]. Safe in case the external provider ID changes.
     */
    val provider_id: Long,
    /**
     * The row ID of a [SgShow2].
     */
    val show_id: Long
)
