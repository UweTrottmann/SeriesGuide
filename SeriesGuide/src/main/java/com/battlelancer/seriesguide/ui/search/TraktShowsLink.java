package com.battlelancer.seriesguide.ui.search;

import android.support.annotation.StringRes;
import android.support.v4.util.SparseArrayCompat;
import com.battlelancer.seriesguide.R;

public enum TraktShowsLink {
    POPULAR(0, R.string.title_popular),
    RECOMMENDED(1, R.string.recommended),
    WATCHED(2, R.string.watched_shows),
    COLLECTION(3, R.string.shows_collection),
    WATCHLIST(4, R.string.watchlist);

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
