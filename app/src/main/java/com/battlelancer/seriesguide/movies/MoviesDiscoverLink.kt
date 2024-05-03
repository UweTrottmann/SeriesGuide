// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.movies;

import androidx.annotation.StringRes;
import androidx.collection.SparseArrayCompat;
import com.battlelancer.seriesguide.R;

public enum MoviesDiscoverLink {
    IN_THEATERS(0, R.string.movies_in_theatres),
    POPULAR(1, R.string.title_popular),
    DIGITAL(2, R.string.title_digital_releases),
    DISC(3, R.string.title_disc_releases),
    UPCOMING(4, R.string.upcoming);

    public final int id;
    public final int titleRes;

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

    public static MoviesDiscoverLink fromId(int id) {
        return MAPPING.get(id);
    }
}
