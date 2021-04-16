package com.battlelancer.seriesguide.ui.search

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TmdbIdExtractor(val context: Context, val text: String) {

    /**
     * Returns the show TMDb ID or -1 if not found. Runs in [Dispatchers.IO] context.
     */
    suspend fun tryToExtract(): Int {
        return withContext(Dispatchers.Default) {
            // match URLs like
            // https://www.themoviedb.org/tv/82856-the-mandalorian/season/1/episode/1
            val result = Regex("themoviedb\\.org/tv/(\\d*)").find(text)
            result?.groupValues?.get(1)?.toIntOrNull() ?: 0
        }
    }

}