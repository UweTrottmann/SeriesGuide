package com.battlelancer.seriesguide.util

import java.util.Locale

object LanguageToolsK {

    /**
     * Based on the first two letters gets the language display name. Except for Portuguese
     * (pt, pt-PT and pt-BR), where the region is added to the display name.
     *
     * For languages other than Portuguese TVDB currently does not support region variants,
     * and for TMDB they are superfluous or make no sense (report to TMDB?).
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
            "pt-PT", "pt-BR" -> {
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