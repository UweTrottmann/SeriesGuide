package com.battlelancer.seriesguide.thetvdbapi

class TvdbLinks {

    companion object {

        /**
         * TVDB link using show slug. Falls back to old series ID link if slug is null or empty.
         */
        @JvmStatic
        fun show(showTvdbSlug: String?, showTvdbId: Int): String {
            return if (showTvdbSlug.isNullOrEmpty()) {
                "https://www.thetvdb.com/?tab=series&id=$showTvdbId"
            } else {
                "https://www.thetvdb.com/series/$showTvdbSlug"
            }
        }

        /**
         * TVDB link using show slug. Falls back to old series ID link if slug is null or empty.
         */
        @JvmStatic
        fun episode(showTvdbSlug: String?, showTvdbId: Int, seasonTvdbId: Int,
                episodeTvdbId: Int): String {
            return if (showTvdbSlug.isNullOrEmpty()) {
                return "https://www.thetvdb.com/?tab=episode&seriesid=$showTvdbId&seasonid=$seasonTvdbId&id=$episodeTvdbId"
            } else {
                "https://www.thetvdb.com/series/$showTvdbSlug/episodes/$episodeTvdbId"
            }
        }
    }

}