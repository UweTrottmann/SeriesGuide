package com.battlelancer.seriesguide.util

import android.content.Context
import android.text.TextUtils
import androidx.annotation.ArrayRes
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.DisplaySettings
import java.util.Locale

/**
 * Helper methods for language strings and codes.
 */
object LanguageTools {

    /**
     * Returns the string representation of the given two letter ISO 639-1 language code,
     * plus optional ISO-3166-1 region tag, if it is supported by SeriesGuide
     * (see R.array.languageCodesShows).
     *
     * If the given language code is `null` uses 'en' to ensure consistent behavior across
     * devices.
     */
    fun getShowLanguageStringFor(context: Context, languageCode: String?): String {
        val actualLanguageCode = if (languageCode.isNullOrEmpty()) {
            // default to 'en'
            DisplaySettings.LANGUAGE_EN
        } else languageCode

        return getLanguageStringFor(context, actualLanguageCode, R.array.languageCodesShows)
    }

    /**
     * Returns the string representation of the given two letter ISO 639-1 language code,
     * plus optional ISO-3166-1 region tag, if it is supported by SeriesGuide
     * (see R.array.languageCodesMovies).
     *
     * If the given language code is `null`, uses [DisplaySettings.getMoviesLanguage].
     */
    @JvmStatic
    fun getMovieLanguageStringFor(context: Context, languageCode: String?): String {
        val actualLanguageCode = if (TextUtils.isEmpty(languageCode)) {
            // fall back to default language
            DisplaySettings.getMoviesLanguage(context)
        } else languageCode

        return getLanguageStringFor(context, actualLanguageCode, R.array.languageCodesMovies)
    }

    private fun getLanguageStringFor(
        context: Context, languageCode: String?,
        @ArrayRes languageCodesRes: Int
    ): String {
        val languageCodes = context.resources.getStringArray(languageCodesRes)
        for (i in languageCodes.indices) {
            if (languageCodes[i] == languageCode) {
                return buildLanguageDisplayName(languageCode!!)
            }
        }

        return context.getString(R.string.unknown)
    }

    /**
     * Together with the language code, returns the string representation of the given
     * two letter ISO 639-1 language code, plus optional ISO-3166-1 region tag,
     * if it is supported by SeriesGuide (see R.array.languageCodesShows).
     *
     * If the given language code is `null` uses 'en' to ensure consistent behavior across
     * devices.
     */
    fun getShowLanguageDataFor(
        context: Context,
        languageCode: String?
    ): LanguageData? {
        val actualLanguageCode = if (languageCode.isNullOrEmpty()) {
            // default to 'en'
            DisplaySettings.LANGUAGE_EN
        } else languageCode

        val languageCodes = context.resources.getStringArray(R.array.languageCodesShows)
        for (i in languageCodes.indices) {
            if (languageCodes[i] == actualLanguageCode) {
                val languageName = buildLanguageDisplayName(actualLanguageCode)
                return LanguageData(actualLanguageCode, languageName)
            }
        }

        return null
    }

    data class LanguageData(val languageCode: String?, val languageString: String)

    /**
     * Based on the first two letters gets the language display name. Except for
     * - Spanish (es-ES, es-MX)
     * - French (fr-FR, fr-CA)
     * - Portuguese (pt, pt-PT and pt-BR) and
     * - Chinese (zh, zh-CN, zh-TW, zh-HK),
     * where the region is added to the display name.
     *
     * For other languages region variants for TMDB appear to be superfluous or make no sense
     * (report to TMDB?).
     */
    fun buildLanguageDisplayName(languageCode: String): String {
        return when (languageCode) {
            "pt" -> {
                // Manually add country for pt-PT as for backwards compat
                // the show language code is still pt.
                Locale(languageCode.substring(0, 2), "PT")
                    .displayName
            }
            "zh" -> {
                // Manually add country for zh-CN as for backwards compat
                // the show language code is still zh (and TMDB returns zh-CN data).
                Locale(languageCode.substring(0, 2), "CN")
                    .displayName
            }
            "es-ES", "es-MX", "fr-CA", "fr-FR", "pt-PT", "pt-BR", "zh-CN", "zh-HK", "zh-TW" -> {
                Locale(languageCode.substring(0, 2), languageCode.substring(3, 5))
                    .displayName
            }
            else -> {
                Locale(languageCode.substring(0, 2), "")
                    .displayName
            }
        }
    }
}