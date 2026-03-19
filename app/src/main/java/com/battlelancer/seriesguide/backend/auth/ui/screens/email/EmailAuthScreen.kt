// SPDX-License-Identifier: Apache-2.0 AND AGPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.ui.screens.email

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import com.battlelancer.seriesguide.backend.auth.AuthException
import com.battlelancer.seriesguide.backend.auth.AuthState
import com.battlelancer.seriesguide.backend.auth.FirebaseAuthUI
import com.battlelancer.seriesguide.backend.auth.configuration.AuthUIConfiguration
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.AuthProvider
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.createOrLinkUserWithEmailAndPassword
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.sendPasswordResetEmail
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.sendSignInLinkToEmail
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.signInWithEmailAndPassword
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.signInWithEmailLink
import com.battlelancer.seriesguide.backend.auth.util.SignInPreferenceManager
import com.google.firebase.auth.AuthCredential
import kotlinx.coroutines.launch

enum class EmailAuthMode {
    SignIn,
    EmailLinkSignIn,
    SignUp,
    ResetPassword,
}

/**
 * A class passed to the content slot, containing all the necessary information to render custom
 * UIs for sign-in, sign-up, and password reset flows.
 *
 * @param mode An enum representing the current UI mode. Use a when expression on this to render
 * the correct screen.
 * @param isLoading true when an asynchronous operation (like signing in or sending an email)
 * is in progress.
 * @param error An optional error message to display to the user.
 * @param email The current value of the email input field.
 * @param onEmailChange (Modes: [EmailAuthMode.SignIn], [EmailAuthMode.SignUp],
 * [EmailAuthMode.ResetPassword]) A callback to be invoked when the email input changes.
 * @param password An optional custom layout composable for the provider buttons.
 * @param onPasswordChange (Modes: [EmailAuthMode.SignIn], [EmailAuthMode.SignUp]) The current
 * value of the password input field.
 * @param confirmPassword (Mode: [EmailAuthMode.SignUp]) A callback to be invoked when the password
 * input changes.
 * @param onConfirmPasswordChange (Mode: [EmailAuthMode.SignUp]) A callback to be invoked when
 * the password confirmation input changes.
 * @param displayName (Mode: [EmailAuthMode.SignUp]) The current value of the display name field.
 * @param onDisplayNameChange (Mode: [EmailAuthMode.SignUp]) A callback to be invoked when the
 * display name input changes.
 * @param onSignInClick (Mode: [EmailAuthMode.SignIn]) A callback to be invoked to attempt a
 * sign-in with the provided credentials.
 * @param onSignUpClick (Mode: [EmailAuthMode.SignUp]) A callback to be invoked to attempt to
 * create a new account.
 * @param onSendResetLinkClick (Mode: [EmailAuthMode.ResetPassword]) A callback to be invoked to
 * send a password reset email.
 * @param resetLinkSent (Mode: [EmailAuthMode.ResetPassword]) true after the password reset link
 * has been successfully sent.
 * @param emailSignInLinkSent (Mode: [EmailAuthMode.SignIn]) true after the email sign in link has
 * been successfully sent.
 * @param onGoToSignUp A callback to switch the UI to the SignUp mode.
 * @param onGoToSignIn A callback to switch the UI to the SignIn mode.
 * @param onGoToResetPassword A callback to switch the UI to the ResetPassword mode.
 */
class EmailAuthContentState(
    val mode: EmailAuthMode,
    val isLoading: Boolean = false,
    val error: String? = null,
    val email: String,
    val onEmailChange: (String) -> Unit,
    val password: String,
    val onPasswordChange: (String) -> Unit,
    val confirmPassword: String,
    val onConfirmPasswordChange: (String) -> Unit,
    val displayName: String,
    val onDisplayNameChange: (String) -> Unit,
    val onRetrievedCredential: (Pair<String, String>) -> Unit,
    val onSignInClick: () -> Unit,
    val onSignInEmailLinkClick: () -> Unit,
    val onSignUpClick: () -> Unit,
    val onSendResetLinkClick: () -> Unit,
    val resetLinkSent: Boolean = false,
    val emailSignInLinkSent: Boolean = false,
    val onGoToSignUp: () -> Unit,
    val onGoToSignIn: () -> Unit,
    val onGoToResetPassword: () -> Unit,
    val onGoToEmailLinkSignIn: () -> Unit,
)

/**
 * A stateful composable that manages the logic for all email-based authentication flows,
 * including sign-in, sign-up, and password reset. It exposes the state for the current mode to
 * a custom UI via a trailing lambda (slot), allowing for complete visual customization.
 *
 * @param configuration
 * @param signInPreference A sign-in preference to pre-populate the email address with
 * @param onSuccess
 * @param onError
 * @param onCancel
 * @param content
 */
@Composable
fun EmailAuthScreen(
    context: Context,
    configuration: AuthUIConfiguration,
    authUI: FirebaseAuthUI,
    mode: EmailAuthMode?,
    changeMode: (EmailAuthMode) -> Unit,
    signInPreference: SignInPreferenceManager.SignInPreference? = null,
    credentialForLinking: AuthCredential? = null,
    emailLinkFromDifferentDevice: String? = null,
    onSuccess: () -> Unit,
    onError: (AuthException) -> Unit,
    onCancel: () -> Unit,
    content: @Composable ((EmailAuthContentState) -> Unit)? = null,
) {
    val provider = configuration.providers.filterIsInstance<AuthProvider.Email>().first()
    val coroutineScope = rememberCoroutineScope()

    // Start in EmailLinkSignIn mode if coming from cross-device flow
    val safeMode = mode
        ?: if (emailLinkFromDifferentDevice != null && provider.isEmailLinkSignInEnabled) {
            EmailAuthMode.EmailLinkSignIn
        } else {
            EmailAuthMode.SignIn
        }
    val displayNameValue = rememberSaveable { mutableStateOf("") }
    // The user has chosen to continue with a specific email address, so pre-populate it. Note that
    // it might get overwritten with a value retrieved from credential manager in SignInUI.
    val emailTextValue = rememberSaveable { mutableStateOf(signInPreference?.identifier.orEmpty()) }
    val passwordTextValue = rememberSaveable { mutableStateOf("") }
    val confirmPasswordTextValue = rememberSaveable { mutableStateOf("") }

    val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
    val isLoading = authState is AuthState.Loading
    val authCredentialForLinking = remember { credentialForLinking }
    val errorMessage =
        if (authState is AuthState.Error) (authState as AuthState.Error).exception.message else null
    val resetLinkSent = authState is AuthState.PasswordResetLinkSent
    val emailSignInLinkSent = authState is AuthState.EmailSignInLinkSent

    // Track if credentials were retrieved from Credential Manager
    val retrievedCredential = remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(authState) {
        Log.d("EmailAuthScreen", "Current state: $authState")
        when (val state = authState) {
            is AuthState.Success -> {
                onSuccess()
            }

            is AuthState.Error -> {
                val exception = AuthException.from(state.exception)
                onError(exception)
            }

            is AuthState.Cancelled -> {
                onCancel()
            }

            else -> Unit
        }
    }

    val state = EmailAuthContentState(
        mode = safeMode,
        displayName = displayNameValue.value,
        email = emailTextValue.value,
        password = passwordTextValue.value,
        confirmPassword = confirmPasswordTextValue.value,
        isLoading = isLoading,
        error = errorMessage,
        resetLinkSent = resetLinkSent,
        emailSignInLinkSent = emailSignInLinkSent,
        onEmailChange = { email ->
            emailTextValue.value = email
        },
        onPasswordChange = { password ->
            passwordTextValue.value = password
        },
        onConfirmPasswordChange = { confirmPassword ->
            confirmPasswordTextValue.value = confirmPassword
        },
        onDisplayNameChange = { displayName ->
            displayNameValue.value = displayName
        },
        onRetrievedCredential = { credential ->
            retrievedCredential.value = credential
        },
        onSignInClick = {
            coroutineScope.launch {
                try {
                    // Check if user is signing in with retrieved credentials
                    val isUsingRetrievedCredential =
                        retrievedCredential.value?.let { (email, password) ->
                            email == emailTextValue.value && password == passwordTextValue.value
                        } ?: false

                    authUI.signInWithEmailAndPassword(
                        context = context,
                        config = configuration,
                        provider = provider,
                        email = emailTextValue.value,
                        password = passwordTextValue.value,
                        credentialForLinking = authCredentialForLinking,
                        skipCredentialSave = isUsingRetrievedCredential
                    )
                } catch (e: Exception) {
                    onError(AuthException.from(e))
                }
            }
        },
        onSignInEmailLinkClick = {
            coroutineScope.launch {
                try {
                    if (emailLinkFromDifferentDevice != null) {
                        authUI.signInWithEmailLink(
                            context = context,
                            config = configuration,
                            provider = provider,
                            email = emailTextValue.value,
                            emailLink = emailLinkFromDifferentDevice,
                        )
                    } else {
                        authUI.sendSignInLinkToEmail(
                            context = context,
                            config = configuration,
                            provider = provider,
                            email = emailTextValue.value,
                            credentialForLinking = authCredentialForLinking,
                        )
                    }
                } catch (e: Exception) {
                    onError(AuthException.from(e))
                }
            }
        },
        onSignUpClick = {
            coroutineScope.launch {
                try {
                    authUI.createOrLinkUserWithEmailAndPassword(
                        context = context,
                        config = configuration,
                        provider = provider,
                        name = displayNameValue.value,
                        email = emailTextValue.value,
                        password = passwordTextValue.value,
                    )
                } catch (e: Exception) {

                }
            }
        },
        onSendResetLinkClick = {
            coroutineScope.launch {
                try {
                    authUI.sendPasswordResetEmail(
                        email = emailTextValue.value,
                        actionCodeSettings = configuration.passwordResetActionCodeSettings,
                    )
                } catch (e: Exception) {

                }
            }
        },
        onGoToSignUp = {
            changeMode(EmailAuthMode.SignUp)
        },
        onGoToSignIn = {
            changeMode(EmailAuthMode.SignIn)
        },
        onGoToResetPassword = {
            // Password must be incorrect, so clear it
            passwordTextValue.value = ""
            changeMode(EmailAuthMode.ResetPassword)
        },
        onGoToEmailLinkSignIn = {
            changeMode(EmailAuthMode.EmailLinkSignIn)
        },
    )

    if (content != null) {
        content(state)
    } else {
        DefaultEmailAuthContent(
            configuration = configuration,
            state = state,
            onCancel = onCancel
        )
    }
}

@Composable
private fun DefaultEmailAuthContent(
    configuration: AuthUIConfiguration,
    state: EmailAuthContentState,
    onCancel: () -> Unit,
) {
    when (state.mode) {
        EmailAuthMode.SignIn -> {
            SignInUI(
                configuration = configuration,
                email = state.email,
                isLoading = state.isLoading,
                password = state.password,
                onEmailChange = state.onEmailChange,
                onPasswordChange = state.onPasswordChange,
                onRetrievedCredential = state.onRetrievedCredential,
                onSignInClick = state.onSignInClick,
                onGoToSignUp = state.onGoToSignUp,
                onGoToResetPassword = state.onGoToResetPassword,
                onGoToEmailLinkSignIn = state.onGoToEmailLinkSignIn,
                onNavigateBack = onCancel
            )
        }

        EmailAuthMode.EmailLinkSignIn -> {
            SignInEmailLinkUI(
                configuration = configuration,
                email = state.email,
                isLoading = state.isLoading,
                emailSignInLinkSent = state.emailSignInLinkSent,
                onEmailChange = state.onEmailChange,
                onSignInWithEmailLink = state.onSignInEmailLinkClick,
                onGoToSignIn = state.onGoToSignIn,
                onGoToResetPassword = state.onGoToResetPassword,
                onNavigateBack = onCancel
            )
        }

        EmailAuthMode.SignUp -> {
            SignUpUI(
                configuration = configuration,
                isLoading = state.isLoading,
                displayName = state.displayName,
                email = state.email,
                password = state.password,
                confirmPassword = state.confirmPassword,
                onDisplayNameChange = state.onDisplayNameChange,
                onEmailChange = state.onEmailChange,
                onPasswordChange = state.onPasswordChange,
                onConfirmPasswordChange = state.onConfirmPasswordChange,
                onSignUpClick = state.onSignUpClick,
                onGoToSignIn = state.onGoToSignIn,
                onNavigateBack = onCancel
            )
        }

        EmailAuthMode.ResetPassword -> {
            ResetPasswordUI(
                isLoading = state.isLoading,
                email = state.email,
                resetLinkSent = state.resetLinkSent,
                onEmailChange = state.onEmailChange,
                onSendResetLink = state.onSendResetLinkClick,
                onGoToSignIn = state.onGoToSignIn,
                onNavigateBack = onCancel
            )
        }
    }
}
