// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2019 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util

import com.battlelancer.seriesguide.EmptyTestApplication
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = EmptyTestApplication::class)
class ErrorsTest {

    @Test
    fun bendingStackTrace() {
        val throwable = Throwable()
        val stackTraceOriginal = throwable.stackTrace

        val lineNumberBelow = stackTraceOriginal[0].lineNumber + 4 /* lines below */
        Errors.logAndReport("dummy", throwable)

        val stackTraceModified = throwable.stackTrace

        assertThat(stackTraceModified.size).isEqualTo(stackTraceOriginal.size + 1)

        val newElement = stackTraceModified[0]

        assertThat(newElement).isNotEqualTo(stackTraceOriginal[0])
        for (i in stackTraceOriginal.indices) {
            assertThat(stackTraceModified[i + 1]).isEqualTo(stackTraceOriginal[i])
        }

        assertThat(newElement.className).isEqualTo(this.javaClass.getName())
        assertThat(newElement.lineNumber).isEqualTo(lineNumberBelow)
    }

    @Test
    fun removeErrorToolsFromStackTrace() {
        val throwable = Errors.testCreateThrowable()
        val stackTraceOriginal = throwable.stackTrace

        Errors.removeErrorToolsFromStackTrace(throwable)

        val stackTraceModified = throwable.stackTrace
        assertThat(stackTraceModified.size < stackTraceOriginal.size).isTrue()
        val sizeDiff = stackTraceOriginal.size - stackTraceModified.size
        for (i in stackTraceModified.indices) {
            assertThat(stackTraceModified[i]).isEqualTo(stackTraceOriginal[i + sizeDiff])
        }
    }
}
