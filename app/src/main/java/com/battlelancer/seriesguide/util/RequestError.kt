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

    constructor(
        action: String,
        code: Int,
        message: String
    ) : super("$action: $code $message") {
        this.event = ""
        this.action = action
        this.code = code
        this.failureMessage = message
    }

    constructor(event: String, action: String, cause: Throwable) : super(action, cause) {
        this.event = event
        this.action = action
    }
}

class ClientError : RequestError {
    constructor(action: String, response: Response<*>) : super(
        action,
        response.code(),
        response.message()
    )

    constructor(action: String, response: Response<*>, additionalMessage: String) : super(
        action,
        response.code(),
        "${response.code()} $additionalMessage"
    )
}

class ServerError : RequestError {
    constructor(action: String, response: Response<*>) : super(
        action,
        response.code(),
        response.message()
    )

    constructor(action: String, response: Response<*>, additionalMessage: String) : super(
        action,
        response.code(),
        "${response.code()} $additionalMessage"
    )
}