package com.battlelancer.seriesguide.ui.search

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class TvdbIdExtractor(val context: Context, val text: String) {

    /**
     * Returns the show TVDb ID or -1 if not found for slug. Runs in [Dispatchers.IO] context.
     */
    suspend fun tryToExtractTvdbId(): Int {
        return withContext(Dispatchers.IO) {
            val showTvdbId = matchLegacyTvdbId()
            if (showTvdbId > 0) {
                showTvdbId
            } else {
                lookUpShowTvdbId()
            }
        }
    }

    private fun lookUpShowTvdbId(): Int {
        // match TVDB URLs like
        // https://www.thetvdb.com/series/lost
        // https://www.thetvdb.com/series/lost/seasons/1
        // https://www.thetvdb.com/series/lost/episodes/127131
        // https://www.thetvdb.com/series/341483
        val tvdbSeriesIdPattern = Pattern.compile("thetvdb\\.com/series/([^/\\n\\r]*)")
        val showSlug = matchShowSlug(tvdbSeriesIdPattern, text)
        if (showSlug.isNullOrBlank()) return -1

        val call = SgApp.getServicesComponent(context).tvdb()
            .search()
            .series(null, null, null, showSlug, null)
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.data?.isNotEmpty() == true) {
                    return body.data?.get(0)?.id ?: -1
                }
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

    private fun matchLegacyTvdbId(): Int {
        // match TVDB URLs like
        // http://thetvdb.com/?tab=season&seriesid=153021&seasonid=627501
        // http://thetvdb.com/?tab=episode&seriesid=281588&seasonid=585321&id=4881155
        // http://thetvdb.com/?tab=series&id=322399

        // match season and episode pages first
        val showTvdbId = matchShowTvdbId(Pattern.compile("thetvdb\\.com.*?seriesid=([0-9]*)"), text)
        return if (showTvdbId > 0) {
            showTvdbId
        } else {
            // match show pages
            matchShowTvdbId(Pattern.compile("thetvdb\\.com.*?id=([0-9]*)"), text)
        }
    }

    private fun matchShowTvdbId(pattern: Pattern, text: String): Int {
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1))
            } catch (ignored: NumberFormatException) {
            }
        }
        return -1
    }

}