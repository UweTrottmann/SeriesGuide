// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2025 Uwe Trottmann

package com.battlelancer.seriesguide.shows.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.ABSOLUTE_NUMBER
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.COLLECTED
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.DIRECTORS
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.DVDNUMBER
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.FIRSTAIREDMS
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.GUESTSTARS
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.IMAGE
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.IMDBID
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.LAST_EDITED
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.LAST_UPDATED
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.NUMBER
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.ORDER
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.OVERVIEW
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.PLAYS
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.RATING_TMDB
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.RATING_TMDB_VOTES
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.RATING_TRAKT
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.RATING_TRAKT_VOTES
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.RATING_USER
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.SEASON
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.TITLE
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.TMDB_ID
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.TVDB_ID
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.WATCHED
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.WRITERS
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns._ID
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgSeason2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgEpisode2.Companion.EPISODE_UNKNOWN_RELEASE
import com.battlelancer.seriesguide.shows.episodes.EpisodeFlags

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
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = _ID) val id: Long = 0,
    @ColumnInfo(name = SgSeason2Columns.REF_SEASON_ID) val seasonId: Long,
    @ColumnInfo(name = SgShow2Columns.REF_SHOW_ID) val showId: Long,
    @ColumnInfo(name = TMDB_ID) val tmdbId: Int?,
    @ColumnInfo(name = TVDB_ID) val tvdbId: Int? = null,
    @ColumnInfo(name = TITLE) val title: String? = "",
    @ColumnInfo(name = OVERVIEW) val overview: String?,
    @ColumnInfo(name = NUMBER) val number: Int = 0,
    @ColumnInfo(name = ABSOLUTE_NUMBER) val absoluteNumber: Int? = null,
    @ColumnInfo(name = SEASON) val season: Int = 0,
    @ColumnInfo(name = ORDER) val order: Int = 0,
    /**
     * No longer used since the TMDB migration.
     *
     * Sometimes episodes are ordered differently when released on DVD. Uses decimal point
     * notation, e.g. 1.0, 1.5.
     */
    @ColumnInfo(name = DVDNUMBER) val dvdNumber: Double? = null,
    /**
     * One of [EpisodeFlags], whether an episode is watched, skipped or unwatched.
     */
    @ColumnInfo(name = WATCHED) val watched: Int = EpisodeFlags.UNWATCHED,
    /**
     * The number of times an episode was watched.
     */
    @ColumnInfo(name = PLAYS) val plays: Int? = 0,
    /**
     * Whether an episode has been added to the collection.
     */
    @ColumnInfo(name = COLLECTED) val collected: Boolean = false,
    /**
     * A pipe-separated list of directors.
     */
    @ColumnInfo(name = DIRECTORS) val directors: String? = "",
    /**
     * A pipe-separated list of guest stars.
     */
    @ColumnInfo(name = GUESTSTARS) val guestStars: String? = "",
    /**
     * A pipe-separated list of writers.
     */
    @ColumnInfo(name = WRITERS) val writers: String? = "",
    /**
     * A TMDB episode image (still) path.
     */
    @ColumnInfo(name = IMAGE) val image: String? = "",
    /**
     * First aired date in ms.
     *
     * This date time is based on the shows (custom) release time and time zone at the time
     * this episode was last updated. It includes country and time zone specific offsets (currently
     * only for US western time zones). It does NOT include the user-set offset.
     *
     * Range:   long
     *
     * Default: [EPISODE_UNKNOWN_RELEASE]
     */
    @ColumnInfo(name = FIRSTAIREDMS) val firstReleasedMs: Long = -1,
    /**
     * See [SgShow2.ratingTmdb].
     *
     * Added with [SgRoomDatabase.VERSION_53_SHOW_TMDB_RATINGS].
     */
    @ColumnInfo(name = RATING_TMDB) val ratingTmdb: Double?,
    /**
     * See [SgShow2.ratingTmdbVotes].
     *
     * Added with [SgRoomDatabase.VERSION_53_SHOW_TMDB_RATINGS].
     */
    @ColumnInfo(name = RATING_TMDB_VOTES) val ratingTmdbVotes: Int?,
    /** See [SgShow2.ratingTrakt]. */
    @ColumnInfo(name = RATING_TRAKT) val ratingTrakt: Double?,
    /** See [SgShow2.ratingTraktVotes]. */
    @ColumnInfo(name = RATING_TRAKT_VOTES) val ratingTraktVotes: Int?,
    /** See [SgShow2.ratingUser]. */
    @ColumnInfo(name = RATING_USER) val ratingUser: Int?,
    /**
     * No longer used since the TMDB migration, resolved on demand.
     *
     * IMDb id for a single episode. Added in db version 27.
     */
    @ColumnInfo(name = IMDBID) val imdbId: String? = "",
    @ColumnInfo(name = LAST_EDITED) val lastEditedSec: Long = 0,
    @ColumnInfo(name = LAST_UPDATED) val lastUpdatedSec: Long = 0
) {
    val playsOrZero: Int
        get() = plays ?: 0

    companion object {
        /**
         * See [com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes.FIRSTAIREDMS].
         */
        const val EPISODE_UNKNOWN_RELEASE = -1L
    }
}
