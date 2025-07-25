// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide.diagnostics

import android.content.Context
import android.net.Uri
import android.os.Build
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.diagnostics.DebugLogBuffer.Companion.BUFFER_SIZE
import com.battlelancer.seriesguide.util.PackageTools
import com.battlelancer.seriesguide.util.TimeTools
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
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

    private val timberTree = object : DebugTree() {

        private val timeZone = safeSystemDefaultZoneId()

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // Warning: code here should not do any Timber log calls,
            // it will cause infinite recursion.
            addEntry(
                DebugLogEntry(
                    priority,
                    tag,
                    message,
                    Instant.now()
                        .atZone(timeZone)
                        .format(LOG_DATE_PATTERN)
                )
            )
        }
    }

    /**
     * ZoneId.systemDefault may fail if the ID returned by the system does not exist in the time
     * zone data included in the current threetenbp version (time zones get renamed and added).
     */
    private fun safeSystemDefaultZoneId(): ZoneId {
        return try {
            ZoneId.systemDefault()
        } catch (e: Exception) {
            ZoneOffset.UTC
        }
    }

    /**
     * Plants this [timberTree].
     */
    fun enable() {
        if (!Timber.forest().contains(timberTree)) {
            Timber.plant(timberTree)
            Timber.i("Turned debug log ON")
        }
    }

    /**
     * Uproots this [timberTree].
     */
    fun disable() {
        if (Timber.forest().contains(timberTree)) {
            Timber.uproot(timberTree)
            Timber.i("Turned debug log OFF")
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
                val separator = System.lineSeparator().toByteArray()

                it.write(systemInfo())
                it.write(separator)
                it.write(separator)

                it.write(logcat())
                it.write(separator)
                it.write(separator)

                val entries = logBufferSnapshot()
                for (entry in entries) {
                    it.write(entry.prettyPrint().toByteArray())
                    it.write(separator)
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

    private fun logcat(): ByteArray {
        try {
            // logcat output is limited by default, so reading everything should be OK regarding
            // memory consumption. Also command exits after running, so no need to destroy process.
            // -d            Dump the log and then exit (don't block).
            val process = Runtime.getRuntime().exec("logcat -d")
            val bufferedReader = process.inputStream.reader().buffered()
            val log = StringBuilder()
            val separator = System.lineSeparator()

            var line: String?
            while ((bufferedReader.readLine().also { line = it }) != null) {
                log.append(line)
                log.append(separator)
            }

            return log.toString().toByteArray()
        } catch (e: Exception) {
            val message = "Failed to get logcat output"
            Timber.e(e, message)
            return message.toByteArray()
        }
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
    }
}