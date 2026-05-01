// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.firebase.ui.auth

import android.util.Log
import androidx.annotation.Keep
import com.google.firebase.components.Component
import com.google.firebase.components.ComponentRegistrar
import com.google.firebase.platforminfo.LibraryVersionComponent

/**
 * Registers the FirebaseUI-Android Compose library with Firebase Analytics.
 * This enables Firebase to track which versions of FirebaseUI are being used.
 */
@Keep
class FirebaseUIComposeRegistrar : ComponentRegistrar {
    override fun getComponents(): List<Component<*>> {
        Log.d("FirebaseUIRegistrar", "FirebaseUI Compose Registrar initialized: " +
                "LIBRARY_NAME: ${BuildConfig.LIBRARY_NAME}, " +
                "VERSION_NAME: ${BuildConfig.VERSION_NAME}")
        return listOf(
            LibraryVersionComponent.create(BuildConfig.LIBRARY_NAME, BuildConfig.VERSION_NAME)
        )
    }
}
