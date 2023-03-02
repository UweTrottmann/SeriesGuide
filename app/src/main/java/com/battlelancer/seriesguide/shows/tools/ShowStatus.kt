package com.battlelancer.seriesguide.shows.tools

import android.content.Context
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.ThemeUtils

/**
 * Show status valued as stored in the database in [com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows.STATUS].
 */
// Compare with https://www.themoviedb.org/bible/tv#59f7403f9251416e7100002b
// Note: used to order shows by status, so ensure similar are next to each other.
interface ShowStatus {
    companion object {
        const val IN_PRODUCTION = 5
        const val PILOT = 4
        const val PLANNED = 2

        /**
         * Episodes are to be released.
         */
        const val RETURNING = 1

        /**
         * Typically all episodes released, with a planned ending.
         */
        const val ENDED = 0
        const val UNKNOWN = -1

        /**
         * Typically all episodes released, but abruptly ended.
         */
        const val CANCELED = -2

        /**
         * Decodes the [ShowTools.Status] and returns the localized text representation.
         * May be `null` if status is unknown.
         */
        fun getStatus(context: Context, encodedStatus: Int): String? {
            return when (encodedStatus) {
                IN_PRODUCTION -> context.getString(R.string.show_status_in_production)
                PILOT -> context.getString(R.string.show_status_pilot)
                CANCELED -> context.getString(R.string.show_status_canceled)
                PLANNED -> context.getString(R.string.show_isUpcoming)
                RETURNING -> context.getString(R.string.show_isalive)
                ENDED -> context.getString(R.string.show_isnotalive)
                else -> {
                    // status unknown, display nothing
                    null
                }
            }
        }

        /**
         * Gets the show status from [getStatus] and sets a status dependant text color on the
         * given view.
         *
         * @param encodedStatus Detection based on [ShowStatus].
         */
        fun setStatusAndColor(view: TextView, encodedStatus: Int) {
            view.text = getStatus(view.context, encodedStatus)
            if (encodedStatus == RETURNING) {
                view.setTextColor(
                    ContextCompat.getColor(
                        view.context,
                        ThemeUtils.resolveAttributeToResourceId(
                            view.context.theme, R.attr.colorSecondary
                        )
                    )
                )
            } else {
                view.setTextColor(
                    ContextCompat.getColor(
                        view.context,
                        ThemeUtils.resolveAttributeToResourceId(
                            view.context.theme, android.R.attr.textColorSecondary
                        )
                    )
                )
            }
        }
    }
}