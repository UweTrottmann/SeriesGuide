package com.battlelancer.seriesguide.util

import java.util.Locale

object LanguageToolsK {

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
    @JvmStatic
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