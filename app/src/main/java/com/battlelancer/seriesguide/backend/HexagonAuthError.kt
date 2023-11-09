// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.backend

import com.firebase.ui.auth.FirebaseUiException
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes

/**
 * Error for tracking Cloud Sign-In failures.
 */
class HexagonAuthError(
    val action: String,
    failure: String,
    cause: Throwable?,
    val statusCode: Int?
) : Throwable("$action: $failure", cause) {

    constructor(action: String, failure: String) : this(action, failure, null, null)

    fun isSignInRequiredError(): Boolean {
        return cause is ApiException && statusCode == CommonStatusCodes.SIGN_IN_REQUIRED
    }

    companion object {

        /**
         * Extracts status code from ApiException, FirebaseUiException.
         */
        @JvmStatic
        fun build(action: String, throwable: Throwable): HexagonAuthError {
            val message = throwable.message ?: ""
            val statusCode = when (throwable) {
                is FirebaseUiException -> throwable.errorCode
                is ApiException -> throwable.statusCode
                else -> null
            }
            return HexagonAuthError(action, message, throwable, statusCode)
        }
    }

}
