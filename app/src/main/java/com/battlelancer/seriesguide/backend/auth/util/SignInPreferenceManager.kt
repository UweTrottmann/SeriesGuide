// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.util

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import timber.log.Timber

private val Context.signInPreferenceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "seriesguide.auth.signin",
    corruptionHandler = ReplaceFileCorruptionHandler { corruptionException ->
        Timber.e(corruptionException, "Reading sign-in preferences failed, clearing file")
        emptyPreferences()
    }
)

/**
 * Manages persistence for the last-used sign-in method.
 *
 * This class tracks which authentication provider was last used to sign in,
 * along with the user identifier (email, phone number, etc.). This enables
 * a better UX by showing "Continue as `identifier`" with the last-used provider
 * prominently on the method picker screen.
 *
 * @since 10.0.0
 */
object SignInPreferenceManager {

    private val KEY_LAST_PROVIDER_ID = stringPreferencesKey("last_provider_id")
    private val KEY_LAST_IDENTIFIER = stringPreferencesKey("last_identifier")
    private val KEY_LAST_TIMESTAMP = longPreferencesKey("last_timestamp")

    /**
     * Tries to save the last-used sign-in method and user identifier for the "Continue as..."
     * feature. On failure will just log.
     *
     * This should be called after a successful sign-in to track the user's
     * preferred sign-in method.
     *
     * @param providerId The AuthProvider.providerId
     * @param identifier The user identifier (email)
     */
    suspend fun tryToSaveLastSignIn(
        context: Context,
        providerId: String,
        identifier: String?,
        logTag: String
    ) {
        try {
            context.signInPreferenceDataStore.edit { prefs ->
                prefs[KEY_LAST_PROVIDER_ID] = providerId
                identifier?.let { prefs[KEY_LAST_IDENTIFIER] = it }
                prefs[KEY_LAST_TIMESTAMP] = System.currentTimeMillis()
            }
            Timber.tag(logTag).d("Sign-in preference saved for: %s", identifier)
        } catch (e: Exception) {
            // Failed to save preference - log but don't break auth flow
            Timber.tag(logTag).w(e, "Failed to save sign-in preference for: %s", identifier)
        }
    }

    /**
     * Retrieves the last-used sign-in preference.
     *
     * @param context The Android context
     * @return [SignInPreference] containing the last-used provider and identifier, or null if none
     */
    suspend fun getLastSignIn(context: Context): SignInPreference? {
        val prefs = context.signInPreferenceDataStore.data.first()
        val providerId = prefs[KEY_LAST_PROVIDER_ID]
        val identifier = prefs[KEY_LAST_IDENTIFIER]
        val timestamp = prefs[KEY_LAST_TIMESTAMP]

        return if (providerId != null && timestamp != null) {
            SignInPreference(
                providerId = providerId,
                identifier = identifier,
                timestamp = timestamp
            )
        } else {
            null
        }
    }

    /**
     * Clears the saved sign-in preference.
     *
     * This should be called when the user signs out permanently or
     * when resetting authentication state.
     *
     * @param context The Android context
     */
    suspend fun clearLastSignIn(context: Context) {
        context.signInPreferenceDataStore.edit { prefs ->
            prefs.remove(KEY_LAST_PROVIDER_ID)
            prefs.remove(KEY_LAST_IDENTIFIER)
            prefs.remove(KEY_LAST_TIMESTAMP)
        }
    }

    /**
     * Data class representing a saved sign-in preference.
     *
     * @property providerId The provider ID (e.g., "google.com", "facebook.com", "password", "phone")
     * @property identifier The user identifier (email, phone number, etc.), may be null
     * @property timestamp The timestamp when this preference was saved
     */
    data class SignInPreference(
        val providerId: String,
        val identifier: String?,
        val timestamp: Long
    )
}
