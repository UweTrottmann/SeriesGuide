// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.traktapi;

/**
 * The trakt action to be performed by {@link TraktTask}.
 */
public enum TraktAction {
    CHECKIN_EPISODE,
    CHECKIN_MOVIE,
    COMMENT
}
