// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.tools

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.battlelancer.seriesguide.shows.database.SgShow2
import com.battlelancer.seriesguide.shows.overview.CustomTimeData
import com.battlelancer.seriesguide.util.TimeTools
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime

/**
 * Note: ensure to test on Android as local JVM handles time and dates differently.
 */
@RunWith(AndroidJUnit4::class)
class CustomReleaseTimeTest {

    @Test
    fun customTimeInfo() {
        val customTime = LocalDate.of(2023, 5, 5)
            .atTime(9, 0)
            .atZone(TimeTools.safeSystemDefaultZoneId())

        val customTimeData = CustomTimeData(
            officialWeekDay = -1, // Not tested, use default.
            customTime = customTime.toLocalTime(),
            customDayOffset = 0
        )

        // Updates time
        val at10 = customTimeData.updateTime(10, 11)
        assertThat(at10.customTime)
            .isEqualTo(LocalTime.of(10, 11))

        // Increases by 1
        assertThat(customTimeData.increaseDayOffset().customDayOffset)
            .isEqualTo(1)

        // Does not exceed max
        var maxDayOffset = customTimeData
        for (i in 0..SgShow2.MAX_CUSTOM_DAY_OFFSET + 5) {
            maxDayOffset = maxDayOffset.increaseDayOffset()
        }
        assertThat(maxDayOffset.customDayOffset)
            .isEqualTo(SgShow2.MAX_CUSTOM_DAY_OFFSET)

        // Decreases by 1
        assertThat(customTimeData.decreaseDayOffset().customDayOffset)
            .isEqualTo(-1)

        // Does not exceed min
        var minDayOffset = customTimeData
        for (i in 0..SgShow2.MAX_CUSTOM_DAY_OFFSET + 5) {
            minDayOffset = minDayOffset.decreaseDayOffset()
        }
        assertThat(minDayOffset.customDayOffset)
            .isEqualTo(-SgShow2.MAX_CUSTOM_DAY_OFFSET)
    }

}