package com.battlelancer.seriesguide.tmdbapi

import com.uwetrottmann.tmdb2.Tmdb
import okhttp3.OkHttpClient

/**
 * Creates a custom [Tmdb] using the given API key and HTTP client.
 */
class SgTmdb(
    private val okHttpClient: OkHttpClient,
    apiKey: String
) : Tmdb(apiKey) {

    @Synchronized
    override fun okHttpClient(): OkHttpClient {
        return okHttpClient
    }
}
