package com.battlelancer.seriesguide.util

import com.google.api.client.http.HttpResponseException
import okhttp3.Response

/**
 * Throwable to track service request errors.
 */
open class RequestError(action: String, code: Int, message: String) :
    Throwable("$action: $code $message") {

    constructor(action: String, response: Response) : this(
        action,
        response.code,
        response.message
    )

    constructor(action: String, response: Response, additionalMessage: String) : this(
        action,
        response.code,
        "${response.code} $additionalMessage"
    )

    constructor(action: String, e: HttpResponseException) : this(
        action,
        e.statusCode,
        e.statusMessage
    )

}

class ClientError : RequestError {
    constructor(action: String, response: Response) : super(action, response)

    constructor(action: String, response: Response, additionalMessage: String) : super(
        action,
        response,
        additionalMessage
    )

    constructor(action: String, e: HttpResponseException) : super(action, e)
}

class ServerError : RequestError {
    constructor(action: String, response: Response) : super(action, response)

    constructor(action: String, response: Response, additionalMessage: String) : super(
        action,
        response,
        additionalMessage
    )

    constructor(action: String, e: HttpResponseException) : super(action, e)
}