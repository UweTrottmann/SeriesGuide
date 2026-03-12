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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.AuthProvider
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.Provider
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.battlelancer.seriesguide.backend.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.battlelancer.seriesguide.backend.auth.configuration.theme.AuthUIAsset
import com.battlelancer.seriesguide.backend.auth.configuration.theme.AuthUITheme
import com.battlelancer.seriesguide.backend.auth.ui.components.AuthProviderButton
import com.battlelancer.seriesguide.backend.auth.util.SignInPreferenceManager.SignInPreference

/**
 * Renders the provider selection screen.
 *
 * **Example usage:**
 * ```kotlin
 * AuthMethodPicker(
 *     providers = listOf(
 *      AuthProvider.Google(),
 *      AuthProvider.Email(),
 *     ),
 *     onProviderSelected = { provider -> /* ... */ }
 * )
 * ```
 *
 * @param modifier A modifier for the screen layout.
 * @param providers The list of providers to display.
 * @param logo An optional logo to display.
 * @param onProviderSelected A callback when a provider is selected.
 * @param customLayout An optional custom layout composable for the provider buttons.
 * @param termsOfServiceUrl The URL for the Terms of Service.
 * @param privacyPolicyUrl The URL for the Privacy Policy.
 * @param lastSignInPreference The last sign-in preference to show a "Continue as..." button.
 *
 * @since 10.0.0
 */
@Composable
fun AuthMethodPicker(
    modifier: Modifier = Modifier,
    providers: List<AuthProvider>,
    logo: AuthUIAsset? = null,
    onProviderSelected: (AuthProvider, SignInPreference?) -> Unit,
    customLayout: @Composable ((List<AuthProvider>, (AuthProvider, SignInPreference?) -> Unit) -> Unit)? = null,
    termsOfServiceUrl: String? = null,
    privacyPolicyUrl: String? = null,
    lastSignInPreference: SignInPreference? = null,
) {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    val stringProvider = LocalAuthUIStringProvider.current

    Column(
        modifier = modifier
    ) {
        Image(
            modifier = Modifier
                .size(48.dp)
                .weight(0.4f)
                .align(Alignment.CenterHorizontally),
            painter = (logo
                ?: AuthUIAsset.Resource(R.drawable.ic_account_circle_control_24dp)).painter,
            contentDescription = if (inPreview) ""
            else stringResource(R.string.fui_auth_method_picker_logo)
        )
        if (customLayout != null) {
            customLayout(providers, onProviderSelected)
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f),
            ) {
                val paddingWidth = maxWidth.value * 0.23
                LazyColumn(
                    modifier = Modifier
                        .padding(horizontal = paddingWidth.dp)
                        .testTag("AuthMethodPicker LazyColumn"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Show "Continue as..." button if last sign-in preference exists
                    lastSignInPreference?.let { preference ->
                        val lastProvider = providers.find { it.providerId == preference.providerId }
                        if (lastProvider != null) {
                            item {
                                ContinueAsButton(
                                    provider = lastProvider,
                                    identifier = preference.identifier,
                                    onClick = { onProviderSelected(lastProvider, preference) }
                                )
                                Spacer(modifier = Modifier.height(24.dp))

                                // Divider with "or"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HorizontalDivider(modifier = Modifier.weight(1f))
                                    Text(
                                        text = stringProvider.orContinueWith,
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    HorizontalDivider(modifier = Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }

                    // Show all providers
                    itemsIndexed(providers) { index, provider ->
                        Box(
                            modifier = Modifier
                                .padding(bottom = if (index < providers.lastIndex) 16.dp else 0.dp)
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
        AnnotatedStringResource(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp),
            context = context,
            inPreview = inPreview,
            previewText = "By continuing, you accept our Terms of Service and Privacy Policy.",
            text = stringProvider.tosAndPrivacyPolicy(
                termsOfServiceLabel = stringProvider.termsOfService,
                privacyPolicyLabel = stringProvider.privacyPolicy
            ),
            links = arrayOf(
                stringProvider.termsOfService to (termsOfServiceUrl ?: ""),
                stringProvider.privacyPolicy to (privacyPolicyUrl ?: "")
            )
        )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                AuthMethodPicker(
                    providers = listOf(
                        AuthProvider.Email(
                            emailLinkActionCodeSettings = null,
                            passwordValidationRules = emptyList()
                        ),
                        AuthProvider.Google(
                            scopes = emptyList(),
                            serverClientId = null
                        )
                    ),
                    onProviderSelected = { _, _ -> },
                    termsOfServiceUrl = "https://example.com/terms",
                    privacyPolicyUrl = "https://example.com/privacy",
                    lastSignInPreference = SignInPreference(
                        providerId = Provider.EMAIL.id,
                        identifier = "someone@domain.example",
                        timestamp = 0
                    )
                )
            }
        }
    }
}