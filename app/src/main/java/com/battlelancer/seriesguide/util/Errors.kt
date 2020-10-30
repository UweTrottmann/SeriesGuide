package com.battlelancer.seriesguide.util

import androidx.annotation.VisibleForTesting
import com.crashlytics.android.core.CrashlyticsCore
import retrofit2.Response
import timber.log.Timber
import java.io.InterruptedIOException
import java.net.UnknownHostException

class Errors {

    companion object {

        private const val CALL_STACK_INDEX = 2

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
        @JvmStatic
        @VisibleForTesting
        fun bendCauseStackTrace(throwable: Throwable) {
            val synthStackTrace = Throwable().stackTrace
            if (synthStackTrace.size <= CALL_STACK_INDEX) {
                throw IllegalStateException("Synthetic stacktrace didn't have enough elements")
            }
            val elementToInject = synthStackTrace[CALL_STACK_INDEX]

            val ultimateCause = throwable.getUlimateCause()

            val stackTrace = ultimateCause.stackTrace
            val newStackTrace = arrayOfNulls<StackTraceElement>(stackTrace.size + 1)
            System.arraycopy(stackTrace, 0, newStackTrace, 1, stackTrace.size)
            newStackTrace[0] = elementToInject
            ultimateCause.stackTrace = newStackTrace
        }

        /**
         * Maps to ClientError, ServerError or RequestError depending on response code.
         * Then logs and reports error. Adds action as key to report. Appends additional message.
         */
        @JvmStatic
        fun logAndReport(action: String, response: Response<*>, message: String?) {
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

            CrashlyticsCore.getInstance().setString("action", action)
            CrashlyticsCore.getInstance().logException(throwable)
        }

        /**
         * Maps to ClientError, ServerError or RequestError depending on response code.
         * Then logs and reports error. Adds action as key to report.
         */
        @JvmStatic
        fun logAndReport(action: String, response: Response<*>) {
            logAndReport(action, response, null)
        }

        @JvmStatic
        @VisibleForTesting
        fun removeErrorToolsFromStackTrace(throwable: Throwable) {
            val stackTrace = throwable.stackTrace
            val callStackIndex = stackTrace.indexOfFirst {
                it.className != Companion::class.java.name
                        && it.className != Errors::class.java.name
            }
            val newStackTrace = arrayOfNulls<StackTraceElement>(stackTrace.size - callStackIndex)
            System.arraycopy(stackTrace, callStackIndex, newStackTrace, 0, newStackTrace.size)
            throwable.stackTrace = newStackTrace
        }

        @JvmStatic
        @VisibleForTesting
        fun testCreateThrowable(): Throwable {
            return Throwable()
        }

    }

}

private fun Response<*>.isClientError(): Boolean {
    return code() in 400..499
}

private fun Response<*>.isServerError(): Boolean {
    return code() in 500..599
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
