// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.configuration.string_provider

import android.content.Context
import com.battlelancer.seriesguide.backend.auth.configuration.AuthUIConfiguration
import com.battlelancer.seriesguide.backend.auth.configuration.authUIConfiguration
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.AuthProvider

class AuthUIStringProviderSample {
    /**
     * Override specific strings while delegating others to default provider
     */
    class CustomAuthUIStringProvider(
        private val defaultProvider: AuthUIStringProvider
    ) : AuthUIStringProvider by defaultProvider {

        // Override only the strings you want to customize
        override val signInWithGoogle: String = "Continue with Google • MyApp"
        override val signInWithFacebook: String = "Continue with Facebook • MyApp"

        // Add custom branding to common actions
        override val continueText: String = "Continue to MyApp"
        override val signInDefault: String = "Sign in to MyApp"

        // Custom MFA messaging
        override val enterTOTPCode: String =
            "Enter the 6-digit code from your authenticator app to secure your MyApp account"
    }

    fun createCustomConfiguration(applicationContext: Context): AuthUIConfiguration {
        val customStringProvider =
            CustomAuthUIStringProvider(DefaultAuthUIStringProvider(applicationContext))
        return authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Google(
                        scopes = listOf(),
                        serverClientId = ""
                    )
                )
            }
            stringProvider = customStringProvider
        }
    }
}