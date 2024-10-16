// SPDX-License-Identifier: Apache-2.0
// Copyright 2013-2024 Uwe Trottmann

package com.battlelancer.seriesguide.util

import com.battlelancer.seriesguide.shows.episodes.EpisodeFlags
import com.battlelancer.seriesguide.shows.episodes.EpisodeTools
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class EpisodeToolsTest {

    @Test
    fun test_isWatched() {
        assertThat(EpisodeTools.isWatched(EpisodeFlags.WATCHED)).isTrue()
        assertThat(EpisodeTools.isWatched(EpisodeFlags.SKIPPED)).isFalse()
        assertThat(EpisodeTools.isWatched(EpisodeFlags.UNWATCHED)).isFalse()
    }

    @Test
    fun test_isUnwatched() {
        assertThat(EpisodeTools.isUnwatched(EpisodeFlags.UNWATCHED)).isTrue()
        assertThat(EpisodeTools.isUnwatched(EpisodeFlags.WATCHED)).isFalse()
        assertThat(EpisodeTools.isUnwatched(EpisodeFlags.SKIPPED)).isFalse()
    }

    @Test
    fun test_validateFlags() {
        EpisodeTools.validateFlags(EpisodeFlags.UNWATCHED)
        EpisodeTools.validateFlags(EpisodeFlags.WATCHED)
        EpisodeTools.validateFlags(EpisodeFlags.SKIPPED)
        assertThrows(IllegalArgumentException::class.java) {
            EpisodeTools.validateFlags(123)
        }
    }

}