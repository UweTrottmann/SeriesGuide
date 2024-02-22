// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.battlelancer.seriesguide.EmptyTestApplication
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = EmptyTestApplication::class)
class TmdbSettingsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun isConfigurationUpToDate() {
        assertThat(TmdbSettings.isConfigurationUpToDate(context)).isFalse()

        TmdbSettings.setConfigurationLastUpdatedNow(context)
        assertThat(TmdbSettings.isConfigurationUpToDate(context)).isTrue()
    }

}