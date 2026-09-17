// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.movies.details

import java.util.Date

/**
 * Holds data and state for [MovieDetailsFragment].
 */
data class UiMovieDetails(
    val imdbId: String?,
    val traktId: Int?,
    val title: String?,
    val titleForMetacritic: String?,
    val overview: String?,
    val inCollection: Boolean,
    val inWatchlist: Boolean,
    val watched: Boolean,
    var plays: Int,
    val releaseDate: Date?,
    /**
     * Release date and runtime: "July 17, 2009 · 1 h 5 min"
     */
    val releaseDateAndRunningTime: String,
    /**
     * When this movie was last updated by this app.
     */
    val lastUpdatedText: String,
    val isShareButtonEnabled: Boolean,
    val isCalendarButtonGone: Boolean,
    val isCheckInButtonGone: Boolean,
    val watchedButtonText: String,
    val tmdbCollectionId: Int?,
    val tmdbCollectionName: String?,
    val tmdbRating: String,
    val tmdbVotes: String,
    val traktRating: String,
    val traktVotes: String,
    val userRating: Int?,
    val userRatingText: String,
    val genres: String,
    val tmdbUrl: String,
    val traktUrl: String?,
    val posterSmallSizeImageUrl: String?,
    val posterOriginalSizeImageUrl: String?
)
