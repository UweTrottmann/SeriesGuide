// SPDX-License-Identifier: Apache-2.0 AND AGPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth

import android.content.Context
import android.content.Intent
import androidx.annotation.RestrictTo
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.AuthProvider
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.Provider
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.signOutFromGoogle
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

/**
 * The central class that coordinates all authentication operations for Firebase Auth UI Compose.
 * This class manages UI state and provides methods for signing in, signing up, and managing
 * user accounts.
 *
 * <h2>Usage</h2>
 *
 * **Default app instance:**
 * ```kotlin
 * val authUI = FirebaseAuthUI.getInstance()
 * ```
 *
 * **Custom app instance:**
 * ```kotlin
 * val customApp = Firebase.app("secondary")
 * val authUI = FirebaseAuthUI.getInstance(customApp)
 * ```
 *
 * **Multi-tenancy with custom auth:**
 * ```kotlin
 * val customAuth = Firebase.auth(customApp).apply {
 *     tenantId = "my-tenant-id"
 * }
 * val authUI = FirebaseAuthUI.create(customApp, customAuth)
 * ```
 *
 * @property app The [FirebaseApp] instance used for authentication
 * @property auth The [FirebaseAuth] instance used for authentication operations
 *
 * @since 10.0.0
 */
class FirebaseAuthUI private constructor(
    val app: FirebaseApp,
    val auth: FirebaseAuth,
) {

    private val _authStateFlow = MutableStateFlow<AuthState>(AuthState.Idle)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    var testCredentialManagerProvider: AuthProvider.Google.CredentialManagerProvider? = null

    /**
     * Checks whether a user is currently signed in.
     *
     * This method directly mirrors the state of [FirebaseAuth] and returns true if there is
     * a currently signed-in user, false otherwise.
     *
     * **Example:**
     * ```kotlin
     * val authUI = FirebaseAuthUI.getInstance()
     * if (authUI.isSignedIn()) {
     *     // User is signed in
     *     navigateToHome()
     * } else {
     *     // User is not signed in
     *     navigateToLogin()
     * }
     * ```
     *
     * @return `true` if a user is signed in, `false` otherwise
     */
    fun isSignedIn(): Boolean = auth.currentUser != null

    /**
     * Returns the currently signed-in user, or null if no user is signed in.
     *
     * This method returns the same value as [FirebaseAuth.currentUser] and provides
     * direct access to the current user object.
     *
     * **Example:**
     * ```kotlin
     * val authUI = FirebaseAuthUI.getInstance()
     * val user = authUI.getCurrentUser()
     * user?.let {
     *     println("User email: ${it.email}")
     *     println("User ID: ${it.uid}")
     * }
     * ```
     *
     * @return The currently signed-in [FirebaseUser], or `null` if no user is signed in
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /**
     * Returns true if this instance can handle the email link in the provided [Intent.getData].
     *
     * See [FirebaseAuth.isSignInWithEmailLink].
     */
    fun canHandleIntent(intent: Intent?): Boolean {
        val link = intent?.data ?: return false
        return auth.isSignInWithEmailLink(link.toString())
    }

    /**
     * Creates a [Flow] that emits [AuthState] changes.
     *
     * This flow observes changes to the internal authentication state and emits appropriate
     * [AuthState] objects. See [AuthState] for all possible states.
     *
     * The initial state depends on if there is a [FirebaseAuth.getCurrentUser]:
     * - [AuthState.RequiresEmailVerification] if there is one using the [Provider.EMAIL] provider,
     *   but the email needs to be verified (Note: temporarily disabled)
     * - [AuthState.Success] if there is one in all other cases
     * - [AuthState.Idle] if there is none
     *
     * **Example:**
     * ```kotlin
     * val authUI = FirebaseAuthUI.getInstance()
     *
     * lifecycleScope.launch {
     *     authUI.authStateFlow().collect { state ->
     *         when (state) {
     *             is AuthState.Success -> {
     *                 // User is signed in
     *                 updateUI(state.user)
     *             }
     *             is AuthState.Error -> {
     *                 // Handle error
     *                 showError(state.exception.message)
     *             }
     *             is AuthState.Loading -> {
     *                 // Show loading indicator
     *                 showProgressBar()
     *             }
     *             // ... handle other states
     *         }
     *     }
     * }
     * ```
     */
    fun authStateFlow(): Flow<AuthState> {
        // Create a flow from FirebaseAuth state listener
        val firebaseAuthFlow = callbackFlow {
            // Set initial state based on current FirebaseAuth user
            val initialState = auth.currentUser.toAuthState()
            trySend(initialState)

            // Listen to changes in the FirebaseAuth user authentication state
            val authStateListener = AuthStateListener { firebaseAuth ->
                val state = firebaseAuth.currentUser.toAuthState()
                trySend(state)
            }
            auth.addAuthStateListener(authStateListener)
            // Stop listening when flow collection is cancelled
            awaitClose {
                auth.removeAuthStateListener(authStateListener)
            }
        }

        // Also observe internal state changes
        return combine(
            firebaseAuthFlow,
            _authStateFlow
        ) { firebaseState, internalState ->
            // Prefer non-idle internal states (like PasswordResetLinkSent, Error, etc.)
            if (internalState !is AuthState.Idle) internalState else firebaseState
        }.distinctUntilChanged()
    }

    private fun FirebaseUser?.toAuthState(): AuthState {
        return if (this != null) {
            // Temporarily remove email verification UI as the signed-in check in
            // CloudSetupFragment and HexagonTools don't enforce it (could probably back out
            // and would be signed in) and the Firebase account is created regardless.
            // Check if email verification is required
            @Suppress("KotlinConstantConditions", "KotlinUnreachableCode")
            if (false &&
                !isEmailVerified &&
                email != null &&
                providerData.any { it.providerId == Provider.EMAIL.id }
            ) {
                AuthState.RequiresEmailVerification(
                    user = this,
                    email = email!!
                )
            } else {
                AuthState.Success(user = this)
            }
        } else {
            AuthState.Idle
        }
    }

    /**
     * Updates the internal authentication state to [state].
     * This method can be used to manually trigger state updates when the Firebase Auth state
     * listener doesn't automatically detect changes (e.g., after reloading user properties).
     */
    fun updateAuthState(state: AuthState) {
        _authStateFlow.value = state
    }

    /**
     * Signs out the current user and if successful sets authentication state to [AuthState.Idle].
     *
     * This method signs out the user from Firebase Auth, if signed in with Google calls
     * [signOutFromGoogle] and updates the auth state flow to reflect the change. The operation is
     * performed partially asynchronous and will emit appropriate states during the process.
     *
     * **Example:**
     * ```kotlin
     * val authUI = FirebaseAuthUI.getInstance()
     *
     * try {
     *     authUI.signOut(context)
     *     // User is now signed out
     * } catch (e: AuthException) {
     *     // Handle sign-out error
     *     when (e) {
     *         is AuthException.AuthCancelledException -> {
     *             // User cancelled sign-out
     *         }
     *         else -> {
     *             // Other error occurred
     *         }
     *     }
     * }
     * ```
     *
     * @param context For [signOutFromGoogle] to create [androidx.credentials.CredentialManager]
     * @throws AuthException.AuthCancelledException if the operation is cancelled
     * @throws AuthException.NetworkException if a network error occurs
     * @throws AuthException.UnknownException for other errors
     * @since 10.0.0
     */
    suspend fun signOut(context: Context) {
        try {
            // Update state to loading
            updateAuthState(AuthState.Loading("Signing out..."))

            // Sign out from Firebase Auth
            auth.signOut()
                .also {
                    signOutFromGoogle(context)
                }

            // Update state to idle (user signed out)
            updateAuthState(AuthState.Idle)

        } catch (e: CancellationException) {
            // Handle coroutine cancellation
            val cancelledException = AuthException.AuthCancelledException(
                message = "Sign-out was cancelled",
                cause = e
            )
            updateAuthState(AuthState.Error(cancelledException))
            throw cancelledException
        } catch (e: AuthException) {
            // Already mapped AuthException, just update state and re-throw
            updateAuthState(AuthState.Error(e))
            throw e
        } catch (e: Exception) {
            // Map to appropriate AuthException
            val authException = AuthException.from(e)
            updateAuthState(AuthState.Error(authException))
            throw authException
        }
    }

    /**
     * Deletes the current user account and if successful sets authentication state to
     * [AuthState.Idle].
     *
     * This method deletes the current user's account from Firebase Auth. If the user
     * hasn't signed in recently, it will throw an exception requiring reauthentication.
     * The operation is performed asynchronously and will emit appropriate states during
     * the process.
     *
     * **Example:**
     * ```kotlin
     * val authUI = FirebaseAuthUI.getInstance()
     *
     * try {
     *     authUI.delete(context)
     *     // User account is now deleted
     * } catch (e: AuthException.InvalidCredentialsException) {
     *     // Recent login required - show reauthentication UI
     *     handleReauthentication()
     * } catch (e: AuthException) {
     *     // Handle other errors
     * }
     * ```
     *
     * @throws AuthException.InvalidCredentialsException if reauthentication is required
     * @throws AuthException.AuthCancelledException if the operation is cancelled
     * @throws AuthException.NetworkException if a network error occurs
     * @throws AuthException.AdminRestrictedException if deleting accounts is not allowed
     * @throws AuthException.UnknownException for other errors
     * @since 10.0.0
     */
    suspend fun delete() {
        try {
            val currentUser = auth.currentUser
                ?: throw AuthException.UnknownException(
                    message = "No user is currently signed in"
                )

            // Update state to loading
            updateAuthState(AuthState.Loading("Deleting account..."))

            // Delete the user account
            currentUser.delete().await()

            // Update state to idle (user deleted and signed out)
            updateAuthState(AuthState.Idle)

        } catch (e: CancellationException) {
            // Handle coroutine cancellation
            val cancelledException = AuthException.AuthCancelledException(
                message = "Account deletion was cancelled",
                cause = e
            )
            updateAuthState(AuthState.Error(cancelledException))
            throw cancelledException
        } catch (e: AuthException) {
            // Already mapped AuthException, just update state and re-throw
            updateAuthState(AuthState.Error(e))
            throw e
        } catch (e: Exception) {
            // Map to appropriate AuthException
            val authException = AuthException.from(e)
            updateAuthState(AuthState.Error(authException))
            throw authException
        }
    }

    companion object {
        /** Cache for singleton instances per FirebaseApp. Thread-safe via ConcurrentHashMap. */
        private val instanceCache = ConcurrentHashMap<String, FirebaseAuthUI>()

        /** Special key for the default app instance to distinguish from named instances. */
        private const val DEFAULT_APP_KEY = "__FIREBASE_UI_DEFAULT__"

        /**
         * Returns a cached singleton instance for the default Firebase app.
         *
         * This method ensures that the same instance is returned for the default app across the
         * entire application lifecycle. The instance is lazily created on first access and cached
         * for subsequent calls.
         *
         * **Example:**
         * ```kotlin
         * val authUI = FirebaseAuthUI.getInstance()
         * val user = authUI.auth.currentUser
         * ```
         *
         * @return The cached [FirebaseAuthUI] instance for the default app
         * @throws IllegalStateException if Firebase has not been initialized. Call
         *         `FirebaseApp.initializeApp(Context)` before using this method.
         */
        @JvmStatic
        fun getInstance(): FirebaseAuthUI {
            val defaultApp = try {
                FirebaseApp.getInstance()
            } catch (e: IllegalStateException) {
                throw IllegalStateException(
                    "Default FirebaseApp is not initialized. " +
                            "Make sure to call FirebaseApp.initializeApp(Context) first.",
                    e
                )
            }

            return instanceCache.getOrPut(DEFAULT_APP_KEY) {
                FirebaseAuthUI(defaultApp, Firebase.auth)
            }
        }

        /**
         * Returns a cached instance for a specific Firebase app.
         *
         * Each [FirebaseApp] gets its own distinct instance that is cached for subsequent calls
         * with the same app. This allows for multiple Firebase projects to be used within the
         * same application.
         *
         * **Example:**
         * ```kotlin
         * val secondaryApp = Firebase.app("secondary")
         * val authUI = FirebaseAuthUI.getInstance(secondaryApp)
         * ```
         *
         * @param app The [FirebaseApp] instance to use
         * @return The cached [FirebaseAuthUI] instance for the specified app
         */
        @JvmStatic
        fun getInstance(app: FirebaseApp): FirebaseAuthUI {
            val cacheKey = app.name
            return instanceCache.getOrPut(cacheKey) {
                FirebaseAuthUI(app, Firebase.auth(app))
            }
        }

        /**
         * Creates a new instance with custom configuration, useful for multi-tenancy.
         *
         * This method always returns a new instance and does **not** use caching, allowing for
         * custom [FirebaseAuth] configurations such as tenant IDs or custom authentication states.
         * Use this when you need fine-grained control over the authentication instance.
         *
         * **Example - Multi-tenancy:**
         * ```kotlin
         * val app = Firebase.app("tenant-app")
         * val auth = Firebase.auth(app).apply {
         *     tenantId = "customer-tenant-123"
         * }
         * val authUI = FirebaseAuthUI.create(app, auth)
         * ```
         *
         * @param app The [FirebaseApp] instance to use
         * @param auth The [FirebaseAuth] instance with custom configuration
         * @return A new [FirebaseAuthUI] instance with the provided dependencies
         */
        @JvmStatic
        fun create(app: FirebaseApp, auth: FirebaseAuth): FirebaseAuthUI {
            return FirebaseAuthUI(app, auth)
        }

        /**
         * Clears all cached instances. This method is intended for testing purposes only.
         *
         * @suppress This is an internal API and should not be used in production code.
         * @RestrictTo RestrictTo.Scope.TESTS
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.TESTS)
        fun clearInstanceCache() {
            instanceCache.clear()
        }

        /**
         * Returns the current number of cached instances. This method is intended for testing
         * purposes only.
         *
         * @return The number of cached [FirebaseAuthUI] instances
         * @suppress This is an internal API and should not be used in production code.
         * @RestrictTo RestrictTo.Scope.TESTS
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.TESTS)
        internal fun getCacheSize(): Int {
            return instanceCache.size
        }

    }
}