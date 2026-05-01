// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.firebase.ui.auth.util

import androidx.annotation.RestrictTo

/**
 * Utility for checking the availability of authentication providers at runtime.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ProviderAvailability {

    /**
     * Checks if Facebook authentication is available.
     * Returns true if the Facebook SDK is present in the classpath.
     */
    val IS_FACEBOOK_AVAILABLE: Boolean = classExists("com.facebook.login.LoginManager")

    private fun classExists(className: String): Boolean {
        return try {
            Class.forName(className)
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}