// SPDX-License-Identifier: Apache-2.0
// Copyright 2013-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.episodes

/**
 * [WATCHED], [SKIPPED] and [UNWATCHED] flag for episode watched state.
 */
object EpisodeFlags {
    const val UNWATCHED: Int = 0x0

    const val WATCHED: Int = 0x1

    const val SKIPPED: Int = 0x2
}
