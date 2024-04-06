// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover;

import androidx.annotation.StringRes;
import androidx.collection.SparseArrayCompat;
import com.battlelancer.seriesguide.R;

public enum DiscoverShowsLink {
    POPULAR(0, R.string.title_popular),
    WATCHED(1, R.string.watched_shows),
    COLLECTION(2, R.string.shows_collection),
    WATCHLIST(3, R.string.watchlist);

    final int id;
    final int titleRes;

    DiscoverShowsLink(int id, @StringRes int titleRes) {
        this.id = id;
        this.titleRes = titleRes;
    }

    private static final SparseArrayCompat<DiscoverShowsLink> MAPPING = new SparseArrayCompat<>();

    static {
        for (DiscoverShowsLink link : values()) {
            MAPPING.put(link.id, link);
        }
    }

    static DiscoverShowsLink fromId(int id) {
        DiscoverShowsLink discoverShowsLink = MAPPING.get(id);
        return discoverShowsLink == null ? POPULAR : discoverShowsLink;
    }
}
