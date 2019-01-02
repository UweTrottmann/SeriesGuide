package com.battlelancer.seriesguide.backend

import com.battlelancer.seriesguide.AnalyticsEvents
import com.battlelancer.seriesguide.util.RequestError

internal class HexagonRequestError : RequestError {

    constructor(action: String, code: Int, message: String) : super(
        AnalyticsEvents.HEXAGON_ERROR,
        action,
        code,
        message
    )

    constructor(action: String, cause: Throwable) : super(
        AnalyticsEvents.HEXAGON_ERROR,
        action,
        cause
    )

}