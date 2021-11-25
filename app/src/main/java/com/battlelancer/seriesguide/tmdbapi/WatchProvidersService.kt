package com.battlelancer.seriesguide.tmdbapi

import com.uwetrottmann.tmdb2.entities.WatchProviders
import retrofit2.http.GET
import retrofit2.http.Query

interface WatchProvidersService {

    @GET("watch/providers/movie")
    suspend fun movie(
        @Query("language") language: String?,
        @Query("watch_region") watchRegion: String?
    ): WatchProviderResults

    @GET("watch/providers/tv")
    suspend fun tv(
        @Query("language") language: String?,
        @Query("watch_region") watchRegion: String?
    ): WatchProviderResults

}

data class WatchProviderResults(
    val results: List<WatchProviders.WatchProvider>
)
