package com.battlelancer.seriesguide.ui.search

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import java.util.regex.Pattern

class TvdbIdExtractor(val context: Context, val text: String) {

    fun tryToExtractTvdbId(): Int {
        // try to match TVDB URLs
        // match season and episode pages first
        val tvdbSeriesIdPattern = Pattern.compile("thetvdb\\.com/series/([^/\\n\\r]*)")
        val showSlug = matchShowSlug(tvdbSeriesIdPattern, text)
        if (showSlug.isNullOrBlank()) return -1

        // TODO async lookup of tvdbId --> use coroutines?
        val call = SgApp.getServicesComponent(context).tvdb()
            .search()
            .series(null, null, null, showSlug, null)
        try {
            val response = call.execute()
            if (response.isSuccessful && response.body() != null) {
                return response.body()!!.data[0].id
            }
        } catch (e: Exception) {
        }
        return -1
    }

    private fun matchShowSlug(pattern: Pattern, text: String): String? {
        val matcher = pattern.matcher(text)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }

}