package com.battlelancer.seriesguide.ui.movies;

import android.support.annotation.StringRes;
import android.support.v4.util.SparseArrayCompat;
import com.battlelancer.seriesguide.R;

enum MoviesDiscoverLink {
    IN_THEATERS(0, R.string.movies_in_theatres),
    POPULAR(1, R.string.title_popular),
    DIGITAL(2, R.string.title_digital_releases),
    DISC(3, R.string.title_disc_releases);

    final int id;
    final int titleRes;

    MoviesDiscoverLink(int id, @StringRes int titleRes) {
        this.id = id;
        this.titleRes = titleRes;
    }

    private static final SparseArrayCompat<MoviesDiscoverLink> MAPPING = new SparseArrayCompat<>();

    static {
        for (MoviesDiscoverLink link : values()) {
            MAPPING.put(link.id, link);
        }
    }

    static MoviesDiscoverLink fromId(int id) {
        return MAPPING.get(id);
    }
}
