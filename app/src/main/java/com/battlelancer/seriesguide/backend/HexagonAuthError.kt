package com.battlelancer.seriesguide.backend

import com.google.android.gms.common.api.ApiException

/**
 * Error for tracking Cloud Sign-In failures.
 */
class HexagonAuthError(action: String, failure: String, cause: Throwable?) :
    Throwable("$action: $failure", cause) {

    constructor(action: String, failure: String) : this(action, failure, null)

    companion object {
        @JvmStatic
        fun build(action: String, throwable: Throwable): HexagonAuthError {
            return HexagonAuthError(action, extractStatusCodeString(throwable), throwable)
        }

        private fun extractStatusCodeString(throwable: Throwable): String {
            // Prefer ApiException message if it is the direct cause.
            val causeMessage = throwable.cause?.message
            return if (causeMessage != null && throwable.cause is ApiException) {
                causeMessage
            } else {
                throwable.message ?: ""
            }
        }
    }

}
