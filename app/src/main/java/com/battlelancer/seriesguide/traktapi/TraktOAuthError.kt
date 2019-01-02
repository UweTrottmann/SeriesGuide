package com.battlelancer.seriesguide.traktapi

/**
 * Error for tracking OAuth failures.
 */
internal class TraktOAuthError(action: String, failure: String) : Throwable("$action: $failure")