/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.backend.auth.configuration.theme

import androidx.compose.ui.graphics.Color
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.Provider

/**
 * Default provider styling configurations for authentication providers.
 *
 * This object provides brand-appropriate visual styling for each supported authentication
 * provider, including background colors, text colors, and other visual properties that
 * match each provider's brand guidelines.
 *
 * The styles are automatically applied when using [AuthUITheme.Default] or can be
 * customized by passing a modified map to [AuthUITheme.fromMaterialTheme].
 *
 * Individual provider styles can be accessed and customized using the public properties
 * (e.g., [Google]) and then modified using the [AuthUITheme.ProviderStyle.copy] method.
 */
object ProviderStyleDefaults {
    val Google = AuthUITheme.ProviderStyle(
        icon = AuthUIAsset.Resource(R.drawable.fui_ic_googleg_color_24dp),
        backgroundColor = Color.White,
        contentColor = Color(0xFF757575)
    )

    val Email = AuthUITheme.ProviderStyle(
        icon = AuthUIAsset.Resource(R.drawable.fui_ic_mail_white_24dp),
        backgroundColor = Color(0xFFD0021B),
        contentColor = Color.White
    )

    val default: Map<String, AuthUITheme.ProviderStyle>
        get() = mapOf(
            Provider.GOOGLE.id to Google,
            Provider.EMAIL.id to Email,
        )
}