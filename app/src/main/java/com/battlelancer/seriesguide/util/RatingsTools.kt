// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.util

import android.content.Context
import android.view.View
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.isGone
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.LayoutRatingsBinding
import com.battlelancer.seriesguide.shows.database.SgEpisode2
import com.battlelancer.seriesguide.traktapi.TraktTools
import com.uwetrottmann.androidutils.AndroidUtils
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Helps using [LayoutRatingsBinding].
 */
object RatingsTools {

    /**
     * If no [onRateClickListener] is passed, hides the user rating views.
     */
    fun LayoutRatingsBinding.initialize(onRateClickListener: View.OnClickListener?) {
        val context = root.context
        context.getString(R.string.format_rating_range, 10).let {
            ratingViewTmdb.apply {
                setRange(it)
                setIcon(R.drawable.ic_tmdb_control_24dp, R.string.tmdb)
            }
            ratingViewTrakt.apply {
                setRange(it)
                setIcon(R.drawable.ic_trakt_icon_control, R.string.trakt)
            }
        }

        if (onRateClickListener != null) {
            viewClickTargetRatingUser.setOnClickListener(onRateClickListener)
            val action = context.getString(R.string.action_rate)
            viewClickTargetRatingUser.contentDescription = action
            TooltipCompat.setTooltipText(viewClickTargetRatingUser, action)
        } else {
            groupRatingsUser.isGone = true
        }
    }

    fun LayoutRatingsBinding.setRatingValues(
        tmdbValue: Double?,
        tmdbVotes: Int?,
        traktValue: Double?,
        traktVotes: Int?
    ) {
        val context = root.context
        ratingViewTmdb.setValues(
            buildRatingString(tmdbValue),
            buildRatingVotesString(context, tmdbVotes)
        )
        ratingViewTrakt.setValues(
            buildRatingString(traktValue),
            buildRatingVotesString(context, traktVotes)
        )
    }

    fun LayoutRatingsBinding.setValuesFor(episode: SgEpisode2) {
        setRatingValues(
            episode.ratingTmdb,
            episode.ratingTmdbVotes,
            episode.ratingTrakt,
            episode.ratingTraktVotes
        )
        textViewRatingsUser.text =
            TraktTools.buildUserRatingString(root.context, episode.ratingUser)
    }

    /**
     * Builds a localized string like "x votes".
     */
    fun buildRatingVotesString(context: Context, votes: Int?): String {
        return votes
            .let { if (it == null || it < 0) 0 else it }
            .let { context.resources.getQuantityString(R.plurals.votes, it, it) }
    }

    /**
     * Returns the given double as number string with one decimal digit, like "1.5". By default,
     * formatted using the default locale.
     */
    fun buildRatingString(rating: Double?, locale: Locale = Locale.getDefault()): String {
        if (rating == null || rating == 0.0) {
            return "--"
        }
        return rating
            .let {
                if (AndroidUtils.isNougatOrHigher) {
                    it
                } else {
                    /*
                    Before Android 7.0 string format seems to round half down, despite docs saying
                    half up it likely used DecimalFormat, which defaults to half even.
                     */
                    BigDecimal(rating).setScale(1, RoundingMode.HALF_UP).toDouble()
                }
            }
            .let { String.format(locale, "%.1f", it) }
    }

}