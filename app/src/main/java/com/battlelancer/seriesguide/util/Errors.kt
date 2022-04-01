package com.battlelancer.seriesguide.util

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.google.api.client.http.HttpResponseException
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import timber.log.Timber
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException


class Errors {

    companion object {

        /**
         * Returns null instead of crashing when Firebase is not configured, e.g. for vanilla debug
         * builds and CI builds.
         */
        @SuppressLint("LogNotTimber")
        fun getReporter(): FirebaseCrashlytics? {
            return try {
                FirebaseCrashlytics.getInstance()
            } catch (e: Exception) {
                // Not using Timber, would cause an endless loop.
                Log.w(
                    Errors::class.java.simpleName,
                    "FirebaseCrashlytics not available, is it configured correctly?",
                    e
                )
                return null
            }
        }

        /**
         * Logs the exception and if it should be, reports it. Adds action as key to report.
         */
        @JvmStatic
        fun logAndReportNoBend(action: String, throwable: Throwable) {
            Timber.e(throwable, action)

            if (!throwable.shouldReport()) return

            getReporter()?.setCustomKey("action", action)
            getReporter()?.recordException(throwable)
        }

        /**
         * Logs the exception and if it should be, reports it. Bends the stack trace of the
         * bottom-most exception to the call site of this method. Adds action as key to report.
         * If [throwable] is a [CancellationException] re-throws it so a coroutine
         * is cancelled properly.
         */
        @JvmStatic
        fun logAndReport(action: String, throwable: Throwable) {
            if (throwable is CancellationException) throw throwable

            Timber.e(throwable, action)

            if (!throwable.shouldReport()) return

            bendCauseStackTrace(throwable)

            getReporter()?.setCustomKey("action", action)
            getReporter()?.recordException(throwable)
        }

        /**
         * Inserts the call site stack trace element at the beginning of the bottom-most exception.
         */
        private fun bendCauseStackTrace(throwable: Throwable) {
            val synthStackTrace = Throwable().stackTrace
            val callStackIndex = indexOfFirstCallSiteElement(synthStackTrace)
            if (callStackIndex == -1) return // keep stack trace as is

            val elementToInject = synthStackTrace[callStackIndex]

            val ultimateCause = throwable.getUltimateCause()

            val stackTrace = ultimateCause.stackTrace
            val newStackTrace = arrayOfNulls<StackTraceElement>(stackTrace.size + 1)
            System.arraycopy(stackTrace, 0, newStackTrace, 1, stackTrace.size)
            newStackTrace[0] = elementToInject
            ultimateCause.stackTrace = newStackTrace
        }

        /**
         * If a HttpResponseException, maps to ClientError, ServerError or RequestError depending on
         * response code. Otherwise bends the stack trace of the bottom-most exception to the call
         * site of this method. Then logs the exception and if it should be, reports it.
         */
        @JvmStatic
        fun logAndReportHexagon(action: String, e: Throwable) {
            var statusCode: Int? = null
            val throwable = if (e is HttpResponseException) {
                statusCode  = e.statusCode
                val requestError = when {
                    e.isClientError() -> ClientError(action, e)
                    e.isServerError() -> ServerError(action, e)
                    else -> RequestError(action, e.statusCode, e.statusMessage)
                }
                removeErrorToolsFromStackTrace(requestError)
                requestError
            } else {
                bendCauseStackTrace(e)
                e
            }

            Timber.e(throwable, action)

            // Also do not report IOException: Error on service connection
            // caused by InterruptedException from GoogleAuthUtil.getToken.
            if (!throwable.shouldReport() || throwable.getUltimateCause() is InterruptedException) {
                return
            }

            getReporter()?.setCustomKey("action", action)
            statusCode?.let { getReporter()?.setCustomKey("code", it) }
            getReporter()?.recordException(throwable)
        }

        /**
         * Maps to ClientError, ServerError or RequestError depending on response code.
         * Then logs and reports error. Adds action as key to report. Appends additional message.
         */
        @JvmStatic
        fun logAndReport(action: String, response: okhttp3.Response, message: String?) {
            val throwable = when {
                response.isClientError() -> when {
                    message != null -> ClientError(action, response, message)
                    else -> ClientError(action, response)
                }
                response.isServerError() -> when {
                    message != null -> ServerError(action, response, message)
                    else -> ServerError(action, response)
                }
                else -> when {
                    message != null -> RequestError(
                        action,
                        response.code,
                        "${response.message} $message"
                    )
                    else -> RequestError(action, response.code, response.message)
                }
            }

            removeErrorToolsFromStackTrace(throwable)

            Timber.e(throwable, action)

            if (response.code == 404) return // Do not report 404 responses.

            getReporter()?.setCustomKey("action", action)
            getReporter()?.setCustomKey("code", response.code)
            getReporter()?.recordException(throwable)
        }

        /**
         * Maps to ClientError, ServerError or RequestError depending on response code.
         * Then logs and reports error. Adds action as key to report. Appends additional message.
         */
        @JvmStatic
        fun logAndReport(action: String, response: Response<*>, message: String?) {
            logAndReport(action, response.raw(), message)
        }

        /**
         * Maps to ClientError, ServerError or RequestError depending on response code.
         * Then logs and reports error. Adds action as key to report.
         */
        @JvmStatic
        fun logAndReport(action: String, response: Response<*>) {
            logAndReport(action, response.raw(), null)
        }

        @JvmStatic
        @VisibleForTesting
        fun removeErrorToolsFromStackTrace(throwable: Throwable) {
            val stackTrace = throwable.stackTrace
            val callStackIndex = indexOfFirstCallSiteElement(stackTrace)

            val newStackTrace = arrayOfNulls<StackTraceElement>(stackTrace.size - callStackIndex)
            System.arraycopy(stackTrace, callStackIndex, newStackTrace, 0, newStackTrace.size)
            throwable.stackTrace = newStackTrace
        }

        @JvmStatic
        @VisibleForTesting
        fun testCreateThrowable(): Throwable {
            return Throwable()
        }

        private fun indexOfFirstCallSiteElement(stackTrace: Array<StackTraceElement>): Int {
            return stackTrace.indexOfFirst {
                it.className != Companion::class.java.name
                        && it.className != Errors::class.java.name
                        && it.className != SgTrakt::class.java.name
            }
        }

    }

}

private fun okhttp3.Response.isClientError(): Boolean {
    return code in 400..499
}

private fun okhttp3.Response.isServerError(): Boolean {
    return code in 500..599
}

private fun HttpResponseException.isClientError(): Boolean {
    return statusCode in 400..499
}

private fun HttpResponseException.isServerError(): Boolean {
    return statusCode in 500..599
}

/**
 * Returns true if the exception is not one of the following:
 * - ConnectException - network issues (e.g. "Failed to connect to x").
 * - InterruptedIOException - network request time outs.
 * - UnknownHostException - network issues.
 */
private fun Throwable.shouldReport(): Boolean {
    return when (this) {
        is ConnectException -> false
        is InterruptedIOException -> false
        is UnknownHostException -> false
        is SSLException -> {
            message?.contains("Connection reset by peer") == false
                    && message?.contains("Software caused connection abort") == false
        }
        else -> true
    }
}

/**
 * Returns true if the throwable is a network exception that might be
 * recovered from by retrying the network request.
 */
fun Throwable.isRetryError(): Boolean {
    return when (this) {
        is ConnectException -> true
        is UnknownHostException -> true
        // Not super type InterruptedIOException as possibly not caused by network issues?
        is SocketTimeoutException -> true
        // Not SSLException as likely not temporary or a network issue.
        is InterruptedIOException -> true // Network request time outs or interrupted by system.
        else -> false
    }
}

private fun Throwable.getUltimateCause(): Throwable {
    return cause?.getUltimateCause() ?: this
}
