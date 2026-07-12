// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.lists

import android.content.Context
import androidx.recyclerview.widget.DiffUtil
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.lists.UiListItem.Companion.DIFF_CALLBACK
import com.battlelancer.seriesguide.lists.database.SgListItemWithDetails
import com.battlelancer.seriesguide.movies.database.SgMovie
import com.battlelancer.seriesguide.movies.tools.MovieTools
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.shows.overview.SeasonTools
import com.battlelancer.seriesguide.shows.tools.ShowStatus
import com.battlelancer.seriesguide.util.ImageUrlTools
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.util.TimeTools.formatWithDeviceZoneToDayAndTime
import org.threeten.bp.Instant

/**
 * UI model for a list item for [SgListItemAdapter].
 */
data class UiListItem(
    /**
     * Database row ID. Used as the stable ID in [DIFF_CALLBACK].
     */
    val id: Long,
    val listItemId: String,
    @ListItemTypes val type: Int,

    /**
     * Only if [type] is [ListItemTypes.TMDB_MOVIE].
     */
    val movieTmdbId: Int?,
    /**
     * Only if [type] is **not** a [ListItemTypes.TMDB_MOVIE].
     */
    val showId: Long?,

    val isSetWatchedButtonVisible: Boolean,
    val isFavorite: Boolean,

    /**
     * A poster URL ready to be loaded.
     */
    val posterUrl: String?,

    val titleText: String,
    val nextEpisodeText: String?,
    val nextEpisodeTimeText: String?,
    val timeAndNetworkText: String,
    val remainingText: String?,

    /**
     * Database ID of the next episode; 0 if there is none.
     */
    val nextEpisodeId: Long,
) {
    /**
     * Whether this item represents a show (TMDB or legacy TVDB).
     */
    val isShow: Boolean
        get() = type == ListItemTypes.TMDB_SHOW || type == ListItemTypes.TVDB_SHOW

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<UiListItem>() {
            override fun areItemsTheSame(oldItem: UiListItem, newItem: UiListItem): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: UiListItem, newItem: UiListItem): Boolean =
                oldItem == newItem
        }
    }
}

/**
 * Converts [SgListItemWithDetails] rows (coming from the database) into
 * [UiListItem] instances that are ready to be bound by [SgListItemAdapter].
 */
class UiListItemBuilder(private val context: Context) {

    private val imageUrlTools = ImageUrlTools(context)
    private val dateFormatMovieReleaseDate = MovieTools.getMovieShortDateFormat()

    fun buildFrom(item: SgListItemWithDetails): UiListItem {
        return when (item.type) {
            ListItemTypes.TMDB_MOVIE -> buildMovie(item)
            ListItemTypes.TMDB_SHOW, ListItemTypes.TVDB_SHOW -> buildShow(item)
            ListItemTypes.SEASON -> buildSeason(item)
            ListItemTypes.EPISODE -> buildEpisode(item)
            else -> throw IllegalArgumentException("Unsupported item type ${item.type}")
        }
    }

    private fun SgListItemWithDetails.posterUrl(): String? =
        imageUrlTools.tmdbOrTvdbPosterUrl(this.poster)

    private fun buildMovie(item: SgListItemWithDetails): UiListItem {
        val releaseDate = if (item.releasedMsOrDefault != SgMovie.RELEASED_MS_UNKNOWN) {
            TimeTools.formatToLocalDateShort(context, item.releasedMsOrDefault)
        } else {
            ""
        }

        val runningTime =
            TimeTools.formatToHoursAndMinutes(context.resources, item.runningTimeMinutesOrZero)

        return UiListItem(
            id = item.id,
            listItemId = item.listItemId,
            type = item.type,
            movieTmdbId = item.movieTmdbId,
            showId = null,
            isSetWatchedButtonVisible = false,
            isFavorite = false,
            posterUrl = item.posterUrl(),
            titleText = item.title,
            nextEpisodeText = runningTime,
            nextEpisodeTimeText = releaseDate,
            timeAndNetworkText = "",
            remainingText = null,
            nextEpisodeId = 0,
        )
    }

    private fun buildShow(item: SgListItemWithDetails): UiListItem {
        val releaseDateTime = TimeTools.getReleaseDateTime(context, item)
        val timeAndNetwork = TextTools.dotSeparate(
            item.network,
            releaseDateTime?.formatWithDeviceZoneToDayAndTime()
        )

        val nextEpisodeText = item.nextText
        val nextEpisodeTimeText: String?
        val hasNextEpisode = !nextEpisodeText.isNullOrEmpty()
        if (!hasNextEpisode) {
            // Display show status if there is no next episode
            nextEpisodeTimeText = ShowStatus.getStatus(context, item.statusOrUnknown)
        } else {
            val releaseTimeEpisode =
                TimeTools.applyUserOffset(context, item.releasedMsOrDefault)
            val displayExactDate = DisplaySettings.isDisplayExactDate(context)
            val dateTime = if (displayExactDate) {
                TimeTools.formatToLocalDateShort(context, releaseTimeEpisode)
            } else {
                TimeTools.formatToLocalRelativeTime(context, releaseTimeEpisode)
            }
            nextEpisodeTimeText = if (
                TimeTools.isSameWeekDay(
                    Instant.ofEpochMilli(releaseTimeEpisode.time),
                    releaseDateTime?.toInstant(),
                    item.releaseWeekDayOrDefault
                )
            ) {
                dateTime
            } else {
                context.getString(
                    R.string.format_date_and_day,
                    dateTime,
                    TimeTools.formatToLocalDay(releaseTimeEpisode)
                )
            }
        }

        // remaining count
        val remainingText = item.unwatchedCount
            .let {
                if (it > 0) {
                    TextTools.getRemainingEpisodes(context.resources, it)
                } else null
            }

        return UiListItem(
            id = item.id,
            listItemId = item.listItemId,
            type = item.type,
            movieTmdbId = null,
            showId = item.showId,
            isSetWatchedButtonVisible = hasNextEpisode,
            isFavorite = item.favorite,
            posterUrl = item.posterUrl(),
            titleText = item.title,
            nextEpisodeText = nextEpisodeText,
            nextEpisodeTimeText = nextEpisodeTimeText,
            timeAndNetworkText = timeAndNetwork,
            remainingText = remainingText,
            nextEpisodeId = item.nextEpisodeId,
        )
    }

    private fun buildSeason(item: SgListItemWithDetails): UiListItem {
        // Note: running a database query per item, but it's for legacy items, so fine for now
        val seasonTvdbId = item.itemRefId.toIntOrNull() ?: 0
        val seasonNumbersOrNull = SgRoomDatabase.getInstance(context)
            .sgSeason2Helper()
            .getSeasonNumbersByTvdbId(seasonTvdbId)
        val nextEpisodeText = if (seasonNumbersOrNull != null) {
            SeasonTools.getSeasonString(context, seasonNumbersOrNull.number)
        } else {
            context.getString(R.string.unknown)
        }

        return UiListItem(
            id = item.id,
            listItemId = item.listItemId,
            type = item.type,
            movieTmdbId = null,
            showId = item.showId,
            isSetWatchedButtonVisible = false,
            isFavorite = item.favorite,
            posterUrl = item.posterUrl(),
            titleText = item.title,
            nextEpisodeText = nextEpisodeText,
            nextEpisodeTimeText = null,
            timeAndNetworkText = context.getString(R.string.season),
            remainingText = null,
            nextEpisodeId = 0,
        )
    }

    private fun buildEpisode(item: SgListItemWithDetails): UiListItem {
        // Note: running a database query per item, but it's for legacy items, so fine for now
        val episodeTvdbId = item.itemRefId.toIntOrNull() ?: 0
        val helper = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
        val episodeIdOrZero = helper.getEpisodeIdByTvdbId(episodeTvdbId)
        val episodeInfo = if (episodeIdOrZero > 0) {
            helper.getEpisodeInfo(episodeIdOrZero)
        } else null

        val nextEpisodeText: String
        val nextEpisodeTimeText: String?

        if (episodeInfo != null) {
            nextEpisodeText = TextTools.getNextEpisodeString(
                context,
                episodeInfo.season,
                episodeInfo.episodenumber,
                episodeInfo.title
            )
            val releaseTime = episodeInfo.firstReleasedMs
            nextEpisodeTimeText = if (releaseTime != -1L) {
                // "in 15 mins (Fri)"
                val actualRelease = TimeTools.applyUserOffset(context, releaseTime)
                context.getString(
                    R.string.format_date_and_day,
                    TimeTools.formatToLocalRelativeTime(context, actualRelease),
                    TimeTools.formatToLocalDay(actualRelease)
                )
            } else null
        } else {
            nextEpisodeText = context.getString(R.string.unknown)
            nextEpisodeTimeText = null
        }

        return UiListItem(
            id = item.id,
            listItemId = item.listItemId,
            type = item.type,
            movieTmdbId = null,
            showId = item.showId,
            isSetWatchedButtonVisible = false,
            isFavorite = item.favorite,
            posterUrl = item.posterUrl(),
            titleText = item.title,
            nextEpisodeText = nextEpisodeText,
            nextEpisodeTimeText = nextEpisodeTimeText,
            timeAndNetworkText = context.getString(R.string.episode),
            remainingText = null,
            nextEpisodeId = 0,
        )
    }
}

