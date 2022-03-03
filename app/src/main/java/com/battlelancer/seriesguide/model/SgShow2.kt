package com.battlelancer.seriesguide.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows
import com.battlelancer.seriesguide.ui.shows.ShowTools
import com.battlelancer.seriesguide.util.NextEpisodeUpdater

@Entity(
    tableName = "sg_show",
    indices = [
        Index(SgShow2Columns.TMDB_ID),
        Index(SgShow2Columns.TVDB_ID)
    ]
)
data class SgShow2(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = SgShow2Columns._ID) val id: Long = 0,
    @ColumnInfo(name = SgShow2Columns.TMDB_ID) val tmdbId: Int?,
    @ColumnInfo(name = SgShow2Columns.TVDB_ID) val tvdbId: Int?,
    @ColumnInfo(name = SgShow2Columns.SLUG) val slug: String? = "",
    @ColumnInfo(name = SgShow2Columns.TRAKT_ID) val traktId: Int? = 0,
    /**
     * Ensure this is NOT null (enforced through database constraint).
     */
    @ColumnInfo(name = SgShow2Columns.TITLE) val title: String = "",
    @ColumnInfo(name = SgShow2Columns.TITLE_NOARTICLE) val titleNoArticle: String?,
    @ColumnInfo(name = SgShow2Columns.OVERVIEW) val overview: String? = "",
    /**
     * Local release time. Encoded as integer (hhmm).
     *
     * <pre>
     * Example: 2035
     * Default: -1
     * </pre>
     */
    @ColumnInfo(name = SgShow2Columns.RELEASE_TIME) val releaseTime: Int?,
    /**
     * Local release week day. Encoded as integer.
     * <pre>
     * Range:   1-7
     * Daily:   0
     * Default: -1
     * </pre>
     */
    @ColumnInfo(name = SgShow2Columns.RELEASE_WEEKDAY) val releaseWeekDay: Int?,
    @ColumnInfo(name = SgShow2Columns.RELEASE_COUNTRY) val releaseCountry: String?,
    @ColumnInfo(name = SgShow2Columns.RELEASE_TIMEZONE) val releaseTimeZone: String?,
    @ColumnInfo(name = SgShow2Columns.FIRST_RELEASE) val firstRelease: String?,
    @ColumnInfo(name = SgShow2Columns.GENRES) val genres: String? = "",
    @ColumnInfo(name = SgShow2Columns.NETWORK) val network: String? = "",
    @ColumnInfo(name = SgShow2Columns.IMDBID) val imdbId: String? = "",
    @ColumnInfo(name = SgShow2Columns.RATING_GLOBAL) val ratingGlobal: Double?,
    @ColumnInfo(name = SgShow2Columns.RATING_VOTES) val ratingVotes: Int?,
    @ColumnInfo(name = SgShow2Columns.RATING_USER) val ratingUser: Int? = 0,
    @ColumnInfo(name = SgShow2Columns.RUNTIME) val runtime: Int? = 0,
    @ColumnInfo(name = SgShow2Columns.STATUS) val status: Int? = ShowTools.Status.UNKNOWN,
    @ColumnInfo(name = SgShow2Columns.CONTENTRATING) val contentRating: String? = "",
    @ColumnInfo(name = SgShow2Columns.NEXTEPISODE) val nextEpisode: String? = "",
    @ColumnInfo(name = SgShow2Columns.POSTER) val poster: String? = "",
    @ColumnInfo(name = SgShow2Columns.POSTER_SMALL) val posterSmall: String? = "",
    @ColumnInfo(name = SgShow2Columns.NEXTAIRDATEMS) val nextAirdateMs: Long? = NextEpisodeUpdater.UNKNOWN_NEXT_RELEASE_DATE,
    @ColumnInfo(name = SgShow2Columns.NEXTTEXT) val nextText: String? = "",
    @ColumnInfo(name = SgShow2Columns.LASTUPDATED) val lastUpdatedMs: Long,
    @ColumnInfo(name = SgShow2Columns.LASTEDIT) val lastEditedSec: Long = 0,
    @ColumnInfo(name = SgShow2Columns.LASTWATCHEDID) val lastWatchedEpisodeId: Long = 0,
    @ColumnInfo(name = SgShow2Columns.LASTWATCHED_MS) val lastWatchedMs: Long = 0,
    @ColumnInfo(name = SgShow2Columns.LANGUAGE) val language: String? = "",
    @ColumnInfo(name = SgShow2Columns.UNWATCHED_COUNT) val unwatchedCount: Int = UNKNOWN_UNWATCHED_COUNT,
    @ColumnInfo(name = SgShow2Columns.FAVORITE) var favorite: Boolean = false,
    @ColumnInfo(name = SgShow2Columns.HIDDEN) var hidden: Boolean = false,
    @ColumnInfo(name = SgShow2Columns.NOTIFY) var notify: Boolean = true,
    @ColumnInfo(name = SgShow2Columns.HEXAGON_MERGE_COMPLETE) val hexagonMergeComplete: Boolean = true
) {
    val releaseTimeOrDefault: Int
        get() = releaseTime ?: -1
    val releaseWeekDayOrDefault: Int
        get() = releaseWeekDay ?: -1
    val statusOrUnknown: Int
        get() = status ?: ShowTools.Status.UNKNOWN
    val ratingGlobalOrZero: Double
        get() = ratingGlobal ?: 0.0
    val ratingVotesOrZero: Int
        get() = ratingVotes ?: 0

    companion object {
        /**
         * Used if the number of remaining episodes to watch for a show is not (yet) known.
         *
         * @see Shows.UNWATCHED_COUNT
         */
        const val UNKNOWN_UNWATCHED_COUNT = -1
    }
}
