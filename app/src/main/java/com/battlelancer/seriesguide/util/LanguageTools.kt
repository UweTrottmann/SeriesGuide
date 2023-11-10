// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

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

    fun mapLegacyMovieCode(languageCode: String): String {
        return legacyCodesMappingMovies[languageCode] ?: languageCode
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
//        "pt-BR"
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
//        "zh-HK"
//        "zh-TW"
        "zu" to "zu-ZA",
    )

    private val legacyCodesMappingMovies = mapOf(
        "ab-AB" to LANGUAGE_EN,
        "aa-AA" to LANGUAGE_EN,
        "af-AF" to "af-ZA",
        "ak-AK" to LANGUAGE_EN,
        "sq-SQ" to "sq-AL",
        "am-AM" to LANGUAGE_EN,
        "ar-AR" to "ar-AE",
        "an-AN" to LANGUAGE_EN,
        "hy-HY" to LANGUAGE_EN,
        "as-AS" to LANGUAGE_EN,
        "av-AV" to LANGUAGE_EN,
        "ae-AE" to LANGUAGE_EN,
        "ay-AY" to LANGUAGE_EN,
        "az-AZ" to LANGUAGE_EN,
        "bm-BM" to LANGUAGE_EN,
        "ba-BA" to LANGUAGE_EN,
//        "eu-ES"
        "be-BE" to "be-BY",
//        "bn-BD"
        "bi-BI" to LANGUAGE_EN,
        "bs-BS" to LANGUAGE_EN,
        "br-BR" to LANGUAGE_EN,
//        "bg-BG"
        "my-MY" to LANGUAGE_EN,
        "cn-CN" to LANGUAGE_EN,
//        "ca-ES"
//        "ch-GU"
        "ce-CE" to LANGUAGE_EN,
        "ny-NY" to LANGUAGE_EN,
        "cv-CV" to LANGUAGE_EN,
        "kw-KW" to LANGUAGE_EN,
        "co-CO" to LANGUAGE_EN,
        "cr-CR" to LANGUAGE_EN,
//        "hr-HR"
//        "cs-CZ"
//        "da-DK"
        "dv-DV" to LANGUAGE_EN,
//        "nl-NL"
        "dz-DZ" to LANGUAGE_EN,
//        "en-US"
//        "eo-EO"
        "et-ET" to "et-EE",
        "ee-EE" to LANGUAGE_EN,
        "fo-FO" to LANGUAGE_EN,
        "fj-FJ" to LANGUAGE_EN,
//        "fi-FI"
//        "fr-CA"
//        "fr-FR"
        "fy-FY" to LANGUAGE_EN,
        "ff-FF" to LANGUAGE_EN,
        "gd-GD" to "gd-GB",
        "gl-GL" to "gl-ES",
        "lg-LG" to LANGUAGE_EN,
//        "ka-GE"
//        "de-DE"
//        "el-GR"
        "gn-GN" to LANGUAGE_EN,
        "gu-GU" to LANGUAGE_EN,
        "ht-HT" to LANGUAGE_EN,
        "ha-HA" to LANGUAGE_EN,
//        "he-IL"
        "hz-HZ" to LANGUAGE_EN,
//        "hi-IN"
        "ho-HO" to LANGUAGE_EN,
//        "hu-HU"
        "is-IS" to LANGUAGE_EN,
        "io-IO" to LANGUAGE_EN,
        "ig-IG" to LANGUAGE_EN,
//        "id-ID"
        "ia-IA" to LANGUAGE_EN,
        "ie-IE" to LANGUAGE_EN,
        "iu-IU" to LANGUAGE_EN,
        "ik-IK" to LANGUAGE_EN,
        "ga-GA" to "ga-IE",
//        "it-IT"
//        "ja-JP"
        "jv-JV" to LANGUAGE_EN,
        "kl-KL" to LANGUAGE_EN,
        "kn-KN" to "kn-IN",
        "kr-KR" to LANGUAGE_EN,
        "ks-KS" to LANGUAGE_EN,
        "kk-KK" to "kk-KZ",
        "km-KM" to LANGUAGE_EN,
        "ki-KI" to LANGUAGE_EN,
        "rw-RW" to LANGUAGE_EN,
        "ky-KY" to "ky-KG",
        "kv-KV" to LANGUAGE_EN,
        "kg-KG" to LANGUAGE_EN,
//        "ko-KR"
        "kj-KJ" to LANGUAGE_EN,
        "ku-KU" to LANGUAGE_EN,
        "lo-LO" to LANGUAGE_EN,
        "la-LA" to LANGUAGE_EN,
//        "lv-LV"
        "lb-LB" to LANGUAGE_EN,
        "li-LI" to LANGUAGE_EN,
        "ln-LN" to LANGUAGE_EN,
//        "lt-LT"
        "lu-LU" to LANGUAGE_EN,
        "mk-MK" to LANGUAGE_EN,
        "mg-MG" to LANGUAGE_EN,
        "ms-MS" to "ms-MY",
        "ml-ML" to "ml-IN",
        "mt-MT" to LANGUAGE_EN,
//        "zh-CN"
//        "zh-HK"
//        "zh-TW"
        "gv-GV" to LANGUAGE_EN,
        "mi-MI" to LANGUAGE_EN,
        "mr-MR" to "mr-IN",
        "mh-MH" to LANGUAGE_EN,
        "mo-MO" to LANGUAGE_EN,
        "mn-MN" to LANGUAGE_EN,
        "na-NA" to LANGUAGE_EN,
        "nv-NV" to LANGUAGE_EN,
        "nd-ND" to LANGUAGE_EN,
        "nr-NR" to LANGUAGE_EN,
        "ng-NG" to LANGUAGE_EN,
        "ne-NE" to LANGUAGE_EN,
        "xx-XX" to LANGUAGE_EN,
        "se-SE" to LANGUAGE_EN,
//        "no-NO"
//        "nb-NO"
        "nn-NN" to LANGUAGE_EN,
        "oc-OC" to LANGUAGE_EN,
        "oj-OJ" to LANGUAGE_EN,
        "or-OR" to LANGUAGE_EN,
        "om-OM" to LANGUAGE_EN,
        "os-OS" to LANGUAGE_EN,
        "pi-PI" to LANGUAGE_EN,
//        "fa-IR"
//        "pl-PL"
//        "pt-BR"
//        "pt-PT"
        "pa-PA" to "pa-IN",
        "ps-PS" to LANGUAGE_EN,
        "qu-QU" to LANGUAGE_EN,
        "rm-RM" to LANGUAGE_EN,
//        "ro-RO"
        "rn-RN" to LANGUAGE_EN,
//        "ru-RU"
        "sm-SM" to LANGUAGE_EN,
        "sg-SG" to LANGUAGE_EN,
        "sa-SA" to LANGUAGE_EN,
        "sc-SC" to LANGUAGE_EN,
//        "sr-RS"
        "sh-SH" to LANGUAGE_EN,
        "sn-SN" to LANGUAGE_EN,
        "sd-SD" to LANGUAGE_EN,
        "si-SI" to "si-LK",
        "cu-CU" to LANGUAGE_EN,
//        "sk-SK"
//        "sl-SI"
        "so-SO" to LANGUAGE_EN,
        "st-ST" to LANGUAGE_EN,
//        "es-ES"
//        "es-MX"
        "su-SU" to LANGUAGE_EN,
        "sw-SW" to LANGUAGE_EN,
        "ss-SS" to LANGUAGE_EN,
//        "sv-SE"
        "tl-TL" to "tl-PH",
        "ty-TY" to LANGUAGE_EN,
        "tg-TG" to LANGUAGE_EN,
//        "ta-IN"
        "tt-TT" to LANGUAGE_EN,
        "te-TE" to "te-IN",
//        "th-TH"
        "bo-BO" to LANGUAGE_EN,
        "ti-TI" to LANGUAGE_EN,
        "to-TO" to LANGUAGE_EN,
        "ts-TS" to LANGUAGE_EN,
        "tn-TN" to LANGUAGE_EN,
//        "tr-TR"
        "tk-TK" to LANGUAGE_EN,
        "tw-TW" to LANGUAGE_EN,
        "ug-UG" to LANGUAGE_EN,
//        "uk-UA"
        "ur-UR" to LANGUAGE_EN,
        "uz-UZ" to LANGUAGE_EN,
        "ve-VE" to LANGUAGE_EN,
//        "vi-VN"
        "vo-VO" to LANGUAGE_EN,
        "wa-WA" to LANGUAGE_EN,
        "cy-CY" to "cy-GB",
        "wo-WO" to LANGUAGE_EN,
        "xh-XH" to LANGUAGE_EN,
        "ii-II" to LANGUAGE_EN,
        "yi-YI" to LANGUAGE_EN,
        "za-ZA" to LANGUAGE_EN,
        "zu-ZU" to "zu-ZA",
    )
}