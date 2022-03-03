package com.battlelancer.seriesguide.util

import android.content.Context
import android.text.Spannable
import android.text.style.TextAppearanceSpan
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.core.text.set
import androidx.core.text.toSpannable
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

    @JvmStatic
    fun textNoTranslationMovieLanguage(context: Context, languageCode: String?): String {
        return context.getString(
            R.string.no_translation,
            LanguageTools.getMovieLanguageStringFor(context, languageCode),
            context.getString(R.string.tmdb)
        )
    }

    /**
     * Useful for check boxes to add a summary without an additional TextView.
     */
    fun buildTitleAndSummary(
        context: Context,
        title: String,
        summary: String
    ): Spannable {
        val titleAndSummary = "$title\n$summary".toSpannable()
        titleAndSummary[0, title.length] =
            TextAppearanceSpan(context, R.style.TextAppearance_SeriesGuide_Subtitle1_Bold)
        titleAndSummary[title.length, titleAndSummary.length] =
            TextAppearanceSpan(context, R.style.TextAppearance_SeriesGuide_Body2_Secondary)
        return titleAndSummary
    }

    /**
     * Useful for check boxes to add a summary without an additional TextView.
     */
    fun buildTitleAndSummary(
        context: Context,
        @StringRes titleRes: Int,
        @StringRes summaryRes: Int
    ): Spannable {
        val title = context.getString(titleRes)
        val summary = context.getString(summaryRes)
        return buildTitleAndSummary(context, title, summary)
    }

    /**
     * Spannable with TextAppearance applied to the whole string.
     */
    fun buildTextAppearanceSpan(
        context: Context,
        @StringRes textRes: Int,
        @StyleRes appearanceRes: Int
    ): Spannable {
        return context.getString(textRes).toSpannable()
            .apply { this[0, length] = TextAppearanceSpan(context, appearanceRes) }
    }

}