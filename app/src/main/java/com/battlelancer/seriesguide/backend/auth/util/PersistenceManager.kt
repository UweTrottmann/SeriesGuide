// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.util

import android.content.Context

/**
 * Interface for managing email link authentication session persistence.
 *
 * This interface abstracts the persistence layer for email link sign-in sessions,
 * allowing for different implementations (DataStore, in-memory for testing, etc.).
 *
 * @since 10.0.0
 */
interface PersistenceManager {

    /**
     * Saves email and session information for email link sign-in.
     *
     * @param context Android context for storage access
     * @param email Email address to save
     * @param sessionId Unique session identifier for same-device validation
     * @param anonymousUserId Optional anonymous user ID for upgrade flows
     */
    suspend fun saveEmail(
        context: Context,
        email: String,
        sessionId: String,
        anonymousUserId: String?
    )

    /**
     * Saves social provider credential information for linking after email link sign-in.
     *
     * @param context Android context for storage access
     * @param providerType Provider ID ("google.com", "facebook.com", etc.)
     * @param idToken ID token from the provider
     * @param accessToken Access token from the provider (optional, used by Facebook)
     */
    suspend fun saveCredentialForLinking(
        context: Context,
        providerType: String,
        idToken: String?,
        accessToken: String?
    )

    /**
     * Retrieves session information from storage.
     *
     * @param context Android context for storage access
     * @return SessionRecord containing saved session data, or null if no session exists
     */
    suspend fun retrieveSessionRecord(context: Context): EmailLinkPersistenceManager.SessionRecord?

    /**
     * Clears all saved data from storage.
     *
     * @param context Android context for storage access
     */
    suspend fun clear(context: Context)
}
