package com.battlelancer.seriesguide.tmdbapi

import com.battlelancer.seriesguide.AnalyticsEvents
import com.battlelancer.seriesguide.util.RequestError

internal class TmdbRequestError : RequestError {

    constructor(action: String, code: Int, message: String) : super(
        AnalyticsEvents.TMDB_ERROR,
        action,
        code,
        message
    )

    constructor(action: String, cause: Throwable) : super(
        AnalyticsEvents.TMDB_ERROR,
        action,
        cause
    )
}