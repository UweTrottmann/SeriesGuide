package com.battlelancer.seriesguide.backend

/**
 * Error for tracking authorization revoke failures.
 */
internal class HexagonRevokeError(action: String, failure: String) :
    HexagonAuthError(action, failure) {

    companion object {
        @JvmStatic
        fun build(action: String, throwable: Throwable): HexagonRevokeError {
            return HexagonRevokeError(action, extractFailureMessage(throwable))
        }
    }
}
