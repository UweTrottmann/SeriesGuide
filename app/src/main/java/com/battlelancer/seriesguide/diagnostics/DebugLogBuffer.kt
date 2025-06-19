// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide.diagnostics

import android.annotation.SuppressLint
import android.content.Context
import com.battlelancer.seriesguide.diagnostics.DebugLogBuffer.Companion.BUFFER_SIZE
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Calendar
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

    private var onLogListener: OnLogListener? = null

    fun timberTree(): Timber.Tree {
        return object : DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                addEntry(
                    DebugLogEntry(
                        priority,
                        tag,
                        message,
                        LOG_DATE_PATTERN.format(Calendar.getInstance().time)
                    )
                )
            }
        }
    }

    fun setOnLogListener(onLogListener: OnLogListener?) {
        this.onLogListener = onLogListener
    }

    @Synchronized
    private fun addEntry(entry: DebugLogEntry) {
        entries.addLast(entry)

        if (entries.size > BUFFER_SIZE) {
            entries.removeFirst()
        }

        onLog(entry)
    }

    fun bufferedLogs(): List<DebugLogEntry> {
        return ArrayList(entries)
    }

    /**
     * Save the current logs to disk.
     */
    fun save(listener: OnSaveLogListener) {
        val dir = logDir

        if (dir == null) {
            listener.onError(
                "Can't save logs. External storage is not mounted. " +
                        "Check android.permission.WRITE_EXTERNAL_STORAGE permission"
            )
            return
        }

        var fileWriter: FileWriter? = null

        try {
            val output = File(dir, logFileName)
            fileWriter = FileWriter(output, true)

            val entries = bufferedLogs()
            for (entry in entries) {
                fileWriter.write(entry.prettyPrint() + "\n")
            }

            listener.onSave(output)
        } catch (e: IOException) {
            listener.onError(e.message)
            e.printStackTrace()
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close()
                } catch (e: IOException) {
                    listener.onError(e.message)
                    e.printStackTrace()
                }
            }
        }
    }

    // TODO
    fun cleanUp() {
        val dir = logDir
        if (dir != null) {
            val files = dir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.name.endsWith(LOG_FILE_END)) {
                        file.delete()
                    }
                }
            }
        }
    }

    private val logDir: File?
        get() = context.getExternalFilesDir(null)

    private fun onLog(entry: DebugLogEntry) {
        if (onLogListener != null) {
            onLogListener!!.onLog(entry)
        }
    }

    private val logFileName: String
        get() {
            val pattern = "%s%s"
            val currentDate = FILENAME_DATE.format(Calendar.getInstance().time)

            return String.format(pattern, currentDate, LOG_FILE_END)
        }

    interface OnSaveLogListener {
        fun onSave(file: File?)

        fun onError(message: String?)
    }

    interface OnLogListener {
        fun onLog(logEntry: DebugLogEntry?)
    }

    companion object {

        private const val BUFFER_SIZE = 200

        private val FILENAME_DATE: DateFormat = SimpleDateFormat("yyyy-MM-dd HHmm a", Locale.US)
        private val LOG_DATE_PATTERN: DateFormat = SimpleDateFormat("MM-dd HH:mm:ss.S", Locale.US)

        private const val LOG_FILE_END = ".log"

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