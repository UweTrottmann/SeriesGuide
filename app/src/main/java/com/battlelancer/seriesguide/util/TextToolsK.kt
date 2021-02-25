package com.battlelancer.seriesguide.util

import android.content.Context
import com.battlelancer.seriesguide.R

object TextToolsK {

    @JvmStatic
    fun getWatchedButtonText(context: Context, isWatched: Boolean, plays: Int?): String {
        return if (isWatched) {
            if (plays == null || plays <= 1) {
                context.getString(R.string.state_watched)
            } else {
                context.getString(R.string.state_watched_multiple_format, plays)
            }
        } else {
            context.getString(R.string.action_watched)
        }
    }

    @JvmStatic
    fun textNoTranslation(context: Context, languageCode: String?): String {
        return context.getString(
            R.string.no_translation,
            LanguageTools.getShowLanguageStringFor(context, languageCode),
            context.getString(R.string.tmdb)
        )
    }

}