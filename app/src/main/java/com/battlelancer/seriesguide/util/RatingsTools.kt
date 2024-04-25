package com.battlelancer.seriesguide.util

import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.LayoutRatingsBinding
import com.uwetrottmann.androidutils.AndroidUtils
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Helps using [LayoutRatingsBinding].
 */
object RatingsTools {

    fun LayoutRatingsBinding.setRangeValues() {
        root.context.getString(R.string.format_rating_range, 10).let {
            textViewRatingsTmdbRange.text = it
            textViewRatingsTraktRange.text = it
        }
    }

    fun LayoutRatingsBinding.setRatingValues(
        tmdbValue: Double?,
        tmdbVotes: Int?,
        traktValue: Double?,
        traktVotes: Int?
    ) {
        val context = root.context
        textViewRatingsTmdbValue.text = buildRatingString(tmdbValue)
        textViewRatingsTmdbVotes.text = buildRatingVotesString(context, tmdbVotes)
        textViewRatingsTraktValue.text = buildRatingString(traktValue)
        textViewRatingsTraktVotes.text = buildRatingVotesString(context, traktVotes)
    }

    /**
     * Builds a localized string like "x votes".
     */
    private fun buildRatingVotesString(context: Context, votes: Int?): String {
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