// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.configuration.theme

import androidx.compose.ui.graphics.Color
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.Provider
import com.battlelancer.seriesguide.backend.auth.configuration.theme.ProviderStyleDefaults.Google

/**
 * Default provider styling configurations for authentication providers.
 *
 * The styles are automatically applied when using [AuthUITheme.Default] or can be
 * customized by passing a modified map to [AuthUITheme.fromMaterialTheme].
 *
 * Individual provider styles can be accessed and customized using the public properties
 * (e.g., [Google]) and then modified using the [AuthUITheme.ProviderStyle.copy] method.
 */
object ProviderStyleDefaults {
    val Google = AuthUITheme.ProviderStyle(
        icon = R.drawable.ic_account_circle_on_surface_light_24dp,
        backgroundColor = Color.White,
        contentColor = Color(0xFF49454E /* light colorOnSurfaceVariant */ )
    )

    val Email = AuthUITheme.ProviderStyle(
        icon = R.drawable.ic_email_white_24dp,
        backgroundColor = Color(0xFFD0021B),
        contentColor = Color.White
    )

    val default: Map<String, AuthUITheme.ProviderStyle>
        get() = mapOf(
            Provider.GOOGLE.id to Google,
            Provider.EMAIL.id to Email,
        )
}