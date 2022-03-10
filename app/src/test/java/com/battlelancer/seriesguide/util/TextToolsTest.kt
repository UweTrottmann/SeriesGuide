package com.battlelancer.seriesguide.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TextToolsTest {

    @Test
    fun splitPipeSeparatedStrings() {
        assertThat(
            TextTools.splitPipeSeparatedStrings(null)
        ).isEqualTo("")
        assertThat(
            TextTools.splitPipeSeparatedStrings("")
        ).isEqualTo("")
        assertThat(
            TextTools.splitPipeSeparatedStrings("First|Second | Third")
        ).isEqualTo("First, Second, Third")
    }

    @Test
    fun buildPipeSeparatedString() {
        assertThat(
            TextTools.buildPipeSeparatedString(null)
        ).isEqualTo("")
        assertThat(
            TextTools.buildPipeSeparatedString(emptyList())
        ).isEqualTo("")
        assertThat(
            TextTools.buildPipeSeparatedString(listOf("First", "Second ", null, "", " Third"))
        ).isEqualTo("First|Second | Third")
    }
}