// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: GPL-3.0-or-later

package com.battlelancer.seriesguide.traktapi;

/**
 * The trakt action to be performed by {@link TraktTask}.
 */
public enum TraktAction {
    CHECKIN_EPISODE,
    CHECKIN_MOVIE,
    COMMENT
}
