package com.battlelancer.seriesguide.util

import android.content.Context
import com.battlelancer.seriesguide.R

object TextToolsK {

    @JvmStatic
    fun getWatchedButtonText(context: Context, isWatched: Boolean, plays: Int): String {
        return if (isWatched) {
            if (plays <= 1) {
                context.getString(R.string.state_watched)
            } else {
                context.getString(R.string.state_watched_multiple_format, plays)
            }
        } else {
            context.getString(R.string.action_watched)
        }
    }

}