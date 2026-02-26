// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.credentialmanager

/**
 * Represents a password credential retrieved from the system credential manager.
 *
 * @property username The username/identifier associated with the credential
 * @property password The password associated with the credential
 */
data class PasswordCredential(
    val username: String,
    val password: String
)
