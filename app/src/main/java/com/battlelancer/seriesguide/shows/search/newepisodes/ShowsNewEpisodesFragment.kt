// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.newepisodes

import androidx.fragment.app.viewModels
import com.battlelancer.seriesguide.shows.search.popular.ShowsDiscoverPagingFragment

/**
 * Displays shows with new episodes with filters provided by
 * [com.battlelancer.seriesguide.shows.search.discover.DiscoverShowsActivity].
 */
class ShowsNewEpisodesFragment : ShowsDiscoverPagingFragment() {

    override val model: ShowsNewEpisodesViewModel by viewModels()

}