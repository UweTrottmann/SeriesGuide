package com.battlelancer.seriesguide.util

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.TextAppearanceSpan
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.core.text.set
import androidx.core.text.toSpannable
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.DisplaySettings
import java.text.NumberFormat
import java.util.Date

/**
 * Tools to help build text fragments to be used throughout the user interface.
 */
object TextTools {

    /**
     * Must match numberData string-array in resources.
     */
    enum class EpisodeFormat(val value: String) {
        DEFAULT("default"),
        PREFIX_ENGLISH("english"),
        PREFIX_ENGLISH_LOWER("englishlower"),
        SUFFIX_LONG("-long"),
    }

    /**
     * Returns the [episode] number formatted according to the users preference (e.g. '1x1',
     * 'S1:E1', ...). If `episode` is -1 only the [season] number is returned.
     */
    @JvmStatic
    fun getEpisodeNumber(context: Context, season: Int, episode: Int): String {
        val format = DisplaySettings.getNumberFormat(context)
        val useLongNumbers = format.endsWith(EpisodeFormat.SUFFIX_LONG.value)

        val numberFormat = NumberFormat.getIntegerInstance()
            .apply { isGroupingUsed = false }

        val seasonStr = if (season < 10 && useLongNumbers) {
            // Make season number at least two chars long.
            numberFormat.format(0) + numberFormat.format(season.toLong())
        } else {
            numberFormat.format(season.toLong())
        }

        val includeEpisode = episode != -1
        var episodeStr: String? = null
        if (includeEpisode) {
            episodeStr = if (episode < 10
                && (useLongNumbers || EpisodeFormat.DEFAULT.value == format)) {
                // Make episode number at least two chars long.
                numberFormat.format(0) + numberFormat.format(episode.toLong())
            } else {
                numberFormat.format(episode.toLong())
            }
        }

        return if (format.startsWith(EpisodeFormat.PREFIX_ENGLISH_LOWER.value)) {
            // s1:e1 or s01e01 format
            if (includeEpisode) {
                "s" + seasonStr + (if (useLongNumbers) "" else ":") + "e" + episodeStr
            } else {
                "s$seasonStr"
            }
        } else if (format.startsWith(EpisodeFormat.PREFIX_ENGLISH.value)) {
            // S1:E1 or S01E01 format
            if (includeEpisode) {
                "S" + seasonStr + (if (useLongNumbers) "" else ":") + "E" + episodeStr
            } else {
                "S$seasonStr"
            }
        } else {
            // Default
            // 1x01 format
            if (includeEpisode) {
                seasonStr + "x" + episodeStr
            } else {
                seasonStr + "x"
            }
        }
    }

    /**
     * Returns the [title] or if it's empty a string like "Episode 2".
     */
    fun getEpisodeTitle(context: Context, title: String?, episode: Int): String {
        return if (title.isNullOrEmpty()) {
            context.getString(R.string.episode_number, episode)
        } else {
            title
        }
    }

    /**
     * Returns a string like "1x01 Title". The number format may change based on user preference.
     */
    @JvmStatic
    fun getNextEpisodeString(
        context: Context, season: Int, episode: Int,
        title: String?
    ): String {
        return (getEpisodeNumber(context, season, episode) + " "
                + getEpisodeTitle(context, title, episode))
    }

    /**
     * Returns a string like "Title 1x01". The number format may change based on user preference.
     */
    fun getShowWithEpisodeNumber(
        context: Context, title: String, season: Int,
        episode: Int
    ): String {
        val number = getEpisodeNumber(context, season, episode)
        return "$title $number"
    }

    /**
     * Splits the string on the pipe character `"|"` and reassembles it, separating the items
     * with commas. The given object is returned with the new string.
     *
     * @see buildPipeSeparatedString
     */
    fun splitPipeSeparatedStrings(pipeSeparatedStrings: String?): String {
        if (pipeSeparatedStrings == null) {
            return ""
        }
        val split = pipeSeparatedStrings.split("|")
        val builder = StringBuilder()
        for (item in split) {
            if (builder.isNotEmpty()) {
                builder.append(", ")
            }
            builder.append(item.trim())
        }
        return builder.toString()
    }

    /**
     * Combines the strings into a single string, separated by the pipe character `"|"`.
     * Skips null or empty strings.
     *
     * @see splitPipeSeparatedStrings
     */
    fun buildPipeSeparatedString(strings: List<String?>?): String {
        if (strings == null || strings.isEmpty()) {
            return ""
        }
        // Pre-size builder based on reasonable average length of a string.
        val result = StringBuilder(strings.size * 10)
        for (string in strings) {
            if (string.isNullOrEmpty()) {
                continue // Skip.
            }
            if (result.isNotEmpty()) {
                result.append("|")
            }
            result.append(string)
        }
        return result.toString()
    }

    /**
     * Removes a leading article from the given string (including the first whitespace that
     * follows).
     *
     * *Currently only supports English articles (the, a and an).*
     */
    @JvmStatic
    fun trimLeadingArticle(title: String?): String? {
        if (title.isNullOrEmpty()) {
            return title
        }
        if (title.length > 4 &&
            (title.startsWith("The ") || title.startsWith("the "))) {
            return title.substring(4)
        }
        if (title.length > 2 &&
            (title.startsWith("A ") || title.startsWith("a "))) {
            return title.substring(2)
        }
        if (title.length > 3 &&
            (title.startsWith("An ") || title.startsWith("an "))) {
            return title.substring(3)
        }
        return title
    }

    /**
     * Dot separates the two given strings. If one is empty, just returns the other string (no dot).
     */
    @JvmStatic
    fun dotSeparate(left: String?, right: String?): String {
        val dotString = StringBuilder(left ?: "")
        if (!right.isNullOrEmpty()) {
            if (dotString.isNotEmpty()) {
                dotString.append(" · ")
            }
            dotString.append(right)
        }
        return dotString.toString()
    }

    /**
     * Builds a network + release time string for a show formatted like "Network · Tue 08:00 PM".
     */
    fun networkAndTime(
        context: Context, release: Date?, weekDay: Int,
        network: String?
    ): String {
        return if (release != null) {
            val dayString = TimeTools.formatToLocalDayOrDaily(context, release, weekDay)
            val timeString = TimeTools.formatToLocalTime(context, release)
            dotSeparate(network, "$dayString $timeString")
        } else {
            dotSeparate(network, null)
        }
    }

    /**
     * Appends an empty new line and a new line listing the source of the text as TMDB.
     */
    fun textWithTmdbSource(context: Context, text: String?): SpannableStringBuilder {
        return textWithSource(
            context, text,
            context.getString(R.string.format_source, context.getString(R.string.tmdb))
        )
    }

    private fun textWithSource(
        context: Context, text: String?,
        source: String
    ): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        if (text != null) {
            builder.append(text)
            builder.append("\n\n")
        }
        val sourceStartIndex = builder.length
        builder.append(source)
        builder.setSpan(
            TextAppearanceSpan(context, R.style.TextAppearance_SeriesGuide_Body2_Italic),
            sourceStartIndex, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return builder
    }

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