// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.ui.screens.email

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.battlelancer.seriesguide.backend.auth.configuration.theme.AuthUITheme
import com.battlelancer.seriesguide.backend.auth.configuration.validators.EmailValidator
import com.battlelancer.seriesguide.backend.auth.ui.components.AuthEmailTextField
import com.battlelancer.seriesguide.backend.auth.ui.components.AuthTopAppBar
import com.battlelancer.seriesguide.backend.auth.ui.components.BoxWithCenteredColumn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordUI(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    email: String,
    onEmailChange: (String) -> Unit,
    onSendResetLink: () -> Unit,
    isConfirmationDialogVisible: Boolean,
    onConfirmationDialogDismissed: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
) {
    val stringProvider = LocalAuthUIStringProvider.current
    val emailValidator = remember {
        EmailValidator(stringProvider)
    }

    val isFormValid = remember(email) {
        derivedStateOf { emailValidator.validate(email) }
    }

    // Note: currently don't need to remember dialog visible state as on dismissal leaving this
    // screen, see dismiss callback.
    if (isConfirmationDialogVisible) {
        AlertDialog(
            title = {
                Text(
                    text = stringProvider.recoverPasswordLinkSentDialogTitle,
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = stringProvider.recoverPasswordLinkSentDialogBody,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmationDialogDismissed()
                    }
                ) {
                    Text(stringProvider.dismissAction)
                }
            },
            onDismissRequest = {
                onConfirmationDialogDismissed()
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AuthTopAppBar(
                title = stringProvider.resetPasswordAction,
                onNavigateBack = onNavigateBack
            )
        },
    ) { innerPadding ->
        BoxWithCenteredColumn(
            insetPadding = innerPadding
        ) {
            AuthEmailTextField(
                value = email,
                validator = emailValidator,
                enabled = !isLoading,
                onValueChange = { text ->
                    onEmailChange(text)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .align(Alignment.End),
            ) {
                TextButton(
                    onClick = {
                        onConfirmationDialogDismissed()
                    },
                    enabled = !isLoading,
                ) {
                    Text(stringProvider.signInDefault)
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
                        Text(stringProvider.sendButtonText)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewResetPasswordUI() {
    val applicationContext = LocalContext.current
    val stringProvider = DefaultAuthUIStringProvider(applicationContext)

    AuthUITheme {
        CompositionLocalProvider(
            LocalAuthUIStringProvider provides stringProvider
        ) {
            ResetPasswordUI(
                email = "someone@domain.example",
                isLoading = false,
                onEmailChange = { _ -> },
                onSendResetLink = {},
                isConfirmationDialogVisible = true,
                onConfirmationDialogDismissed = {},
            )
        }
    }
}