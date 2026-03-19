// SPDX-License-Identifier: Apache-2.0 AND AGPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.AuthProvider
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.Provider
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "com.firebase.ui.auth.util.EmailLinkPersistenceManager")

/**
 * Manages saving/retrieving from DataStore for email link sign in.
 *
 * This class provides persistence for email link authentication sessions, including:
 * - Email address
 * - Session ID for same-device validation
 * - Social provider credentials for linking flows
 *
 * @since 10.0.0
 */
object EmailLinkPersistenceManager {
    
    /**
     * Default instance.
     */
    internal val default: PersistenceManager = DefaultPersistenceManager()
    
    /**
     * The default implementation of [PersistenceManager] that uses DataStore.
     */
    private class DefaultPersistenceManager : PersistenceManager {
        override suspend fun saveEmail(
            context: Context,
            email: String,
            sessionId: String
        ) {
            context.dataStore.edit { prefs ->
                prefs[AuthProvider.Email.KEY_EMAIL] = email
                prefs[AuthProvider.Email.KEY_SESSION_ID] = sessionId
            }
        }
        
        override suspend fun saveCredentialForLinking(
            context: Context,
            providerType: String,
            idToken: String?,
            accessToken: String?
        ) {
            context.dataStore.edit { prefs ->
                prefs[AuthProvider.Email.KEY_PROVIDER] = providerType
                prefs[AuthProvider.Email.KEY_IDP_TOKEN] = idToken ?: ""
                prefs[AuthProvider.Email.KEY_IDP_SECRET] = accessToken ?: ""
            }
        }
        
        override suspend fun retrieveSessionRecord(context: Context): SessionRecord? {
            val prefs = context.dataStore.data.first()
            val email = prefs[AuthProvider.Email.KEY_EMAIL]
            val sessionId = prefs[AuthProvider.Email.KEY_SESSION_ID]

            if (email == null || sessionId == null) {
                return null
            }

            val providerType = Provider.fromId(prefs[AuthProvider.Email.KEY_PROVIDER])
            val idToken = prefs[AuthProvider.Email.KEY_IDP_TOKEN]
            val accessToken = prefs[AuthProvider.Email.KEY_IDP_SECRET]

            // Rebuild credential if we have provider data
            val credentialForLinking = if (providerType != null && idToken != null) {
                when (providerType) {
                    Provider.GOOGLE -> GoogleAuthProvider.getCredential(idToken, accessToken)
                    else -> null
                }
            } else {
                null
            }

            return SessionRecord(
                sessionId = sessionId,
                email = email,
                credentialForLinking = credentialForLinking
            )
        }
        
        override suspend fun clear(context: Context) {
            context.dataStore.edit { prefs ->
                prefs.remove(AuthProvider.Email.KEY_SESSION_ID)
                prefs.remove(AuthProvider.Email.KEY_EMAIL)
                prefs.remove(AuthProvider.Email.KEY_PROVIDER)
                prefs.remove(AuthProvider.Email.KEY_IDP_TOKEN)
                prefs.remove(AuthProvider.Email.KEY_IDP_SECRET)
            }
        }
    }

    /**
     * Holds the necessary information to complete the email link sign in flow.
     *
     * @property sessionId Unique session identifier for same-device validation
     * @property email Email address for sign-in
     * @property credentialForLinking Optional social provider credential to link after sign-in
     */
    data class SessionRecord(
        val sessionId: String,
        val email: String,
        val credentialForLinking: AuthCredential?
    )
}
