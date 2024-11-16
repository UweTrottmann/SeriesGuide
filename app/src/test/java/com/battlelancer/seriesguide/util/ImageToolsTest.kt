// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2024 Uwe Trottmann

package com.battlelancer.seriesguide.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.battlelancer.seriesguide.EmptyTestApplication
import com.battlelancer.seriesguide.settings.TmdbSettings
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = EmptyTestApplication::class)
class ImageToolsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun posterUrl() {
        assertImageUrl(
            TmdbSettings.POSTER_SIZE_SPEC_W154,
            tmdbUrlBuilder = { path ->
                ImageTools.tmdbOrTvdbPosterUrl(path, context)
            },
            tmdbUrlOriginalBuilder = { path ->
                ImageTools.tmdbOrTvdbPosterUrl(path, context, true)
            },
            tvdbUrlBuilder = { path ->
                ImageTools.tmdbOrTvdbPosterUrl(path, context)
            }
        )
    }

    @Test
    fun episodeImageUrl() {
        assertImageUrl(
            TmdbSettings.BACKDROP_SMALL_SIZE_SPEC,
            tmdbUrlBuilder = { path ->
                ImageTools.buildEpisodeImageUrl(path, context)
            },
            tmdbUrlOriginalBuilder = { path ->
                ImageTools.buildEpisodeImageUrl(path, context, true)
            },
            tvdbUrlBuilder = { path ->
                ImageTools.buildEpisodeImageUrl(path, context)
            }
        )
    }

    private fun assertImageUrl(
        smallSize: String,
        tmdbUrlBuilder: (String) -> String?,
        tmdbUrlOriginalBuilder: (String) -> String?,
        tvdbUrlBuilder: (String) -> String?
    ) {
        // Note: TMDB image paths start with / whereas TVDB paths do not.
        val tmdbUrl = tmdbUrlBuilder("/example.jpg")
        val tmdbUrlOriginal = tmdbUrlOriginalBuilder("/example.jpg")
        val tvdbUrl = tvdbUrlBuilder("posters/example.jpg")
        println("TMDB URL: $tmdbUrl")
        println("TMDB original URL: $tmdbUrlOriginal")
        println("TVDB URL: $tvdbUrl")
        assertThat(tmdbUrl).isNotEmpty()
        assertThat(tmdbUrl).endsWith("https://image.tmdb.org/t/p/$smallSize/example.jpg")
        assertThat(tmdbUrlOriginal).isNotEmpty()
        assertThat(tmdbUrlOriginal).endsWith("https://image.tmdb.org/t/p/original/example.jpg")
        assertThat(tvdbUrl).isNotEmpty()
        assertThat(tvdbUrl).endsWith("https://artworks.thetvdb.com/banners/posters/example.jpg")
    }

    @Test
    fun posterUrl_withLegacyCachePath() {
        val url = ImageTools.tmdbOrTvdbPosterUrl("_cache/posters/example.jpg", context)
        println("TVDB legacy URL: $url")
        assertThat(url).isNotEmpty()
        assertThat(url).endsWith("https://www.thetvdb.com/banners/_cache/posters/example.jpg")
    }

    @Test
    fun posterUrlOrResolve() {
        val url = ImageTools.posterUrlOrResolve(null, 42, null, context)
        assertThat(url).isNotEmpty()
        assertThat(url).isEqualTo("showtmdb://42")
        val urlLang = ImageTools.posterUrlOrResolve(null, 42, "de", context)
        assertThat(urlLang).isNotEmpty()
        assertThat(urlLang).isEqualTo("showtmdb://42?language=de")
    }
}
