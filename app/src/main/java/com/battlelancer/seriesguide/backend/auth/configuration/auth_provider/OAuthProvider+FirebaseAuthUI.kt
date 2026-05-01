// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.configuration.auth_provider

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.battlelancer.seriesguide.backend.auth.AuthException
import com.battlelancer.seriesguide.backend.auth.AuthState
import com.battlelancer.seriesguide.backend.auth.FirebaseAuthUI
import com.battlelancer.seriesguide.backend.auth.configuration.AuthUIConfiguration
import com.battlelancer.seriesguide.backend.auth.util.SignInPreferenceManager
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val LOG_TAG = "OAuthProvider"

/**
 * Creates a Composable handler for OAuth provider sign-in.
 *
 * This function creates a remember-scoped sign-in handler that can be invoked
 * from button clicks or other UI events. It automatically handles:
 * - Activity retrieval from LocalActivity
 * - Coroutine scope management
 * - Error handling and state updates
 *
 * **Usage:**
 * ```kotlin
 * val onSignInWithGitHub = authUI.rememberOAuthSignInHandler(
 *     config = configuration,
 *     provider = githubProvider
 * )
 *
 * Button(onClick = onSignInWithGitHub) {
 *     Text("Sign in with GitHub")
 * }
 * ```
 *
 * @param config Authentication UI configuration
 * @param provider OAuth provider configuration
 *
 * @return Lambda that triggers OAuth sign-in when invoked
 *
 * @throws IllegalStateException if LocalActivity.current is null
 *
 * @see signInWithProvider
 */
@Composable
internal fun FirebaseAuthUI.rememberOAuthSignInHandler(
    context: Context,
    activity: Activity?,
    config: AuthUIConfiguration,
    provider: AuthProvider.OAuth,
): () -> Unit {
    val coroutineScope = rememberCoroutineScope()
    activity ?: throw IllegalStateException(
        "OAuth sign-in requires an Activity. " +
                "Ensure FirebaseAuthScreen is used within an Activity."
    )

    return remember(this, provider.providerId) {
        {
            coroutineScope.launch {
                try {
                    signInWithProvider(
                        context = context,
                        activity = activity,
                        provider = provider
                    )
                } catch (e: AuthException) {
                    updateAuthState(AuthState.Error(e))
                } catch (e: Exception) {
                    val authException = AuthException.from(e)
                    updateAuthState(AuthState.Error(authException))
                }
            }
        }
    }
}

/**
 * Signs in with an OAuth provider (GitHub, Microsoft, Yahoo, Apple, Twitter).
 *
 * This function implements OAuth provider authentication using Firebase's native OAuthProvider.
 *
 * **Supported Providers:**
 * - GitHub (github.com)
 * - Microsoft (microsoft.com)
 * - Yahoo (yahoo.com)
 * - Apple (apple.com)
 * - Twitter (twitter.com)
 *
 * **Flow:**
 * 1. Checks for pending auth results (e.g., from app restart during OAuth flow)
 * 2. Performs normal sign-in
 * 3. Updates auth state to Idle on success
 *
 * **Error Handling:**
 * - [AuthException.AuthCancelledException]: User cancelled OAuth flow
 * - [AuthException.AccountLinkingRequiredException]: Account collision (email already exists)
 * - [AuthException]: Other authentication errors
 *
 * @param activity Activity for OAuth flow
 * @param provider OAuth provider configuration with scopes and custom parameters
 *
 * @throws AuthException.AuthCancelledException if user cancels
 * @throws AuthException.AccountLinkingRequiredException if account collision occurs
 * @throws AuthException if OAuth flow or sign-in fails
 *
 * @see AuthProvider.OAuth
 * @see signInWithCredential
 */
internal suspend fun FirebaseAuthUI.signInWithProvider(
    context: Context,
    activity: Activity,
    provider: AuthProvider.OAuth,
) {
    try {
        updateAuthState(AuthState.Loading("Signing in with ${provider.providerName}..."))

        // Build OAuth provider with scopes and custom parameters
        val oauthProvider = OAuthProvider
            .newBuilder(provider.providerId)
            .apply {
                // Add scopes if provided
                if (provider.scopes.isNotEmpty()) {
                    scopes = provider.scopes
                }
                // Add custom parameters if provided
                provider.customParameters.forEach { (key, value) ->
                    addCustomParameter(key, value)
                }
            }
            .build()

        // Check for pending auth result (e.g., app was killed during OAuth flow)
        val pendingResult = auth.pendingAuthResult
        if (pendingResult != null) {
            val authResult = pendingResult.await()
            val credential = authResult.credential as? OAuthCredential

            if (credential != null) {
                // Complete the pending sign-in/link flow
                signInWithCredential(
                    credential = credential,
                    displayName = authResult.user?.displayName,
                    photoUrl = authResult.user?.photoUrl,
                )
            }
            updateAuthState(AuthState.Idle)
            return
        }

        val authResult = auth.startActivityForSignInWithProvider(activity, oauthProvider)
            .await()

        // Extract OAuth credential and complete sign-in
        val credential = authResult?.credential as? OAuthCredential
        if (credential != null) {
            // The user is already signed in via startActivityForSignInWithProvider/startActivityForLinkWithProvider

            // Save sign-in preference for "Continue as..." feature
            val user = auth.currentUser
            val identifier = user?.email
            if (identifier != null) {
                SignInPreferenceManager.tryToSaveLastSignIn(
                    context = context,
                    providerId = provider.providerId,
                    identifier = identifier,
                    LOG_TAG
                )
            }

            // Just update state to Idle
            updateAuthState(AuthState.Idle)
        } else {
            throw AuthException.UnknownException(
                message = "OAuth sign-in did not return a valid credential"
            )
        }

    } catch (e: FirebaseAuthUserCollisionException) {
        // Account collision: account already exists with different sign-in method
        val credential = e.updatedCredential
        val accountLinkingException = AuthException.AccountLinkingRequiredException(
            collisionException = e,
            credential = credential
        )
        updateAuthState(AuthState.Error(accountLinkingException))
        throw accountLinkingException
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Signing in with ${provider.providerName} was cancelled",
            cause = e
        )
        updateAuthState(AuthState.Error(cancelledException))
        throw cancelledException

    } catch (e: AuthException) {
        updateAuthState(AuthState.Error(e))
        throw e

    } catch (e: Exception) {
        val authException = AuthException.from(e)
        updateAuthState(AuthState.Error(authException))
        throw authException
    }
}
