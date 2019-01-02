package com.battlelancer.seriesguide.backend

/**
 * Error for tracking sign-in failures.
 */
internal class HexagonSignInError(action: String, failure: String) : Throwable("$action: $failure")
