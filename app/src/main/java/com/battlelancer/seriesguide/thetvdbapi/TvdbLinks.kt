package com.battlelancer.seriesguide.thetvdbapi

class TvdbLinks {

    companion object {

        private val languageCodeToId = mapOf(
                "en" to 7,
                "sv" to 8,
                "no" to 9,
                "da" to 10,
                "fi" to 11,
                "nl" to 13,
                "de" to 14,
                "it" to 15,
                "es" to 16,
                "fr" to 17,
                "pl" to 18,
                "hu" to 19,
                "el" to 20,
                "tr" to 21,
                "ru" to 22,
                "he" to 24,
                "ja" to 25,
                "pt" to 26,
                "zh" to 27,
                "cs" to 28,
                "ls" to 30,
                "hr" to 31,
                "ko" to 32
        )

        private fun getLanguageId(languageCode: String?): Int {
            return languageCodeToId[languageCode] ?: 7 // fall back to 'en'
        }

        @JvmStatic
        fun show(showTvdbId: Int, languageCode: String?): String {
            val languageId = getLanguageId(languageCode)
            return "https://thetvdb.com/?tab=series&id=$showTvdbId&lid=$languageId"
        }

        @JvmStatic
        fun episode(showTvdbId: Int, seasonTvdbId: Int, episodeTvdbId: Int,
                languageCode: String?): String {
            val languageId = getLanguageId(languageCode)
            return "https://thetvdb.com/?tab=episode" +
                    "&seriesid=$showTvdbId&seasonid=$seasonTvdbId&id=$episodeTvdbId&lid=$languageId"
        }
    }

}