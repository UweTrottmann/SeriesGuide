package com.battlelancer.seriesguide.thetvdbapi

import com.battlelancer.seriesguide.EmptyTestApplication
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = EmptyTestApplication::class)
class TvdbImageToolsTest {

    @Test
    fun artworkUrl() {
        val artworkUrl = TvdbImageTools.artworkUrl("example.jpg")
        println("Artwork URL: $artworkUrl")
        assertThat(artworkUrl).isNotEmpty()
        assertThat(artworkUrl).endsWith("https://artworks.thetvdb.com/banners/example.jpg")
    }

    @Test
    fun artworkUrl_withLegacyCachePath() {
        val url = TvdbImageTools.artworkUrl("_cache/posters/example.jpg")
        println("Artwork URL: $url")
        assertThat(url).isNotEmpty()
        assertThat(url).endsWith("https://www.thetvdb.com/banners/_cache/posters/example.jpg")
    }

    @Test
    fun posterUrlOrResolve() {
        val url = TvdbImageTools.posterUrlOrResolve(null, 42, null)
        assertThat(url).isNotEmpty()
        assertThat(url).isEqualTo("showtvdb://42")
        val urlLang = TvdbImageTools.posterUrlOrResolve(null, 42, "de")
        assertThat(urlLang).isNotEmpty()
        assertThat(urlLang).isEqualTo("showtvdb://42?language=de")
    }
}
