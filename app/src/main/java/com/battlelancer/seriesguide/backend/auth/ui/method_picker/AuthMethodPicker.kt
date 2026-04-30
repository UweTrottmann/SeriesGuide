// SPDX-License-Identifier: Apache-2.0 AND AGPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.ui.method_picker

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.AuthProvider
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.Provider
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.battlelancer.seriesguide.backend.auth.configuration.theme.AuthUITheme
import com.battlelancer.seriesguide.backend.auth.ui.components.AuthHorizontalDivider
import com.battlelancer.seriesguide.backend.auth.ui.components.AuthProviderButton
import com.battlelancer.seriesguide.backend.auth.ui.components.AuthTopAppBar
import com.battlelancer.seriesguide.backend.auth.util.SignInPreferenceManager.SignInPreference
import com.battlelancer.seriesguide.util.ThemeUtils.plus

/**
 * Renders the provider selection screen.
 *
 * @param providers The list of providers to display.
 * @param logo An optional logo to display.
 * @param onNavigateBack When the back navigation icon was selected.
 * @param onProviderSelected A callback when a provider is selected.
 * @param privacyPolicyUrl The URL for the Privacy Policy.
 * @param lastSignInPreference The last sign-in preference to show a "Continue as..." button.
 */
@Composable
fun AuthMethodPicker(
    providers: List<AuthProvider>,
    logo: Int? = null,
    onNavigateBack: () -> Unit,
    onProviderSelected: (AuthProvider, SignInPreference?) -> Unit,
    privacyPolicyUrl: String? = null,
    lastSignInPreference: SignInPreference? = null,
) {
    val context = LocalContext.current
    val stringProvider = LocalAuthUIStringProvider.current

    Scaffold(
        topBar = {
            AuthTopAppBar(
                title = stringProvider.methodPickerTitle,
                onNavigateBack = onNavigateBack
            )
        }
    ) { contentPadding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            // If wider than 300 dp center align, use padding so whole screen remains scrollable
            val maxContentWidth = 300.dp
            val defaultContentPadding = 16.dp
            val contentCenteredPadding =
                if (maxWidth > defaultContentPadding + maxContentWidth + defaultContentPadding) {
                    val horizontalPadding = (maxWidth - maxContentWidth) / 2
                    PaddingValues(horizontal = horizontalPadding, vertical = defaultContentPadding)
                } else {
                    PaddingValues(defaultContentPadding)
                }

            LazyColumn(
                contentPadding = contentPadding + contentCenteredPadding
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 24.dp)
                            .fillMaxWidth()
                    ) {
                        Image(
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.CenterHorizontally),
                            painter = painterResource(
                                logo ?: R.drawable.ic_account_circle_control_24dp
                            ),
                            contentDescription = null /* Title is below */
                        )
                        Text(
                            text = stringProvider.methodPickerDescription,
                            modifier = Modifier.padding(top = 16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        AnnotatedStringResource(
                            modifier = Modifier.padding(top = 16.dp),
                            context = context,
                            template = stringProvider.privacyPolicyMessage(
                                privacyPolicyLabel = stringProvider.privacyPolicy
                            ),
                            links = arrayOf(
                                stringProvider.privacyPolicy to (privacyPolicyUrl ?: "")
                            )
                        )
                    }
                }

                // Show "Continue with..." button if last sign-in preference exists
                lastSignInPreference?.let { preference ->
                    val lastProvider = providers.find { it.providerId == preference.providerId }
                    if (lastProvider != null) {
                        item {
                            ContinueAsButton(
                                provider = lastProvider,
                                identifier = preference.identifier,
                                onClick = { onProviderSelected(lastProvider, preference) }
                            )

                            AuthHorizontalDivider()
                        }
                    }
                }

                // Show all providers
                itemsIndexed(providers) { _, provider ->
                    Box(
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                    ) {
                        AuthProviderButton(
                            modifier = Modifier
                                .fillMaxWidth(),
                            onClick = {
                                onProviderSelected(provider, null)
                            },
                            provider = provider,
                            stringProvider = LocalAuthUIStringProvider.current
                        )
                    }
                }
            }
        }
    }
}

/**
 * A prominent "Continue as..." button that shows the last-used provider and identifier.
 *
 * @param provider The authentication provider
 * @param identifier The user identifier (email, phone number, etc.)
 * @param onClick Callback when the button is clicked
 */
@Composable
private fun ContinueAsButton(
    provider: AuthProvider,
    identifier: String?,
    onClick: () -> Unit
) {
    val stringProvider = LocalAuthUIStringProvider.current

    AuthProviderButton(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ContinueAsButton"),
        onClick = onClick,
        provider = provider,
        stringProvider = stringProvider,
        subtitle = identifier,
        showAsContinue = true
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAuthMethodPicker() {
    val applicationContext = LocalContext.current
    val stringProvider = DefaultAuthUIStringProvider(applicationContext)

    AuthUITheme {
        CompositionLocalProvider(
            LocalAuthUIStringProvider provides stringProvider
        ) {
            AuthMethodPicker(
                providers = listOf(
                    AuthProvider.Email(
                        emailLinkActionCodeSettings = null
                    ),
                    AuthProvider.Google(
                        serverClientId = "EXAMPLE"
                    )
                ),
                onNavigateBack = {},
                onProviderSelected = { _, _ -> },
                privacyPolicyUrl = "https://app.example/privacy",
                lastSignInPreference = SignInPreference(
                    providerId = Provider.EMAIL.id,
                    identifier = "someone@domain.example",
                    timestamp = 0
                )
            )
        }
    }
}