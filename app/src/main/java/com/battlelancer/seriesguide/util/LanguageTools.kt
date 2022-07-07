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
     * Returns the string representation of the given two letter ISO 639-1 language code
     * plus ISO-3166-1 region tag, if it is supported by SeriesGuide.
     *
     * If the given language code is `null` uses [DisplaySettings.LANGUAGE_EN] to ensure consistent
     * behavior across devices.
     */
    fun getShowLanguageStringFor(context: Context, languageCode: String?): String {
        val actualLanguageCode = if (languageCode.isNullOrEmpty()) {
            DisplaySettings.LANGUAGE_EN // default
        } else languageCode

        return getLanguageStringFor(context, actualLanguageCode, R.array.content_languages)
    }

    /**
     * Returns the string representation of the given two letter ISO 639-1 language code
     * plus ISO-3166-1 region tag, if it is supported by SeriesGuide.
     *
     * If the given language code is `null`, uses [fallback].
     */
    @JvmStatic
    fun getMovieLanguageStringFor(
        context: Context,
        languageCode: String?,
        fallback: String
    ): String {
        val actualLanguageCode = if (TextUtils.isEmpty(languageCode)) {
            // fall back to default language
            fallback
        } else languageCode

        return getLanguageStringFor(context, actualLanguageCode, R.array.content_languages)
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
     * Together with the language code, returns the string representation of the given two letter
     * ISO 639-1 language code plus ISO-3166-1 region tag, if it is supported by SeriesGuide.
     *
     * If the given language code is `null` uses [DisplaySettings.LANGUAGE_EN] to ensure consistent
     * behavior across devices.
     */
    fun getShowLanguageDataFor(
        context: Context,
        languageCode: String?
    ): LanguageData? {
        val actualLanguageCode = if (languageCode.isNullOrEmpty()) {
            DisplaySettings.LANGUAGE_EN // default
        } else languageCode

        val languageCodes = context.resources.getStringArray(R.array.content_languages)
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
     * - Arabic (ar-AE, ar-SA)
     * - German (de-AT, de-CH, de-DE)
     * - English (en-AU, en-CA, en-GB, en-IE, en-NZ, en-US)
     * - Spanish (es-ES, es-MX)
     * - French (fr-CA, fr-FR)
     * - Malay (ms-MY, ms-SG)
     * - Dutch (nl-BE, nl-NL)
     * - Portuguese (pt-PT, pt-BR) and
     * - Chinese (zh-CN, zh-HK, zh-SG, zh-TW),
     * where the region is added to the display name.
     */
    fun buildLanguageDisplayName(languageCode: String): String {
        return when (languageCode) {
            "ar-AE", "ar-SA",
            "de-AT", "de-CH", "de-DE",
            "en-AU", "en-CA", "en-GB", "en-IE", "en-NZ", "en-US",
            "es-ES", "es-MX",
            "fr-CA", "fr-FR",
            "ms-MY", "ms-SG",
            "nl-BE", "nl-NL",
            "pt-PT", "pt-BR",
            "zh-CN", "zh-HK", "zh-SG", "zh-TW" -> {
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