// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.episodes;

/**
 * {@link #WATCHED}, {@link #SKIPPED} and {@link #UNWATCHED} flag for episode watched state.
 */
public interface EpisodeFlags {

    int UNWATCHED = 0x0;

    int WATCHED = 0x1;

    int SKIPPED = 0x2;

}
