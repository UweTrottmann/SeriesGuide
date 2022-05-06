package com.battlelancer.seriesguide.backend

import com.firebase.ui.auth.FirebaseUiException
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes

/**
 * Error for tracking Cloud Sign-In failures.
 */
class HexagonAuthError(
    val action: String,
    failure: String,
    cause: Throwable?,
    val statusCode: Int?
) : Throwable("$action: $failure", cause) {

    constructor(action: String, failure: String) : this(action, failure, null, null)

    fun isSignInRequiredError(): Boolean {
        return cause is ApiException && statusCode == CommonStatusCodes.SIGN_IN_REQUIRED
    }

    companion object {

        /**
         * Extracts info from Tasks API exceptions with ApiException cause, FirebaseUiException.
         */
        @JvmStatic
        fun build(action: String, throwable: Throwable): HexagonAuthError {
            // Prefer ApiException message.
            // Note: when using the Tasks API, ApiException is wrapped in an ExecutionException.
            val cause = throwable.cause
            val causeMessage = cause?.message
            val message = if (causeMessage != null && cause is ApiException) {
                causeMessage
            } else {
                throwable.message ?: ""
            }
            val statusCode = when {
                throwable is FirebaseUiException -> throwable.errorCode
                cause is ApiException -> cause.statusCode
                else -> null
            }
            return HexagonAuthError(action, message, throwable, statusCode)
        }
    }

}
