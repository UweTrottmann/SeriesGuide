// SPDX-License-Identifier: Apache-2.0 AND AGPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth

import com.battlelancer.seriesguide.backend.auth.AuthException.Companion.from
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/**
 * Abstract base class representing all possible authentication exceptions in Firebase Auth UI.
 *
 * This class provides a unified exception hierarchy for authentication operations, allowing
 * for consistent error handling across the entire Auth UI system.
 *
 * Use the companion object [from] method to create specific exception instances from
 * Firebase authentication exceptions.
 *
 * **Example usage:**
 * ```kotlin
 * try {
 *     // Perform authentication operation
 * } catch (firebaseException: Exception) {
 *     val authException = AuthException.from(firebaseException)
 *     when (authException) {
 *         is AuthException.NetworkException -> {
 *             // Handle network error
 *         }
 *         is AuthException.InvalidCredentialsException -> {
 *             // Handle invalid credentials
 *         }
 *         // ... handle other exception types
 *     }
 * }
 * ```
 *
 * @property message The detailed error message
 * @property cause The underlying [Throwable] that caused this exception
 *
 * @since 10.0.0
 */
abstract class AuthException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * A network error occurred during the authentication operation.
     *
     * This exception is thrown when there are connectivity issues, timeouts,
     * or other network-related problems.
     *
     * @property message The detailed error message
     * @property cause The underlying [Throwable] that caused this exception
     */
    class NetworkException(
        message: String,
        cause: Throwable? = null
    ) : AuthException(message, cause)

    /**
     * The provided credentials are not valid.
     *
     * This exception is thrown when the user provides incorrect login information,
     * such as wrong email/password combinations, the account doesn't exist or is disabled or the
     * account requires a different type of credentials (such as Google Sign-In).
     *
     * @property message The detailed error message
     * @property cause The underlying [Throwable] that caused this exception
     */
    class InvalidCredentialsException(
        message: String,
        cause: Throwable? = null
    ) : AuthException(message, cause)

    /**
     * The password provided does not meet the password policy configured in Firebase Authentication
     * settings.
     *
     * This exception is thrown when creating an account or updating a password.
     *
     * @property message The detailed error message
     * @property cause The underlying [Throwable] that caused this exception
     * @property reason The specific reason why the password is considered weak that can be shown to
     * the user.
     */
    class WeakPasswordException(
        message: String,
        cause: Throwable? = null,
        val reason: String? = null
    ) : AuthException(message, cause)

    /**
     * An account with the given email already exists.
     *
     * This exception is thrown when attempting to create a new account or linking to an existing
     * account with an email address that is already used by an existing account.
     *
     * @property message The detailed error message
     * @property cause The underlying [Throwable] that caused this exception
     */
    class EmailAlreadyInUseException(
        message: String,
        cause: Throwable? = null
    ) : AuthException(message, cause)

    /**
     * Multi-Factor Authentication is required to proceed.
     *
     * This exception is thrown when a user has MFA enabled and needs to
     * complete additional authentication steps.
     *
     * @property message The detailed error message
     * @property cause The underlying [Throwable] that caused this exception
     */
    class MfaRequiredException(
        message: String,
        cause: Throwable? = null
    ) : AuthException(message, cause)

    /**
     * Account linking is required to complete sign-in.
     *
     * This exception is thrown when a user tries to sign in with a provider
     * that needs to be linked to an existing account. For example, when a user
     * tries to sign in with Facebook but an account already exists with that
     * email using a different provider (like email/password).
     *
     * @property message The detailed error message
     * @property email The email address that already has an account (optional)
     * @property credential The credential that should be linked after signing in (optional)
     * @property cause The underlying [Throwable] that caused this exception
     */
    class AccountLinkingRequiredException(
        message: String,
        val email: String? = null,
        val credential: AuthCredential? = null,
        cause: Throwable? = null
    ) : AuthException(message, cause)

    /**
     * An operation was cancelled, such as a coroutine getting cancelled or the user cancelling an
     * operation.
     */
    class AuthCancelledException(
        message: String,
        cause: Throwable? = null
    ) : AuthException(message, cause)

    /**
     * If an action, like sign-up or deletion, is (temporarily) restricted to
     * admins server-side (so no user self-service).
     * https://firebase.google.com/docs/auth/users#user-actions
     */
    class AdminRestrictedException(
        message: String,
        cause: Throwable? = null
    ) : AuthException(message, cause)

    /**
     * An unknown or unhandled error occurred.
     *
     * This exception is thrown for errors that don't match any of the specific
     * exception types or for unexpected system errors.
     *
     * @property message The detailed error message
     * @property cause The underlying [Throwable] that caused this exception
     */
    class UnknownException(
        message: String,
        cause: Throwable? = null
    ) : AuthException(message, cause)

    /**
     * The email link used for sign-in is invalid or malformed.
     *
     * This exception is thrown when the link is not a valid Firebase email link,
     * has incorrect format, or is missing required parameters.
     *
     * @property cause The underlying [Throwable] that caused this exception
     */
    class InvalidEmailLinkException(
        cause: Throwable? = null
    ) : AuthException("You are are attempting to sign in with an invalid email link", cause)

    /**
     * The email link is being used on a different device than where it was requested.
     *
     * This exception is thrown when `forceSameDevice = true` and the user opens
     * the link on a different device than the one used to request it.
     *
     * @property cause The underlying [Throwable] that caused this exception
     */
    class EmailLinkWrongDeviceException(
        cause: Throwable? = null
    ) : AuthException("You must open the email link on the same device.", cause)

    /**
     * Cross-device account linking is required to complete email link sign-in.
     *
     * This exception is thrown when the email link matches an existing account with
     * a social provider (Google/Facebook), and the user needs to sign in with that
     * provider to link accounts.
     *
     * @property providerName The name of the social provider that needs to be linked
     * @property emailLink The email link being processed
     * @property cause The underlying [Throwable] that caused this exception
     */
    class EmailLinkCrossDeviceLinkingException(
        val providerName: String? = null,
        val emailLink: String? = null,
        cause: Throwable? = null
    ) : AuthException("You must determine if you want to continue linking or " +
            "complete the sign in", cause)

    /**
     * User needs to provide their email address to complete email link sign-in.
     *
     * This exception is thrown when the email link is opened on a different device
     * and the email address cannot be determined from stored session data.
     *
     * @property emailLink The email link to be used after email is provided
     * @property cause The underlying [Throwable] that caused this exception
     */
    class EmailLinkPromptForEmailException(
        cause: Throwable? = null,
        val emailLink: String? = null,
    ) : AuthException("Please enter your email to continue signing in", cause)

    /**
     * The email address provided does not match the email link.
     *
     * This exception is thrown when the user enters an email address that doesn't
     * match the email to which the sign-in link was sent.
     *
     * @property cause The underlying [Throwable] that caused this exception
     */
    class EmailMismatchException(
        cause: Throwable? = null
    ) : AuthException("You are are attempting to sign in a different email " +
            "than previously provided", cause)

    companion object {
        private const val FIREBASE_ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL =
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL"
        const val FIREBASE_ERROR_ADMIN_RESTRICTED_OPERATION =
            "ERROR_ADMIN_RESTRICTED_OPERATION"
        private const val FIREBASE_ERROR_CREDENTIAL_ALREADY_IN_USE =
            "ERROR_CREDENTIAL_ALREADY_IN_USE"
        private const val FIREBASE_ERROR_EMAIL_ALREADY_IN_USE = "ERROR_EMAIL_ALREADY_IN_USE"
        private const val FIREBASE_ERROR_USER_DISABLED = "ERROR_USER_DISABLED"
        private const val FIREBASE_ERROR_USER_NOT_FOUND = "ERROR_USER_NOT_FOUND"

        /**
         * Creates an appropriate [AuthException] instance from a Firebase authentication exception.
         *
         * This method maps known Firebase exception types to their corresponding [AuthException]
         * subtypes, providing a consistent exception hierarchy for error handling.
         *
         */
        @JvmStatic
        fun from(firebaseException: Exception): AuthException {
            return when (firebaseException) {
                // If already an AuthException, return it directly
                is AuthException -> firebaseException

                // Is a FirebaseAuthInvalidCredentialsException, so handle before
                is FirebaseAuthWeakPasswordException -> {
                    WeakPasswordException(
                        message = firebaseException.message ?: "Password is too weak",
                        cause = firebaseException,
                        reason = firebaseException.reason
                    )
                }

                // Handle specific Firebase Auth exceptions first (before general FirebaseException)
                is FirebaseAuthInvalidCredentialsException -> {
                    InvalidCredentialsException(
                        message = firebaseException.message ?: "Invalid credentials provided",
                        cause = firebaseException
                    )
                }

                // Don't differentiate in UI between invalid credentials and user not found or
                // disabled to avoid enumeration.
                is FirebaseAuthInvalidUserException -> {
                    when (firebaseException.errorCode) {
                        FIREBASE_ERROR_USER_NOT_FOUND -> InvalidCredentialsException(
                            message = firebaseException.message ?: "User not found",
                            cause = firebaseException
                        )

                        FIREBASE_ERROR_USER_DISABLED -> InvalidCredentialsException(
                            message = firebaseException.message ?: "User account has been disabled",
                            cause = firebaseException
                        )

                        else -> InvalidCredentialsException(
                            message = firebaseException.message ?: "User account error",
                            cause = firebaseException
                        )
                    }
                }

                is FirebaseAuthUserCollisionException -> {
                    when (firebaseException.errorCode) {
                        FIREBASE_ERROR_EMAIL_ALREADY_IN_USE -> EmailAlreadyInUseException(
                            message = firebaseException.message
                                ?: "Email address is already in use",
                            cause = firebaseException
                        )

                        FIREBASE_ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL -> AccountLinkingRequiredException(
                            message = firebaseException.message
                                ?: "Account already exists with different credentials",
                            cause = firebaseException
                        )

                        FIREBASE_ERROR_CREDENTIAL_ALREADY_IN_USE -> AccountLinkingRequiredException(
                            message = firebaseException.message
                                ?: "Credential is already associated with a different user account",
                            cause = firebaseException
                        )

                        else -> AccountLinkingRequiredException(
                            message = firebaseException.message ?: "Account collision error",
                            cause = firebaseException
                        )
                    }
                }

                is FirebaseAuthMultiFactorException -> {
                    MfaRequiredException(
                        message = firebaseException.message
                            ?: "Multi-factor authentication required",
                        cause = firebaseException
                    )
                }

                is FirebaseAuthRecentLoginRequiredException -> {
                    InvalidCredentialsException(
                        message = firebaseException.message
                            ?: "Recent login required for this operation",
                        cause = firebaseException
                    )
                }

                is FirebaseAuthException -> {
                    when (firebaseException.errorCode) {
                        FIREBASE_ERROR_ADMIN_RESTRICTED_OPERATION -> {
                            AdminRestrictedException(
                                message = firebaseException.message
                                    ?: "This action is restricted to admins",
                                cause = firebaseException
                            )
                        }

                        else -> UnknownException(
                            message = firebaseException.message
                                ?: "An unknown authentication error occurred",
                            cause = firebaseException
                        )
                    }
                }

                is FirebaseException -> {
                    // Handle general Firebase exceptions, which include network errors
                    NetworkException(
                        message = firebaseException.message ?: "Network error occurred",
                        cause = firebaseException
                    )
                }

                else -> {
                    // Check for common cancellation patterns
                    if (firebaseException.message?.contains(
                            "cancelled",
                            ignoreCase = true
                        ) == true ||
                        firebaseException.message?.contains("canceled", ignoreCase = true) == true
                    ) {
                        AuthCancelledException(
                            message = firebaseException.message ?: "Authentication was cancelled",
                            cause = firebaseException
                        )
                    } else {
                        UnknownException(
                            message = firebaseException.message ?: "An unknown error occurred",
                            cause = firebaseException
                        )
                    }
                }
            }
        }
    }
}
