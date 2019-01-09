package com.battlelancer.seriesguide.backend

import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException

/**
 * Error for tracking sign-out failures.
 */
internal abstract class HexagonAuthError(val action: String, val failure: String) :
    Throwable("$action: $failure") {

    companion object {
        internal fun extractFailureMessage(throwable: Throwable): String {
            return if (throwable is ApiException) {
                GoogleSignInStatusCodes.getStatusCodeString(throwable.statusCode)
            } else {
                "${throwable.javaClass.simpleName}: ${throwable.message}"
            }
        }
    }

}
