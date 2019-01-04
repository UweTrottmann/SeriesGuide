package com.battlelancer.seriesguide.util

/**
 * Throwable to track service request errors.
 */
internal abstract class RequestError : Throwable {
    var event: String
    var action: String
    var code: Int? = null
    var failureMessage: String? = null

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