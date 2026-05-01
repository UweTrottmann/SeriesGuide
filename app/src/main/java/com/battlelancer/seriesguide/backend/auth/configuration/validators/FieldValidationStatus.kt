// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.firebase.ui.auth.configuration.validators

/**
 * Class for encapsulating [hasError] and [errorMessage] properties in
 * internal FieldValidator subclasses.
 */
internal class FieldValidationStatus(
    val hasError: Boolean,
    val errorMessage: String? = null,
)
