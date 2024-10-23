// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

/**
 * Event posted when a show is about to be added to the database.
 */
class OnAddingShowEvent(
    /**
     * Is -1 if adding all shows this lists.
     */
    val showTmdbId: Int
) {
    /**
     * Sets TMDB id to -1 to indicate all shows of this are added.
     */
    constructor() : this(-1)
}