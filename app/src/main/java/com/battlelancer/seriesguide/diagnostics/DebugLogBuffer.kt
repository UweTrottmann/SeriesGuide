// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.diagnostics

import android.content.Context
import android.net.Uri
import android.os.Build
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.diagnostics.DebugLogBuffer.Companion.LOG_MAX_AGE_MILLIS
import com.battlelancer.seriesguide.util.PackageTools
import com.battlelancer.seriesguide.util.TimeTools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.util.Locale
import kotlin.time.Duration.Companion.days


/**
 * Keeps any log message sent through [Timber] up to [LOG_MAX_AGE_MILLIS] in the [DebugLogDatabase].
 */
class DebugLogBuffer(
    context: Context,
    private val dbHelper: DebugLogHelper,
    private val coroutineScope: CoroutineScope
) {

    private val context: Context = context.applicationContext

    // At most 1 coroutine at a time should update the log database
    private val debugLogDatabaseDispatcher = Dispatchers.Default.limitedParallelism(1)

    private val timberTree = object : DebugTree() {

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // Warning: code here should not do any Timber log calls,
            // it will cause infinite recursion.
            addEntry(priority, tag, message)
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

    private fun addEntry(priority: Int, tag: String?, message: String) {
        coroutineScope.launch(debugLogDatabaseDispatcher) {
            val currentTime = Instant.now().toEpochMilli()

            try {
                // Delete before adding new data
                dbHelper.deleteOlderThan(currentTime - LOG_MAX_AGE_MILLIS)
                // To avoid the database growing too large if there are many messages for whatever
                // reason, enforce a maximum number of rows.
                dbHelper.trim()

                dbHelper.insert(
                    DbDebugLogEntry(
                        priority = priority,
                        tag = tag,
                        message = message,
                        createdAt = currentTime
                    )
                )
            } catch (e: Exception) {
                // Disable first to avoid calling this again. It will be enabled again during
                // initialization when the app is restarted.
                disable()
                Timber.e(e, "Trimming or saving debug log entry failed")
            }
        }
    }

    /**
     * ZoneId.systemDefault may fail if the ID returned by the system does not exist in the time
     * zone data included in the current threetenbp version (time zones get renamed and added).
     */
    private fun safeSystemDefaultZoneId(): ZoneId {
        return try {
            ZoneId.systemDefault()
        } catch (_: Exception) {
            ZoneOffset.UTC
        }
    }

    suspend fun logBufferSnapshot(): List<DebugLogEntry> {
        val timeZone = safeSystemDefaultZoneId()
        return dbHelper.getAll().mapNotNull { (_, priority, tag, message, createdAt) ->
            if (priority == null || message == null || createdAt == null) {
                return@mapNotNull null
            }
            DebugLogEntry(
                priority,
                tag,
                message,
                Instant.ofEpochMilli(createdAt).atZone(timeZone)
                    .format(LOG_DATE_PATTERN)
            )
        }
    }

    /**
     * Save the current logs to the given file [uri].
     */
    suspend fun save(uri: Uri, listener: OnSaveLogListener) {
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

        /**
         * Long enough to catch issues happening while in the background, but short enough to keep
         * the database file size small.
         */
        private val LOG_MAX_AGE_MILLIS = 3.days.inWholeMilliseconds

        private val FILENAME_PATTERN =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss", Locale.US)
        private val LOG_DATE_PATTERN =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

        // When using the 'text/plain' MIME type, the Android Storage Access Framework appends the
        // '.txt' file extension regardless of which one is used, so use '.txt'.
        private const val LOG_FILE_END = ".txt"
    }
}