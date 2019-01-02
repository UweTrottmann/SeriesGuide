package com.battlelancer.seriesguide.traktapi

import com.battlelancer.seriesguide.AnalyticsEvents
import com.battlelancer.seriesguide.util.RequestError

internal class TraktRequestError : RequestError {
    constructor(action: String, code: Int, message: String) : super(
        AnalyticsEvents.TRAKT_ERROR,
        action,
        code,
        message
    )

    constructor(action: String, cause: Throwable) : super(
        AnalyticsEvents.TRAKT_ERROR,
        action,
        cause
    )
}