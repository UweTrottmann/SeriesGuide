// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.configuration.theme

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource

/**
 * Represents a visual asset used in the authentication UI.
 *
 * This sealed class allows specifying icons and images from either Android drawable
 * resources ([Resource]) or Jetpack Compose [ImageVector]s ([Vector]). The [painter]
 * property provides a unified way to get a [Painter] for the asset within a composable.
 *
 * **Example usage:**
 * ```kotlin
 * // To use a drawable resource:
 * val asset = AuthUIAsset.Resource(R.drawable.my_logo)
 *
 * // To use a vector asset:
 * val vectorAsset = AuthUIAsset.Vector(Icons.Default.Info)
 * ```
 */
sealed class AuthUIAsset {
    /**
     * An asset loaded from a drawable resource.
     *
     * @param resId The resource ID of the drawable (e.g., `R.drawable.my_icon`).
     */
    class Resource(@param:DrawableRes val resId: Int) : AuthUIAsset()

    /**
     * An asset represented by an [ImageVector].
     *
     * @param image The [ImageVector] to be displayed.
     */
    class Vector(val image: ImageVector) : AuthUIAsset()

    /**
     * A [Painter] that can be used to draw this asset in a composable.
     *
     * This property automatically resolves the asset type and returns the appropriate
     * [Painter] for rendering.
     */
    @get:Composable
    internal val painter: Painter
        get() = when (this) {
            is Resource -> painterResource(resId)
            is Vector -> rememberVectorPainter(image)
        }
}