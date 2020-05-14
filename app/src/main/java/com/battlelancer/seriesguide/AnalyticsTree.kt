package com.battlelancer.seriesguide

import android.database.sqlite.SQLiteException
import android.util.Log
import com.battlelancer.seriesguide.util.Errors
import com.google.gson.JsonParseException
import org.threeten.bp.format.DateTimeParseException
import timber.log.Timber.DebugTree

/**
 * A customized [timber.log.Timber.DebugTree] that logs to Crashlytics.
 * Always drops debug and verbose logs.
 */
class AnalyticsTree : DebugTree() {

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?
    ) {
        // drop debug and verbose logs
        if (priority == Log.DEBUG || priority == Log.VERBOSE) {
            return
        }

        // transform priority into string
        var level: String? = null
        when (priority) {
            Log.INFO -> level = "INFO"
            Log.WARN -> level = "WARN"
            Log.ERROR -> level = "ERROR"
        }

        // finally log to crashlytics
        Errors.getReporter()?.log("$level/$tag: $message")

        // track some non-fatal exceptions with crashlytics
        if (priority == Log.ERROR) {
            if (t is SQLiteException /* Content provider */
                || t is JsonParseException /* Retrofit */
                || t is DateTimeParseException /* TheTVDB */) {
                Errors.getReporter()?.recordException(t)
            }
        }
    }
}