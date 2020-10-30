package com.battlelancer.seriesguide.backend

import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException

/**
 * Error for tracking Google Sign-In failures.
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
            return if (throwable is ApiException) {
                throwable.getStatusCodeString()
            } else {
                throwable.message ?: ""
            }
        }
    }

}

private fun ApiException.getStatusCodeString(): String {
    return GoogleSignInStatusCodes.getStatusCodeString(statusCode)
}
