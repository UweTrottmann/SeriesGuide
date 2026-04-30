// SPDX-License-Identifier: Apache-2.0 AND AGPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.configuration.auth_provider

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.battlelancer.seriesguide.backend.auth.AuthException
import com.battlelancer.seriesguide.backend.auth.AuthState
import com.battlelancer.seriesguide.backend.auth.FirebaseAuthUI
import com.battlelancer.seriesguide.backend.auth.util.EmailLinkPersistenceManager
import com.battlelancer.seriesguide.backend.auth.util.SignInPreferenceManager
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber

private const val LOG_TAG = "GoogleAuthProvider"

/**
 * Creates a remembered callback for Google Sign-In that can be invoked from UI components.
 *
 * This Composable function returns a lambda that, when invoked, initiates the Google Sign-In
 * flow using [signInWithGoogle]. The callback is stable across recompositions and automatically
 * handles coroutine scoping and error state management.
 *
 * **Error Handling:**
 * - Catches all exceptions and converts them to [AuthException]
 * - Automatically updates [AuthState.Error] on failures
 *
 * @param context Android context for Credential Manager
 * @param provider Google provider configuration
 * @return A callback function that initiates Google Sign-In when invoked
 *
 * @see signInWithGoogle
 * @see AuthProvider.Google
 */
@Composable
internal fun FirebaseAuthUI.rememberGoogleSignInHandler(
    context: Context,
    provider: AuthProvider.Google,
): () -> Unit {
    val coroutineScope = rememberCoroutineScope()
    return remember(this) {
        {
            coroutineScope.launch {
                try {
                    try {
                        signInWithGoogle(context, provider)
                    } catch (e: CancellationException) {
                        throw AuthException.AuthCancelledException(
                            message = "Google sign-in coroutine interrupted",
                            cause = e
                        )
                    } catch (e: Exception) {
                        // Don't crash on any unhandled exceptions
                        throw AuthException.from(e)
                    }
                } catch (e: AuthException) {
                    updateAuthState(AuthState.Error(e))
                }
            }
        }
    }
}

/**
 * Signs in with Google using Credential Manager.
 *
 * **Flow:**
 * 1. Attempts to retrieve Google account credential using Credential Manager
 * 2. Creates Firebase credential and calls [signInWithCredential]
 *
 * **Error Handling:**
 * - [GoogleIdTokenParsingException]: if creating a GoogleIdTokenCredential in [getGoogleCredential]
 *   failed and the googleid library likely needs to be updated
 * - [NoCredentialException]: No Google accounts on device
 * - [GetCredentialException]: User cancellation, configuration errors, or no credentials
 *
 * @param context Android context for Credential Manager
 * @param provider Google provider configuration
 * @param credentialManagerProvider Provider for Credential Manager flow (for testing)
 *
 * @throws AuthException.AuthCancelledException if user cancels, no accounts found or other error
 * related to Google sign-in
 * @throws AuthException.AccountLinkingRequiredException if an account using email sign-in exists
 * after the Google credential was saved using [EmailLinkPersistenceManager].
 *
 * @see AuthProvider.Google
 * @see signInWithCredential
 */
internal suspend fun FirebaseAuthUI.signInWithGoogle(
    context: Context,
    provider: AuthProvider.Google,
    credentialManagerProvider: AuthProvider.Google.CredentialManagerProvider = AuthProvider.Google.DefaultCredentialManagerProvider(),
) {
    updateAuthState(AuthState.Loading("Signing in with Google..."))

    // Get Google account from user using credential manager
    val result =
        try {
            try {
                getGoogleCredential(
                    context = context,
                    provider = provider,
                    credentialManagerProvider = credentialManagerProvider,
                    filterByAuthorizedAccounts = provider.filterByAuthorizedAccounts
                )
            } catch (e: NoCredentialException) {
                if (provider.filterByAuthorizedAccounts) {
                    // Should try again without filter according to
                    // https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation
                    Timber.d("No authorized accounts found, trying again and showing all Google accounts")
                    getGoogleCredential(
                        context = context,
                        provider = provider,
                        credentialManagerProvider = credentialManagerProvider,
                        filterByAuthorizedAccounts = false
                    )
                } else {
                    throw e // let outer try-catch handle it
                }
            }
        } catch (e: NoCredentialException) {
            // Display error message, stay on picker screen
            throw AuthException.NoGoogleAccountAvailableException(
                "Google sign-in failed: no Google account available", e
            )
        } catch (e: GetCredentialCancellationException) {
            // Display no error message, stay on picker screen
            throw AuthException.AuthCancelledException(
                "Google sign-in cancelled by user", e
            )
        } catch (e: GetCredentialException) {
            // Display no error message, stay on picker screen
            throw AuthException.AuthCancelledException(
                "Google sign-in failed due to other", e
            )
        }

    // Sign in using the Google account info
    try {
        signInWithCredential(
            credential = result.credential,
            displayName = result.displayName,
            photoUrl = result.photoUrl,
        )
    } catch (e: AuthException.AccountLinkingRequiredException) {
        // Account collision occurred - save credential for linking after email link sign-in
        // This happens when a user tries to sign in but an email link account exists
        EmailLinkPersistenceManager.default.saveCredentialForLinking(
            context = context,
            providerType = provider.providerId,
            idToken = result.idToken,
            accessToken = null
        )
        // Re-throw to let UI handle the account linking flow
        throw e
    }

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
}

/**
 * @throws GetCredentialException
 * @see CredentialManager.getCredential
 */
private suspend fun FirebaseAuthUI.getGoogleCredential(
    context: Context,
    provider: AuthProvider.Google,
    credentialManagerProvider: AuthProvider.Google.CredentialManagerProvider,
    filterByAuthorizedAccounts: Boolean
): AuthProvider.Google.GoogleSignInResult {
    return (testCredentialManagerProvider ?: credentialManagerProvider)
        .getGoogleCredential(
            context = context,
            credentialManager = CredentialManager.create(context),
            serverClientId = provider.serverClientId,
            filterByAuthorizedAccounts = filterByAuthorizedAccounts,
            autoSelectEnabled = provider.autoSelectEnabled
        )
}

/**
 * Signs out from Google and clears credential state.
 *
 * This function clears the cached Google credentials, ensuring that the account picker
 * will be shown on the next sign-in attempt instead of automatically signing in with
 * the previously used account.
 *
 * **When to call:**
 * - After user explicitly signs out
 * - Before allowing user to select a different Google account
 * - When switching between accounts
 *
 * **Note:** This does not sign out from Firebase Auth itself. Call [FirebaseAuthUI.signOut]
 * separately if you need to sign out from Firebase.
 *
 * @param context Android context for Credential Manager
 */
internal suspend fun FirebaseAuthUI.signOutFromGoogle(
    context: Context,
    credentialManagerProvider: AuthProvider.Google.CredentialManagerProvider = AuthProvider.Google.DefaultCredentialManagerProvider(),
) {
    try {
        if (Provider.fromId(getCurrentUser()?.providerId) != Provider.GOOGLE) return
        (testCredentialManagerProvider ?: credentialManagerProvider).clearCredentialState(
            credentialManager = CredentialManager.create(context)
        )
    } catch (e: Exception) {
        Timber.e(e, "Error during Google sign out")
    }
}