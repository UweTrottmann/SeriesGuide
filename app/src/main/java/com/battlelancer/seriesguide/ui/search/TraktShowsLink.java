package com.battlelancer.seriesguide.ui.search;

import androidx.annotation.StringRes;
import androidx.collection.SparseArrayCompat;
import com.battlelancer.seriesguide.R;

public enum TraktShowsLink {
    POPULAR(0, R.string.title_popular),
    WATCHED(1, R.string.watched_shows),
    COLLECTION(2, R.string.shows_collection),
    WATCHLIST(3, R.string.watchlist);

    final int id;
    final int titleRes;

    TraktShowsLink(int id, @StringRes int titleRes) {
        this.id = id;
        this.titleRes = titleRes;
    }

    private static final SparseArrayCompat<TraktShowsLink> MAPPING = new SparseArrayCompat<>();

    static {
        for (TraktShowsLink link : values()) {
            MAPPING.put(link.id, link);
        }
    }

    static TraktShowsLink fromId(int id) {
        TraktShowsLink traktShowsLink = MAPPING.get(id);
        return traktShowsLink == null ? POPULAR : traktShowsLink;
    }
}
