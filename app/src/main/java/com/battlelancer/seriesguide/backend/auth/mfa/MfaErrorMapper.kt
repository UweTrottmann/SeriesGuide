// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.firebase.ui.auth.mfa

import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import java.io.IOException

/**
 * Maps Firebase Auth exceptions to localized error messages for MFA enrollment.
 *
 * @param stringProvider Provider for localized strings
 * @return Localized error message appropriate for the exception type
 */
fun Exception.toMfaErrorMessage(stringProvider: AuthUIStringProvider): String {
    return when (this) {
        is FirebaseAuthRecentLoginRequiredException ->
            stringProvider.mfaErrorRecentLoginRequired
        is FirebaseAuthInvalidCredentialsException ->
            stringProvider.mfaErrorInvalidVerificationCode
        is IOException, is FirebaseNetworkException ->
            stringProvider.mfaErrorNetwork
        else -> stringProvider.mfaErrorGeneric
    }
}
