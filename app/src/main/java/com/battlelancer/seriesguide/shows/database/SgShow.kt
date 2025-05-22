// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2025 Uwe Trottmann

package com.battlelancer.seriesguide.shows.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables

/**
 * Legacy show entity kept for migration of legacy data. See [SgShow2].
 */
@Entity(tableName = Tables.SHOWS)
data class SgShow(
    @PrimaryKey
    @ColumnInfo(name = Shows._ID)
    var tvdbId: Int,

    @ColumnInfo(name = Shows.SLUG)
    var slug: String? = "",

    /**
     * Ensure this is NOT null (enforced through database constraint).
     */
    @ColumnInfo(name = Shows.TITLE)
    var title: String = "",

    /**
     * The title without any articles (e.g. 'the' or 'an'). Added with db version 33.
     */
    @ColumnInfo(name = Shows.TITLE_NOARTICLE)
    var titleNoArticle: String? = null,

    @ColumnInfo(name = Shows.OVERVIEW)
    var overview: String? = "",

    /**
     * Local release time. Encoded as integer (hhmm).
     *
     * ```
     * Example: 2035
     * Default: -1
     * ```
     */
    @ColumnInfo(name = Shows.RELEASE_TIME)
    var releaseTime: Int? = null,
    /**
     * Local release week day. Encoded as integer.
     *
     * ```
     * Range:   1-7
     * Daily:   0
     * Default: -1
     * ```
     */
    @ColumnInfo(name = Shows.RELEASE_WEEKDAY)
    var releaseWeekDay: Int? = null,
    @ColumnInfo(name = Shows.RELEASE_COUNTRY)
    var releaseCountry: String? = null,
    @ColumnInfo(name = Shows.RELEASE_TIMEZONE)
    var releaseTimeZone: String? = null,

    @ColumnInfo(name = Shows.FIRST_RELEASE)
    var firstRelease: String? = null,

    @ColumnInfo(name = Shows.GENRES)
    var genres: String? = "",
    @ColumnInfo(name = Shows.NETWORK)
    var network: String? = "",

    @ColumnInfo(name = Shows.RATING_GLOBAL)
    var ratingGlobal: Double? = null,
    @ColumnInfo(name = Shows.RATING_VOTES)
    var ratingVotes: Int? = null,
    @ColumnInfo(name = Shows.RATING_USER)
    var ratingUser: Int? = null,

    @ColumnInfo(name = Shows.RUNTIME)
    var runtime: String? = "",
    @ColumnInfo(name = Shows.STATUS)
    var status: String? = "",
    @ColumnInfo(name = Shows.CONTENTRATING)
    var contentRating: String? = "",

    @ColumnInfo(name = Shows.NEXTEPISODE)
    var nextEpisode: String? = "",

    @ColumnInfo(name = Shows.POSTER)
    var poster: String? = "",

    @ColumnInfo(name = Shows.POSTER_SMALL)
    var posterSmall: String? = "",

    @ColumnInfo(name = Shows.NEXTAIRDATEMS)
    var nextAirdateMs: Long? = null,
    @ColumnInfo(name = Shows.NEXTTEXT)
    var nextText: String? = "",

    @ColumnInfo(name = Shows.IMDBID)
    var imdbId: String? = "",
    @ColumnInfo(name = Shows.TRAKT_ID)
    var traktId: Int? = 0,

    @ColumnInfo(name = Shows.FAVORITE)
    var favorite: Boolean = false,

    @ColumnInfo(name = Shows.HEXAGON_MERGE_COMPLETE)
    var hexagonMergeComplete: Boolean = true,

    @ColumnInfo(name = Shows.HIDDEN)
    var hidden: Boolean = false,

    @ColumnInfo(name = Shows.LASTUPDATED)
    var lastUpdatedMs: Long = 0L,
    @ColumnInfo(name = Shows.LASTEDIT)
    var lastEditedSec: Long = 0L,

    @ColumnInfo(name = Shows.LASTWATCHEDID)
    var lastWatchedEpisodeId: Int = 0,
    @ColumnInfo(name = Shows.LASTWATCHED_MS)
    var lastWatchedMs: Long = 0L,

    @ColumnInfo(name = Shows.LANGUAGE)
    var language: String? = "",

    @ColumnInfo(name = Shows.UNWATCHED_COUNT)
    var unwatchedCount: Int = SgShow2.UNKNOWN_UNWATCHED_COUNT,

    @ColumnInfo(name = Shows.NOTIFY)
    var notify: Boolean = true
)
