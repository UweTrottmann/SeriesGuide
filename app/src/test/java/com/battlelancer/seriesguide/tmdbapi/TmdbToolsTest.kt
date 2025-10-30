// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide.tmdbapi

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TmdbToolsTest {

    @Test
    fun watchProviderLinkSeasonPathRemoval() {
        assertThat(
            "https://www.themoviedb.org/tv/10283-archer/season/14/watch?locale=DE"
                .replace(TmdbTools2.WATCH_PROVIDER_SEASON_PATH_REGEX, "")
        ).isEqualTo("https://www.themoviedb.org/tv/10283-archer/watch?locale=DE")

        assertThat(
            "https://www.themoviedb.org/tv/10283-archer/watch?locale=DE"
                .replace(TmdbTools2.WATCH_PROVIDER_SEASON_PATH_REGEX, "")
        ).isEqualTo("https://www.themoviedb.org/tv/10283-archer/watch?locale=DE")
    }

}