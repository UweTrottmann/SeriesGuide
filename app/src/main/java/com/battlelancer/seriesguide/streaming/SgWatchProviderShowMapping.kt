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
     * The provider ID of a [SgWatchProvider].
     */
    val provider_id: Int,
    /**
     * The row ID of a [SgShow2].
     */
    val show_id: Long
)
