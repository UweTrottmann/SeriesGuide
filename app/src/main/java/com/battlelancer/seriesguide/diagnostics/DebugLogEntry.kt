// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide.diagnostics

import android.util.Log

class DebugLogEntry(
    val level: Int,
    val tag: String?,
    val message: String,
    val timeStamp: String
) {
    fun prettyPrint(): String {
        return String.format("%18s %18s %s %s", timeStamp, tag, displayLevel(), message)
    }

    private fun displayLevel(): String {
        return when (level) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "A"
            else -> "?"
        }
    }
}