// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth

import com.battlelancer.seriesguide.backend.auth.AuthState.Companion.Cancelled
import com.battlelancer.seriesguide.backend.auth.AuthState.Companion.Idle
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorResolver

/**
 * Represents the authentication state in Firebase Auth UI.
 *
 * This class encapsulates all possible authentication states that can occur during
 * the authentication flow:
 *
 * - [AuthState.Idle] when there's no active authentication operation
 * - [AuthState.Loading] during authentication operations
 * - [AuthState.Success] when a user successfully signs in
 * - [AuthState.Error] when an authentication error occurs
 * - [AuthState.Cancelled] when authentication is cancelled
 * - [AuthState.RequiresMfa] when multi-factor authentication is needed
 * - [AuthState.RequiresEmailVerification] when email verification is needed
 * - [AuthState.PasswordResetLinkSent] when a password reset link has been sent
 * - [AuthState.EmailSignInLinkSent] when an email sign in link has been sent
 *
 * Use the companion object factory methods or specific subclass constructors to create instances.
 *
 * @since 10.0.0
 */
abstract class AuthState private constructor() {

    /**
     * Initial state before any authentication operation has been started.
     */
    class Idle internal constructor() : AuthState() {
        override fun equals(other: Any?): Boolean = other is Idle
        override fun hashCode(): Int = javaClass.hashCode()
        override fun toString(): String = "AuthState.Idle"
    }

    /**
     * Authentication operation is in progress.
     *
     * @property message Optional message describing what is being loaded
     */
    class Loading(val message: String? = null) : AuthState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Loading) return false
            return message == other.message
        }

        override fun hashCode(): Int = message?.hashCode() ?: 0

        override fun toString(): String = "AuthState.Loading(message=$message)"
    }

    /**
     * Authentication completed successfully.
     *
     * @property user The authenticated [FirebaseUser]
     */
    class Success(
        val user: FirebaseUser
    ) : AuthState()

    /**
     * An error occurred during authentication.
     *
     * @property exception The [Exception] that occurred
     * @property isRecoverable Whether the error can be recovered from
     */
    class Error(
        val exception: Exception,
        val isRecoverable: Boolean = true
    ) : AuthState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Error) return false
            return exception == other.exception &&
                    isRecoverable == other.isRecoverable
        }

        override fun hashCode(): Int {
            var result = exception.hashCode()
            result = 31 * result + isRecoverable.hashCode()
            return result
        }

        override fun toString(): String =
            "AuthState.Error(exception=$exception, isRecoverable=$isRecoverable)"
    }

    /**
     * Authentication was cancelled by the user.
     */
    class Cancelled internal constructor() : AuthState() {
        override fun equals(other: Any?): Boolean = other is Cancelled
        override fun hashCode(): Int = javaClass.hashCode()
        override fun toString(): String = "AuthState.Cancelled"
    }

    /**
     * Multi-factor authentication is required to complete sign-in.
     *
     * @property resolver The [MultiFactorResolver] to complete MFA
     * @property hint Optional hint about which factor to use
     */
    class RequiresMfa(
        val resolver: MultiFactorResolver,
        val hint: String? = null
    ) : AuthState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RequiresMfa) return false
            return resolver == other.resolver &&
                    hint == other.hint
        }

        override fun hashCode(): Int {
            var result = resolver.hashCode()
            result = 31 * result + (hint?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String =
            "AuthState.RequiresMfa(resolver=$resolver, hint=$hint)"
    }

    /**
     * Email verification is required before the user can access the app.
     *
     * @property user The [FirebaseUser] who needs to verify their email
     * @property email The email address that needs verification
     */
    class RequiresEmailVerification(
        val user: FirebaseUser,
        val email: String
    ) : AuthState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RequiresEmailVerification) return false
            return user == other.user &&
                    email == other.email
        }

        override fun hashCode(): Int {
            var result = user.hashCode()
            result = 31 * result + email.hashCode()
            return result
        }

        override fun toString(): String =
            "AuthState.RequiresEmailVerification(user=$user, email=$email)"
    }

    /**
     * Password reset link has been sent to the user's email.
     */
    class PasswordResetLinkSent : AuthState() {
        override fun equals(other: Any?): Boolean = other is PasswordResetLinkSent
        override fun hashCode(): Int = javaClass.hashCode()
        override fun toString(): String = "AuthState.PasswordResetLinkSent"
    }

    /**
     * Email sign in link has been sent to the user's email.
     */
    class EmailSignInLinkSent : AuthState() {
        override fun equals(other: Any?): Boolean = other is EmailSignInLinkSent
        override fun hashCode(): Int = javaClass.hashCode()
        override fun toString(): String = "AuthState.EmailSignInLinkSent"
    }

    companion object {
        /**
         * Creates an Idle state instance.
         * @return A new [Idle] state
         */
        @JvmStatic
        val Idle: Idle = Idle()

        /**
         * Creates a Cancelled state instance.
         * @return A new [Cancelled] state
         */
        @JvmStatic
        val Cancelled: Cancelled = Cancelled()
    }
}
