// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2025 Uwe Trottmann

package com.battlelancer.seriesguide.shows.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.CONTENTRATING
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.CUSTOM_RELEASE_DAY_OFFSET
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.CUSTOM_RELEASE_TIME
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.CUSTOM_RELEASE_TIME_ZONE
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.FAVORITE
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.FIRST_RELEASE
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.GENRES
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.HEXAGON_MERGE_COMPLETE
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.HIDDEN
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.IMDBID
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.LANGUAGE
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.LASTEDIT
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.LASTUPDATED
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.LASTWATCHEDID
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.LASTWATCHED_MS
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.NETWORK
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.NEXTAIRDATEMS
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.NEXTEPISODE
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.NEXTTEXT
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.NOTIFY
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.OVERVIEW
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.POSTER
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.POSTER_SMALL
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.RATING_TMDB
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.RATING_TMDB_VOTES
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.RATING_TRAKT
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.RATING_TRAKT_VOTES
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.RATING_USER
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.RELEASE_COUNTRY
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.RELEASE_TIME
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.RELEASE_TIMEZONE
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.RELEASE_WEEKDAY
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.RUNTIME
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.SLUG
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.STATUS
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.TITLE
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.TITLE_NOARTICLE
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.TMDB_ID
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.TRAKT_ID
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.TVDB_ID
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.UNWATCHED_COUNT
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.USER_NOTE
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns.USER_NOTE_TRAKT_ID
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns._ID
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgShow2.Companion.CUSTOM_RELEASE_DAY_OFFSET_NOT_SET
import com.battlelancer.seriesguide.shows.database.SgShow2.Companion.CUSTOM_RELEASE_TIME_NOT_SET
import com.battlelancer.seriesguide.shows.database.SgShow2.Companion.MAX_CUSTOM_DAY_OFFSET
import com.battlelancer.seriesguide.shows.database.SgShow2.Companion.MAX_USER_NOTE_LENGTH
import com.battlelancer.seriesguide.shows.tools.NextEpisodeUpdater
import com.battlelancer.seriesguide.shows.tools.ShowStatus
import com.battlelancer.seriesguide.util.TimeTools

@Entity(
    tableName = "sg_show",
    indices = [
        Index(TMDB_ID),
        Index(TVDB_ID)
    ]
)
data class SgShow2(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = _ID) val id: Long = 0,
    @ColumnInfo(name = TMDB_ID) val tmdbId: Int?,
    @ColumnInfo(name = TVDB_ID) val tvdbId: Int?,
    @ColumnInfo(name = SLUG) val slug: String? = "",
    @ColumnInfo(name = TRAKT_ID) val traktId: Int? = 0,
    /**
     * Ensure this is NOT null (enforced through database constraint).
     */
    @ColumnInfo(name = TITLE) val title: String = "",
    @ColumnInfo(name = TITLE_NOARTICLE) val titleNoArticle: String?,
    @ColumnInfo(name = OVERVIEW) val overview: String? = "",
    /**
     * Local release time. Encoded as integer (hhmm).
     *
     * ```
     * Example: 2035
     * Default: -1
     * ```
     */
    @ColumnInfo(name = RELEASE_TIME) val releaseTime: Int?,
    /**
     * Local release week day. Encoded as integer.
     *
     * ```
     * Range:   1-7
     * Daily:   0
     * Default: -1
     * ```
     */
    @ColumnInfo(name = RELEASE_WEEKDAY) val releaseWeekDay: Int?,
    /**
     * Release country. Encoded as ISO3166-1 alpha-2 string.
     *
     * ```
     * Example: "us"
     * Default: ""
     * ```
     *
     * Previous use: Was added in db version 21 to store the air time in pure text.
     */
    @ColumnInfo(name = RELEASE_COUNTRY) val releaseCountry: String?,
    /**
     * Release time zone. Encoded as tzdata "Area/Location" string.
     *
     * ```
     * Example: "America/New_York"
     * Default: ""
     * ```
     *
     * Added with [SeriesGuideDatabase.DBVER_34_TRAKT_V2].
     */
    @ColumnInfo(name = RELEASE_TIMEZONE) val releaseTimeZone: String?,
    @ColumnInfo(name = FIRST_RELEASE) val firstRelease: String?,
    /**
     * A pipe-separated list of genres.
     */
    @ColumnInfo(name = GENRES) val genres: String? = "",
    @ColumnInfo(name = NETWORK) val network: String? = "",
    @ColumnInfo(name = IMDBID) val imdbId: String? = "",
    /**
     * TMDB rating. Encoded as double.
     * ```
     * Range:   0.0-10.0
     * Default: null
     * ```
     *
     * Added with [SgRoomDatabase.VERSION_53_SHOW_TMDB_RATINGS].
     */
    @ColumnInfo(name = RATING_TMDB) val ratingTmdb: Double?,
    /**
     * TMDB rating number of votes.
     * ```
     * Example: 42
     * Default: null
     * ```
     *
     * Added with [SgRoomDatabase.VERSION_53_SHOW_TMDB_RATINGS].
     */
    @ColumnInfo(name = RATING_TMDB_VOTES) val ratingTmdbVotes: Int?,
    /**
     * Trakt rating. Encoded as double.
     * ```
     * Range:   0.0-10.0
     * Default: null
     * ```
     */
    @ColumnInfo(name = RATING_TRAKT) val ratingTrakt: Double?,
    /**
     * Trakt rating number of votes.
     * ```
     * Example: 42
     * Default: null
     * ```
     */
    @ColumnInfo(name = RATING_TRAKT_VOTES) val ratingTraktVotes: Int?,
    /**
     * User rating. Encoded as integer.
     * ```
     * Range:   0-10
     * Default: null
     * ```
     */
    @ColumnInfo(name = RATING_USER) val ratingUser: Int?,
    /**
     * Typical length of an episode in minutes. Used to calculate watch time.
     *
     * Default: 45 (by GetShowTools)
     */
    @ColumnInfo(name = RUNTIME) val runtime: Int? = 0,
    /**
     * Show status. Encoded as integer. See [ShowStatus].
     */
    @ColumnInfo(name = STATUS) val status: Int? = ShowStatus.UNKNOWN,
    @ColumnInfo(name = CONTENTRATING) val contentRating: String? = "",
    @ColumnInfo(name = NEXTEPISODE) val nextEpisode: String? = "",
    /**
     * A poster path. Needs to be prefixed with the poster server URL.
     */
    @ColumnInfo(name = POSTER) val poster: String? = "",
    @ColumnInfo(name = POSTER_SMALL) val posterSmall: String? = "",
    @ColumnInfo(name = NEXTAIRDATEMS) val nextAirdateMs: Long? = NextEpisodeUpdater.UNKNOWN_NEXT_RELEASE_DATE,
    @ColumnInfo(name = NEXTTEXT) val nextText: String? = "",
    @ColumnInfo(name = LASTUPDATED) val lastUpdatedMs: Long,
    @ColumnInfo(name = LASTEDIT) val lastEditedSec: Long = 0,
    @ColumnInfo(name = LASTWATCHEDID) val lastWatchedEpisodeId: Long = 0,
    /**
     * The time in milliseconds an episode was last watched for this show.
     *
     * Used to sort shows by last watched.
     *
     * Added with [SeriesGuideDatabase.DBVER_39_SHOW_LAST_WATCHED].
     */
    @ColumnInfo(name = LASTWATCHED_MS) val lastWatchedMs: Long = 0,
    @ColumnInfo(name = LANGUAGE) val language: String? = "",
    @ColumnInfo(name = UNWATCHED_COUNT) val unwatchedCount: Int = UNKNOWN_UNWATCHED_COUNT,
    /**
     * Whether this show has been added to favorites.
     */
    @ColumnInfo(name = FAVORITE) var favorite: Boolean = false,
    /**
     * Whether this show has been hidden.
     *
     * Added in db version 23.
     */
    @ColumnInfo(name = HIDDEN) var hidden: Boolean = false,
    /**
     * Whether notifications for new episodes of this show should be shown.
     *
     * ```
     * Range: 0-1
     * Default: 1
     * ```
     * Added with [SeriesGuideDatabase.DBVER_40_NOTIFY_PER_SHOW].
     */
    @ColumnInfo(name = NOTIFY) var notify: Boolean = true,
    @ColumnInfo(name = HEXAGON_MERGE_COMPLETE) val hexagonMergeComplete: Boolean = true,
    /**
     * Custom local release time to override the actual one. Encoded as integer (hhmm).
     * This being set also determines if the other custom release time values (day offset,
     * time zone) should be used.
     *
     * Example: 2035
     *
     * Default: [CUSTOM_RELEASE_TIME_NOT_SET]
     *
     * Added in [SgRoomDatabase.VERSION_51_CUSTOM_RELEASE_TIME].
     */
    @ColumnInfo(name = CUSTOM_RELEASE_TIME) var customReleaseTime: Int?,
    /**
     * Positive or negative day offset to shift the release day of episodes by.
     *
     * Default: [CUSTOM_RELEASE_DAY_OFFSET_NOT_SET]
     *
     * Range: -/+ [MAX_CUSTOM_DAY_OFFSET]
     *
     * Added in [SgRoomDatabase.VERSION_51_CUSTOM_RELEASE_TIME].
     */
    @ColumnInfo(name = CUSTOM_RELEASE_DAY_OFFSET) var customReleaseDayOffset: Int?,
    /**
     * Custom time zone for computing an episode release time.
     *
     * Same format as [releaseTimeZone].
     *
     * Default: [SgShow2.CUSTOM_RELEASE_TIME_ZONE_NOT_SET]
     *
     * Added in [SgRoomDatabase.VERSION_51_CUSTOM_RELEASE_TIME].
     */
    @ColumnInfo(name = CUSTOM_RELEASE_TIME_ZONE) var customReleaseTimeZone: String?,
    /**
     * A user editable text note for this show.
     *
     * Default `null`. Should be at most [MAX_USER_NOTE_LENGTH].
     *
     * Added with [SgRoomDatabase.VERSION_54_SHOW_NOTES].
     */
    @ColumnInfo(name = USER_NOTE) var userNote: String?,
    /**
     * Trakt ID for [userNote].
     *
     * Default `null`.
     *
     * Added with [SgRoomDatabase.VERSION_54_SHOW_NOTES].
     */
    @ColumnInfo(name = USER_NOTE_TRAKT_ID) val userNoteTraktId: Long?
) {
    val releaseTimeOrDefault: Int
        get() = releaseTime ?: -1
    val customReleaseTimeOrDefault: Int
        get() = customReleaseTime ?: CUSTOM_RELEASE_TIME_NOT_SET
    val customReleaseDayOffsetOrDefault: Int
        get() = customReleaseDayOffset ?: CUSTOM_RELEASE_DAY_OFFSET_NOT_SET
    val customReleaseTimeZoneOrDefault: String
        get() = customReleaseTimeZone ?: CUSTOM_RELEASE_TIME_ZONE_NOT_SET
    val firstReleaseOrDefault: String
        get() = firstRelease ?: ""
    val releaseWeekDayOrDefault: Int
        get() = releaseWeekDay ?: TimeTools.RELEASE_WEEKDAY_UNKNOWN
    val statusOrUnknown: Int
        get() = status ?: ShowStatus.UNKNOWN

    /**
     * Maps blank or null to empty string.
     */
    val userNoteOrEmpty: String
        get() = userNote?.ifBlank { "" } ?: ""

    companion object {
        /**
         * Used if the number of remaining episodes to watch for a show is not (yet) known.
         *
         * @see SgShow2Columns.UNWATCHED_COUNT
         */
        const val UNKNOWN_UNWATCHED_COUNT = -1

        const val CUSTOM_RELEASE_TIME_NOT_SET = -1

        const val CUSTOM_RELEASE_DAY_OFFSET_NOT_SET = 0

        const val CUSTOM_RELEASE_TIME_ZONE_NOT_SET = ""

        /**
         * Maximum absolute (so positive or negative) value of the [customReleaseDayOffset].
         */
        const val MAX_CUSTOM_DAY_OFFSET = 28

        /**
         * Max note length to support Trakt and Hexagon
         * (Trakt supports up to 500 characters, Hexagon up to 2000).
         */
        const val MAX_USER_NOTE_LENGTH = 500
    }
}
