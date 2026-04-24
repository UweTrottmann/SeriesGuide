// SPDX-License-Identifier: Apache-2.0 AND AGPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.configuration.auth_provider

import android.content.Context
import android.net.Uri
import com.battlelancer.seriesguide.backend.auth.AuthException
import com.battlelancer.seriesguide.backend.auth.AuthException.EmailAlreadyInUseException
import com.battlelancer.seriesguide.backend.auth.AuthState
import com.battlelancer.seriesguide.backend.auth.FirebaseAuthUI
import com.battlelancer.seriesguide.backend.auth.configuration.AuthUIConfiguration
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.AuthProvider.Companion.mergeProfile
import com.battlelancer.seriesguide.backend.auth.credentialmanager.PasswordCredentialCancelledException
import com.battlelancer.seriesguide.backend.auth.credentialmanager.PasswordCredentialException
import com.battlelancer.seriesguide.backend.auth.credentialmanager.PasswordCredentialHandler
import com.battlelancer.seriesguide.backend.auth.util.EmailLinkParser
import com.battlelancer.seriesguide.backend.auth.util.EmailLinkPersistenceManager
import com.battlelancer.seriesguide.backend.auth.util.PersistenceManager
import com.battlelancer.seriesguide.backend.auth.util.SessionUtils
import com.battlelancer.seriesguide.backend.auth.util.SignInPreferenceManager
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import timber.log.Timber

private const val LOG_TAG = "EmailAuthProvider"

/**
 * Creates an email/password account.
 *
 * **Flow:**
 * 1. Check if new accounts are allowed
 * 2. Validate password against [AuthProvider.Email.passwordValidationRules]
 * 3. Create new account with `createUserWithEmailAndPassword`
 * 4. Merge display name into user profile
 *
 * **Example: Normal sign-up**
 * ```kotlin
 * try {
 *     val result = firebaseAuthUI.createUserWithEmailAndPassword(
 *         context = context,
 *         config = authUIConfig,
 *         provider = emailProvider,
 *         name = "John Doe",
 *         email = "john@example.com",
 *         password = "SecurePass123!"
 *     )
 *     // User account created successfully
 * } catch (e: AuthException.WeakPasswordException) {
 *     // Password doesn't meet validation rules
 * } catch (e: AuthException.EmailAlreadyInUseException) {
 *     // Email already exists - redirect to sign-in
 * }
 * ```
 *
 * @param context Android [Context] for localized strings
 * @param config Auth UI configuration describing provider settings
 * @param provider Email provider configuration
 * @param name Optional display name collected during sign-up
 * @param email Email address for the new account
 * @param password Password for the new account
 *
 * @return [AuthResult] containing the newly created or linked user, or null if failed
 *
 * @throws AuthException.AdminRestrictedException if new accounts are not allowed
 * @throws AuthException.WeakPasswordException if the password fails validation rules
 * @throws AuthException.InvalidCredentialsException if the email or password is invalid
 * @throws AuthException.EmailAlreadyInUseException if the email already exists
 * @throws AuthException.AuthCancelledException if the coroutine is cancelled
 * @throws AuthException.NetworkException for network-related failures
 *
 */
internal suspend fun FirebaseAuthUI.createUserWithEmailAndPassword(
    context: Context,
    config: AuthUIConfiguration,
    provider: AuthProvider.Email,
    name: String?,
    email: String,
    password: String,
): AuthResult? {
    try {
        // This should never get called if isNewAccountsAllowed is false.
        // Note: this only checks the configuration in this app. Sign-ups can also be turned off
        // server side in which case the API call below should throw FirebaseAuthException with
        // ERROR_ADMIN_RESTRICTED_OPERATION which is then wrapped into a AdminRestrictedException.
        if (!provider.isNewAccountsAllowed) {
            throw AuthException.AdminRestrictedException("Called despite provider.isNewAccountsAllowed = false")
        }

        // Validate password against rules
        for (rule in provider.passwordValidationRules) {
            if (!rule.isValid(password)) {
                val reason = rule.getErrorMessage(config.stringProvider)
                throw AuthException.WeakPasswordException(
                    message = "Password does not meet rules: $reason",
                    reason = reason
                )
            }
        }

        updateAuthState(AuthState.Loading("Creating user..."))
        val result = auth.createUserWithEmailAndPassword(email, password)
            .await()
            .also { authResult ->
                authResult?.user?.let {
                    // Merge display name into profile (photoUri is always null for email/password)
                    mergeProfile(auth, name, null)
                }
            }

        result?.let {
            saveCredentialAndSignInPreference(
                context, config, provider.providerId, email, password
            )
        }

        updateAuthState(AuthState.Idle)
        return result
    } catch (e: FirebaseAuthUserCollisionException) {
        // When trying to create an account, failed because an account using this email address
        // already exists.
        val authException = EmailAlreadyInUseException(
            message = e.message ?: "Email address is already in use",
            cause = e
        )
        updateAuthState(AuthState.Error(authException))
        throw authException
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Create or link user with email and password was cancelled",
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

/**
 * Signs in a user with email and password, optionally linking a social credential.
 *
 * **Flow:**
 * - Sign in with email/password
 * - If credential provided: link it and merge profile
 *
 * **Example: Normal sign-in**
 * ```kotlin
 * try {
 *     val result = firebaseAuthUI.signInWithEmailAndPassword(
 *         context = context,
 *         config = authUIConfig,
 *         provider = emailProvider,
 *         email = "user@example.com",
 *         password = "password123"
 *     )
 *     // User signed in successfully
 * } catch (e: AuthException.InvalidCredentialsException) {
 *     // Wrong password
 * }
 * ```
 *
 * **Example: Sign-in with social credential linking**
 * ```kotlin
 * // User tried to sign in with Google, but account exists with email/password
 * // Prompt for password, then link Google credential
 * val googleCredential = GoogleAuthProvider.getCredential(idToken, null)
 *
 * val result = firebaseAuthUI.signInWithEmailAndPassword(
 *     context = context,
 *     config = authUIConfig,
 *     provider = emailProvider,
 *     email = "user@example.com",
 *     password = "password123",
 *     credentialForLinking = googleCredential
 * )
 * // User signed in with email/password AND Google is now linked
 * // Profile updated with Google display name and photo
 * ```
 *
 * @param context Android [Context]
 * @param config Auth UI configuration describing provider settings
 * @param email Email address for sign-in
 * @param password Password for sign-in
 * @param credentialForLinking Optional social provider credential to link after sign-in
 *
 * @return [AuthResult] containing the signed-in user, or null if multi-factor auth is required
 *
 * @throws AuthException.InvalidCredentialsException if email or password is incorrect or the user
 * is not found or disabled
 * @throws AuthException.AuthCancelledException if the operation is cancelled
 * @throws AuthException.NetworkException for network-related failures
 */
internal suspend fun FirebaseAuthUI.signInWithEmailAndPassword(
    context: Context,
    config: AuthUIConfiguration,
    provider: AuthProvider,
    email: String,
    password: String,
    credentialForLinking: AuthCredential? = null,
    skipCredentialSave: Boolean = false,
): AuthResult? {
    try {
        updateAuthState(AuthState.Loading("Signing in..."))
        return auth.signInWithEmailAndPassword(email, password)
            .await()
            .let { result ->
                // If there's a credential to link, link it after sign-in
                if (credentialForLinking != null) {
                    val linkResult = result.user
                        ?.linkWithCredential(credentialForLinking)
                        ?.await()

                    // Merge profile from social provider
                    linkResult?.user?.let { user ->
                        mergeProfile(
                            auth,
                            user.displayName,
                            user.photoUrl
                        )
                    }

                    linkResult ?: result
                } else {
                    result
                }
            }
            .also { result ->
                result?.let {
                    saveCredentialAndSignInPreference(
                        context, config, provider.providerId, email, password, skipCredentialSave
                    )
                }

                updateAuthState(AuthState.Idle)
            }
    } catch (e: FirebaseAuthMultiFactorException) {
        // MFA required - extract resolver and update state
        val resolver = e.resolver
        val hint = resolver.hints.firstOrNull()?.displayName
        updateAuthState(AuthState.RequiresMfa(resolver, hint))
        return null
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Sign in with email and password was cancelled",
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

/**
 * Saves the password credential to Credential Manager and records the sign-in preference.
 * As this isn't necessary to complete sign-in, failures are only logged.
 */
private suspend fun saveCredentialAndSignInPreference(
    context: Context,
    config: AuthUIConfiguration,
    providerId: String,
    email: String,
    password: String,
    skipCredentialSave: Boolean = false,
) {
    // Save credentials to Credential Manager if enabled
    // Skip if user signed in with a retrieved credential (already saved)
    if (config.isCredentialManagerEnabled && !skipCredentialSave) {
        try {
            val credentialHandler = PasswordCredentialHandler(context)
            credentialHandler.savePassword(email, password)
            Timber.d("Password credential saved successfully for: %s", email)
        } catch (_: PasswordCredentialCancelledException) {
            // User cancelled - this is fine, don't break the auth flow
            Timber.d("User cancelled credential save for: %s", email)
        } catch (e: PasswordCredentialException) {
            // Failed to save - log but don't break the auth flow
            Timber.w(e, "Failed to save password credential for: %s", email)
        }
    }

    // Save sign-in preference for "Continue as..." feature
    SignInPreferenceManager.tryToSaveLastSignIn(
        context = context,
        providerId = providerId,
        identifier = email,
        LOG_TAG
    )
}

/**
 * Signs in with a [credential].
 *
 * **Flow:**
 * - Sign in with credential
 * - Merge profile information ([displayName], [photoUrl]) into Firebase user
 *
 * @return [AuthResult] containing the signed-in user, or null if multi-factor auth is required
 *
 * @throws AuthException.InvalidCredentialsException if credential is invalid or expired
 * @throws AuthException.AccountLinkingRequiredException if account was created with a different
 * provider, but not if provider is considered trusted by Firebase. See the exception for details.
 * @throws AuthException.EmailAlreadyInUseException if linking and email is already in use
 * @throws AuthException.AuthCancelledException if the operation is cancelled
 * @throws AuthException.NetworkException if a network error occurs
 */
internal suspend fun FirebaseAuthUI.signInWithCredential(
    credential: AuthCredential,
    displayName: String? = null,
    photoUrl: Uri? = null,
): AuthResult? {
    try {
        updateAuthState(AuthState.Loading("Signing in user..."))
        return auth.signInWithCredential(credential)
            .await()
            .also { result ->
                // Merge profile information from the provider
                result?.user?.let {
                    mergeProfile(auth, displayName, photoUrl)
                }
                updateAuthState(AuthState.Idle)
            }
    } catch (e: FirebaseAuthMultiFactorException) {
        // MFA required - extract resolver and update state
        val resolver = e.resolver
        val hint = resolver.hints.firstOrNull()?.displayName
        updateAuthState(AuthState.RequiresMfa(resolver, hint))
        return null
    } catch (e: FirebaseAuthUserCollisionException) {
        // Account collision: account already exists with different sign-in method
        // Create AccountLinkingRequiredException with credential for linking
        // Note: this is *not* thrown if the new provider is trusted, see
        // AccountLinkingRequiredException for details.
        val accountLinkingException = AuthException.AccountLinkingRequiredException(
            collisionException = e,
            credential = credential
        )
        updateAuthState(AuthState.Error(accountLinkingException))
        throw accountLinkingException
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Sign in and link with credential was cancelled",
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

/**
 * Sends a passwordless sign-in link to the specified email address.
 *
 * This method initiates the email-link (passwordless) authentication flow by sending
 * an email containing a magic link. The link includes session information for validation
 * and security.
 *
 * **How it works:**
 * 1. Generates a unique session ID for same-device validation
 * 2. Creates [ActionCodeSettings] with session data based on the [AuthProvider.Email.emailLinkActionCodeSettings] of the given [provider]
 * 3. Sends the email via [com.google.firebase.auth.FirebaseAuth.sendSignInLinkToEmail] using the action code settings
 * 4. Saves session data to DataStore for validation when the user clicks the link later
 * 5. User receives email with a magic link containing the session information
 * 6. When user clicks link, app opens via deep link and calls [signInWithEmailLink] to complete authentication
 *
 * **Account Linking Support:**
 * If a user tries to sign in with a social provider (Google, Facebook) but an email link
 * account already exists with that email, the social provider implementation should:
 * 1. Catch the [FirebaseAuthUserCollisionException] from the sign-in attempt
 * 2. Call [EmailLinkPersistenceManager.DefaultPersistenceManager.saveCredentialForLinking] with the provider tokens
 * 3. Call this method to send the email link
 * 4. When [signInWithEmailLink] completes, it automatically retrieves and links the saved credential
 *
 * **Session Security:**
 * - **Session ID**: Random 10-character string for same-device validation
 * - **Force Same Device**: Can be configured via [AuthProvider.Email.isEmailLinkForceSameDeviceEnabled]
 * - All session data is validated in [signInWithEmailLink] before completing authentication
 *
 * @param context Android [Context] for DataStore access
 * @param provider The [AuthProvider.Email] configuration with [ActionCodeSettings]
 * @param email The email address to send the sign-in link to
 * @param credentialForLinking Optional [AuthCredential] from a social provider to link after email sign-in.
 *                             If provided, the credential is saved to DataStore and automatically linked
 *                             when [signInWithEmailLink] completes. Used for account linking flows.
 *
 * @throws AuthException.InvalidCredentialsException if email is invalid
 * @throws AuthException.AuthCancelledException if the operation is cancelled
 * @throws AuthException.NetworkException if a network error occurs
 * @throws IllegalStateException if ActionCodeSettings is not configured
 *
 * @see signInWithEmailLink
 * @see EmailLinkPersistenceManager
 * @see com.google.firebase.auth.FirebaseAuth.sendSignInLinkToEmail
 */
internal suspend fun FirebaseAuthUI.sendSignInLinkToEmail(
    context: Context,
    provider: AuthProvider.Email,
    email: String,
    credentialForLinking: AuthCredential?,
    persistenceManager: PersistenceManager = EmailLinkPersistenceManager.default,
) {
    try {
        updateAuthState(AuthState.Loading("Sending sign in email link..."))

        val sessionId =
            SessionUtils.generateRandomAlphaNumericString(AuthProvider.Email.SESSION_ID_LENGTH)

        val actionCodeSettings =
            provider.buildActionCodeSettings(
                sessionId = sessionId,
                credentialForLinking = credentialForLinking
            )

        auth.sendSignInLinkToEmail(email, actionCodeSettings).await()

        // Save Email to dataStore for use in signInWithEmailLink
        persistenceManager.saveEmail(context, email, sessionId)

        updateAuthState(AuthState.EmailSignInLinkSent())
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Send sign in link to email was cancelled",
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

/**
 * Signs in a user using an email link (passwordless authentication).
 *
 * This method completes the email link sign-in flow after the user clicks the magic link
 * sent to their email. It validates the link, extracts session information, and signs the user in.
 *
 * **Flow:**
 * - User receives email with magic link
 * - User clicks link, app opens via deep link
 * - Activity extracts emailLink from Intent.data
 * - This method validates and completes sign-in
 *
 * **Same-Device Flow:**
 * - Email is retrieved from DataStore automatically
 * - Session ID from link matches stored session ID
 * - User is signed in immediately without additional input
 *
 * **Cross-Device Flow:**
 * - Session ID from link doesn't match (or no local session exists)
 * - If [email] is empty: throws [AuthException.EmailLinkPromptForEmailException]
 * - User must provide their email address
 * - Call this method again with user-provided email to complete sign-in
 *
 * @param context Android [Context] for DataStore access
 * @param provider The [AuthProvider.Email] configuration with email-link settings
 * @param email The email address of the user. On same-device, retrieved from DataStore.
 *              On cross-device first call, pass empty string to trigger validation.
 *              On cross-device second call, pass user-provided email.
 * @param emailLink The complete deep link URL received from the Intent.
 * @param persistenceManager Optional [PersistenceManager] for testing. Defaults to [EmailLinkPersistenceManager.default]
 *
 * This URL contains:
 * - Firebase action code (oobCode) for authentication
 * - Session ID (ui_sid) for same-device validation
 * - Force same-device flag (ui_sd) for security enforcement
 * - Provider ID (ui_pid) if linking social provider credential
 *
 * Example:
 * `https://yourapp.page.link/__/auth/action?oobCode=ABC123&continueUrl=https://yourapp.com?ui_sid=123456`
 *
 * @return [AuthResult] containing the signed-in user, or null if cross-device validation is required
 *
 * @throws AuthException.InvalidEmailLinkException if the email link is invalid or expired
 * @throws AuthException.EmailLinkPromptForEmailException if cross-device and email is empty
 * @throws AuthException.EmailLinkWrongDeviceException if force same-device is enabled on different device
 * @throws AuthException.EmailLinkCrossDeviceLinkingException if trying to link provider on different device
 * @throws AuthException.EmailMismatchException if email is empty on same-device flow
 * @throws AuthException.AuthCancelledException if the operation is cancelled
 * @throws AuthException.NetworkException if a network error occurs
 * @throws AuthException.UnknownException for other errors
 *
 * @see sendSignInLinkToEmail for sending the initial email link
 * @see EmailLinkPersistenceManager for session data management
 */
internal suspend fun FirebaseAuthUI.signInWithEmailLink(
    context: Context,
    provider: AuthProvider.Email,
    email: String,
    emailLink: String,
    persistenceManager: PersistenceManager = EmailLinkPersistenceManager.default,
): AuthResult? {
    try {
        updateAuthState(AuthState.Loading("Signing in with email link..."))

        // Validate link format
        if (!auth.isSignInWithEmailLink(emailLink)) {
            throw AuthException.InvalidEmailLinkException()
        }

        // Parse email link for session data
        val parser = EmailLinkParser(emailLink)
        val sessionIdFromLink = parser.sessionId
        val oobCode = parser.oobCode
        val providerIdFromLink = parser.providerId
        val isEmailLinkForceSameDeviceEnabled = parser.forceSameDeviceBit

        // Retrieve stored session record from DataStore
        val sessionRecord = persistenceManager.retrieveSessionRecord(context)
        val storedSessionId = sessionRecord?.sessionId

        // Check if this is a different device flow
        val isDifferentDevice = provider.isDifferentDevice(
            sessionIdFromLocal = storedSessionId,
            sessionIdFromLink = sessionIdFromLink
                ?: "" // Convert null to empty string to match legacy behavior
        )

        if (isDifferentDevice) {
            // Handle cross-device flow
            // Session ID must always be present in the link
            if (sessionIdFromLink.isNullOrEmpty()) {
                val exception = AuthException.InvalidEmailLinkException()
                updateAuthState(AuthState.Error(exception))
                throw exception
            }

            // These scenarios require same-device flow
            if (isEmailLinkForceSameDeviceEnabled) {
                val exception = AuthException.EmailLinkWrongDeviceException()
                updateAuthState(AuthState.Error(exception))
                throw exception
            }

            // If we have no SessionRecord/there is a session ID mismatch, this means that we were
            // not the ones to send the link. The only way forward is to prompt the user for their
            // email before continuing the flow. We should only do that after validating the link.
            // However, if email is already provided (cross-device with user input), skip validation
            if (email.isEmpty()) {
                handleDifferentDeviceErrorFlow(oobCode, providerIdFromLink, emailLink)
                return null
            }
            // Email provided - validate it and continue with normal flow
        }

        // Validate email is not empty (same-device flow only)
        if (email.isEmpty()) {
            throw AuthException.EmailMismatchException()
        }

        // Get credential for linking from session record
        val storedCredentialForLink = sessionRecord?.credentialForLinking
        val emailLinkCredential = EmailAuthProvider.getCredentialWithLink(email, emailLink)

        val result = if (storedCredentialForLink == null) {
            // Normal Flow: Just sign in with email link
            handleEmailLinkNormalFlow(emailLinkCredential)
        } else {
            // Linking Flow: Sign in with email link, then link the social credential
            handleEmailLinkCredentialLinkingFlow(
                emailLinkCredential = emailLinkCredential,
                storedCredentialForLink = storedCredentialForLink,
            )
        }
        // Clear DataStore after success
        persistenceManager.clear(context)
        updateAuthState(AuthState.Idle)
        return result
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Sign in with email link was cancelled",
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

private suspend fun FirebaseAuthUI.handleDifferentDeviceErrorFlow(
    oobCode: String,
    providerIdFromLink: String?,
    emailLink: String
) {
    // Validate the action code
    try {
        auth.checkActionCode(oobCode).await()
    } catch (e: Exception) {
        // Invalid action code
        val exception = AuthException.InvalidEmailLinkException(cause = e)
        updateAuthState(AuthState.Error(exception))
        throw exception
    }

    // If there's a provider ID, this is a linking flow which can't be done cross-device
    if (!providerIdFromLink.isNullOrEmpty()) {
        val providerNameForMessage =
            Provider.fromId(providerIdFromLink)?.providerName ?: providerIdFromLink
        val exception = AuthException.EmailLinkCrossDeviceLinkingException(
            providerName = providerNameForMessage,
            emailLink = emailLink
        )
        updateAuthState(AuthState.Error(exception))
        throw exception
    }

    // Link is valid but we need the user to provide their email
    val exception = AuthException.EmailLinkPromptForEmailException(
        cause = null,
        emailLink = emailLink
    )
    updateAuthState(AuthState.Error(exception))
    throw exception
}

private suspend fun FirebaseAuthUI.handleEmailLinkNormalFlow(
    emailLinkCredential: AuthCredential,
): AuthResult? {
    return signInWithCredential(emailLinkCredential)
}

private suspend fun FirebaseAuthUI.handleEmailLinkCredentialLinkingFlow(
    emailLinkCredential: AuthCredential,
    storedCredentialForLink: AuthCredential,
): AuthResult? {
    // Sign in with email link, then link social credential
    return auth.signInWithCredential(emailLinkCredential).await()
        // Link the social credential
        .user?.linkWithCredential(storedCredentialForLink)?.await()
        .also { result ->
            result?.user?.let { user ->
                // Merge profile from the linked social credential
                mergeProfile(
                    auth,
                    user.displayName,
                    user.photoUrl
                )
            }
        }
}

/**
 * Sends a password reset email to the specified email address.
 *
 * **Error Handling:**
 *
 * - If the email doesn't exist: completes successfully to avoid email enumeration
 * - If the email is invalid: throws [AuthException.InvalidCredentialsException]
 * - If network error occurs: throws [AuthException.NetworkException]
 *
 * @param email The email address to send the password reset email to
 * @param actionCodeSettings Optional [ActionCodeSettings] to configure the password reset link.
 *                           Use this to customize the continue URL, dynamic link domain, and other settings.
 *
 * @throws AuthException.InvalidCredentialsException if the email format is invalid
 * @throws AuthException.NetworkException if a network error occurs
 * @throws AuthException.AuthCancelledException if the operation is cancelled
 * @throws AuthException.UnknownException for other errors
 *
 * @see com.google.firebase.auth.ActionCodeSettings
 */
internal suspend fun FirebaseAuthUI.sendPasswordResetEmail(
    email: String,
    actionCodeSettings: ActionCodeSettings? = null,
) {
    try {
        updateAuthState(AuthState.Loading("Sending password reset email..."))
        auth.sendPasswordResetEmail(email, actionCodeSettings).await()
        updateAuthState(AuthState.PasswordResetLinkSent())
    } catch (_: FirebaseAuthInvalidUserException) {
        // To protect against email enumeration don't indicate failure if user doesn't exist
        updateAuthState(AuthState.PasswordResetLinkSent())
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Send password reset email was cancelled",
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
