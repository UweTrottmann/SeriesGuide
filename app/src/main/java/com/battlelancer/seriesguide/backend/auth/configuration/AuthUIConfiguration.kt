// SPDX-License-Identifier: Apache-2.0 AND AGPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.configuration

import android.content.Context
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.AuthProvider
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.AuthProvidersBuilder
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.Provider
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.AuthUIStringProvider
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.battlelancer.seriesguide.backend.auth.configuration.theme.AuthUIAsset
import com.battlelancer.seriesguide.backend.auth.configuration.theme.AuthUITheme
import com.google.firebase.auth.ActionCodeSettings
import java.util.Locale

fun authUIConfiguration(block: AuthUIConfigurationBuilder.() -> Unit) =
    AuthUIConfigurationBuilder().apply(block).build()

@DslMarker
annotation class AuthUIConfigurationDsl

@AuthUIConfigurationDsl
class AuthUIConfigurationBuilder {
    var context: Context? = null
    private val providers = mutableListOf<AuthProvider>()
    var theme: AuthUITheme? = null
    var locale: Locale? = null
    var stringProvider: AuthUIStringProvider? = null
    var isCredentialManagerEnabled: Boolean = true
    var isMfaEnabled: Boolean = true
    var privacyPolicyUrl: String? = null
    var logo: AuthUIAsset? = null
    var passwordResetActionCodeSettings: ActionCodeSettings? = null
    var transitions: AuthUITransitions? = null

    fun providers(block: AuthProvidersBuilder.() -> Unit) =
        providers.addAll(AuthProvidersBuilder().apply(block).build())

    internal fun build(): AuthUIConfiguration {
        val context = requireNotNull(context) {
            "Application context is required"
        }

        require(providers.isNotEmpty()) {
            "At least one provider must be configured"
        }

        // No unsupported providers (allow predefined providers and custom OIDC providers starting with "oidc.")
        val supportedProviderIds = Provider.entries.map { it.id }.toSet()
        val unknownProviders = providers.filter { provider ->
            provider.providerId !in supportedProviderIds && !provider.providerId.startsWith("oidc.")
        }
        require(unknownProviders.isEmpty()) {
            "Unknown providers: ${unknownProviders.joinToString { it.providerId }}"
        }

        // Check for duplicate providers
        val providerIds = providers.map { it.providerId }
        val duplicates = providerIds.groupingBy { it }.eachCount().filter { it.value > 1 }

        require(duplicates.isEmpty()) {
            val message = duplicates.keys.joinToString(", ")
            throw IllegalArgumentException(
                "Each provider can only be set once. Duplicates: $message"
            )
        }

        // Provider specific validations
        providers.forEach { provider ->
            when (provider) {
                is AuthProvider.Email -> provider.validate()
                is AuthProvider.Google -> provider.validate()
                is AuthProvider.GenericOAuth -> provider.validate()
            }
        }

        return AuthUIConfiguration(
            context = context,
            providers = providers.toList(),
            theme = theme,
            locale = locale,
            stringProvider = stringProvider ?: DefaultAuthUIStringProvider(context, locale),
            isCredentialManagerEnabled = isCredentialManagerEnabled,
            isMfaEnabled = isMfaEnabled,
            privacyPolicyUrl = privacyPolicyUrl,
            logo = logo,
            passwordResetActionCodeSettings = passwordResetActionCodeSettings,
            transitions = transitions
        )
    }
}

/**
 * Configuration object for the authentication flow.
 */
class AuthUIConfiguration(
    /**
     * Application context
     */
    val context: Context,

    /**
     * The list of enabled authentication providers.
     */
    val providers: List<AuthProvider> = emptyList(),

    /**
     * The theming configuration for the UI. If null, inherits from the outer AuthUITheme wrapper
     * or defaults to [AuthUITheme.Default] if no wrapper is present.
     */
    val theme: AuthUITheme? = null,

    /**
     * The locale for internationalization.
     */
    val locale: Locale? = null,

    /**
     * A custom provider for localized strings.
     */
    val stringProvider: AuthUIStringProvider = DefaultAuthUIStringProvider(context, locale),

    /**
     * Enables integration with Android's Credential Manager API. Defaults to true.
     */
    val isCredentialManagerEnabled: Boolean = true,

    /**
     * Enables Multi-Factor Authentication support. Defaults to true.
     */
    val isMfaEnabled: Boolean = true,

    /**
     * The URL for the privacy policy.
     */
    val privacyPolicyUrl: String? = null,

    /**
     * The logo to display on the authentication screens.
     */
    val logo: AuthUIAsset? = null,

    /**
     * Configuration for sending email reset link.
     */
    val passwordResetActionCodeSettings: ActionCodeSettings? = null,

    /**
     * Custom screen transition animations.
     * If null, uses default fade in/out transitions.
     */
    val transitions: AuthUITransitions? = null,
)
