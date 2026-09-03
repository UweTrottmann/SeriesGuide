// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.credentialmanager

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.PasswordCredential as AndroidPasswordCredential

/**
 * Handler for password credential operations using Android's Credential Manager.
 *
 * This class provides methods to save and retrieve password credentials through
 * the system credential manager, which displays native UI prompts to the user.
 *
 * @param context The Android context used for credential operations.
 * @param credentialManager Optional, to mock CredentialManager for testing purposes.
 */
class PasswordCredentialHandler(
    private val context: Context,
    private val credentialManager: CredentialManager = CredentialManager.create(context)
) {

    /**
     * Retrieves a password credential from the system credential manager.
     *
     * This method displays a system prompt showing available credentials for the user
     * to select from. The operation is performed asynchronously using Kotlin coroutines.
     *
     * @return PasswordCredential containing the username and password
     * @throws NoCredentialException if no credentials are available
     * @throws GetCredentialCancellationException if the user cancels the retrieval operation
     * @throws GetCredentialException if the credential cannot be retrieved
     */
    suspend fun getPassword(): PasswordCredential {
        val getPasswordOption = GetPasswordOption()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(getPasswordOption)
            .build()

        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential is AndroidPasswordCredential) {
                return PasswordCredential(
                    username = credential.id,
                    password = credential.password
                )
            } else {
                throw PasswordCredentialException("Retrieved credential is not a password credential")
            }
        } catch (e: GetCredentialCancellationException) {
            // User cancelled the retrieval operation
            throw PasswordCredentialCancelledException("User cancelled password retrieval operation", e)
        } catch (e: NoCredentialException) {
            // No credentials available
            throw PasswordCredentialNotFoundException("No password credentials found", e)
        } catch (e: GetCredentialException) {
            // Other credential retrieval errors
            throw PasswordCredentialException("Failed to retrieve password credential", e)
        }
    }
}

/**
 * Base exception for password credential operations.
 */
open class PasswordCredentialException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when a password credential operation is cancelled by the user.
 */
class PasswordCredentialCancelledException(
    message: String,
    cause: Throwable? = null
) : PasswordCredentialException(message, cause)

/**
 * Exception thrown when no password credentials are found.
 */
class PasswordCredentialNotFoundException(
    message: String,
    cause: Throwable? = null
) : PasswordCredentialException(message, cause)
