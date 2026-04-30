// SPDX-License-Identifier: Apache-2.0 AND AGPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.configuration.auth_provider

import android.content.Context
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.compose.ui.graphics.Color
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.battlelancer.seriesguide.backend.auth.configuration.AuthUIConfigurationDsl
import com.battlelancer.seriesguide.backend.auth.configuration.PasswordRule
import com.battlelancer.seriesguide.backend.auth.configuration.theme.AuthUIAsset
import com.battlelancer.seriesguide.backend.auth.util.ContinueUrlBuilder
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.actionCodeSettings
import kotlinx.coroutines.tasks.await
import timber.log.Timber

@AuthUIConfigurationDsl
class AuthProvidersBuilder {
    private val providers = mutableListOf<AuthProvider>()

    fun provider(provider: AuthProvider) {
        providers.add(provider)
    }

    internal fun build(): List<AuthProvider> = providers.toList()
}

/**
 * Enum class to represent all possible providers.
 */
internal enum class Provider(
    val id: String,
    val providerName: String
) {
    GOOGLE(GoogleAuthProvider.PROVIDER_ID, providerName = "Google"),
    EMAIL(EmailAuthProvider.PROVIDER_ID, providerName = "Email");

    companion object {
        fun fromId(id: String?): Provider? {
            return entries.find { it.id == id }
        }
    }
}

/**
 * Base abstract class for authentication providers.
 */
abstract class AuthProvider(open val providerId: String, open val providerName: String) {
    /**
     * Base abstract class for OAuth authentication providers with common properties.
     */
    abstract class OAuth(
        override val providerId: String,

        override val providerName: String,
        open val scopes: List<String> = emptyList(),
        open val customParameters: Map<String, String> = emptyMap(),
    ) : AuthProvider(providerId = providerId, providerName = providerName)

    /**
     * Email/Password authentication provider configuration.
     */
    class Email(
        /**
         * Requires the user to provide a display name. Defaults to true.
         */
        val isDisplayNameRequired: Boolean = true,

        /**
         * Enables email link sign-in, Defaults to false.
         */
        val isEmailLinkSignInEnabled: Boolean = false,

        /**
         * Forces email link sign-in to complete on the same device that initiated it.
         *
         * This is required for security when upgrading anonymous users, but as this doesn't support
         * anonymous accounts defaults to false.
         */
        val isEmailLinkForceSameDeviceEnabled: Boolean = false,

        /**
         * Settings for email link actions.
         */
        val emailLinkActionCodeSettings: ActionCodeSettings?,

        /**
         * Allows new accounts to be created. Defaults to true.
         */
        val isNewAccountsAllowed: Boolean = true,

        /**
         * The minimum length for a password. Defaults to 6.
         *
         * This should match or exceed the value configured in the Firebase Authentication password
         * policy settings. Otherwise, creating an account will fail.
         *
         * Additional rules can be defined with [passwordValidationRules].
         */
        val minimumPasswordLength: Int = 6,

        /**
         * A list of password validation rules in addition to [minimumPasswordLength].
         *
         * This should match or exceed the rules configured in the Firebase Authentication password
         * policy settings. Otherwise, creating an account will fail.
         */
        val additionalPasswordValidationRules: List<PasswordRule> = emptyList(),
    ) : AuthProvider(providerId = Provider.EMAIL.id, providerName = Provider.EMAIL.providerName) {

        /**
         * At least a [PasswordRule.MinimumLength] rule using [minimumPasswordLength] and any
         * [additionalPasswordValidationRules].
         */
        val passwordValidationRules: List<PasswordRule> = buildList {
            // Add minimum length rule first to avoid misleading error messages if the password is
            // empty
            add(PasswordRule.MinimumLength(minimumPasswordLength))
            addAll(additionalPasswordValidationRules)
        }

        companion object {
            const val SESSION_ID_LENGTH = 10
        }

        internal fun validate() {
            if (isEmailLinkSignInEnabled) {
                val actionCodeSettings = requireNotNull(emailLinkActionCodeSettings) {
                    "ActionCodeSettings cannot be null when using " +
                            "email link sign in."
                }

                check(actionCodeSettings.canHandleCodeInApp()) {
                    "You must set canHandleCodeInApp in your " +
                            "ActionCodeSettings to true for Email-Link Sign-in."
                }
            }

            check(
                additionalPasswordValidationRules
                    .find { it is PasswordRule.MinimumLength } == null
            ) {
                "Must not add a MinimumLength rule, set minimumPasswordLength instead"
            }
        }

        /**
         * Using this providers [emailLinkActionCodeSettings] builds [ActionCodeSettings] for
         * sending an email link for sign-in.
         */
        internal fun buildActionCodeSettings(
            sessionId: String,
            credentialForLinking: AuthCredential? = null,
        ): ActionCodeSettings {
            requireNotNull(emailLinkActionCodeSettings) {
                "ActionCodeSettings is required for email link sign in"
            }

            val continueUrl = continueUrl(emailLinkActionCodeSettings.url) {
                appendSessionId(sessionId)
                appendForceSameDeviceBit(isEmailLinkForceSameDeviceEnabled)
                // Only append providerId for linking flows (when credentialForLinking is not null)
                if (credentialForLinking != null) {
                    appendProviderId(credentialForLinking.provider)
                }
            }

            return actionCodeSettings {
                url = continueUrl
                handleCodeInApp = emailLinkActionCodeSettings.canHandleCodeInApp()
                iosBundleId = emailLinkActionCodeSettings.iosBundle
                setAndroidPackageName(
                    emailLinkActionCodeSettings.androidPackageName ?: "",
                    emailLinkActionCodeSettings.androidInstallApp,
                    emailLinkActionCodeSettings.androidMinimumVersion
                )
            }
        }

        // For Sign In With Email Link
        internal fun isDifferentDevice(
            sessionIdFromLocal: String?,
            sessionIdFromLink: String,
        ): Boolean {
            return sessionIdFromLocal == null || sessionIdFromLocal.isEmpty()
                    || sessionIdFromLink.isEmpty()
                    || (sessionIdFromLink != sessionIdFromLocal)
        }

        private fun continueUrl(continueUrl: String, block: ContinueUrlBuilder.() -> Unit) =
            ContinueUrlBuilder(continueUrl).apply(block).build()

    }

    /**
     * Google Sign-In provider configuration.
     */
    class Google(
        /**
         * The list of scopes to request.
         */
        override val scopes: List<String>,

        /**
         * The OAuth 2.0 client ID for your server.
         *
         * When using the google-services plugin, this is the default_web_client_id string.
         */
        val serverClientId: String,

        /**
         * Whether to filter by authorized accounts.
         * When true, only shows Google accounts that have previously authorized this app.
         * Defaults to true, with automatic fallback to false if no authorized accounts found.
         */
        val filterByAuthorizedAccounts: Boolean = true,

        /**
         * Whether to enable auto-select for single account scenarios.
         * When true, automatically selects the account if only one is available.
         * Defaults to false for better user control.
         */
        val autoSelectEnabled: Boolean = false,

        /**
         * A map of custom OAuth parameters.
         */
        override val customParameters: Map<String, String> = emptyMap(),
    ) : OAuth(
        providerId = Provider.GOOGLE.id,
        providerName = Provider.GOOGLE.providerName,
        scopes = scopes,
        customParameters = customParameters
    ) {
        internal fun validate() {
            require(serverClientId.isNotBlank()) {
                "Server client ID cannot be blank."
            }

            val hasEmailScope = scopes.contains("email")
            if (!hasEmailScope) {
                Timber.w("The scopes do not include 'email'. In most cases this is a mistake!")
            }
        }

        /**
         * Result container for Google Sign-In credential flow.
         * @suppress
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class GoogleSignInResult(
            val credential: AuthCredential,
            val idToken: String,
            val displayName: String?,
            val photoUrl: Uri?,
        )

        /**
         * An interface to wrap the Authorization API for requesting OAuth scopes.
         * @suppress
         */
        internal interface AuthorizationProvider {
            suspend fun authorize(context: Context, scopes: List<Scope>)
        }

        /**
         * The default implementation of [AuthorizationProvider].
         * @suppress
         */
        internal class DefaultAuthorizationProvider : AuthorizationProvider {
            override suspend fun authorize(context: Context, scopes: List<Scope>) {
                // https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/Identity
                // https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/AuthorizationClient

                val authorizationRequest = AuthorizationRequest.builder()
                    .setRequestedScopes(scopes)
                    .build()

                Identity.getAuthorizationClient(context)
                    .authorize(authorizationRequest)
                    .await()
            }
        }

        /**
         * An interface to wrap the Credential Manager flow for Google Sign-In.
         * @suppress
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        interface CredentialManagerProvider {

            /**
             * @throws GetCredentialException
             * @throws com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
             * @see CredentialManager.getCredential
             */
            suspend fun getGoogleCredential(
                context: Context,
                credentialManager: CredentialManager,
                serverClientId: String,
                filterByAuthorizedAccounts: Boolean,
                autoSelectEnabled: Boolean,
            ): GoogleSignInResult

            suspend fun clearCredentialState(credentialManager: CredentialManager)
        }

        /**
         * The default implementation of [CredentialManagerProvider].
         * @suppress
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class DefaultCredentialManagerProvider : CredentialManagerProvider {

            override suspend fun getGoogleCredential(
                context: Context,
                credentialManager: CredentialManager,
                serverClientId: String,
                filterByAuthorizedAccounts: Boolean,
                autoSelectEnabled: Boolean,
            ): GoogleSignInResult {
                // https://developers.google.com/identity/android-credential-manager/android/reference/com/google/android/libraries/identity/googleid/GetGoogleIdOption.Builder
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId(serverClientId)
                    .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
                    .setAutoSelectEnabled(autoSelectEnabled)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)

                // According to https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation#create-shared
                // createFrom may throw GoogleIdTokenParsingException if the googleid library is
                // incompatible.
                val googleIdTokenCredential =
                    GoogleIdTokenCredential.createFrom(result.credential.data)
                val firebaseCredential =
                    GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

                return GoogleSignInResult(
                    credential = firebaseCredential,
                    idToken = googleIdTokenCredential.idToken,
                    displayName = googleIdTokenCredential.displayName,
                    photoUrl = googleIdTokenCredential.profilePictureUri,
                )
            }

            override suspend fun clearCredentialState(credentialManager: CredentialManager) {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            }
        }
    }

    /**
     * A generic OAuth provider for any unsupported provider.
     */
    class GenericOAuth(
        /**
         * The provider name.
         */
        override val providerName: String,

        /**
         * The provider ID as configured in the Firebase console.
         */
        override val providerId: String,

        /**
         * The list of scopes to request.
         */
        override val scopes: List<String>,

        /**
         * A map of custom OAuth parameters.
         */
        override val customParameters: Map<String, String>,

        /**
         * The text to display on the provider button.
         */
        val buttonLabel: String,

        /**
         * An optional icon for the provider button.
         */
        val buttonIcon: AuthUIAsset?,

        /**
         * An optional background color for the provider button.
         */
        val buttonColor: Color?,

        /**
         * An optional content color for the provider button.
         */
        val contentColor: Color?,
    ) : OAuth(
        providerId = providerId,
        providerName = providerName,
        scopes = scopes,
        customParameters = customParameters
    ) {
        internal fun validate() {
            require(providerId.isNotBlank()) {
                "Provider ID cannot be null or empty"
            }

            require(buttonLabel.isNotBlank()) {
                "Button label cannot be null or empty"
            }
        }
    }

    companion object {

        /**
         * Merges profile information (display name and photo URL) with the current user's profile.
         *
         * This method updates the user's profile only if the current profile is incomplete
         * (missing display name or photo URL). This prevents overwriting existing profile data.
         *
         * **Use case:**
         * After creating a new user account or linking credentials, update the profile with
         * information from the sign-up form or social provider.
         *
         * @param auth The [FirebaseAuth] instance
         * @param displayName The display name to set (if current is empty)
         * @param photoUri The photo URL to set (if current is null)
         *
         * **Note:** This operation always succeeds to minimize login interruptions.
         * Failures are logged but don't prevent sign-in completion.
         */
        internal suspend fun mergeProfile(
            auth: FirebaseAuth,
            displayName: String?,
            photoUri: Uri?,
        ) {
            try {
                val currentUser = auth.currentUser ?: return

                // Only update if current profile is incomplete
                val currentDisplayName = currentUser.displayName
                val currentPhotoUrl = currentUser.photoUrl

                if (!currentDisplayName.isNullOrEmpty() && currentPhotoUrl != null) {
                    // Profile is complete, no need to update
                    return
                }

                // Build profile update with provided values
                val nameToSet =
                    if (currentDisplayName.isNullOrEmpty()) displayName else currentDisplayName
                val photoToSet = currentPhotoUrl ?: photoUri

                if (nameToSet != null || photoToSet != null) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(nameToSet)
                        .setPhotoUri(photoToSet)
                        .build()

                    currentUser.updateProfile(profileUpdates).await()
                }
            } catch (e: Exception) {
                // Log error but don't throw - profile update failure shouldn't prevent sign-in
                Timber.e(e, "Error updating profile")
            }
        }
    }
}
