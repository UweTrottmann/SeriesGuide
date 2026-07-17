// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import timber.log.Timber

private val Context.credentialDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "seriesguide.auth.credentialmanager",
    corruptionHandler = ReplaceFileCorruptionHandler { corruptionException ->
        Timber.e(
            corruptionException,
            "Reading credential manager preferences failed, clearing file"
        )
        emptyPreferences()
    }
)

/**
 * Manages persistence for credential manager state.
 *
 * This class tracks whether credentials have been saved to the Android Credential Manager
 * to prevent unnecessary credential retrieval attempts when no credentials exist.
 *
 * @since 10.0.0
 */
object CredentialPersistenceManager {

    private val KEY_HAS_SAVED_CREDENTIALS = booleanPreferencesKey("has_saved_credentials")

    /**
     * Marks that credentials have been successfully saved to the credential manager.
     *
     * @param context The Android context
     */
    suspend fun setCredentialsSaved(context: Context) {
        context.credentialDataStore.edit { prefs ->
            prefs[KEY_HAS_SAVED_CREDENTIALS] = true
        }
    }

    /**
     * Checks if credentials have been saved at least once.
     * This prevents unnecessary credential retrieval attempts.
     *
     * @param context The Android context
     * @return true if credentials have been saved, false otherwise
     */
    suspend fun hasSavedCredentials(context: Context): Boolean {
        val prefs = context.credentialDataStore.data.first()
        return prefs[KEY_HAS_SAVED_CREDENTIALS] ?: false
    }

    /**
     * Clears the saved credentials flag.
     * Useful for testing or when user signs out permanently.
     *
     * @param context The Android context
     */
    suspend fun clearSavedCredentialsFlag(context: Context) {
        context.credentialDataStore.edit { prefs ->
            prefs.remove(KEY_HAS_SAVED_CREDENTIALS)
        }
    }
}
