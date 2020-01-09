package com.battlelancer.seriesguide.thetvdbapi

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TvdbImageToolsTest {

    @Test
    fun artworkUrl() {
        val artworkUrl = TvdbImageTools.artworkUrl("example.jpg")
        println("Artwork URL: $artworkUrl")
        assertThat(artworkUrl).isNotEmpty()
        assertThat(artworkUrl).endsWith("https://artworks.thetvdb.com/banners/example.jpg")
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
