// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.popular

import androidx.fragment.app.viewModels
import com.battlelancer.seriesguide.shows.search.discover.ShowsDiscoverPagingFragment

/**
 * Displays popular shows with filters provided by
 * [com.battlelancer.seriesguide.shows.search.discover.DiscoverShowsActivity].
 */
class ShowsPopularFragment : ShowsDiscoverPagingFragment() {

    override val model: ShowsPopularViewModel by viewModels()

}