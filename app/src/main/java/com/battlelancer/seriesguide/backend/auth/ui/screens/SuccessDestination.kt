// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.backend.auth.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.backend.auth.AuthState
import com.battlelancer.seriesguide.backend.auth.FirebaseAuthUI
import com.battlelancer.seriesguide.backend.auth.configuration.AuthUIConfiguration
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.AuthUIStringProvider
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.battlelancer.seriesguide.backend.auth.configuration.theme.AuthUITheme

data class AuthSuccessUiContext(
    val authUI: FirebaseAuthUI,
    val stringProvider: AuthUIStringProvider,
    val configuration: AuthUIConfiguration,
    val onSignOut: () -> Unit,
    val onManageMfa: () -> Unit,
    val onSendVerification: () -> Unit,
    /**
     * Callback to reload the signed-in user to check if email is now verified.
     * Should update auth state to either [AuthState.Success] or
     * [AuthState.RequiresEmailVerification].
     */
    val onReloadUser: () -> Unit,
    val onNavigate: (AuthRoute) -> Unit,
)

/**
 * On [AuthState.Success] displays [AuthSuccessContent] with details, MFA management link and sign
 * out action for the contained user.
 *
 * On [AuthState.RequiresEmailVerification] displays [EmailVerificationContent] with actions to
 * verify the contained email address.
 *
 * Otherwise, displays a progress indicator.
 */
@Composable
fun SuccessDestination(
    authState: AuthState,
    uiContext: AuthSuccessUiContext,
) {
    when (authState) {
        is AuthState.Success -> {
            val userIdentifier =
                authState.user.email ?: authState.user.phoneNumber ?: authState.user.uid
            AuthSuccessContent(
                userIdentifier = userIdentifier,
                stringProvider = uiContext.stringProvider,
                showManageMfaAction = uiContext.configuration.isMfaEnabled,
                onManageMfa = uiContext.onManageMfa,
                onSignOut = uiContext.onSignOut
            )
        }

        is AuthState.RequiresEmailVerification -> {
            EmailVerificationContent(
                stringProvider = uiContext.stringProvider,
                onCheckVerificationStatus = uiContext.onReloadUser,
                onSendVerification = uiContext.onSendVerification,
                onSignOut = uiContext.onSignOut,
                email = authState.email
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

@Composable
private fun AuthSuccessContent(
    userIdentifier: String,
    stringProvider: AuthUIStringProvider,
    showManageMfaAction: Boolean,
    onManageMfa: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.CenterHorizontally),
            painter = painterResource(R.drawable.ic_account_circle_control_24dp),
            contentDescription = null /* Title is below */
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (userIdentifier.isNotBlank()) {
            Text(
                text = userIdentifier,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (showManageMfaAction) {
            Button(onClick = onManageMfa) {
                Text(stringProvider.manageMfaAction)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(onClick = onSignOut) {
            Text(stringProvider.signOutAction)
        }
    }
}

@Composable
private fun EmailVerificationContent(
    email: String,
    stringProvider: AuthUIStringProvider,
    onSendVerification: () -> Unit,
    onCheckVerificationStatus: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringProvider.verifyEmailInstruction(email),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSendVerification) {
            Text(stringProvider.sendVerificationEmailAction)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onCheckVerificationStatus) {
            Text(stringProvider.verifiedEmailAction)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onSignOut) {
            Text(stringProvider.signOutAction)
        }
    }
}

@Preview
@Composable
fun AuthSuccessContentPreview() {
    val applicationContext = LocalContext.current
    val stringProvider = DefaultAuthUIStringProvider(applicationContext)

    AuthUITheme {
        CompositionLocalProvider(
            LocalAuthUIStringProvider provides stringProvider
        ) {
            AuthSuccessContent(
                userIdentifier = "user@app.example",
                showManageMfaAction = true,
                stringProvider = stringProvider,
                onSignOut = { },
                onManageMfa = { }
            )
        }
    }
}

@Preview
@Composable
fun EmailVerificationContentPreview() {
    val applicationContext = LocalContext.current
    val stringProvider = DefaultAuthUIStringProvider(applicationContext)

    AuthUITheme {
        CompositionLocalProvider(
            LocalAuthUIStringProvider provides stringProvider
        ) {
            EmailVerificationContent(
                email = "user@app.example",
                stringProvider = stringProvider,
                onSendVerification = { },
                onCheckVerificationStatus = { },
                onSignOut = { }
            )
        }
    }
}
