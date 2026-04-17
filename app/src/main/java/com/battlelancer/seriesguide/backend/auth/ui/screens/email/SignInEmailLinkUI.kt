// SPDX-License-Identifier: Apache-2.0 AND AGPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.ui.screens.email

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.battlelancer.seriesguide.backend.auth.configuration.AuthUIConfiguration
import com.battlelancer.seriesguide.backend.auth.configuration.authUIConfiguration
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.AuthProvider
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.battlelancer.seriesguide.backend.auth.configuration.theme.AuthUITheme
import com.battlelancer.seriesguide.backend.auth.configuration.validators.EmailValidator
import com.battlelancer.seriesguide.backend.auth.ui.components.AuthEmailTextField
import com.battlelancer.seriesguide.backend.auth.ui.components.AuthHorizontalDivider
import com.battlelancer.seriesguide.backend.auth.ui.components.AuthTopAppBar
import com.google.firebase.auth.actionCodeSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInEmailLinkUI(
    modifier: Modifier = Modifier,
    configuration: AuthUIConfiguration,
    isLoading: Boolean,
    emailSignInLinkSent: Boolean,
    email: String,
    onEmailChange: (String) -> Unit,
    onSignInWithEmailLink: () -> Unit,
    onGoToSignIn: () -> Unit,
    onGoToResetPassword: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
) {
    val provider = configuration.providers.filterIsInstance<AuthProvider.Email>().first()
    val stringProvider = LocalAuthUIStringProvider.current
    val emailValidator = remember { EmailValidator(stringProvider) }

    val isFormValid = remember(email) {
        derivedStateOf {
            emailValidator.validate(email)
        }
    }

    if (provider.isEmailLinkSignInEnabled) {
        val isDialogVisible =
            remember(emailSignInLinkSent) { mutableStateOf(emailSignInLinkSent) }

        if (isDialogVisible.value) {
            AlertDialog(
                title = {
                    Text(
                        text = stringProvider.emailSignInLinkSentDialogTitle,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                text = {
                    Text(
                        text = stringProvider.emailSignInLinkSentDialogBody(email),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
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
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AuthTopAppBar(
                title = stringProvider.signInDefault,
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
            AuthEmailTextField(
                value = email,
                validator = emailValidator,
                enabled = !isLoading,
                onValueChange = { text ->
                    onEmailChange(text)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                modifier = Modifier
                    .align(Alignment.Start),
                onClick = {
                    onGoToResetPassword()
                },
                enabled = !isLoading,
                contentPadding = PaddingValues.Zero
            ) {
                Text(
                    modifier = modifier,
                    text = stringProvider.resetPasswordAction,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    textDecoration = TextDecoration.Underline
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    onSignInWithEmailLink()
                },
                modifier = Modifier.align(Alignment.End),
                enabled = !isLoading && isFormValid.value,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(stringProvider.signInDefault)
                }
            }

            // Show toggle to go back to password mode
            AuthHorizontalDivider()
            Button(
                onClick = {
                    onGoToSignIn()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(stringProvider.signInWithPassword)
            }
        }
    }
}

@Preview
@Composable
fun PreviewSignInEmailLinkUI() {
    val applicationContext = LocalContext.current
    val provider = AuthProvider.Email(
        isDisplayNameRequired = true,
        isEmailLinkSignInEnabled = true,
        isEmailLinkForceSameDeviceEnabled = true,
        emailLinkActionCodeSettings = actionCodeSettings {
            url = "https://fake-project-id.firebaseapp.com"
            handleCodeInApp = true
            setAndroidPackageName(
                "fake.project.id",
                true,
                null
            )
        },
        isNewAccountsAllowed = true
    )
    val stringProvider = DefaultAuthUIStringProvider(applicationContext)

    AuthUITheme {
        CompositionLocalProvider(
            LocalAuthUIStringProvider provides stringProvider
        ) {
            SignInEmailLinkUI(
                configuration = authUIConfiguration {
                    context = applicationContext
                    providers { provider(provider) }
                    privacyPolicyUrl = ""
                },
                email = "",
                isLoading = false,
                emailSignInLinkSent = false,
                onEmailChange = { _ -> },
                onSignInWithEmailLink = {},
                onGoToSignIn = {},
                onGoToResetPassword = {},
            )
        }
    }
}
