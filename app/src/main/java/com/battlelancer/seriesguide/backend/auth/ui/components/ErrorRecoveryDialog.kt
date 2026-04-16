// SPDX-License-Identifier: Apache-2.0 AND AGPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import com.battlelancer.seriesguide.backend.auth.AuthException
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.AuthUIStringProvider

/**
 * A composable dialog for displaying authentication errors with recovery options.
 *
 * This dialog provides friendly error messages and actionable recovery suggestions
 * based on the specific [AuthException] type. It integrates with [AuthUIStringProvider]
 * for localization support.
 *
 * @param error The [AuthException] to display recovery information for
 * @param stringProvider The [AuthUIStringProvider] for localized strings
 * @param onDismiss Callback invoked when the user dismisses the dialog
 * @param modifier Optional [Modifier] for the dialog
 * @param onRecover Optional callback for custom recovery actions based on the exception type
 * @param properties Optional [DialogProperties] for dialog configuration
 */
@Composable
fun ErrorRecoveryDialog(
    error: AuthException,
    stringProvider: AuthUIStringProvider,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onRecover: ((AuthException) -> Unit),
    properties: DialogProperties = DialogProperties()
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringProvider.errorDialogTitle,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = getRecoveryMessage(error, stringProvider),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start
            )
        },
        confirmButton = {
            if (isRecoverable(error)) {
                TextButton(
                    onClick = {
                        onRecover.invoke(error)
                    }
                ) {
                    Text(
                        text = getRecoveryActionText(error, stringProvider),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringProvider.dismissAction,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        modifier = modifier,
        properties = properties
    )
}

/**
 * Gets the appropriate recovery message for the given [AuthException].
 *
 * @param error The [AuthException] to get the message for
 * @param stringProvider The [AuthUIStringProvider] for localized strings
 * @return The localized recovery message
 */
private fun getRecoveryMessage(
    error: AuthException,
    stringProvider: AuthUIStringProvider
): String {
    return when (error) {
        is AuthException.NetworkException -> stringProvider.networkErrorRecoveryMessage
        is AuthException.InvalidCredentialsException ->
            stringProvider.invalidCredentialsRecoveryMessage

        is AuthException.UserNotFoundException -> stringProvider.userNotFoundRecoveryMessage
        is AuthException.WeakPasswordException -> {
            // Include specific reason if available
            val baseMessage = stringProvider.weakPasswordRecoveryMessage
            error.reason?.let { reason ->
                "$baseMessage\n\n$reason"
            } ?: baseMessage
        }

        is AuthException.EmailAlreadyInUseException -> stringProvider.emailAlreadyInUseRecoveryMessage

        is AuthException.MfaRequiredException -> stringProvider.mfaRequiredRecoveryMessage
        is AuthException.AccountLinkingRequiredException -> {
            // Use the custom message which includes email and provider details
            error.message ?: stringProvider.accountLinkingRequiredRecoveryMessage
        }

        is AuthException.EmailMismatchException -> stringProvider.emailMismatchMessage
        is AuthException.InvalidEmailLinkException -> stringProvider.emailLinkInvalidLinkMessage
        is AuthException.EmailLinkWrongDeviceException -> stringProvider.emailLinkWrongDeviceMessage

        is AuthException.EmailLinkPromptForEmailException -> stringProvider.emailLinkPromptForEmailMessage
        is AuthException.EmailLinkCrossDeviceLinkingException -> {
            val providerName = error.providerName ?: stringProvider.emailProvider
            stringProvider.emailLinkCrossDeviceLinkingMessage(providerName)
        }

        is AuthException.AuthCancelledException -> stringProvider.authCancelledRecoveryMessage

        is AuthException.AdminRestrictedException -> {
            // AdminRestrictedException currently only by SignUpUI, deletion is handled by
            // RemoveCloudAccountDialogFragment. So this should only occur when trying to create a
            // new account.
            stringProvider.newAccountsDisabled
        }

        is AuthException.UnknownException -> {
            // Use custom message if available (e.g., for configuration errors)
            error.message?.takeIf { it.isNotBlank() } ?: stringProvider.unknownErrorRecoveryMessage
        }

        else -> stringProvider.unknownErrorRecoveryMessage
    }
}

/**
 * Gets the appropriate recovery action text for the given [AuthException].
 *
 * @param error The [AuthException] to get the action text for
 * @param stringProvider The [AuthUIStringProvider] for localized strings
 * @return The localized action text
 */
private fun getRecoveryActionText(
    error: AuthException,
    stringProvider: AuthUIStringProvider
): String {
    return when (error) {
        is AuthException.AuthCancelledException -> error.message ?: stringProvider.continueText

        is AuthException.EmailAlreadyInUseException, // onRecover switches to sign-in mode
        is AuthException.AccountLinkingRequiredException // onRecover navigates to email auth screen
            -> stringProvider.signInDefault

        is AuthException.MfaRequiredException, // Dialog is just dismissed
        is AuthException.EmailLinkPromptForEmailException, // onRecover navigates to email auth screen
        is AuthException.EmailLinkCrossDeviceLinkingException, // onRecover navigates to email auth screen
        is AuthException.EmailLinkWrongDeviceException // Dialog is just dismissed
            -> stringProvider.continueText

        is AuthException.UserNotFoundException // onRecover switches to sign-up mode
            -> stringProvider.signupPageTitle

        else -> stringProvider.retryAction
    }
}

/**
 * If for the given [AuthException] the recover action should be shown.
 * If so, may also want to supply a custom [getRecoveryActionText].
 *
 * @param error The [AuthException] to check
 * @return `true` if the error is recoverable, `false` otherwise
 */
private fun isRecoverable(error: AuthException): Boolean {
    return when (error) {
        is AuthException.NetworkException,
        is AuthException.AuthCancelledException,
        is AuthException.EmailAlreadyInUseException,
        is AuthException.AccountLinkingRequiredException,
        is AuthException.MfaRequiredException,
        is AuthException.EmailLinkPromptForEmailException,
        is AuthException.EmailLinkCrossDeviceLinkingException,
        is AuthException.EmailLinkWrongDeviceException,
        is AuthException.UserNotFoundException,
        is AuthException.UnknownException
            -> true

        else -> false
    }
}
