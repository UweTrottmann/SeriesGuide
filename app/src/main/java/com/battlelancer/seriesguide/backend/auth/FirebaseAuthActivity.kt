// SPDX-License-Identifier: Apache-2.0 AND AGPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.auth.configuration.authUIConfiguration
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.AuthProvider
import com.battlelancer.seriesguide.backend.auth.configuration.theme.AuthUITheme
import com.battlelancer.seriesguide.backend.auth.ui.screens.FirebaseAuthScreen
import com.battlelancer.seriesguide.backend.auth.util.EmailLinkConstants
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.theme.SeriesGuideTheme
import kotlinx.coroutines.launch

/**
 * Activity that hosts the Firebase authentication flow UI.
 *
 * This activity displays a [FirebaseAuthScreen] composable and manages
 * the authentication flow lifecycle. It automatically finishes when the user
 * signs in successfully or cancels the flow.
 *
 * ```kotlin
 * private val signInWithFirebase = registerForActivityResult(
 *     ActivityResultContracts.StartActivityForResult()
 * ) { result: ActivityResult ->
 *         if (result.resultCode == Activity.RESULT_OK) {
 *             // User has signed in
 *         } else {
 *             // User has left, there might have been an error
 *             // Optional: access AuthException
 *             val error = result.data
 *                 ?.let {
 *                     if (AndroidUtils.isAtLeastTiramisu) {
 *                         it.getSerializableExtra(
 *                             FirebaseAuthActivity.EXTRA_ERROR,
 *                             AuthException::class.java
 *                         )
 *                     } else {
 *                         @Suppress("DEPRECATION")
 *                         it.getSerializableExtra(FirebaseAuthActivity.EXTRA_ERROR)
 *                                 as? AuthException
 *                     }
 *                 }
 *         }
 *     }
 *
 * val intent = FirebaseAuthActivity.createIntent(requireContext())
 * signInWithFirebase.launch(intent)
 * ```
 *
 * **Result Codes:**
 * - [Activity.RESULT_OK] - User signed in successfully
 * - [Activity.RESULT_CANCELED] - User has left, an error may have occurred
 *
 * **Result Data:**
 * - [EXTRA_ERROR] - [AuthException] when an error occurs
 *
 * **Note:** To get the full user object after successful sign-in, use:
 * ```kotlin
 * FirebaseAuth.getInstance().currentUser
 * ```
 *
 * @see FirebaseAuthScreen
 */
class FirebaseAuthActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authUI = FirebaseAuthUI.getInstance()
        if (savedInstanceState == null) {
            // Clear any previous error before starting a new sign-in flow
            authUI.updateAuthState(AuthState.Idle)
        }

        val hexagonTools = SgApp.getServicesComponent(this).hexagonTools()

        val configuration = authUIConfiguration {
            context = applicationContext
            privacyPolicyUrl = getString(R.string.url_privacy)
            isMfaEnabled = false
            providers {
                provider(
                    AuthProvider.Email(
                        emailLinkActionCodeSettings = null
                    )
                )
                if (hexagonTools.isGoogleSignInAvailable) {
                    provider(
                        AuthProvider.Google(
                            scopes = listOf("email"),
                            // The string resource is created by the google-services plugin
                            serverClientId = getString(R.string.default_web_client_id),
                            filterByAuthorizedAccounts = false
                        )
                    )
                }
            }
        }

        // Extract email link if present
        val emailLink = intent.getStringExtra(EmailLinkConstants.EXTRA_EMAIL_LINK)

        // Observe auth state to automatically finish when done
        lifecycleScope.launch {
            authUI.authStateFlow().collect { state ->
                when (state) {
                    is AuthState.Success -> {
                        // User signed in successfully
                        setResult(RESULT_OK)
                        finish()
                    }

                    is AuthState.Cancelled -> {
                        // User canceled the flow
                        setResult(RESULT_CANCELED)
                        finish()
                    }

                    is AuthState.Error -> {
                        // Error occurred, finish with error info
                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_ERROR, state.exception)
                        }
                        setResult(RESULT_CANCELED, resultIntent)
                        // Don't finish on error, let user see error and retry
                    }

                    else -> {
                        // Other states, keep showing UI
                    }
                }
            }
        }

        // Set up Compose UI
        setContent {
            SeriesGuideTheme(useDynamicColor = DisplaySettings.isDynamicColorsEnabled(this)) {
                AuthUITheme(theme = AuthUITheme.fromMaterialTheme()) {
                    FirebaseAuthScreen(
                        authUI = authUI,
                        configuration = configuration,
                        emailLink = emailLink,
                        onSignInSuccess = {
                            // State flow will handle finishing
                        },
                        onSignInFailure = { _ ->
                            // State flow will handle error
                        },
                        onSignInCancelled = {
                            authUI.updateAuthState(AuthState.Cancelled)
                        }
                    )
                }
            }
        }
    }

    companion object {

        /**
         * Intent extra key for [AuthException] on error.
         */
        const val EXTRA_ERROR = "seriesguide.auth.error"

        /**
         * Creates an Intent to launch the Firebase authentication flow.
         *
         * @param context Android [Context]
         * @return Configured [Intent] to start [FirebaseAuthActivity]
         */
        fun createIntent(context: Context): Intent {
            return Intent(context, FirebaseAuthActivity::class.java)
        }
    }
}
