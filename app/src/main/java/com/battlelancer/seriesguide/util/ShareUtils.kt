// SPDX-License-Identifier: Apache-2.0
// Copyright 2011-2025 Uwe Trottmann

package com.battlelancer.seriesguide.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.text.format.DateUtils
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.app.ShareCompat
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.tmdbapi.TmdbTools
import java.util.concurrent.TimeUnit

/**
 * Contains helpers to share a show, episode (share intent, calendar event) or movie.
 */
object ShareUtils {

    fun shareEpisode(
        activity: Activity,
        showTmdbId: Int,
        seasonNumber: Int,
        episodeNumber: Int,
        showTitle: String,
        episodeTitle: String
    ) {
        val nextEpisodeString =
            TextTools.getNextEpisodeString(activity, seasonNumber, episodeNumber, episodeTitle)
        val episodeUrl = TmdbTools.buildEpisodeUrl(showTmdbId, seasonNumber, episodeNumber)
        val message = "$showTitle - $nextEpisodeString $episodeUrl"
        startShareIntentChooser(activity, message, R.string.share_episode)
    }

    fun shareShow(
        activity: Activity,
        showTmdbId: Int,
        showTitle: String
    ) {
        val showUrl = TmdbTools.buildShowUrl(showTmdbId)
        val message = "$showTitle $showUrl"
        startShareIntentChooser(activity, message, R.string.share_show)
    }

    fun shareMovie(
        activity: Activity,
        movieTmdbId: Int,
        movieTitle: String
    ) {
        val movieUrl = TmdbTools.buildMovieUrl(movieTmdbId)
        val message = "$movieTitle $movieUrl"
        startShareIntentChooser(activity, message, R.string.share_movie)
    }

    /**
     * Share a text snippet. Displays a share intent chooser with the given title, share type is
     * text/plain.
     */
    fun startShareIntentChooser(
        activity: Activity,
        message: String,
        @StringRes titleResId: Int
    ) {
        val ib = ShareCompat.IntentBuilder(activity)
        ib.setText(message)
        ib.setChooserTitle(titleResId)
        ib.setType("text/plain")
        try {
            ib.startChooser()
        } catch (e: ActivityNotFoundException) {
            // no activity available to handle the intent
            Toast.makeText(activity, R.string.app_not_available, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Launches a calendar insert intent for the given episode.
     */
    fun suggestCalendarEvent(
        context: Context,
        showTitle: String,
        episodeTitle: String,
        episodeReleaseTime: Long,
        showRunTimeOrNull: Int?
    ) {
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, showTitle)
            .putExtra(CalendarContract.Events.DESCRIPTION, episodeTitle)

        val beginTime = TimeTools.applyUserOffset(context, episodeReleaseTime).time
        val showRunTime = showRunTimeOrNull ?: 0
        val endTime = beginTime + showRunTime * DateUtils.MINUTE_IN_MILLIS
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime)
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)

        if (!context.tryStartActivity(intent, false)) {
            Toast.makeText(
                context,
                context.getString(R.string.addtocalendar_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun suggestAllDayCalendarEvent(
        context: Context,
        eventTitle: String,
        eventTimeMs: Long
    ) {
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, eventTitle)
            // For all day events start at midnight and end at midnight of next day
            // (end time treated as non-inclusive).
            .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, eventTimeMs)
            .putExtra(
                CalendarContract.EXTRA_EVENT_END_TIME,
                eventTimeMs + TimeUnit.DAYS.toMillis(1)
            )

        if (!context.tryStartActivity(intent, false)) {
            Toast.makeText(
                context,
                context.getString(R.string.addtocalendar_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
