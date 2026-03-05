// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import com.battlelancer.seriesguide.backend.auth.configuration.MfaConfiguration
import com.battlelancer.seriesguide.backend.auth.configuration.MfaFactor
import com.battlelancer.seriesguide.backend.auth.mfa.MfaEnrollmentContentState
import com.battlelancer.seriesguide.backend.auth.mfa.MfaEnrollmentStep
import com.battlelancer.seriesguide.backend.auth.mfa.TotpEnrollmentHandler
import com.battlelancer.seriesguide.backend.auth.mfa.TotpSecret
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A stateful composable that manages the Multi-Factor Authentication (MFA) enrollment flow.
 *
 * This screen handles all steps of MFA enrollment including factor selection, configuration,
 * verification, and recovery code display. It uses the provided handlers to communicate with
 * Firebase Authentication and exposes state through a content slot for custom UI rendering.
 *
 * **Enrollment Flow:**
 * 1. **SelectFactor** - User chooses between SMS or TOTP
 * 2. **ConfigureSms** or **ConfigureTotp** - User sets up their chosen factor
 * 3. **VerifyFactor** - User verifies with a code
 * 4. **ShowRecoveryCodes** - (Optional) User receives backup codes
 *
 * @param user The currently authenticated [FirebaseUser] to enroll in MFA
 * @param auth The [FirebaseAuth] instance
 * @param configuration MFA configuration controlling available factors and behavior
 * @param onComplete Callback invoked when enrollment completes successfully
 * @param onSkip Callback invoked when user skips enrollment (only if not required)
 * @param onError Callback invoked when an error occurs during enrollment
 * @param content A composable lambda that receives [MfaEnrollmentContentState] to render custom UI
 *
 * @since 10.0.0
 */
@Composable
fun MfaEnrollmentScreen(
    user: FirebaseUser,
    auth: FirebaseAuth,
    configuration: MfaConfiguration,
    onComplete: () -> Unit,
    onSkip: () -> Unit = {},
    onError: (Exception) -> Unit = {},
    content: @Composable ((MfaEnrollmentContentState) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()

    val totpHandler = remember(auth, user) { TotpEnrollmentHandler(auth, user) }

    val currentStep = rememberSaveable { mutableStateOf(MfaEnrollmentStep.SelectFactor) }
    val selectedFactor = rememberSaveable { mutableStateOf<MfaFactor?>(null) }
    val isLoading = remember { mutableStateOf(false) }
    val error = remember { mutableStateOf<String?>(null) }
    val lastException = remember { mutableStateOf<Exception?>(null) }
    val enrolledFactors = remember { mutableStateOf(user.multiFactor.enrolledFactors) }

    val totpSecret = remember { mutableStateOf<TotpSecret?>(null) }
    val totpQrCodeUrl = remember { mutableStateOf<String?>(null) }

    val verificationCode = rememberSaveable { mutableStateOf("") }

    val recoveryCodes = remember { mutableStateOf<List<String>?>(null) }

    val resendTimerSeconds = rememberSaveable { mutableIntStateOf(0) }

    // Handle resend timer countdown
    LaunchedEffect(resendTimerSeconds.intValue) {
        if (resendTimerSeconds.intValue > 0) {
            delay(1000)
            resendTimerSeconds.intValue--
        }
    }

    LaunchedEffect(Unit) {
        if (configuration.allowedFactors.size == 1) {
            selectedFactor.value = configuration.allowedFactors.first()
            when (selectedFactor.value) {
                MfaFactor.Totp -> {
                    currentStep.value = MfaEnrollmentStep.ConfigureTotp
                    isLoading.value = true
                    try {
                        val secret = totpHandler.generateSecret()
                        totpSecret.value = secret
                        totpQrCodeUrl.value = secret.generateQrCodeUrl(
                            accountName = user.email ?: user.phoneNumber ?: "User",
                            issuer = auth.app.name
                        )
                        error.value = null
                        lastException.value = null
                    } catch (e: Exception) {
                        error.value = e.message
                        lastException.value = e
                        onError(e)
                    } finally {
                        isLoading.value = false
                    }
                }
                null -> {}
            }
        }
    }

    val state = MfaEnrollmentContentState(
        step = currentStep.value,
        isLoading = isLoading.value,
        error = error.value,
        exception = lastException.value,
        onBackClick = {
            when (currentStep.value) {
                MfaEnrollmentStep.SelectFactor -> {}
                MfaEnrollmentStep.ConfigureTotp -> {
                    currentStep.value = MfaEnrollmentStep.SelectFactor
                    selectedFactor.value = null
                    totpSecret.value = null
                    totpQrCodeUrl.value = null
                }
                MfaEnrollmentStep.VerifyFactor -> {
                    verificationCode.value = ""
                    when (selectedFactor.value) {
                        MfaFactor.Totp -> currentStep.value = MfaEnrollmentStep.ConfigureTotp
                        null -> currentStep.value = MfaEnrollmentStep.SelectFactor
                    }
                }
                MfaEnrollmentStep.ShowRecoveryCodes -> {
                    currentStep.value = MfaEnrollmentStep.VerifyFactor
                }
            }
            error.value = null
            lastException.value = null
        },
        availableFactors = configuration.allowedFactors,
        enrolledFactors = enrolledFactors.value,
        onFactorSelected = { factor ->
            selectedFactor.value = factor
            when (factor) {
                MfaFactor.Totp -> {
                    currentStep.value = MfaEnrollmentStep.ConfigureTotp
                    coroutineScope.launch {
                        isLoading.value = true
                        try {
                            val secret = totpHandler.generateSecret()
                            totpSecret.value = secret
                            totpQrCodeUrl.value = secret.generateQrCodeUrl(
                                accountName = user.email ?: user.phoneNumber ?: "User",
                                issuer = auth.app.name
                            )
                            error.value = null
                            lastException.value = null
                        } catch (e: Exception) {
                            error.value = e.message
                            lastException.value = e
                            onError(e)
                        } finally {
                            isLoading.value = false
                        }
                    }
                }
            }
        },
        onUnenrollFactor = { factorInfo ->
            coroutineScope.launch {
                isLoading.value = true
                try {
                    user.multiFactor.unenroll(factorInfo).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Refresh the enrolled factors list
                            enrolledFactors.value = user.multiFactor.enrolledFactors
                            error.value = null
                        } else {
                            error.value = task.exception?.message
                            task.exception?.let {
                                lastException.value = it
                                onError(it)
                            }
                        }
                        isLoading.value = false
                    }
                } catch (e: Exception) {
                    error.value = e.message
                    lastException.value = e
                    onError(e)
                    isLoading.value = false
                }
            }
        },
        onSkipClick = if (!configuration.requireEnrollment) {
            { onSkip() }
        } else null,
        totpSecret = totpSecret.value,
        totpQrCodeUrl = totpQrCodeUrl.value,
        onContinueToVerifyClick = {
            currentStep.value = MfaEnrollmentStep.VerifyFactor
        },
        verificationCode = verificationCode.value,
        onVerificationCodeChange = { code ->
            verificationCode.value = code
            error.value = null
        },
        onVerifyClick = {
            coroutineScope.launch {
                isLoading.value = true
                try {
                    when (selectedFactor.value) {
                        MfaFactor.Totp -> {
                            val secret = totpSecret.value
                            if (secret != null) {
                                totpHandler.enrollWithVerificationCode(
                                    totpSecret = secret,
                                    verificationCode = verificationCode.value,
                                    displayName = "Authenticator App"
                                )
                            } else {
                                throw IllegalStateException("No TOTP secret available")
                            }
                        }
                        null -> throw IllegalStateException("No factor selected")
                    }

                    // Refresh enrolled factors after successful enrollment
                    enrolledFactors.value = user.multiFactor.enrolledFactors

                    if (configuration.enableRecoveryCodes) {
                        recoveryCodes.value = generateRecoveryCodes()
                        currentStep.value = MfaEnrollmentStep.ShowRecoveryCodes
                    } else {
                        onComplete()
                    }
                    error.value = null
                    lastException.value = null
                } catch (e: Exception) {
                    error.value = e.message
                    lastException.value = e
                    onError(e)
                } finally {
                    isLoading.value = false
                }
            }
        },
        selectedFactor = selectedFactor.value,
        resendTimer = resendTimerSeconds.intValue,
        recoveryCodes = recoveryCodes.value,
        onCodesSavedClick = {
            onComplete()
        }
    )

    if (content != null) {
        content(state)
    } else {
        DefaultMfaEnrollmentContent(
            state = state,
            user = user
        )
    }
}

/**
 * Generates placeholder recovery codes.
 * In a production implementation, these would come from Firebase or a backend service.
 */
private fun generateRecoveryCodes(): List<String> {
    return List(10) { index ->
        List(4) { (0..9).random() }
            .joinToString("")
            .let { if (index % 2 == 0) "$it-${(1000..9999).random()}" else it }
    }
}
