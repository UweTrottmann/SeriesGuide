package com.battlelancer.seriesguide.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgSeason2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags

@Entity(
    tableName = "sg_episode",
    foreignKeys = [ForeignKey(
        entity = SgShow2::class,
        parentColumns = [SgShow2Columns._ID],
        childColumns = [SgShow2Columns.REF_SHOW_ID]
    )],
    indices = [
        Index(SgSeason2Columns.REF_SEASON_ID),
        Index(SgShow2Columns.REF_SHOW_ID)
    ]
)
data class SgEpisode2(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = SgEpisode2Columns._ID) val id: Long = 0,
    @ColumnInfo(name = SgSeason2Columns.REF_SEASON_ID) val seasonId: Long,
    @ColumnInfo(name = SgShow2Columns.REF_SHOW_ID) val showId: Long,
    @ColumnInfo(name = SgEpisode2Columns.TMDB_ID) val tmdbId: Int?,
    @ColumnInfo(name = SgEpisode2Columns.TVDB_ID) val tvdbId: Int? = null,
    @ColumnInfo(name = SgEpisode2Columns.TITLE) val title: String? = "",
    @ColumnInfo(name = SgEpisode2Columns.OVERVIEW) val overview: String?,
    @ColumnInfo(name = SgEpisode2Columns.NUMBER) val number: Int = 0,
    @ColumnInfo(name = SgEpisode2Columns.ABSOLUTE_NUMBER) val absoluteNumber: Int? = null,
    @ColumnInfo(name = SgEpisode2Columns.SEASON) val season: Int = 0,
    @ColumnInfo(name = SgEpisode2Columns.ORDER) val order: Int = 0,
    @ColumnInfo(name = SgEpisode2Columns.DVDNUMBER) val dvdNumber: Double? = null,
    @ColumnInfo(name = SgEpisode2Columns.WATCHED) val watched: Int = EpisodeFlags.UNWATCHED,
    @ColumnInfo(name = SgEpisode2Columns.PLAYS) val plays: Int? = 0,
    @ColumnInfo(name = SgEpisode2Columns.COLLECTED) val collected: Boolean = false,
    @ColumnInfo(name = SgEpisode2Columns.DIRECTORS) val directors: String? = "",
    @ColumnInfo(name = SgEpisode2Columns.GUESTSTARS) val guestStars: String? = "",
    @ColumnInfo(name = SgEpisode2Columns.WRITERS) val writers: String? = "",
    @ColumnInfo(name = SgEpisode2Columns.IMAGE) val image: String? = "",
    @ColumnInfo(name = SgEpisode2Columns.FIRSTAIREDMS) val firstReleasedMs: Long = -1,
    @ColumnInfo(name = SgEpisode2Columns.RATING_GLOBAL) val ratingGlobal: Double? = null,
    @ColumnInfo(name = SgEpisode2Columns.RATING_VOTES) val ratingVotes: Int? = null,
    @ColumnInfo(name = SgEpisode2Columns.RATING_USER) val ratingUser: Int? = null,
    @ColumnInfo(name = SgEpisode2Columns.IMDBID) val imdbId: String? = "",
    @ColumnInfo(name = SgEpisode2Columns.LAST_EDITED) val lastEditedSec: Long = 0,
    @ColumnInfo(name = SgEpisode2Columns.LAST_UPDATED) val lastUpdatedSec: Long = 0
) {
    val playsOrZero: Int
        get() = plays ?: 0
}
