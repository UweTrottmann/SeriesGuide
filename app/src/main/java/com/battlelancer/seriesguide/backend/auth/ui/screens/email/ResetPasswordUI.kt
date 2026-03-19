// SPDX-License-Identifier: Apache-2.0 AND AGPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.ui.screens.email

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.battlelancer.seriesguide.backend.auth.configuration.AuthUIConfiguration
import com.battlelancer.seriesguide.backend.auth.configuration.authUIConfiguration
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.AuthProvider
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.battlelancer.seriesguide.backend.auth.configuration.theme.AuthUITheme
import com.battlelancer.seriesguide.backend.auth.configuration.validators.EmailValidator
import com.battlelancer.seriesguide.backend.auth.ui.components.AuthTextField
import com.battlelancer.seriesguide.backend.auth.ui.components.AuthTopAppBar
import com.battlelancer.seriesguide.backend.auth.ui.components.TermsAndPrivacyForm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordUI(
    modifier: Modifier = Modifier,
    configuration: AuthUIConfiguration,
    isLoading: Boolean,
    email: String,
    resetLinkSent: Boolean,
    onEmailChange: (String) -> Unit,
    onSendResetLink: () -> Unit,
    onGoToSignIn: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
) {
    val stringProvider = LocalAuthUIStringProvider.current
    val emailValidator = remember {
        EmailValidator(stringProvider)
    }

    val isFormValid = remember(email) {
        derivedStateOf { emailValidator.validate(email) }
    }

    val isDialogVisible = remember(resetLinkSent) { mutableStateOf(resetLinkSent) }

    if (isDialogVisible.value) {
        AlertDialog(
            title = {
                Text(
                    text = stringProvider.recoverPasswordLinkSentDialogTitle,
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = stringProvider.recoverPasswordLinkSentDialogBody(email),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onGoToSignIn()
                        isDialogVisible.value = false
                    }
                ) {
                    Text(stringProvider.dismissAction)
                }
            },
            onDismissRequest = {
                isDialogVisible.value = false
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AuthTopAppBar(
                title = stringProvider.recoverPasswordPageTitle,
                onNavigateBack = onNavigateBack
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .safeDrawingPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            AuthTextField(
                value = email,
                validator = emailValidator,
                enabled = !isLoading,
                label = {
                    Text(stringProvider.emailHint)
                },
                onValueChange = { text ->
                    onEmailChange(text)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .align(Alignment.End),
            ) {
                Button(
                    onClick = {
                        onGoToSignIn()
                    },
                    enabled = !isLoading,
                ) {
                    Text(stringProvider.signInDefault.uppercase())
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = {
                        onSendResetLink()
                    },
                    enabled = !isLoading && isFormValid.value,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp)
                        )
                    } else {
                        Text(stringProvider.sendButtonText.uppercase())
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TermsAndPrivacyForm(
                modifier = Modifier.align(Alignment.End),
                tosUrl = configuration.tosUrl,
                ppUrl = configuration.privacyPolicyUrl,
            )
        }
    }
}

@Preview
@Composable
fun PreviewResetPasswordUI() {
    val applicationContext = LocalContext.current
    val provider = AuthProvider.Email(
        isDisplayNameRequired = true,
        isEmailLinkSignInEnabled = false,
        isEmailLinkForceSameDeviceEnabled = true,
        emailLinkActionCodeSettings = null,
        isNewAccountsAllowed = true,
        minimumPasswordLength = 8,
        passwordValidationRules = listOf()
    )
    val stringProvider = DefaultAuthUIStringProvider(applicationContext)

    AuthUITheme {
        CompositionLocalProvider(
            LocalAuthUIStringProvider provides stringProvider
        ) {
            ResetPasswordUI(
                configuration = authUIConfiguration {
                    context = applicationContext
                    providers { provider(provider) }
                    tosUrl = ""
                    privacyPolicyUrl = ""
                },
                email = "someone@domain.example",
                isLoading = false,
                resetLinkSent = true,
                onEmailChange = { email -> },
                onSendResetLink = {},
                onGoToSignIn = {},
            )
        }
    }
}