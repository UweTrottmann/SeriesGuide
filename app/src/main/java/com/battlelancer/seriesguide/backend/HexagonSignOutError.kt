package com.battlelancer.seriesguide.backend

/**
 * Error for tracking sign-out failures.
 */
internal class HexagonSignOutError(action: String, failure: String) :
    HexagonAuthError(action, failure) {

    companion object {
        @JvmStatic
        fun build(action: String, throwable: Throwable): HexagonSignOutError {
            return HexagonSignOutError(action, extractFailureMessage(throwable))
        }
    }
}
