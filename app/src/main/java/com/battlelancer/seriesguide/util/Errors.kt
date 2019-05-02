package com.battlelancer.seriesguide.util

import androidx.annotation.VisibleForTesting
import com.battlelancer.seriesguide.thetvdbapi.TvdbException
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.crashlytics.android.core.CrashlyticsCore
import com.google.api.client.http.HttpResponseException
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import java.io.InterruptedIOException
import java.net.UnknownHostException

class Errors {

    companion object {

        /**
         * Logs the exception and if it should be, reports it. Adds action as key to report.
         */
        @JvmStatic
        fun logAndReportNoBend(action: String, throwable: Throwable) {
            Timber.e(throwable, action)

            if (!throwable.shouldReport()) return

            CrashlyticsCore.getInstance().setString("action", action)
            CrashlyticsCore.getInstance().logException(throwable)
        }

        /**
         * Logs the exception and if it should be, reports it. Bends the stack trace of the
         * bottom-most exception to the call site of this method. Adds action as key to report.
         */
        @JvmStatic
        fun logAndReport(action: String, throwable: Throwable) {
            Timber.e(throwable, action)

            if (!throwable.shouldReport()) return

            bendCauseStackTrace(throwable)

            CrashlyticsCore.getInstance().setString("action", action)
            CrashlyticsCore.getInstance().logException(throwable)
        }

        /**
         * Inserts the call site stack trace element at the beginning of the bottom-most exception.
         */
        private fun bendCauseStackTrace(throwable: Throwable) {
            val synthStackTrace = Throwable().stackTrace
            val callStackIndex = indexOfFirstCallSiteElement(synthStackTrace)
            if (callStackIndex == -1) return // keep stack trace as is

            val elementToInject = synthStackTrace[callStackIndex]

            val ultimateCause = throwable.getUlimateCause()

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
        fun logAndReportHexagon(action: String, e: IOException) {
            val throwable = if (e is HttpResponseException) {
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

            if (!throwable.shouldReport()) return

            CrashlyticsCore.getInstance().setString("action", action)
            CrashlyticsCore.getInstance().logException(throwable)
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
                        response.code(),
                        "${response.message()} $message"
                    )
                    else -> RequestError(action, response.code(), response.message())
                }
            }

            removeErrorToolsFromStackTrace(throwable)

            Timber.e(throwable, action)

            if (response.code() == 404) return // do not send 404 to Crashlytics

            CrashlyticsCore.getInstance().setString("action", action)
            CrashlyticsCore.getInstance().logException(throwable)
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
        @Throws(TvdbException::class)
        fun throwAndReportIfNotSuccessfulTvdb(action: String, response: okhttp3.Response) {
            if (!response.isSuccessful) {
                logAndReport(action, response, null)

                if (response.code() == 404) {
                    // special case: item does not exist (any longer)
                    throw TvdbException("$action: ${response.code()} ${response.message()}", true)
                } else {
                    // other non-2xx response
                    throw TvdbException("$action: ${response.code()} ${response.message()}")
                }
            }
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
    return code() in 400..499
}

private fun okhttp3.Response.isServerError(): Boolean {
    return code() in 500..599
}

private fun HttpResponseException.isClientError(): Boolean {
    return statusCode in 400..499
}

private fun HttpResponseException.isServerError(): Boolean {
    return statusCode in 500..599
}

/**
 * Returns true if the exception is not an UnknownHostException or InterruptedIOException.
 */
private fun Throwable.shouldReport(): Boolean {
    return when (this) {
        is InterruptedIOException -> false // do not track, mostly timeouts
        is UnknownHostException -> false // do not track, mostly devices loosing connection
        else -> true
    }
}

private fun Throwable.getUlimateCause(): Throwable {
    return cause?.getUlimateCause() ?: this
}
