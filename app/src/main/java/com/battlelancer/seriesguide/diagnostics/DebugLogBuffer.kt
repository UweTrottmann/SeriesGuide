// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide.diagnostics

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.diagnostics.DebugLogBuffer.Companion.BUFFER_SIZE
import com.battlelancer.seriesguide.util.PackageTools
import com.battlelancer.seriesguide.util.TimeTools
import org.threeten.bp.Instant
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.util.ArrayDeque
import java.util.Deque
import java.util.Locale


/**
 * Keeps any log sent through [Timber] up to [BUFFER_SIZE] in memory.
 */
class DebugLogBuffer(context: Context) {

    private val context: Context = context.applicationContext

    private val entries: Deque<DebugLogEntry> = ArrayDeque(
        BUFFER_SIZE + 1
    )

    fun timberTree(): Timber.Tree {
        return object : DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                addEntry(
                    DebugLogEntry(
                        priority,
                        tag,
                        message,
                        Instant.now()
                            .atZone(TimeTools.safeSystemDefaultZoneId())
                            .format(LOG_DATE_PATTERN)
                    )
                )
            }
        }
    }

    @Synchronized
    private fun addEntry(entry: DebugLogEntry) {
        entries.addLast(entry)

        if (entries.size > BUFFER_SIZE) {
            entries.removeFirst()
        }
    }

    fun logBufferSnapshot(): List<DebugLogEntry> {
        return ArrayList(entries)
    }

    /**
     * Save the current logs to the given file [uri].
     */
    fun save(uri: Uri, listener: OnSaveLogListener) {
        try {
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(systemInfo())
                it.write("\n\n".toByteArray())

                val entries = logBufferSnapshot()
                for (entry in entries) {
                    it.write(entry.prettyPrint().toByteArray())
                    it.write("\n".toByteArray())
                }
            }
            listener.onSuccess()
        } catch (e: Exception) {
            Timber.e(e, "Failed to write log file")
            listener.onError()
        }
    }

    private fun systemInfo(): ByteArray {
        val time = Instant.now()
            .atZone(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        return """
        Time          : $time
        Version       : ${PackageTools.getVersionString(context)}
        Package       : ${BuildConfig.APPLICATION_ID}
        Manufacturer  : ${Build.MANUFACTURER}
        Model         : ${Build.MODEL}
        Product       : ${Build.PRODUCT}
        Android       : ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT} (${Build.VERSION.INCREMENTAL}, ${Build.DISPLAY})
        Locale        : ${Locale.getDefault()}
        """.trimIndent().toByteArray()
    }

    val logFileName: String
        get() {
            val pattern = "seriesguide-log-%s%s"
            // Note: as this is user visible, use the device time zone
            val currentDate = Instant.now()
                .atZone(TimeTools.safeSystemDefaultZoneId())
                .format(FILENAME_PATTERN)

            return String.format(pattern, currentDate, LOG_FILE_END)
        }

    interface OnSaveLogListener {
        fun onSuccess()

        fun onError()
    }

    companion object {

        private const val BUFFER_SIZE = 200

        private val FILENAME_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss", Locale.US)
        private val LOG_DATE_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

        // When using the 'text/plain' MIME type, the Android Storage Access Framework appends the
        // '.txt' file extension regardless of which one is used, so use '.txt'.
        private const val LOG_FILE_END = ".txt"

        @SuppressLint("StaticFieldLeak") // Using application context
        private var sInstance: DebugLogBuffer? = null

        fun getInstance(context: Context): DebugLogBuffer {
            if (sInstance == null) {
                sInstance = DebugLogBuffer(context)
            }

            return sInstance!!
        }
    }
}