// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class RatingsToolsTest {

    /**
     * Note: ensure to test on Android as Android uses a different formatter than the local JVM.
     *
     * Added due to Android before version 7.0 behaving unexpectedly with String.format, see note
     * in buildRatingString().
     */
    @Test
    fun buildRatingString() {
        assertThat(RatingsTools.buildRatingString(1.0, Locale.GERMAN)).isEqualTo("1,0")
        assertThat(RatingsTools.buildRatingString(1.5, Locale.GERMAN)).isEqualTo("1,5")
        assertThat(RatingsTools.buildRatingString(1.05, Locale.GERMAN)).isEqualTo("1,1")
    }
}