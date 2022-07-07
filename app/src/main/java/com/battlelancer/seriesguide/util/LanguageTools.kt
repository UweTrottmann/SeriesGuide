package com.battlelancer.seriesguide.util

import android.content.Context
import android.text.TextUtils
import androidx.annotation.ArrayRes
import com.battlelancer.seriesguide.R
import java.util.Locale

/**
 * Helper methods for language strings and codes.
 */
object LanguageTools {

    const val LANGUAGE_EN = "en-US"

    /**
     * Returns the string representation of the given two letter ISO 639-1 language code
     * plus ISO-3166-1 region tag, if it is supported by SeriesGuide.
     *
     * If the given language code is `null` uses [LANGUAGE_EN] to ensure consistent
     * behavior across devices.
     */
    fun getShowLanguageStringFor(context: Context, languageCode: String?): String {
        val actualLanguageCode = if (languageCode.isNullOrEmpty()) {
            LANGUAGE_EN // default
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
     * If the given language code is `null` uses [LANGUAGE_EN] to ensure consistent
     * behavior across devices.
     */
    fun getShowLanguageDataFor(
        context: Context,
        languageCode: String?
    ): LanguageData? {
        val actualLanguageCode = if (languageCode.isNullOrEmpty()) {
            LANGUAGE_EN // default
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

    fun mapLegacyShowCode(languageCode: String): String {
        return legacyCodesMappingShows[languageCode] ?: languageCode
    }

    private val legacyCodesMappingShows = mapOf(
        "en" to "en-US",
        "cs" to "cs-CZ",
        "da" to "da-DK",
        "de" to "de-DE",
        "el" to "el-GR",
        "es" to "es-ES",
        "fr" to "fr-FR",
        "hr" to "hr-HR",
        "it" to "it-IT",
        "hu" to "hu-HU",
        "nl" to "nl-NL",
        "no" to "no-NO",
        "pl" to "pl-PL",
        "ru" to "ru-RU",
        "pt" to "pt-PT",
        "sl" to "sl-SI",
        "fi" to "fi-FI",
        "sv" to "sv-SE",
        "tr" to "tr-TR",
        "ko" to "ko-KR",
        "zh" to "zh-CN",
        "he" to "he-IL",
        "ja" to "ja-JP",
        "aa" to LANGUAGE_EN,
        "ab" to LANGUAGE_EN,
        "af" to "af-ZA",
        "ak" to LANGUAGE_EN,
        "am" to LANGUAGE_EN,
        "ar" to "ar-AE",
        "an" to LANGUAGE_EN,
        "as" to LANGUAGE_EN,
        "av" to LANGUAGE_EN,
        "ae" to LANGUAGE_EN,
        "ay" to LANGUAGE_EN,
        "az" to LANGUAGE_EN,
        "ba" to LANGUAGE_EN,
        "bm" to LANGUAGE_EN,
        "be" to "be-BY",
        "bn" to "bn-BD",
        "bh" to LANGUAGE_EN,
        "bi" to LANGUAGE_EN,
        "bo" to LANGUAGE_EN,
        "bs" to LANGUAGE_EN,
        "br" to LANGUAGE_EN,
        "bg" to LANGUAGE_EN,
        "ca" to "ca-ES",
        "ch" to "ch-GU",
        "ce" to LANGUAGE_EN,
        "cu" to LANGUAGE_EN,
        "cv" to LANGUAGE_EN,
        "kw" to LANGUAGE_EN,
        "co" to LANGUAGE_EN,
        "cr" to LANGUAGE_EN,
        "cy" to "cy-GB",
        "dv" to LANGUAGE_EN,
        "dz" to LANGUAGE_EN,
        "eo" to "eo-EO",
        "et" to "et-EE",
        "eu" to "eu-ES",
        "ee" to LANGUAGE_EN,
        "fo" to LANGUAGE_EN,
        "fa" to "fa-IR",
        "fj" to LANGUAGE_EN,
        "fy" to LANGUAGE_EN,
        "ff" to LANGUAGE_EN,
        "gd" to "gd-GB",
        "ga" to "ga-IE",
        "gl" to "gl-ES",
        "gv" to LANGUAGE_EN,
        "gn" to LANGUAGE_EN,
        "gu" to LANGUAGE_EN,
        "ht" to LANGUAGE_EN,
        "ha" to LANGUAGE_EN,
        "hz" to LANGUAGE_EN,
        "hi" to "hi-IN",
        "ho" to LANGUAGE_EN,
        "hy" to LANGUAGE_EN,
        "ig" to LANGUAGE_EN,
        "io" to LANGUAGE_EN,
        "ii" to LANGUAGE_EN,
        "iu" to LANGUAGE_EN,
        "ie" to LANGUAGE_EN,
        "ia" to LANGUAGE_EN,
        "id" to "id-ID",
        "ik" to LANGUAGE_EN,
        "is" to LANGUAGE_EN,
        "jv" to LANGUAGE_EN,
        "kl" to LANGUAGE_EN,
        "kn" to "kn-IN",
        "ks" to LANGUAGE_EN,
        "ka" to "ka-GE",
        "kr" to LANGUAGE_EN,
        "kk" to "kk-KZ",
        "km" to LANGUAGE_EN,
        "ki" to LANGUAGE_EN,
        "rw" to LANGUAGE_EN,
        "ky" to "ky-KG",
        "kv" to LANGUAGE_EN,
        "kg" to LANGUAGE_EN,
        "kj" to LANGUAGE_EN,
        "ku" to LANGUAGE_EN,
        "lo" to LANGUAGE_EN,
        "la" to LANGUAGE_EN,
        "lv" to "lv-LV",
        "li" to LANGUAGE_EN,
        "ln" to LANGUAGE_EN,
        "lt" to "lt-LT",
        "lb" to LANGUAGE_EN,
        "lu" to LANGUAGE_EN,
        "lg" to LANGUAGE_EN,
        "mh" to LANGUAGE_EN,
        "ml" to "ml-IN",
        "mr" to "mr-IN",
        "mk" to LANGUAGE_EN,
        "mg" to LANGUAGE_EN,
        "mt" to LANGUAGE_EN,
        "mn" to LANGUAGE_EN,
        "mi" to LANGUAGE_EN,
        "ms" to "ms-MY",
        "my" to LANGUAGE_EN,
        "na" to LANGUAGE_EN,
        "nv" to LANGUAGE_EN,
        "nr" to LANGUAGE_EN,
        "nd" to LANGUAGE_EN,
        "ng" to LANGUAGE_EN,
        "ne" to LANGUAGE_EN,
        "nn" to LANGUAGE_EN,
        "nb" to "nb-NO",
        "ny" to LANGUAGE_EN,
        "oc" to LANGUAGE_EN,
        "oj" to LANGUAGE_EN,
        "or" to LANGUAGE_EN,
        "om" to LANGUAGE_EN,
        "os" to LANGUAGE_EN,
        "pa" to "pa-IN",
        "pi" to LANGUAGE_EN,
        "ps" to LANGUAGE_EN,
        "qu" to LANGUAGE_EN,
        "rm" to LANGUAGE_EN,
        "ro" to "ro-RO",
        "rn" to LANGUAGE_EN,
        "sg" to LANGUAGE_EN,
        "sa" to LANGUAGE_EN,
        "si" to "si-LK",
        "sk" to "sk-SK",
        "se" to LANGUAGE_EN,
        "sm" to LANGUAGE_EN,
        "sn" to LANGUAGE_EN,
        "sd" to LANGUAGE_EN,
        "so" to LANGUAGE_EN,
        "st" to LANGUAGE_EN,
        "sq" to "sq-AL",
        "sc" to LANGUAGE_EN,
        "sr" to "sr-RS",
        "ss" to LANGUAGE_EN,
        "su" to LANGUAGE_EN,
        "sw" to LANGUAGE_EN,
        "ty" to LANGUAGE_EN,
        "ta" to "ta-IN",
        "tt" to LANGUAGE_EN,
        "te" to "te-IN",
        "tg" to LANGUAGE_EN,
        "tl" to "tl-PH",
        "th" to "th-TH",
        "ti" to LANGUAGE_EN,
        "to" to LANGUAGE_EN,
        "tn" to LANGUAGE_EN,
        "ts" to LANGUAGE_EN,
        "tk" to LANGUAGE_EN,
        "tw" to LANGUAGE_EN,
        "ug" to LANGUAGE_EN,
        "uk" to "uk-UA",
        "ur" to LANGUAGE_EN,
        "uz" to LANGUAGE_EN,
        "ve" to LANGUAGE_EN,
        "vi" to "vi-VN",
        "vo" to LANGUAGE_EN,
        "wa" to LANGUAGE_EN,
        "wo" to LANGUAGE_EN,
        "xh" to LANGUAGE_EN,
        "yi" to LANGUAGE_EN,
        "yo" to LANGUAGE_EN,
        "za" to LANGUAGE_EN,
        "zu" to "zu-ZA",
    )
}