package com.battlelancer.seriesguide.util

import retrofit2.Response

/**
 * Throwable to track service request errors.
 */
open class RequestError : Throwable {
    var event: String
    var action: String
    var code: Int? = null
    var failureMessage: String? = null

    // message like "action: 404 not found"
    constructor(
        event: String,
        action: String,
        code: Int,
        message: String
    ) : super("$action: $code $message") {
        this.event = event
        this.action = action
        this.code = code
        this.failureMessage = message
    }

    constructor(event: String, action: String, cause: Throwable) : super(action, cause) {
        this.event = event
        this.action = action
    }
}

class ClientError(action: String, response: Response<*>) :
    RequestError("", action, response.code(), response.message())
class ServerError(action: String, response: Response<*>) :
    RequestError("", action, response.code(), response.message())