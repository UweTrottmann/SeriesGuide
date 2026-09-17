// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.configuration

import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.AuthUIStringProvider

/**
 * A set of validation rules that can be applied to a password when using
 * [com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.AuthProvider.Email].
 */
abstract class PasswordRule {
    /**
     * Requires the password to have at least a certain number of characters.
     */
    class MinimumLength(val value: Int) : PasswordRule() {
        override fun isValid(password: String): Boolean {
            return password.length >= this@MinimumLength.value
        }

        override fun getErrorMessage(stringProvider: AuthUIStringProvider): String {
            return stringProvider.passwordTooShort(value)
        }
    }

    /**
     * Requires the password to contain at least one uppercase letter (A-Z).
     */
    object RequireUppercase : PasswordRule() {
        override fun isValid(password: String): Boolean {
            return password.any { it.isUpperCase() }
        }

        override fun getErrorMessage(stringProvider: AuthUIStringProvider): String {
            return stringProvider.passwordMissingUppercase
        }
    }

    /**
     * Requires the password to contain at least one lowercase letter (a-z).
     */
    object RequireLowercase : PasswordRule() {
        override fun isValid(password: String): Boolean {
            return password.any { it.isLowerCase() }
        }

        override fun getErrorMessage(stringProvider: AuthUIStringProvider): String {
            return stringProvider.passwordMissingLowercase
        }
    }

    /**
     * Requires the password to contain at least one numeric digit (0-9).
     */
    object RequireDigit : PasswordRule() {
        override fun isValid(password: String): Boolean {
            return password.any { it.isDigit() }
        }

        override fun getErrorMessage(stringProvider: AuthUIStringProvider): String {
            return stringProvider.passwordMissingDigit
        }
    }

    /**
     * Requires the password to contain at least one special character (e.g., !@#$%^&*).
     */
    object RequireSpecialCharacter : PasswordRule() {
        private val specialCharacters = "!@#$%^&*()_+-=[]{}|;:,.<>?".toSet()

        override fun isValid(password: String): Boolean {
            return password.any { it in specialCharacters }
        }

        override fun getErrorMessage(stringProvider: AuthUIStringProvider): String {
            return stringProvider.passwordMissingSpecialCharacter
        }
    }

    /**
     * Defines a custom validation rule using a regular expression and provides a specific error
     * message on failure.
     */
    class Custom(
        val regex: Regex,
        val errorMessage: String
    ) : PasswordRule() {
        override fun isValid(password: String): Boolean {
            return regex.matches(password)
        }

        override fun getErrorMessage(stringProvider: AuthUIStringProvider): String {
            return errorMessage
        }
    }

    /**
     * Validates whether the given password meets this rule's requirements.
     *
     * @param password The password to validate
     * @return true if the password meets this rule's requirements, false otherwise
     */
    internal abstract fun isValid(password: String): Boolean

    /**
     * Returns the appropriate error message for this rule when validation fails.
     *
     * @param stringProvider The string provider for localized error messages
     * @return The localized error message for this rule
     */
    internal abstract fun getErrorMessage(stringProvider: AuthUIStringProvider): String
}