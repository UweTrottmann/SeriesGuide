package com.battlelancer.seriesguide.backend

/**
 * Error for tracking sign-in failures.
 */
internal class HexagonSignInError(action: String, failure: String) :
    HexagonAuthError(action, failure) {

    companion object {
        @JvmStatic
        fun build(action: String, throwable: Throwable): HexagonSignInError {
            return HexagonSignInError(action, extractFailureMessage(throwable))
        }
    }

}
