// SPDX-License-Identifier: Apache-2.0 AND AGPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.ui.screens

import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.battlelancer.seriesguide.backend.auth.AuthException
import com.battlelancer.seriesguide.backend.auth.AuthState
import com.battlelancer.seriesguide.backend.auth.FirebaseAuthUI
import com.battlelancer.seriesguide.backend.auth.configuration.AuthUIConfiguration
import com.battlelancer.seriesguide.backend.auth.configuration.MfaConfiguration
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.AuthProvider
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.rememberGoogleSignInHandler
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.rememberOAuthSignInHandler
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.signInWithEmailLink
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.AuthUIStringProvider
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.battlelancer.seriesguide.backend.auth.configuration.theme.LocalAuthUITheme
import com.battlelancer.seriesguide.backend.auth.ui.components.ErrorRecoveryDialog
import com.battlelancer.seriesguide.backend.auth.ui.method_picker.AuthMethodPicker
import com.battlelancer.seriesguide.backend.auth.ui.screens.email.EmailAuthMode
import com.battlelancer.seriesguide.backend.auth.ui.screens.email.EmailAuthScreen
import com.battlelancer.seriesguide.backend.auth.util.EmailLinkPersistenceManager
import com.battlelancer.seriesguide.backend.auth.util.SignInPreferenceManager
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.MultiFactorResolver
import kotlinx.coroutines.launch

/**
 * High-level authentication screen that wires together provider selection, individual provider
 * flows, error handling, and multi-factor enrollment/challenge flows. Back navigation is driven by
 * the Jetpack Navigation stack so presses behave like native Android navigation.
 *
 * @param authenticatedContent Optional slot that allows callers to render the authenticated
 * state themselves. When provided, it receives the current [AuthState] alongside an
 * [AuthSuccessUiContext] containing common callbacks (sign out, manage MFA, reload user).
 *
 * @since 10.0.0
 */
@Composable
fun FirebaseAuthScreen(
    configuration: AuthUIConfiguration,
    onSignInSuccess: () -> Unit,
    onSignInFailure: (AuthException) -> Unit,
    onSignInCancelled: () -> Unit,
    modifier: Modifier = Modifier,
    authUI: FirebaseAuthUI = FirebaseAuthUI.getInstance(),
    emailLink: String? = null,
    mfaConfiguration: MfaConfiguration = MfaConfiguration(),
    authenticatedContent: (@Composable (state: AuthState, uiContext: AuthSuccessUiContext) -> Unit)? = null,
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val stringProvider = DefaultAuthUIStringProvider(context)
    val navController = rememberNavController()

    val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
    val errorDialogException = remember { mutableStateOf<AuthException?>(null) }
    val lastSuccessfulUserId = remember { mutableStateOf<String?>(null) }
    val pendingLinkingCredential = remember { mutableStateOf<AuthCredential?>(null) }
    val pendingResolver = remember { mutableStateOf<MultiFactorResolver?>(null) }
    // The email screen mode state is remembered here instead of inside EmailAuthScreen as error
    // recovery might have to change it.
    val emailScreenMode = rememberSaveable { mutableStateOf<EmailAuthMode?>(null) }
    val emailLinkFromDifferentDevice = remember { mutableStateOf<String?>(null) }
    val lastSignInPreference =
        remember { mutableStateOf<SignInPreferenceManager.SignInPreference?>(null) }
    val signInPreference =
        remember { mutableStateOf<SignInPreferenceManager.SignInPreference?>(null) }

    // Load last sign-in preference on launch
    LaunchedEffect(authState) {
        lastSignInPreference.value = SignInPreferenceManager.getLastSignIn(context)
    }

    val googleProvider =
        configuration.providers.filterIsInstance<AuthProvider.Google>().firstOrNull()
    val emailProvider = configuration.providers.filterIsInstance<AuthProvider.Email>().firstOrNull()
    val genericOAuthProviders =
        configuration.providers.filterIsInstance<AuthProvider.GenericOAuth>()

    val logoAsset = configuration.logo

    val onSignInWithGoogle = googleProvider?.let {
        authUI.rememberGoogleSignInHandler(
            context = context,
            config = configuration,
            provider = it
        )
    }

    val genericOAuthHandlers = genericOAuthProviders.associateWith {
        authUI.rememberOAuthSignInHandler(
            context = context,
            activity = activity,
            config = configuration,
            provider = it
        )
    }

    CompositionLocalProvider(
        LocalAuthUIStringProvider provides configuration.stringProvider,
        LocalAuthUITheme provides (configuration.theme ?: LocalAuthUITheme.current)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            NavHost(
                navController = navController,
                startDestination = AuthRoute.MethodPicker.route,
                enterTransition = configuration.transitions?.enterTransition ?: {
                    fadeIn(animationSpec = tween(700))
                },
                exitTransition = configuration.transitions?.exitTransition ?: {
                    fadeOut(animationSpec = tween(700))
                },
                popEnterTransition = configuration.transitions?.popEnterTransition ?: {
                    fadeIn(animationSpec = tween(700))
                },
                popExitTransition = configuration.transitions?.popExitTransition ?: {
                    fadeOut(animationSpec = tween(700))
                }
            ) {
                composable(AuthRoute.MethodPicker.route) {
                    Scaffold { innerPadding ->
                        AuthMethodPicker(
                            modifier = modifier
                                .padding(innerPadding),
                            providers = configuration.providers,
                            logo = logoAsset,
                            termsOfServiceUrl = configuration.tosUrl,
                            privacyPolicyUrl = configuration.privacyPolicyUrl,
                            lastSignInPreference = lastSignInPreference.value,
                            onProviderSelected = { provider, signInPref ->
                                when (provider) {
                                    is AuthProvider.Email -> {
                                        signInPreference.value = signInPref
                                        navController.navigate(AuthRoute.Email.route)
                                    }

                                    is AuthProvider.Google -> onSignInWithGoogle?.invoke()

                                    is AuthProvider.GenericOAuth -> genericOAuthHandlers[provider]?.invoke()

                                    else -> {
                                        onSignInFailure(
                                            AuthException.UnknownException(
                                                message = "Provider ${provider.providerId} is not supported in FirebaseAuthScreen",
                                                cause = IllegalArgumentException(
                                                    "Provider ${provider.providerId} is not supported in FirebaseAuthScreen"
                                                )
                                            )
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                composable(AuthRoute.Email.route) {
                    EmailAuthScreen(
                        context = context,
                        configuration = configuration,
                        authUI = authUI,
                        mode = emailScreenMode.value,
                        changeMode = { mode ->
                            emailScreenMode.value = mode
                        },
                        signInPreference = signInPreference.value,
                        credentialForLinking = pendingLinkingCredential.value,
                        emailLinkFromDifferentDevice = emailLinkFromDifferentDevice.value,
                        onSuccess = {
                            pendingLinkingCredential.value = null
                        },
                        onError = { exception ->
                            onSignInFailure(exception)
                        },
                        onCancel = {
                            pendingLinkingCredential.value = null
                            if (!navController.popBackStack()) {
                                navController.navigate(AuthRoute.MethodPicker.route) {
                                    popUpTo(AuthRoute.MethodPicker.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }

                composable(AuthRoute.Success.route) {
                    val uiContext = remember(authState, stringProvider) {
                        AuthSuccessUiContext(
                            authUI = authUI,
                            stringProvider = stringProvider,
                            configuration = configuration,
                            onSignOut = {
                                coroutineScope.launch {
                                    try {
                                        authUI.signOut(context)
                                        // Keep sign-in preference for "Continue as..." on next launch
                                    } catch (e: Exception) {
                                        onSignInFailure(AuthException.from(e))
                                    } finally {
                                        pendingLinkingCredential.value = null
                                        pendingResolver.value = null
                                    }
                                }
                            },
                            onManageMfa = {
                                if (configuration.isMfaEnabled) {
                                    navController.navigate(AuthRoute.MfaEnrollment.route)
                                } else {
                                    val exception = AuthException.AuthCancelledException(
                                        message = "Multi-factor authentication is disabled in the configuration. " +
                                                "Enable MFA in AuthUIConfiguration to use this feature."
                                    )
                                    authUI.updateAuthState(AuthState.Error(exception))
                                }
                            },
// Temporarily don't require email verification, see notes in FirebaseAuthUI
//                            onReloadUser = {
//                                coroutineScope.launch {
//                                    try {
//                                        // Reload user to get fresh data from server
//                                        authUI.getCurrentUser()?.reload()
//                                        authUI.getCurrentUser()?.getIdToken(true)
//
//                                        // Check the user's email verification status after reload
//                                        val user = authUI.getCurrentUser()
//                                        if (user != null) {
//                                            // If email is now verified, transition to Success state
//                                            if (user.isEmailVerified) {
//                                                authUI.updateAuthState(
//                                                    AuthState.Success(
//                                                        result = null,
//                                                        user = user,
//                                                        isNewUser = false
//                                                    )
//                                                )
//                                            } else {
//                                                // Email still not verified, keep showing verification screen
//                                                authUI.updateAuthState(
//                                                    AuthState.RequiresEmailVerification(
//                                                        user = user,
//                                                        email = user.email ?: ""
//                                                    )
//                                                )
//                                            }
//                                        }
//                                    } catch (e: Exception) {
//                                        Log.e("FirebaseAuthScreen", "Failed to refresh user", e)
//                                    }
//                                }
//                            },
                            onNavigate = { route ->
                                navController.navigate(route.route)
                            }
                        )
                    }

                    if (authenticatedContent != null) {
                        authenticatedContent(authState, uiContext)
                    } else {
                        SuccessDestination(
                            authState = authState,
                            stringProvider = stringProvider,
                            configuration = configuration,
                            uiContext = uiContext
                        )
                    }
                }

                composable(AuthRoute.MfaEnrollment.route) {
                    val user = authUI.getCurrentUser()
                    if (user != null) {
                        MfaEnrollmentScreen(
                            user = user,
                            auth = authUI.auth,
                            configuration = mfaConfiguration,
                            onComplete = { navController.popBackStack() },
                            onSkip = { navController.popBackStack() },
                            onError = { exception ->
                                onSignInFailure(AuthException.from(exception))
                            }
                        )
                    } else {
                        navController.popBackStack()
                    }
                }

                composable(AuthRoute.MfaChallenge.route) {
                    val resolver = pendingResolver.value
                    if (resolver != null) {
                        MfaChallengeScreen(
                            resolver = resolver,
                            auth = authUI.auth,
                            onSuccess = {
                                pendingResolver.value = null
                                // Reset auth state to Idle so the firebaseAuthFlow Success state takes over
                                authUI.updateAuthState(AuthState.Idle)
                            },
                            onCancel = {
                                pendingResolver.value = null
                                authUI.updateAuthState(AuthState.Cancelled)
                                navController.popBackStack()
                            },
                            onError = { exception ->
                                onSignInFailure(AuthException.from(exception))
                            }
                        )
                    } else {
                        navController.popBackStack()
                    }
                }
            }

            // Handle email link sign-in (deep links)
            LaunchedEffect(emailLink) {
                if (emailLink != null && emailProvider != null) {
                    try {
                        // Try to retrieve saved email from DataStore (same-device flow)
                        val savedEmail =
                            EmailLinkPersistenceManager.default.retrieveSessionRecord(context)?.email

                        if (savedEmail != null) {
                            // Same device - we have the email, sign in automatically
                            authUI.signInWithEmailLink(
                                context = context,
                                config = configuration,
                                provider = emailProvider,
                                email = savedEmail,
                                emailLink = emailLink
                            )
                        } else {
                            // Different device - no saved email
                            // Call signInWithEmailLink with empty email to trigger validation
                            // This will throw EmailLinkPromptForEmailException or EmailLinkWrongDeviceException
                            authUI.signInWithEmailLink(
                                context = context,
                                config = configuration,
                                provider = emailProvider,
                                email = "", // Empty email triggers cross-device detection
                                emailLink = emailLink
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("FirebaseAuthScreen", "Failed to complete email link sign-in", e)
                    }
                }
            }

            // Synchronise auth state changes with navigation stack.
            LaunchedEffect(authState) {
                val state = authState
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                when (state) {
                    is AuthState.Success -> {
                        pendingResolver.value = null
                        pendingLinkingCredential.value = null

                        if (state.user.uid != lastSuccessfulUserId.value) {
                            // Set before callback in case callback changes state
                            lastSuccessfulUserId.value = state.user.uid
                            onSignInSuccess()

                            // Reload sign-in preference (may have been updated by provider)
                            coroutineScope.launch {
                                lastSignInPreference.value =
                                    SignInPreferenceManager.getLastSignIn(context)
                            }
                        }

                        if (currentRoute != AuthRoute.Success.route) {
                            navController.navigate(AuthRoute.Success.route) {
                                popUpTo(AuthRoute.MethodPicker.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }

                    // Temporarily don't require email verification, see notes in FirebaseAuthUI
//                    is AuthState.RequiresEmailVerification,
                    is AuthState.RequiresProfileCompletion,
                        -> {
                        pendingResolver.value = null
                        pendingLinkingCredential.value = null
                        if (currentRoute != AuthRoute.Success.route) {
                            navController.navigate(AuthRoute.Success.route) {
                                popUpTo(AuthRoute.MethodPicker.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }

                    is AuthState.RequiresMfa -> {
                        pendingResolver.value = state.resolver
                        if (currentRoute != AuthRoute.MfaChallenge.route) {
                            navController.navigate(AuthRoute.MfaChallenge.route) {
                                launchSingleTop = true
                            }
                        }
                    }

                    is AuthState.Cancelled -> {
                        pendingResolver.value = null
                        pendingLinkingCredential.value = null
                        lastSuccessfulUserId.value = null
                        if (currentRoute != AuthRoute.MethodPicker.route) {
                            navController.navigate(AuthRoute.MethodPicker.route) {
                                popUpTo(AuthRoute.MethodPicker.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        onSignInCancelled()
                    }

                    is AuthState.Idle -> {
                        pendingResolver.value = null
                        pendingLinkingCredential.value = null
                        lastSuccessfulUserId.value = null
                        if (currentRoute != AuthRoute.MethodPicker.route) {
                            navController.navigate(AuthRoute.MethodPicker.route) {
                                popUpTo(AuthRoute.MethodPicker.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }

                    else -> Unit
                }
            }

            // Handle errors
            val errorState = authState as? AuthState.Error
            if (errorState != null) {
                // The launch state is only run again if the error changes (or if auth state changed
                // to a non-error state before).
                LaunchedEffect(errorState) {
                    val exception = when (val throwable = errorState.exception) {
                        is AuthException -> throwable
                        else -> AuthException.from(throwable)
                    }

                    // Don't show error dialog if the user has canceled an operation (like a
                    // credentials manager popup).
                    if (exception !is AuthException.AuthCancelledException) {
                        errorDialogException.value = exception
                    }
                }
            }

            // Show error dialog
            val currentErrorException = errorDialogException.value
            if (currentErrorException != null) {
                ErrorRecoveryDialog(
                    error = currentErrorException,
                    stringProvider = stringProvider,
                    onRecover = { exception ->
                        errorDialogException.value = null
                        when (exception) {
                            is AuthException.UserNotFoundException -> {
                                val provider = configuration.providers
                                    .filterIsInstance<AuthProvider.Email>()
                                    .first()
                                if (provider.isNewAccountsAllowed) {
                                    // User not found, but new accounts are allowed, switch
                                    // email screen to sign-up mode.
                                    emailScreenMode.value = EmailAuthMode.SignUp
                                }
                            }

                            is AuthException.EmailAlreadyInUseException -> {
                                // Switch email screen to sign-in mode
                                emailScreenMode.value = EmailAuthMode.SignIn
                            }

                            is AuthException.AccountLinkingRequiredException -> {
                                pendingLinkingCredential.value = exception.credential
                                navController.navigate(AuthRoute.Email.route) {
                                    launchSingleTop = true
                                }
                            }

                            is AuthException.EmailLinkPromptForEmailException -> {
                                // Cross-device flow: User needs to enter their email
                                emailLinkFromDifferentDevice.value = exception.emailLink
                                navController.navigate(AuthRoute.Email.route) {
                                    launchSingleTop = true
                                }
                            }

                            is AuthException.EmailLinkCrossDeviceLinkingException -> {
                                // Cross-device linking flow: User needs to enter email to link provider
                                emailLinkFromDifferentDevice.value = exception.emailLink
                                navController.navigate(AuthRoute.Email.route) {
                                    launchSingleTop = true
                                }
                            }

                            else -> Unit
                        }
                    },
                    onDismiss = {
                        errorDialogException.value = null
                    }
                )
            }

            val loadingState = authState as? AuthState.Loading
            if (loadingState != null) {
                LoadingDialog(loadingState.message ?: stringProvider.progressDialogLoading)
            }
        }
    }
}

sealed class AuthRoute(val route: String) {
    object MethodPicker : AuthRoute("auth_method_picker")
    object Email : AuthRoute("auth_email")
    object Success : AuthRoute("auth_success")
    object MfaEnrollment : AuthRoute("auth_mfa_enrollment")
    object MfaChallenge : AuthRoute("auth_mfa_challenge")
}

data class AuthSuccessUiContext(
    val authUI: FirebaseAuthUI,
    val stringProvider: AuthUIStringProvider,
    val configuration: AuthUIConfiguration,
    val onSignOut: () -> Unit,
    val onManageMfa: () -> Unit,
    // Temporarily don't require email verification, see notes in FirebaseAuthUI
//    val onReloadUser: () -> Unit,
    val onNavigate: (AuthRoute) -> Unit,
)

@Composable
private fun SuccessDestination(
    authState: AuthState,
    stringProvider: AuthUIStringProvider,
    configuration: AuthUIConfiguration,
    uiContext: AuthSuccessUiContext,
) {
    when (authState) {
        is AuthState.Success -> {
            AuthSuccessContent(
                authUI = uiContext.authUI,
                stringProvider = stringProvider,
                configuration = configuration,
                onSignOut = uiContext.onSignOut,
                onManageMfa = uiContext.onManageMfa
            )
        }

        // Temporarily don't require email verification, see notes in FirebaseAuthUI
//        is AuthState.RequiresEmailVerification -> {
//            EmailVerificationContent(
//                authUI = uiContext.authUI,
//                stringProvider = stringProvider,
//                onCheckStatus = uiContext.onReloadUser,
//                onSignOut = uiContext.onSignOut
//            )
//        }

        is AuthState.RequiresProfileCompletion -> {
            ProfileCompletionContent(
                missingFields = authState.missingFields,
                stringProvider = stringProvider
            )
        }

        else -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthSuccessContent(
    authUI: FirebaseAuthUI,
    stringProvider: AuthUIStringProvider,
    configuration: AuthUIConfiguration,
    onSignOut: () -> Unit,
    onManageMfa: () -> Unit,
) {
    val user = authUI.getCurrentUser()
    val userIdentifier = user?.email ?: user?.phoneNumber ?: user?.uid.orEmpty()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (userIdentifier.isNotBlank()) {
            Text(
                text = stringProvider.signedInAs(userIdentifier),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (user != null && authUI.auth.app.options.projectId != null) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Above
                ),
                tooltip = {
                    PlainTooltip {
                        Text(stringProvider.mfaDisabledTooltip)
                    }
                },
                state = rememberTooltipState(
                    initialIsVisible = !configuration.isMfaEnabled
                )
            ) {
                Button(
                    onClick = onManageMfa,
                    enabled = configuration.isMfaEnabled
                ) {
                    Text(stringProvider.manageMfaAction)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(onClick = onSignOut) {
            Text(stringProvider.signOutAction)
        }
    }
}

// Temporarily don't require email verification, see notes in FirebaseAuthUI
//@Composable
//private fun EmailVerificationContent(
//    authUI: FirebaseAuthUI,
//    stringProvider: AuthUIStringProvider,
//    onCheckStatus: () -> Unit,
//    onSignOut: () -> Unit,
//) {
//    val user = authUI.getCurrentUser()
//    val emailLabel = user?.email ?: stringProvider.emailProvider
//    Column(
//        modifier = Modifier.fillMaxSize(),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text(
//            text = stringProvider.verifyEmailInstruction(emailLabel),
//            textAlign = TextAlign.Center,
//            style = MaterialTheme.typography.bodyMedium
//        )
//        Spacer(modifier = Modifier.height(16.dp))
//        Button(onClick = { user?.sendEmailVerification() }) {
//            Text(stringProvider.sendVerificationEmailAction)
//        }
//        Spacer(modifier = Modifier.height(8.dp))
//        Button(onClick = onCheckStatus) {
//            Text(stringProvider.verifiedEmailAction)
//        }
//        Spacer(modifier = Modifier.height(8.dp))
//        Button(onClick = onSignOut) {
//            Text(stringProvider.signOutAction)
//        }
//    }
//}

@Composable
private fun ProfileCompletionContent(
    missingFields: List<String>,
    stringProvider: AuthUIStringProvider,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringProvider.profileCompletionMessage,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (missingFields.isNotEmpty()) {
            Text(
                text = stringProvider.profileMissingFieldsMessage(missingFields.joinToString()),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun LoadingDialog(message: String) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        containerColor = Color.Transparent,
        text = {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
            }
        }
    )
}
