// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

/**
 * Note: ensure to test with Android SDK as JDK handles locales differently (see [java.util.Locale].
 */
class LanguageToolsTest {

    /**
     * Ensure that using empty strings for region/language code continues to work to obtain display
     * names.
     */
    @Test
    fun languageAndRegionCodeDisplayNames() {
        assertThat(LanguageTools.getDisplayNameForLanguageCode("en"))
            .isEqualTo(Locale.ENGLISH.displayName)
        assertThat(LanguageTools.getDisplayNameForRegionCode("US"))
            .isEqualTo(Locale.US.displayCountry)

        // Edge cases must not result in exceptions
        assertThat(LanguageTools.getDisplayNameForLanguageCode(""))
            .isEqualTo("")
        assertThat(LanguageTools.getDisplayNameForRegionCode(""))
            .isEqualTo("")
        assertThat(LanguageTools.getDisplayNameForLanguageCode("doesnotexist"))
            .isEqualTo("doesnotexist")
        assertThat(LanguageTools.getDisplayNameForRegionCode("doesnotexist"))
            .isEqualTo("DOESNOTEXIST")
    }

}