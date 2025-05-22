// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2025 Uwe Trottmann

package com.battlelancer.seriesguide.shows.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SeasonsColumns
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ShowsColumns
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables

/**
 * Legacy episode entity kept for migration of legacy data. See [SgEpisode2].
 */
@Entity(
    tableName = Tables.EPISODES,
    foreignKeys = [
        ForeignKey(
            entity = SgSeason::class,
            parentColumns = [Seasons._ID],
            childColumns = [SeasonsColumns.REF_SEASON_ID]
        ),
        ForeignKey(
            entity = SgShow::class,
            parentColumns = [Shows._ID],
            childColumns = [ShowsColumns.REF_SHOW_ID]
        )
    ],
    indices = [
        Index(SeasonsColumns.REF_SEASON_ID),
        Index(ShowsColumns.REF_SHOW_ID)
    ]
)
data class SgEpisode(
    @PrimaryKey
    @ColumnInfo(name = Episodes._ID)
    var tvdbId: Int,

    @ColumnInfo(name = Episodes.TITLE)
    var title: String = "",
    @ColumnInfo(name = Episodes.OVERVIEW)
    var overview: String? = null,

    @ColumnInfo(name = Episodes.NUMBER)
    var number: Int = 0,
    @ColumnInfo(name = Episodes.SEASON)
    var season: Int = 0,
    @ColumnInfo(name = Episodes.DVDNUMBER)
    var dvdNumber: Double? = null,

    @ColumnInfo(name = SeasonsColumns.REF_SEASON_ID)
    var seasonTvdbId: Int,
    @ColumnInfo(name = ShowsColumns.REF_SHOW_ID)
    var showTvdbId: Int,

    @ColumnInfo(name = Episodes.WATCHED)
    var watched: Int = 0,
    @ColumnInfo(name = Episodes.PLAYS)
    var plays: Int? = 0,

    @ColumnInfo(name = Episodes.DIRECTORS)
    var directors: String? = "",
    @ColumnInfo(name = Episodes.GUESTSTARS)
    var guestStars: String? = "",
    @ColumnInfo(name = Episodes.WRITERS)
    var writers: String? = "",
    @ColumnInfo(name = Episodes.IMAGE)
    var image: String? = "",

    @ColumnInfo(name = Episodes.FIRSTAIREDMS)
    var firstReleasedMs: Long = -1L,

    @ColumnInfo(name = Episodes.COLLECTED)
    var collected: Boolean = false,

    @ColumnInfo(name = Episodes.RATING_GLOBAL)
    var ratingGlobal: Double? = null,
    @ColumnInfo(name = Episodes.RATING_VOTES)
    var ratingVotes: Int? = null,
    @ColumnInfo(name = Episodes.RATING_USER)
    var ratingUser: Int? = null,

    @ColumnInfo(name = Episodes.IMDBID)
    var imdbId: String? = "",

    @ColumnInfo(name = Episodes.LAST_EDITED)
    var lastEditedSec: Long = 0L,

    @ColumnInfo(name = Episodes.ABSOLUTE_NUMBER)
    var absoluteNumber: Int? = null,

    @ColumnInfo(name = Episodes.LAST_UPDATED)
    var lastUpdatedSec: Long = 0L
)
